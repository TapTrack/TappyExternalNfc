package com.taptrack.echimamish;

import android.content.Context;
import android.nfc.NdefRecord;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate;

import java.nio.charset.Charset;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by lvanoort on 18/11/15.
 */
public class NdefExternalDelegate extends
        AbsListItemAdapterDelegate<NdefRecord,NdefRecord,NdefExternalDelegate.NdefExternalViewHolder> {

    public NdefExternalDelegate() {
    }

    @Override
    protected boolean isForViewType(@NonNull NdefRecord record,
                                    @NonNull List<NdefRecord> items,
                                    int position) {
        return record.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE;
    }

    @NonNull
    @Override
    public NdefExternalDelegate.NdefExternalViewHolder onCreateViewHolder(ViewGroup parent) {
        Context ctx = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        NdefExternalViewHolder holder = new NdefExternalViewHolder(inflater.inflate(R.layout.listitem_one_text_image,parent,false));
        holder.mIcon.setImageDrawable(ContextCompat.getDrawable(ctx,R.drawable.ic_extension_black_24dp));
        holder.mIcon.setAlpha(0.54f);
        return holder;
    }

    @Override
    protected void onBindViewHolder(@NonNull NdefRecord record,
                                    @NonNull NdefExternalViewHolder viewHolder,
                                    @NonNull List<Object> payloads) {
        viewHolder.setData(
                getTextFromBytes(record.getType()),
                getTextFromBytes(record.getId()),
                getTextFromBytes(record.getPayload()));
    }

    @NonNull
    private String getTextFromBytes(@Nullable byte[] payload) {
        if(payload == null || payload.length == 0) {
            return "";
        }
        Charset textEncoding = Charset.forName("UTF-8");
        String field = new String(payload, textEncoding);
        return field.trim();
    }

    static class NdefExternalViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tv_title)
        public TextView mTitle;
        @BindView(R.id.iv_icon)
        public ImageView mIcon;

        public NdefExternalViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void setData(@NonNull String type, @Nullable String id, @NonNull String payload) {
            String idDescription;
            if(id == null || id.length() == 0) {
                idDescription = "none";
            } else {
                idDescription = id;
            }

            mTitle.setText(mTitle.getContext()
                    .getString(R.string.external_description,
                            type,
                            idDescription,
                            payload));

        }

    }
}
