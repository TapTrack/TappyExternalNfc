package com.taptrack.echimamish;

import android.content.Context;
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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by lvanoort on 18/11/15.
 */
public class NdefTextDelegate extends AbsListItemAdapterDelegate<NdefRecord,NdefRecord,NdefTextDelegate.NdefTextViewHolder> {

    public NdefTextDelegate() {
    }

    @Override
    protected boolean isForViewType(@NonNull NdefRecord record,
                                    @NonNull List<NdefRecord> items,
                                    int position) {
        return record.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.getType(),
                NdefRecord.RTD_TEXT);
    }

    @NonNull
    @Override
    public NdefTextDelegate.NdefTextViewHolder onCreateViewHolder(ViewGroup parent) {
        Context ctx = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        NdefTextViewHolder holder = new NdefTextViewHolder(inflater.inflate(R.layout.listitem_one_text_image,parent,false));
        holder.mIcon.setImageDrawable(ContextCompat.getDrawable(ctx,R.drawable.ic_description_black_24dp));
        holder.mIcon.setAlpha(0.54f);
        return holder;
    }

    @Override
    protected void onBindViewHolder(@NonNull NdefRecord record,
                                    @NonNull NdefTextViewHolder viewHolder,
                                    @NonNull List<Object> payloads) {
        String text = getTextFromNdef(record).trim();
        viewHolder.mTitle.setText(text);
    }

    private String getTextFromNdef(NdefRecord record) {
        byte[] payload = record.getPayload();

        int status = payload[0] & 0xff;
        int languageCodeLength = (status & 0x1F);
        //not needed currently
        //String languageCode = new String(payload, 1, languageCodeLength);

        Charset textEncoding = ((status & 0x80) != 0) ? Charset.forName("UTF-16") : Charset.forName("UTF-8");
        String field = new String(payload, 1 + languageCodeLength, payload.length - languageCodeLength - 1, textEncoding);
        return field.trim();
    }

    static class NdefTextViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tv_title)
        public TextView mTitle;
        @BindView(R.id.iv_icon)
        public ImageView mIcon;

        public NdefTextViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
