package com.taptrack.roaring.findtappies

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.SystemClock
import com.taptrack.tcmptappy2.ble.TappyBleDeviceDefinition
import com.taptrack.tcmptappy2.ble.TappyBleFoundListener
import com.taptrack.tcmptappy2.ble.TappyBleScanner
import com.taptrack.tcmptappy2.usb.TappyUsb
import timber.log.Timber


private data class SearchManagementState(val isResumed: Boolean, val scanningDesired: Boolean, val hasFineLocation: Boolean) {
    val shouldBeScanningBluetooth: Boolean
    get() = isResumed && scanningDesired && hasFineLocation

    val shouldBeScanningUsb: Boolean
    get() = isResumed && scanningDesired
}

interface SearchResultsListener {
    fun searchResultsUpdated(bleDevices: Collection<TappyBleDeviceDefinition>, usbDevices: Collection<UsbDevice>)
}

class SearchManagementDelegate constructor(val ctx: Context, val resultsListener: SearchResultsListener) {
    private var state = SearchManagementState(false,false,false)

    private val adapter: BluetoothAdapter?
    private var bleScanner: TappyBleScanner?
    private var isAdapterOn: Boolean = false
    private var scanSupposedlyActive: Boolean = false

    private var usbDevices: Collection<UsbDevice> = emptySet()


    private var bleDeviceMap: HashMap<String,Pair<TappyBleDeviceDefinition,Long>> = hashMapOf()

    private val bleDeviceColl: Collection<TappyBleDeviceDefinition>
            get() {
                return bleDeviceMap.entries.map { it.value.first }
            }


    private var lifecycleSensitiveItemsAttached = false
    val receiverFilter: IntentFilter
    var lastStarted: Long = 0

    private val handler = Handler(ctx.mainLooper)

    private val resetRunnable = Runnable {
        reset()
        startResetRunnable(false)
    }
    private val trimBleRunnable = Runnable {
        trimOldBle()
        startTrimRunnable(false)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> isAdapterOn = false
//                    BluetoothAdapter.STATE_TURNING_OFF ->
                    BluetoothAdapter.STATE_ON -> isAdapterOn = true
//                    BluetoothAdapter.STATE_TURNING_ON -> setButtonText("Turning Bluetooth on...")
                }
                reset()
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                searchUsb()
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                searchUsb()
            }
        }
    }
    private val scanCallback = object : TappyBleFoundListener {
        override fun onTappyBleFound(tappyBle: TappyBleDeviceDefinition?) {
            foundBle(tappyBle!!)
        }
    }

    init {
        try {
            bleScanner = TappyBleScanner.get()
        } catch (e: IllegalStateException) {
            bleScanner = null
        }

        adapter = BluetoothAdapter.getDefaultAdapter()

        receiverFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        receiverFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        receiverFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        isAdapterOn = adapter?.state == BluetoothAdapter.STATE_ON
    }

    private fun searchUsb() {
        usbDevices = TappyUsb.getPotentialTappies(ctx) ?: emptySet<UsbDevice>()
        notifyResultsListener()
    }

    @Synchronized
    private fun foundBle(device: TappyBleDeviceDefinition) {
        if (bleDeviceMap.containsKey(device.address)) {
            bleDeviceMap[device.address] = Pair(device,SystemClock.uptimeMillis())
        } else {
            bleDeviceMap[device.address] = Pair(device,SystemClock.uptimeMillis())
            notifyResultsListener()
        }
    }

    private fun notifyResultsListener() {
        resultsListener.searchResultsUpdated(bleDevices = bleDeviceColl, usbDevices = usbDevices)
    }

    @Synchronized
    fun requestActivate() {
        state = state.copy(scanningDesired = true)
        reset()
    }

    fun clearBluetoothDevices() {
        bleDeviceMap = hashMapOf()
        notifyResultsListener()
    }

    @Synchronized
    fun requestDeactivate() {
        state = state.copy(scanningDesired = false)
        reset()
    }

    @Synchronized
    fun fineLocationRequestResult(result: Boolean) {
        state = state.copy(hasFineLocation = result)
        reset()
    }

    @Synchronized
    fun resume() {
        state = state.copy(isResumed = true)
        reset()
        searchUsb()
    }

    @Synchronized
    fun pause() {
        state = state.copy(isResumed = false)
        reset()
    }

    @Synchronized
    fun trimOldBle() {
        val uptime = SystemClock.uptimeMillis()
        val toRemove = bleDeviceMap.entries
                .filter { (uptime - it.value.second) > MAX_TIME_KEEP_IN_LIST_MS }
                .map { it.key }
        if (toRemove.isNotEmpty()) {
            toRemove.forEach {
                bleDeviceMap.remove(it)
            }
            notifyResultsListener()
        }
    }

    private fun startResetRunnable(shortDelay: Boolean) {
        stopResetRunnable()
        handler.postDelayed(resetRunnable, if (shortDelay) AUTO_RESET_SHORT_INTERVAL else AUTO_RESET_LONG_INTERVAL)
    }

    private fun stopResetRunnable() {
        handler.removeCallbacks(resetRunnable)
    }

    private fun startTrimRunnable(runImmediately: Boolean) {
        stopTrimRunnable()
        handler.postDelayed(trimBleRunnable, if(runImmediately) 0 else TRIM_OLD_BLE_INTERVAL_MS)
    }

    private fun stopTrimRunnable() {
        handler.removeCallbacks(trimBleRunnable)
    }

    private fun needsRestart() : Boolean {
        return (SystemClock.uptimeMillis() - lastStarted) > RESTART_SCAN_INTERVAL_MS
    }

    @Synchronized
    private fun reset() {
        var requestShortReset = false
        if(adapter != null && bleScanner != null && adapter.isEnabled) {
            if(state.shouldBeScanningBluetooth && isAdapterOn && !scanSupposedlyActive) {
                Timber.v("Starting scan")
                bleScanner?.startScan()
                lastStarted = SystemClock.uptimeMillis()
                scanSupposedlyActive = true
            } else if(!state.shouldBeScanningBluetooth){ // this used to check isAdapterOn, not sure why
                bleScanner?.stopScan()
                scanSupposedlyActive = false
            } else if (state.shouldBeScanningBluetooth && scanSupposedlyActive && needsRestart()) {
                Timber.v("Stopping scan to allow restart")
                bleScanner?.stopScan()
                scanSupposedlyActive = false
                requestShortReset = true
            }
        }

        if(state.isResumed) {
            if(!lifecycleSensitiveItemsAttached) {
                startTrimRunnable(true)
                startResetRunnable(false)

                ctx.registerReceiver(receiver,receiverFilter)
                bleScanner?.registerTappyBleFoundListener(scanCallback)
                lifecycleSensitiveItemsAttached = true
            }
            if (requestShortReset) {
                startResetRunnable(true)
            }
        } else {
            if(lifecycleSensitiveItemsAttached) {
                stopTrimRunnable()
                stopResetRunnable()

                ctx.unregisterReceiver(receiver)
                bleScanner?.unregisterTappyBleFoundListener(scanCallback)
                lifecycleSensitiveItemsAttached = false
            }
        }
    }

    companion object {
        val RESTART_SCAN_INTERVAL_MS = 10000L
        val MAX_TIME_KEEP_IN_LIST_MS = 10000L
        val AUTO_RESET_LONG_INTERVAL = 100L
        val AUTO_RESET_SHORT_INTERVAL = 50L
        val TRIM_OLD_BLE_INTERVAL_MS = 1000L
    }
}