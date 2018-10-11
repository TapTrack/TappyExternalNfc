package com.taptrack.echimamish;

import android.content.Context;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.util.AttributeSet;

import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class DisplayNdefView extends RecyclerView {
    protected NdefRecordAdapter adapter;

    public DisplayNdefView(Context context) {
        super(context);
        init(context);
    }

    public DisplayNdefView(Context context,
                           @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DisplayNdefView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void init(Context ctx) {
        adapter = new NdefRecordAdapter();
        setLayoutManager(new LinearLayoutManager(ctx,RecyclerView.VERTICAL,false));
        setAdapter(adapter);
    }



    public void displayNdefMessage(@Nullable final NdefMessage message) {
        post(new Runnable() {
            @Override
            public void run() {
                if(message != null) {
                    adapter.setItems(Arrays.asList(message.getRecords()));
                } else {
                    adapter.setItems(new ArrayList<NdefRecord>(0));
                }
                adapter.notifyDataSetChanged();
            }
        });
    }


    private class NdefRecordAdapter extends ListDelegationAdapter<List<NdefRecord>> {
        public NdefRecordAdapter() {
            super();
            delegatesManager.addDelegate(new NdefUrlDelegate());
            delegatesManager.addDelegate(new NdefTextDelegate());
            delegatesManager.addDelegate(new NdefExternalDelegate());
            delegatesManager.addDelegate(new NdefEmptyRecordDelegate());
            delegatesManager.addDelegate(new NdefUnsupportedDelegate());
        }
    }
}
