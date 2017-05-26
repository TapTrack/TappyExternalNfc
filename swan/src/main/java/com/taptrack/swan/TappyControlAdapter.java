package com.taptrack.swan;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

class TappyControlAdapter extends RecyclerView.Adapter<TappyControlAdapter.VH> {
    @NonNull
    private List<TappyWithStatus> tappies;
    @Nullable
    private TappyControlListener listener;

    public TappyControlAdapter() {
        super();
        tappies = Collections.emptyList();
    }

    static final class TappyDiffCb extends DiffUtil.Callback {
        private final List<TappyWithStatus> newTappies;
        private final List<TappyWithStatus> oldTappies;

        TappyDiffCb(List<TappyWithStatus> newTappies, List<TappyWithStatus> oldTappies) {
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
            return newTappies.get(newItemPosition).getDeviceDefinition().getAddress().equals(oldTappies.get(oldItemPosition).getDeviceDefinition().getAddress());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            TappyWithStatus oldTappy = oldTappies.get(oldItemPosition);
            TappyWithStatus newTappy = newTappies.get(newItemPosition);

            return newTappy.getStatus() == oldTappy.getStatus();
        }
    }

    public void setTappies(@NonNull List<TappyWithStatus> newList) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new TappyDiffCb(newList,this.tappies));
        tappies = newList;
        result.dispatchUpdatesTo(this);
    }

    public void setTappyControlListener(@Nullable TappyControlListener listener) {
        this.listener = listener;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        return new VH(inflater.inflate(R.layout.list_tappy_control,parent,false));
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        TappyWithStatus status = tappies.get(position);
        if(status != null) {
            holder.bind(status);
        }
    }

    @Override
    public int getItemCount() {
        return tappies.size();
    }

    public class VH extends RecyclerView.ViewHolder {
        @Nullable
        TappyWithStatus status;

        TextView tappyNameView;
        TextView tappyStatusView;

        ImageButton removeButton;
        ImageView tappyIcon;

        ProgressBar connectingView;

        Context ctx;

        public VH(View itemView) {
            super(itemView);
            ctx = itemView.getContext();

            tappyNameView = (TextView) itemView.findViewById(R.id.tv_title);
            tappyStatusView = (TextView) itemView.findViewById(R.id.tv_subtitle);

            removeButton = (ImageButton) itemView.findViewById(R.id.ib_secondary_icon);
            tappyIcon = (ImageView) itemView.findViewById(R.id.iv_icon);

            connectingView = (ProgressBar) itemView.findViewById(R.id.pb_connecting_indicator);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(TappyControlAdapter.this.listener != null && status != null) {
                        TappyControlAdapter.this.listener.requestConnect(status.getDeviceDefinition());
                    }
                }
            });

            removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(TappyControlAdapter.this.listener != null && status != null) {
                        TappyControlAdapter.this.listener.requestRemove(status.getDeviceDefinition());
                    }
                }
            });
        }

        public void bind(@Nullable TappyWithStatus status) {
            this.status = status;
            reset();
        }

        private void reset() {
            if(status != null) {
                tappyNameView.setText(status.getDeviceDefinition().getName());
                tappyStatusView.setText(status.getDeviceDefinition().getAddress());

                boolean showProgress;
                if(status.isReady()) {
                    tappyIcon.setImageDrawable(
                            DrawableTinter.getColorResTintedDrawable(
                                    ctx,
                                    R.drawable.ic_cloud_black_24dp,
                                    R.color.connected_tappy_color));
                    showProgress = false;
                } else if (status.isInErrorState()) {
                    tappyIcon.setImageDrawable(
                            DrawableTinter.getColorResTintedDrawable(
                                    ctx,
                                    R.drawable.ic_error_black_24dp,
                                    R.color.error_tappy_color));
                    showProgress = false;
                } else if (status.isStatusUnknown() || status.isDisconnected()) {
                    tappyIcon.setImageDrawable(
                            DrawableTinter.getColorResTintedDrawable(
                                    ctx,
                                    R.drawable.ic_cloud_black_24dp,
                                    R.color.disconnected_tappy_color));
                    showProgress = false;
                } else {
                    tappyIcon.setImageDrawable(null);
                    showProgress = true;
                }

                if(showProgress) {
                    connectingView.setVisibility(View.VISIBLE);
                    tappyIcon.setVisibility(View.GONE);
                } else {
                    connectingView.setVisibility(View.GONE);
                    tappyIcon.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
