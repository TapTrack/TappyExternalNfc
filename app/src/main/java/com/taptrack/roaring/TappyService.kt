package com.taptrack.roaring

import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.hardware.usb.UsbDevice
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.*
import android.util.Log
import com.taptrack.experiments.rancheria.business.TappyNotificationManager
import com.taptrack.tcmptappy.tcmp.MalformedPayloadException
import com.taptrack.tcmptappy2.*
import com.taptrack.tcmptappy2.ble.TappyBle
import com.taptrack.tcmptappy2.ble.TappyBleDeviceDefinition
import com.taptrack.tcmptappy2.commandfamilies.basicnfc.BasicNfcCommandResolver
import com.taptrack.tcmptappy2.commandfamilies.basicnfc.commands.DispatchTagsCommand
import com.taptrack.tcmptappy2.commandfamilies.basicnfc.responses.NdefFoundResponse
import com.taptrack.tcmptappy2.commandfamilies.basicnfc.responses.TagFoundResponse
import com.taptrack.tcmptappy2.commandfamilies.systemfamily.SystemCommandResolver
import com.taptrack.tcmptappy2.usb.TappyUsb
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock

interface TappyConnectionsListener {
    fun tappyConnectionsChanged(newCollection: Collection<Tappy>)
}

private sealed class TappyTrio {
    data class TappyBleTrio(
            val tappy: TappyBle,
            val statusListener: Tappy.StatusListener,
            val responseListener: Tappy.ResponseListener) : TappyTrio()
    data class TappyUsbTrio(val tappy: TappyUsb,
                            val statusListener: Tappy.StatusListener,
                            val responseListener: Tappy.ResponseListener) : TappyTrio()
}

class TappyService: Service() {

    private val connectionListenerSet = HashSet<TappyConnectionsListener>()

    private var allConnections = mutableMapOf<String,Tappy>()

    private var nonMutableConnections = emptySet<Tappy>()

    private val usbTappies = mutableMapOf<Int, TappyTrio.TappyUsbTrio>()
    private val bleTappies = mutableMapOf<String, TappyTrio.TappyBleTrio>()

    private val connectionsRwLock = ReentrantReadWriteLock()

    private var mtHandler = Handler(Looper.getMainLooper())

    private var autolaunchDisposable: Disposable? = null
    private var shouldAutolaunch: Boolean = false
    private var lastLaunched = 0.toLong()

    private var heartBeatDisposable: Disposable? = null

    private var wakeLock: PowerManager.WakeLock? = null

