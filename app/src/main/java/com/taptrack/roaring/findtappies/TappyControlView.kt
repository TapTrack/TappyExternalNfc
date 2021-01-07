package com.taptrack.roaring.findtappies

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.taptrack.roaring.R
import com.taptrack.roaring.utils.getColorResTintedDrawable
import com.taptrack.tcmptappy2.Tappy
import com.taptrack.tcmptappy2.usb.TappyUsb
import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.find
import org.jetbrains.anko.image
import java.util.*

data class TappyControlViewState(val tappies: Collection<NamedTappy>)

data class NamedTappy(val tappy: Tappy, val name: String)

interface TappyControlListener {
    fun requestRemove(namedTappy: NamedTappy)
}


fun ViewManager.tappyControlView() = tappyControlView { }

inline fun ViewManager.tappyControlView(init: TappyControlView.() -> Unit): TappyControlView {
    return ankoView({ TappyControlView(it) }, theme = 0, init = init)
}

class TappyControlView : androidx.recyclerview.widget.RecyclerView {
    private var state: TappyControlViewState = TappyControlViewState(emptySet())
    private lateinit var adapter: TappyControlAdapter

    private val tappySorter = Comparator<NamedTappy> {
        o1, o2 -> o1!!.name.compareTo(o2!!.name)
    }

    constructor(context: Context) : super(context) {
        initTappyControlView(context)
    }

    constructor(context: Context,
                attrs: AttributeSet?) : super(context, attrs) {
        initTappyControlView(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        initTappyControlView(context)
    }

    private fun initTappyControlView(context: Context) {
        layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        adapter = TappyControlAdapter()
        setAdapter(adapter)
        reset()
    }

    fun setViewState(newState: TappyControlViewState) {
        state = newState
        reset()
    }

    fun setTappyControlListener(listener: TappyControlListener?) {
        adapter.setTappyControlListener(listener)
    }

    fun clearTappyControlListener() {
        adapter.setTappyControlListener(null)
    }

    private fun reset() {
        val tappyCollection = state.tappies
        val sortedList = ArrayList(tappyCollection)
        Collections.sort(sortedList, tappySorter)
        adapter.setTappies(sortedList)
    }
}

private class TappyControlAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<TappyControlAdapter.VH>() {
    private var namedTappyWithStatuses: List<NamedTappy>
    private var listener: TappyControlListener? = null

    init {
        namedTappyWithStatuses = emptyList<NamedTappy>()
    }


    fun setTappies(newList: List<NamedTappy>) {
        namedTappyWithStatuses = newList
        notifyDataSetChanged()
    }

    fun setTappyControlListener(listener: TappyControlListener?) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val inflater = LayoutInflater.from(ctx)
        return VH(inflater.inflate(R.layout.list_tappy_control, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tappy = namedTappyWithStatuses[position]
        holder.bind(tappy)
    }

    override fun getItemCount(): Int {
        return namedTappyWithStatuses.size
    }

    inner class VH(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        internal var namedTappy: NamedTappy? = null

        internal var tappyNameView: TextView
        internal var tappyStatusView: TextView

        internal var removeButton: ImageButton
        internal var tappyIcon: ImageView

        internal var connectingView: ProgressBar

        internal var ctx: Context

        init {
            ctx = itemView.context

            tappyNameView = itemView.find<TextView>(R.id.tv_title)
            tappyStatusView = itemView.find<TextView>(R.id.tv_subtitle)

            tappyIcon = itemView.find<ImageView>(R.id.iv_icon)

            connectingView = itemView.find<ProgressBar>(R.id.pb_connecting_indicator)

            removeButton = itemView.find<ImageButton>(R.id.ib_secondary_icon)
            removeButton.image = itemView.context.getColorResTintedDrawable(
                    R.drawable.ic_remove_circle_outline_black_24dp,
                    R.color.removeTappyColor
            )
            removeButton.setOnClickListener {
                val localTappy = namedTappy
                if (this@TappyControlAdapter.listener != null && localTappy != null) {
                    this@TappyControlAdapter.listener!!.requestRemove(localTappy)
                }
            }
        }

        fun bind(status: NamedTappy?) {
            this.namedTappy = status
            reset()
        }

        private fun reset() {
            val namedTappy = this.namedTappy
            if (namedTappy != null) {
                tappyNameView.text = namedTappy.name


                var iconInt = R.drawable.ic_bluetooth_black_24dp
                if(namedTappy.tappy is TappyUsb) {
                    iconInt = R.drawable.ic_usb_black_24dp
                }

                var showProgress = false

                when(namedTappy.tappy.latestStatus) {
                    Tappy.STATUS_CONNECTING -> {
                       tappyStatusView.setText(R.string.status_connecting)
                        tappyIcon.setImageDrawable(null)
                        showProgress = true
                    }
                    Tappy.STATUS_READY -> {
                        tappyStatusView.setText(R.string.status_ready)
                        tappyIcon.setImageDrawable(
                                ctx.getColorResTintedDrawable(
                                        iconInt,
                                        R.color.connected_tappy_color))
                        showProgress = false
                    }
                    Tappy.STATUS_DISCONNECTING -> {
                        tappyStatusView.setText(R.string.status_disconnecting)
                        tappyIcon.setImageDrawable(null)
                        showProgress = true
                    }
                    Tappy.STATUS_DISCONNECTED -> {
                        tappyStatusView.setText(R.string.status_connected)
                        tappyIcon.setImageDrawable(
                                ctx.getColorResTintedDrawable(
                                        iconInt,
                                        R.color.disconnected_tappy_color))
                    }
                    Tappy.STATUS_ERROR -> {
                        tappyStatusView.setText(R.string.status_error)
                        tappyIcon.setImageDrawable(
                                ctx.getColorResTintedDrawable(
                                        iconInt,
                                        R.color.error_tappy_color))
                        showProgress = false
                    }
                    Tappy.STATUS_CLOSED -> {
                        tappyStatusView.setText(R.string.status_closed)
                        tappyIcon.setImageDrawable(
                                ctx.getColorResTintedDrawable(
                                        iconInt,
                                        R.color.disconnected_tappy_color))
                    }
                }

                if (showProgress) {
                    connectingView.visibility = View.VISIBLE
                    tappyIcon.visibility = View.GONE
                } else {
                    connectingView.visibility = View.GONE
                    tappyIcon.visibility = View.VISIBLE
                }
            }
        }
    }
}
