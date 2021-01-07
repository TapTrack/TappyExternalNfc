package com.taptrack.roaring

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxrelay2.BehaviorRelay
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import com.taptrack.roaring.findtappies.*
import com.taptrack.roaring.utils.getColorResTintedDrawable
import com.taptrack.tcmptappy2.Tappy
import com.taptrack.tcmptappy2.ble.TappyBle
import com.taptrack.tcmptappy2.ble.TappyBleDeviceDefinition
import com.taptrack.tcmptappy2.usb.TappyUsb
import com.taptrack.tcmptappy2.usb.UsbPermissionDelegate
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.jetbrains.anko.contentView
import timber.log.Timber


class MainActivity : androidx.appcompat.app.AppCompatActivity(), ChooseTappiesViewModelProvider {
    private val TAG = MainActivity::class.java.name

    private lateinit var permissionDelegate: UsbPermissionDelegate
    private lateinit var searchManager: SearchManagementDelegate

    private val handler = android.os.Handler(Looper.getMainLooper())

    private var preferencesDisposable: Disposable? = null

    private var isAutolaunchingEnabled: Boolean = false

    private val recreateRunnable = Runnable {
        recreate()
    }

    private var serviceBinder: TappyService.TappyServiceBinder? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBinder = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceBinder = service as? TappyService.TappyServiceBinder
            registerWithService()
        }

    }

    private var usbPermissionListener: UsbPermissionDelegate.PermissionListener = object : UsbPermissionDelegate.PermissionListener {
        override fun permissionDenied(device: UsbDevice) {
            Timber.i("Permission denied")
        }

        override fun permissionGranted(device: UsbDevice) {
            Timber.i(TAG, "Permission granted")
            connectToUsbDevice(device)
        }
    }

    private val chooseTappiesViewModel = object: ChooseTappiesViewModel {
        private var state: ChooseTappiesViewState = ChooseTappiesViewState.initialState()
        private val stateMutationLock: Any = Object()
        private var stateRelay: BehaviorRelay<ChooseTappiesViewState> = BehaviorRelay.createDefault(state)

        fun setSearchResults(bleDevices: Collection<TappyBleDeviceDefinition>, usbDevices: Collection<UsbDevice>) {
            var newState: ChooseTappiesViewState?
            synchronized(stateMutationLock) {
                newState = state.copy(foundBleDevices = bleDevices, foundUsbDevices = usbDevices)
                state = newState ?: state
            }
            if(newState != null) {
                stateRelay.accept(newState as ChooseTappiesViewState)
            }
        }

        fun setActiveTappies(tappies: Collection<Tappy>) {
            val named = tappies.map {
                when(it) {
                    is TappyBle -> NamedTappy(tappy = it, name = it.backingDeviceDefinition.name)
                    is TappyUsb -> NamedTappy(tappy = it, name = getString(R.string.tappy_usb_name))
                    else -> NamedTappy(tappy = it, name = getString(R.string.unknown_tappy_name))
                }
            }
            var newState: ChooseTappiesViewState?
            synchronized(stateMutationLock) {
                newState = state.copy(activeDevices = named)
                state = newState ?: state
            }
            if(newState != null) {
                stateRelay.accept(newState as ChooseTappiesViewState)
            }
        }

        override fun addActiveTappyBle(tappyBleDeviceDefinition: TappyBleDeviceDefinition) {
            connectToBleDevice(device = tappyBleDeviceDefinition)
        }

        override fun addActiveTappyUsb(usbTappy: UsbDevice) {
            permissionDelegate.requestPermission(usbTappy)
        }

        override fun removeActiveTappy(tappy: NamedTappy) {
            removeTappy(tappy.tappy)
        }

        override fun getFindTappiesState(): Observable<ChooseTappiesViewState> {
            return stateRelay
        }

    }

    private val activeTappiesListener = object : TappyConnectionsListener {
        override fun tappyConnectionsChanged(newCollection: Collection<Tappy>) {
            chooseTappiesViewModel.setActiveTappies(newCollection)
        }

    }

    private val coarseLocationListener = object: PermissionListener {
        override fun onPermissionGranted(response: PermissionGrantedResponse?) {
            searchManager.coarseLocationRequestResult(true)
        }

        override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
        }

        override fun onPermissionDenied(response: PermissionDeniedResponse?) {
            searchManager.coarseLocationRequestResult(false)
        }
    }

    private var openUrlsButton: ImageView? = null

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_main)

        val roaringApp = getRoaringApplication()

        openUrlsButton = findViewById(R.id.ib_launch_urls)
        openUrlsButton?.setOnClickListener({
            val localButtonCopy = openUrlsButton
            val shouldBeEnabled = !isAutolaunchingEnabled
            roaringApp.setAutolaunchEnabled(shouldBeEnabled)
            if (localButtonCopy != null) {
                if (shouldBeEnabled) {
                    Snackbar.make(localButtonCopy,R.string.automatic_url_launching_enabled, Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(localButtonCopy,R.string.automatic_url_launching_disabled, Snackbar.LENGTH_SHORT).show()
                }
            }
        })

        permissionDelegate = UsbPermissionDelegate(this, usbPermissionListener)
        searchManager = SearchManagementDelegate(this,object : SearchResultsListener {
            override fun searchResultsUpdated(
                    bleDevices: Collection<TappyBleDeviceDefinition>, usbDevices: Collection<UsbDevice>) {
                chooseTappiesViewModel.setSearchResults(bleDevices,usbDevices)
            }
        })

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val snackbarPermissionListener = SnackbarOnDeniedPermissionListener.Builder
                    .with(contentView as ViewGroup, R.string.coarse_location_needed_rationale)
                    .withOpenSettingsButton(R.string.settings)
                    .build()
            Dexter.withActivity(this)
                    .withPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    .withListener(CompositePermissionListener(snackbarPermissionListener, coarseLocationListener))
                    .check()
        } else {
            searchManager.coarseLocationRequestResult(true)
        }

        addUsbDeviceFromIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        addUsbDeviceFromIntent(intent)
    }

    private fun addUsbDeviceFromIntent(intent: Intent?) {
        if (intent != null && intent.hasExtra(UsbManager.EXTRA_DEVICE)) {
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)!!
            connectToUsbDevice(device)
        }
    }

    override fun provideChooseTappiesViewModel(): ChooseTappiesViewModel {
        return chooseTappiesViewModel
    }

    private fun connectToUsbDevice(device: UsbDevice) {
        handler.postDelayed({
            serviceBinder?.requestConnectToTappyUsb(device)
        }, 32)
    }

    private fun connectToBleDevice(device: TappyBleDeviceDefinition) {
        serviceBinder?.requestConnectToTappyBle(device)
    }

    private fun removeTappy(tappy: Tappy) {
        serviceBinder?.requestDisconnectTappy(tappy)
    }

    private fun registerWithService() {
        serviceBinder?.registerConnectionsChangedListener(activeTappiesListener,true)
    }

    private fun unregisterFromService() {
        serviceBinder?.unregisterConnectionsChangedListener(activeTappiesListener)
    }

    private fun postRecreate(delay: Long) {
        cancelPendingRecreate()
        handler.postDelayed(recreateRunnable,delay)
    }

    private fun cancelPendingRecreate(){
        handler.removeCallbacks(recreateRunnable)
    }

    override fun onStart() {
        super.onStart()

        val app = getRoaringApplication()
        preferencesDisposable = app.getAutolaunchEnabled()
                .subscribe {
                    isAutolaunchingEnabled = it
                    handler.post {
                        resetOpenUrlsButton()
                    }
                }

        permissionDelegate.register()
        bindService(Intent(this, TappyService::class.java),serviceConnection, Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT)

    }

    private fun resetOpenUrlsButton() {
        if (isAutolaunchingEnabled) {
            openUrlsButton?.setImageDrawable(getColorResTintedDrawable(R.drawable.ic_open_links_in_browser_black_24dp,R.color.autolaunchIconColor))
        } else {
            openUrlsButton?.setImageDrawable(getColorResTintedDrawable(R.drawable.ic_dont_open_links_in_browser_black_24dp,R.color.autolaunchIconColor))
        }
    }

    override fun onResume() {
        super.onResume()
        searchManager.resume()
        searchManager.requestActivate()
    }

    override fun onPause() {
        super.onPause()
        searchManager.requestDeactivate()
        searchManager.pause()
    }

    override fun onStop() {
        super.onStop()
        preferencesDisposable?.dispose()
        cancelPendingRecreate()

        unregisterFromService()
        unbindService(serviceConnection)
        permissionDelegate.unregister()
    }


    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}
