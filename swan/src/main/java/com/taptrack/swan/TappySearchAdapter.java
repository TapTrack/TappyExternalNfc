package com.taptrack.swan;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.taptrack.tcmptappy.tappy.ble.TappyBleDeviceDefinition;

import java.util.Collections;
import java.util.List;

class TappySearchAdapter extends RecyclerView.Adapter<TappySearchAdapter.VH> {
    @Nullable
    private TappySelectionListener tappySelectionListener;
    @NonNull
    private List<TappyBleDeviceDefinition> deviceList;

    static final class TappyDeviceDiffCb extends DiffUtil.Callback {
        private final List<TappyBleDeviceDefinition> newTappies;
        private final List<TappyBleDeviceDefinition> oldTappies;

        TappyDeviceDiffCb(List<TappyBleDeviceDefinition> newTappies, List<TappyBleDeviceDefinition> oldTappies) {
            this.oldTappies = oldTappies;
            this.newTappies = newTappies;
        }

        @Override
        public int getOldListSize() {
            return oldTappies.size();
        }

        @Override
        public int getNewListSize() {
            return newTappies.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return newTappies.get(newItemPosition).getAddress().equals(oldTappies.get(oldItemPosition).getAddress());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return newTappies.get(newItemPosition).equals(oldTappies.get(oldItemPosition));
        }
    }

    public TappySearchAdapter() {
            deviceList = Collections.emptyList();
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new VH(inflater.inflate(R.layout.search_tappy_control,parent,false));
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        TappyBleDeviceDefinition deviceDefinition = deviceList.get(position);
        if(deviceDefinition != null) {
            holder.bind(deviceDefinition);
        }
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public void setTappySelectionListener(@Nullable TappySelectionListener tappySelectionListener) {
        this.tappySelectionListener = tappySelectionListener;
    }

    public void setDevices(@NonNull List<TappyBleDeviceDefinition> newList) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new TappyDeviceDiffCb(newList,this.deviceList));
        this.deviceList = newList;
        result.dispatchUpdatesTo(this);
    }

    class VH extends RecyclerView.ViewHolder {
        private final ImageView iconView;
        private final TextView tappyTitleView;
        private final TextView tappySubtitleView;

        private TappyBleDeviceDefinition currentDevice;

        public VH(View itemView) {
            super(itemView);
            iconView = (ImageView) itemView.findViewById(R.id.iv_icon);
            tappyTitleView = (TextView) itemView.findViewById(R.id.tv_title);
            tappySubtitleView = (TextView) itemView.findViewById(R.id.tv_subtitle);
//            iconView.setImageDrawable(DrawableTinter.getColorResTintedDrawable(itemView.getContext(),R.drawable.ic_cloud_black_24dp,R.color.found_tappy_color));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(currentDevice != null && tappySelectionListener != null) {
                        tappySelectionListener.tappySelected(currentDevice);
                    }
                }
            });
        }

        public void bind(@NonNull TappyBleDeviceDefinition definition) {
            tappyTitleView.setText(definition.getName());
            tappySubtitleView.setText(definition.getAddress());
            currentDevice = definition;
        }
    }
}
