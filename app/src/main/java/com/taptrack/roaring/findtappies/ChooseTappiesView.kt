package com.taptrack.roaring.findtappies

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.core.widget.NestedScrollView
import com.taptrack.roaring.R
import com.taptrack.roaring.utils.getHostActivity
import com.taptrack.roaring.utils.setTextAppearanceCompat
import com.taptrack.tcmptappy2.ble.TappyBleDeviceDefinition
import io.reactivex.disposables.Disposable
import org.jetbrains.anko.*
import org.jetbrains.anko.custom.ankoView

inline fun ViewManager.chooseTappiesView() = chooseTappiesView({})

inline fun ViewManager.chooseTappiesView(init: ChooseTappiesView.() -> Unit): ChooseTappiesView {
    return ankoView({ ChooseTappiesView(it) }, theme = 0, init = init)
}

class ChooseTappiesView : NestedScrollView {
    public var vm: ChooseTappiesViewModel? = null

    private lateinit var tappyControlView: TappyControlView
    private lateinit var tappySearchView: TappySearchView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var activeHeadingView: TextView
    private lateinit var searchHeadingView: TextView

    private var state: ChooseTappiesViewState = ChooseTappiesViewState.Companion.initialState()
    private var disposable: Disposable? = null

    constructor(context: Context) :
            super(context) {
        init(context)
    }
    constructor(context: Context, attrs: AttributeSet?) :
            super(context, attrs) {
        init(context)
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {

        val llLayoutParams = ViewGroup.MarginLayoutParams(matchParent, matchParent)
        linearLayout {
            orientation = LinearLayout.VERTICAL

            activeHeadingView = themedTextView {
                text = context.getString(R.string.active_devices_heading,0)
                layoutParams = LayoutParams(matchParent, wrapContent)
            }
            activeHeadingView.setTextAppearanceCompat(R.style.TextAppearance_AppCompat_Medium)

            tappyControlView = tappyControlView {

            }.lparams(matchParent, wrapContent)

            searchHeadingView = themedTextView() {
                textResource = R.string.select_tappy_text
            }.lparams(matchParent, wrapContent)

            searchHeadingView.setTextAppearanceCompat(R.style.TextAppearance_AppCompat_Medium)

            tappySearchView = tappySearchView {

            }.lparams(matchParent, wrapContent)

            loadingIndicator = progressBar {
                isIndeterminate = true
            }.lparams(width = wrapContent, height = wrapContent) {
                topMargin = dip(16)
                gravity = Gravity.CENTER_HORIZONTAL
            }

            padding = dip(16)
            layoutParams = llLayoutParams
        }

        tappyControlView.setTappyControlListener(object: TappyControlListener {
            override fun requestRemove(namedTappy: NamedTappy) {
                vm?.removeActiveTappy(namedTappy)
            }

        })

        tappySearchView.setTappySelectionListener(object: TappySelectionListener {
            override fun tappyUsbSelected(device: UsbDevice) {
                vm?.addActiveTappyUsb(device)
            }

            override fun tappyBleSelected(definition: TappyBleDeviceDefinition) {
                vm?.addActiveTappyBle(definition)
            }
        })

        reset()
    }

    private fun reset() {
        tappySearchView.currentTappies = state
                .foundUsbDevices.map { ChoosableTappyUsb(it.deviceId.toString(),"USB Device",it.deviceName,it) }
                .plus(state.foundBleDevices.map { ChoosableTappyBle(it.address,it.name,it.address,it) })
        tappyControlView.setViewState(TappyControlViewState(state.activeDevices))
        activeHeadingView.text = context.getString(R.string.active_devices_heading,state.activeDevices.size)
    }

    @UiThread
    public fun setState(state: ChooseTappiesViewState) {
        this.state = state
        reset()
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        vm = (getHostActivity() as? ChooseTappiesViewModelProvider)?.provideChooseTappiesViewModel()
        disposable = vm?.getFindTappiesState()?.subscribe {
            post { setState(it) }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposable?.dispose()
    }
}