package com.taptrack.echimamish;

import android.content.Context;
import android.graphics.Typeface;
import android.nfc.NdefRecord;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class NdefEmptyRecordDelegate extends
        AbsListItemAdapterDelegate<NdefRecord,NdefRecord,NdefEmptyRecordDelegate.NdefEmptyRecordViewHolder> {

    public NdefEmptyRecordDelegate() {
    }

    @Override
    protected boolean isForViewType(@NonNull NdefRecord record,
                                    @NonNull List<NdefRecord> items,
                                    int position) {
        return record.getTnf() == NdefRecord.TNF_EMPTY;
    }

    @NonNull
    @Override
    public NdefEmptyRecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
        Context ctx = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        NdefEmptyRecordViewHolder holder = new NdefEmptyRecordViewHolder(inflater.inflate(R.layout.listitem_one_text_image,parent,false));
        holder.mIcon.setImageDrawable(ContextCompat.getDrawable(ctx,R.drawable.ic_check_box_outline_blank_black_24dp));
        holder.mIcon.setAlpha(0.54f);
        holder.mTitle.setTypeface(null, Typeface.ITALIC);
        holder.mTitle.setText(R.string.empty_record);
        return holder;
    }

    @Override
    protected void onBindViewHolder(@NonNull NdefRecord item,
                                    @NonNull NdefEmptyRecordViewHolder viewHolder,
                                    @NonNull List<Object> payloads) {

    }

    static class NdefEmptyRecordViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tv_title)
        public TextView mTitle;
        @BindView(R.id.iv_icon)
        public ImageView mIcon;

        public NdefEmptyRecordViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