    private val broadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(ACTION_DISCONNECT_ALL_TAPPIES == intent?.action) {
                connectionsRwLock.writeLock().lock()
                val connections = allConnections.values
                var tappyList = emptyList<Tappy>()
                for (tappy in connections) {
                    val localTappy = tappy
                    tappyList = tappyList.plus(localTappy)
                }
                connectionsRwLock.writeLock().unlock()
                for (tappy in tappyList) {
                    disconnectTappy(tappy)
                }
            }
        }
    }

    inner class TappyServiceBinder : Binder() {
        fun registerConnectionsChangedListener(listener: TappyConnectionsListener, sendCurrent: Boolean) {
            connectionListenerSet.add(listener)
            if(sendCurrent) {
                listener.tappyConnectionsChanged(getCurrentImmutableConnections())
            }
        }

        fun unregisterConnectionsChangedListener(listener: TappyConnectionsListener) {
            connectionListenerSet.add(listener)
        }

        fun requestConnectToTappyBle(tappyBleDeviceDefinition: TappyBleDeviceDefinition) {
            connectTappyBle(tappyBleDeviceDefinition)
        }

        fun requestConnectToTappyUsb(usbDevice: UsbDevice) {
            connectTappyUsb(usbDevice)
        }

        fun requestDisconnectTappy(tappy: Tappy) {
            disconnectTappy(tappy)
        }
    }

    val binder = TappyServiceBinder()

    private fun getCurrentImmutableConnections(): Collection<Tappy> {
        return nonMutableConnections
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private fun disconnectTappy(tappy: Tappy) {
        when (tappy) {
            is TappyBle -> removeTappyBle(tappy, false)
            is TappyUsb -> removeTappyUsb(tappy)
            else -> Timber.d("Requested disconnect of unsupported Tappy type")
        }
    }

    private fun sendTcmp(message: TCMPMessage) {
        try {
            connectionsRwLock.writeLock().lock()
                    //TODO: change back to writeLock()
            allConnections.values
                    .filter { it.latestStatus == Tappy.STATUS_READY }
                    .forEach { it.sendMessage(message) }
            connectionsRwLock.writeLock().unlock()
        } catch (ignored: TCMPMessageParseException) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        wakeLock = (getSystemService(Context.POWER_SERVICE) as? PowerManager)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
        wakeLock?.setReferenceCounted(false)

        autolaunchDisposable = getRoaringApplication().getAutolaunchEnabled()
                .subscribe {
                    shouldAutolaunch = it
                }

        val filter = IntentFilter(ACTION_DISCONNECT_ALL_TAPPIES)
        registerReceiver(broadcastReceiver,filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        autolaunchDisposable?.dispose()
        heartBeatDisposable?.dispose()

        unregisterReceiver(broadcastReceiver)
        wakeLock?.release()
    }

    private fun connectTappyBle(definition: TappyBleDeviceDefinition) {

        connectionsRwLock.writeLock().lock()
        if(bleTappies.containsKey(definition.address)){
            connectionsRwLock.writeLock().unlock()
            return
        }

        val tappy = TappyBle.getTappyBle(this,definition)
        val statusListener = object : Tappy.StatusListener {
            override fun statusReceived(status: Int) {
                when (status) {
                    Tappy.STATUS_READY -> {
                        signalTappiesToDispatch()
                    }
                    Tappy.STATUS_CONNECTING, Tappy.STATUS_DISCONNECTING -> {
                    }
                    Tappy.STATUS_DISCONNECTED, Tappy.STATUS_CLOSED, Tappy.STATUS_ERROR -> {
                        removeTappyBle(tappyBle = tappy, attemptReconnect = true)
                    }
                }
                notifyListenersOfChange(true)
            }
        }
        val responseListener = Tappy.ResponseListener {
            val response = it
            Log.i(TAG,"Received message from TappyBLE")
            mtHandler.post {
                handleMessage(it)
            }
        }

        tappy.registerStatusListener(statusListener)
        tappy.registerResponseListener(responseListener)

        val trio = TappyTrio.TappyBleTrio(tappy, statusListener, responseListener)

        bleTappies.put(definition.address,trio)
        allConnections.put(tappy.deviceDescription,tappy)
        tappy.connect()

        connectionsRwLock.writeLock().unlock()

        notifyListenersOfChange()
        updateForegroundState()
    }

    private fun updateForegroundState() {
        var activeDeviceCount = 0

        val lock = connectionsRwLock.readLock()
        lock.lock()

        if (allConnections.isNotEmpty()) {
            wakeLock?.acquire()

            val notificationTitle: String
            val notificationContent: String
            if (allConnections.size == 1) {
                notificationTitle = getString(R.string.active_tappies_notification_title)
                notificationContent = getString(R.string.one_tappy_active_notification_content)
            } else {
                notificationTitle = getString(R.string.active_tappies_notification_title)
                notificationContent = getString(R.string.multiple_tappies_active_notification_content, allConnections.size)
            }

            val disconnectTappiesIntent = Intent(ACTION_DISCONNECT_ALL_TAPPIES)
            val disconnectTappiesPendingIntent = PendingIntent.getBroadcast(this, 0, disconnectTappiesIntent, 0)

            val openActivityIntent = Intent(this, MainActivity::class.java)
            val openActivityPendingIntent = PendingIntent.getActivity(this,0,openActivityIntent,0)

            val notification = TappyNotificationManager.createNotificationBuilder(this)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationContent)
                    .setSmallIcon(R.drawable.ic_tappy_connected_notification)
                    .setTicker(notificationContent)
                    .setContentIntent(openActivityPendingIntent)
                    .addAction(R.drawable.ic_remove_all, getString(R.string.remove_all_tappies), disconnectTappiesPendingIntent)
                    .build()

            startForeground(NOTIFICATION_ID, notification)

            // this makes the service actually started so it isn't killed
            val kickmyselfIntent = Intent(this, TappyService::class.java)
            startService(kickmyselfIntent)
        } else {
            stopForeground(true)
            stopSelf()
            wakeLock?.release()
        }

        lock.unlock()
    }

    private fun handleMessage(message: TCMPMessage) {
        try {
            val response = messageResolver.resolveResponse(message)
            when (response) {
                is NdefFoundResponse -> {
                    if(shouldAutolaunch) {
                        throttleAndLaunch(response.message)
                    }
                    broadcastNdefFound(response.tagCode,response.tagType,response.message)
                }
                is TagFoundResponse -> {
                    broadcastTagFound(response.tagCode,response.tagType)
                }
            }
        } catch (e: MalformedPayloadException) {
            Timber.e(e)
        }

    }

    private fun throttleAndLaunch(message: NdefMessage) {
        val received = SystemClock.uptimeMillis()
        val records = message.records
        if (received - lastLaunched > THROTTLE_URL_MIN_TIME && records.isNotEmpty()) {
            val firstRecord = records[0]
            if (firstRecord.tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(firstRecord.type, NdefRecord.RTD_URI)) {
                val uriPayload = firstRecord.payload
                if (uriPayload.size > 1) {
                    val prefixByte = uriPayload[0]
                    var url: String? = null
                    when (prefixByte) {
                        0x01.toByte() -> url = "http://www." + String(Arrays.copyOfRange(uriPayload, 1, uriPayload.size))
                        0x02.toByte() -> url = "https://www." + String(Arrays.copyOfRange(uriPayload, 1, uriPayload.size))
                        0x03.toByte() -> url = "http://" + String(Arrays.copyOfRange(uriPayload, 1, uriPayload.size))
                        0x04.toByte() -> url = "https://" + String(Arrays.copyOfRange(uriPayload, 1, uriPayload.size))
                    }

                    if (url != null) {
                        val launchUrlIntent = Intent(Intent.ACTION_VIEW)
                        launchUrlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        launchUrlIntent.data = Uri.parse(url)
                        if (launchUrlIntent.resolveActivity(packageManager) != null) {
                            Timber.v("Attempting to launch view Intent for url %s",url)
                            // this appears to be necessary on Oreo
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val chooserIntent = Intent.createChooser(launchUrlIntent, getString(R.string.open_url_with))
                                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                if (chooserIntent.resolveActivity(packageManager) != null) {
                                    Timber.v("Launching chooser")
                                } else {
                                    Timber.v("Nothing can handle chooser Intent")
                                }
                            } else {
                                startActivity(launchUrlIntent)
                            }
                        } else {
                            Timber.v("Nothing can handle view Intent")
                        }


                        lastLaunched = received
                    }
                }
            }
        }
    }
    private fun notifyListenersOfChange(onlyStatus: Boolean = false) {
        if(!onlyStatus) {
            connectionsRwLock.readLock().lock()

            nonMutableConnections = allConnections.values.toSet()

            connectionsRwLock.readLock().unlock()
        }

        val results = getCurrentImmutableConnections()

        for(listener in connectionListenerSet) {
            listener.tappyConnectionsChanged(results)
        }
    }

    private fun signalTappiesToDispatch() {
        mtHandler.postDelayed( {
            sendTcmp(DispatchTagsCommand())
        }, 50);

    }

    private fun removeTappyBle(tappyBle:TappyBle, attemptReconnect: Boolean) {
        connectionsRwLock.writeLock().lock()

        val trio = bleTappies[tappyBle.backingDeviceDefinition.address]
        if(trio != null) {
            val (tappy, statusListener, messageListener) = trio
            tappy.unregisterStatusListener(statusListener)
            tappy.unregisterResponseListener(messageListener)
            tappy.close()
            allConnections.remove(tappy.deviceDescription)
            bleTappies.remove(tappy.backingDeviceDefinition.address)
        }

        connectionsRwLock.writeLock().unlock()

        notifyListenersOfChange()
        updateForegroundState()

        if (attemptReconnect) {
            val definition = tappyBle.backingDeviceDefinition
            mtHandler.post({
                connectTappyBle(definition)
            })
        }
    }

    private fun connectTappyUsb(device: UsbDevice) {
        Log.i(TAG,"Attempting to connect to USB device")

        connectionsRwLock.writeLock().lock()

        if(usbTappies.containsKey(device.deviceId)){
            connectionsRwLock.writeLock().unlock()
            return
        }

        val tappy = TappyUsb.getTappyUsb(this,device)
        if(tappy == null) {
            Log.i(TAG,"Tappy was null")
            return
        }

        val statusListener = object : Tappy.StatusListener {
            override fun statusReceived(status: Int) {
                when (status) {
                    Tappy.STATUS_READY -> {
                        signalTappiesToDispatch()
                    }
                    Tappy.STATUS_CONNECTING, Tappy.STATUS_DISCONNECTING -> {
                    }
                    Tappy.STATUS_DISCONNECTED, Tappy.STATUS_CLOSED, Tappy.STATUS_ERROR -> {
                        removeTappyUsb(tappy)
                    }
                }
                notifyListenersOfChange()
            }
        }

        val responseListener = Tappy.ResponseListener {
            val response = it
            Log.i(TAG,"Received message from TappyUSB")
            mtHandler.post {
                handleMessage(it)
            }
        }

        tappy.registerStatusListener(statusListener)
        tappy.registerResponseListener(responseListener)

        val trio = TappyTrio.TappyUsbTrio(tappy, statusListener, responseListener)

        usbTappies.put(device.deviceId,trio)
        allConnections.put(tappy.deviceDescription,tappy)
        notifyListenersOfChange()
        tappy.connect()

        connectionsRwLock.writeLock().unlock()
        updateForegroundState()
    }

    private fun removeTappyUsb(tappyUsb: TappyUsb) {
        connectionsRwLock.writeLock().lock()
        val trio = usbTappies[tappyUsb.backingUsbDevice.deviceId]
        if(trio != null) {
            val (tappy, statusListener, messageListener) = trio
            tappy.unregisterStatusListener(statusListener)
            tappy.unregisterResponseListener(messageListener)
            tappy.close()
            allConnections.remove(tappy.deviceDescription)
            usbTappies.remove(tappy.backingUsbDevice.deviceId)
        }

        connectionsRwLock.writeLock().unlock()

        notifyListenersOfChange()
        updateForegroundState()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun broadcastTagFound(uid: ByteArray, tagType: Byte) {
        val intent = Intent(RoaringApplication.ACTION_TAG_FOUND)
        intent.putExtra(NfcAdapter.EXTRA_ID, uid)
        intent.putExtra(RoaringApplication.EXTRA_TAG_TYPE_INT, tagType)

        broadcastCompat(intent)
    }

    fun broadcastNdefFound(uid: ByteArray, tagType: Byte, message: NdefMessage) {

        val intent = Intent(RoaringApplication.ACTION_NDEF_FOUND)
        intent.putExtra(NfcAdapter.EXTRA_ID, uid)
        intent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, arrayOf(message))
        intent.putExtra(RoaringApplication.EXTRA_TAG_TYPE_INT, tagType)

        broadcastCompat(intent)
    }

    protected fun broadcastCompat(intent: Intent) {
        val pm = packageManager
        val matches = pm.queryBroadcastReceivers(intent, 0)

        for (resolveInfo in matches) {
            val explicit = Intent(intent)
            val cn = ComponentName(resolveInfo.activityInfo.applicationInfo.packageName,
                    resolveInfo.activityInfo.name)

            explicit.component = cn
            sendBroadcast(explicit)
        }
    }
    companion object {
        private val TAG = TappyService::class.java.name

        private val THROTTLE_URL_MIN_TIME: Long = 500

        private val NOTIFICATION_ID = 3415
        private val ACTION_DISCONNECT_ALL_TAPPIES = TappyService::class.java.name+".ACTION_DISCONNECT_ALL_TAPPIES"

        private val WAKELOCK_TAG = TappyService::class.java.name

        private val messageResolver: MessageResolver

        init {
            messageResolver = MessageResolverMux(
                    SystemCommandResolver(),
                    BasicNfcCommandResolver()
            )
        }
    }
}