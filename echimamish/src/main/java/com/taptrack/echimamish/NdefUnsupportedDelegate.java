package com.taptrack.echimamish;

import android.content.Context;
import android.graphics.Typeface;
import android.nfc.NdefRecord;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hannesdorfmann.adapterdelegates3.AbsListItemAdapterDelegate;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NdefUnsupportedDelegate extends
        AbsListItemAdapterDelegate<NdefRecord,NdefRecord,NdefUnsupportedDelegate.NdefUnsupportedViewHolder> {

    public NdefUnsupportedDelegate(){

    }

    @Override
    protected boolean isForViewType(@NonNull NdefRecord item,
                                    @NonNull List<NdefRecord> items,
                                    int position) {
        return true;
    }

    @NonNull
    @Override
    public NdefUnsupportedViewHolder onCreateViewHolder(ViewGroup parent) {
        Context ctx = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        NdefUnsupportedViewHolder holder = new NdefUnsupportedViewHolder(inflater.inflate(R.layout.listitem_one_text_image,parent,false));
        holder.mIcon.setImageDrawable(ContextCompat.getDrawable(ctx,R.drawable.ic_error_outline_black_24dp));
        holder.mIcon.setAlpha(0.54f);
        holder.mTitle.setTypeface(null, Typeface.ITALIC);
        holder.mTitle.setText(R.string.unsupported_record);
        return holder;
    }

    @Override
    protected void onBindViewHolder(@NonNull NdefRecord item,
                                    @NonNull NdefUnsupportedViewHolder viewHolder,
                                    @NonNull List<Object> payloads) {

    }

    static class NdefUnsupportedViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tv_title)
        public TextView mTitle;
        @BindView(R.id.iv_icon)
        public ImageView mIcon;

        public NdefUnsupportedViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
