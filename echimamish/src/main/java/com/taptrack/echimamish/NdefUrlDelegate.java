package com.taptrack.echimamish;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefRecord;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hannesdorfmann.adapterdelegates3.AbsListItemAdapterDelegate;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NdefUrlDelegate extends
        AbsListItemAdapterDelegate<NdefRecord,NdefRecord,NdefUrlDelegate.NdefUrlViewHolder> {

    public NdefUrlDelegate() {
    }


    @Override
    protected boolean isForViewType(@NonNull NdefRecord record,
                                    @NonNull List<NdefRecord> items,
                                    int position) {
        return record.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.getType(),
                NdefRecord.RTD_URI);
    }

    @NonNull
    @Override
    public NdefUrlViewHolder onCreateViewHolder(ViewGroup parent) {
        Context ctx = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        NdefUrlViewHolder holder = new NdefUrlViewHolder(inflater.inflate(R.layout.listitem_one_text_image,parent,false),ctx);
        holder.mIcon.setImageDrawable(ContextCompat.getDrawable(ctx,R.drawable.ic_link_black_24dp));
        holder.mIcon.setAlpha(0.54f);
        return holder;
    }

    @Override
    protected void onBindViewHolder(@NonNull NdefRecord record,
                                    @NonNull NdefUrlViewHolder viewHolder,
                                    @NonNull List<Object> payloads) {
        String url = getUrlFromNdef(record);
        if(url != null) {
            viewHolder.mTitle.setText(url);
            viewHolder.url = url;
        }
        else {
            viewHolder.mTitle.setText(R.string.msg_unsupported_url);
        }
    }

    private String getUrlFromNdef(NdefRecord record) {
        byte[] uriPayload = record.getPayload();
        String url = null;
        if (uriPayload.length > 1) {
            byte prefixByte = uriPayload[0];
            switch (prefixByte) {
                case 0x01:
                    url = "http://www." + new String(Arrays.copyOfRange(uriPayload, 1, uriPayload.length));
                    break;
                case 0x02:
                    url = "https://www." + new String(Arrays.copyOfRange(uriPayload, 1, uriPayload.length));
                    break;
                case 0x03:
                    url = "http://" + new String(Arrays.copyOfRange(uriPayload, 1, uriPayload.length));
                    break;
                case 0x04:
                    url = "https://" + new String(Arrays.copyOfRange(uriPayload, 1, uriPayload.length));
                    break;
                case 0x06:
                    url = "mailto:" + new String(Arrays.copyOfRange(uriPayload, 1, uriPayload.length));
                    break;
            }
        }
        return url;
    }

    static class NdefUrlViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.iv_icon)
        public ImageView mIcon;
        @BindView(R.id.tv_title)
        public TextView mTitle;

        public String url;

        private Context mCtx;

        public NdefUrlViewHolder(View itemView, Context ctx) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            mCtx = ctx;
            itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    urlClicked();
                }
            });
        }

        public void urlClicked() {
            if(url != null) {
                Intent launchUrlIntent = new Intent(Intent.ACTION_VIEW);
                launchUrlIntent.setData(Uri.parse(url));
                launchUrlIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mCtx.startActivity(launchUrlIntent);
            }
            else {
                Toast.makeText(mCtx,R.string.msg_unsupported_url, Toast.LENGTH_SHORT).show();
            }

        }
    }

}
