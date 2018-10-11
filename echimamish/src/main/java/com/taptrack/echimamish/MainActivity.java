package com.taptrack.echimamish;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import com.taptrack.tcmptappy.tappy.constants.TagTypes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    private static final String EXTRA_TAG_TYPE = "com.taptrack.roaring.extra.TAG_TYPE";

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @BindView(R.id.trv_tag_results)
    TagResultView resultView;

    @Nullable
    TagResultView.ViewState currentViewState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            currentViewState = viewStateFromBundle(savedInstanceState);
        } else {
            currentViewState = viewStateFromBundle(getIntent().getExtras());
        }

        reset();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        writeToBundle(outState,currentViewState);
    }

    @Nullable
    private TagResultView.ViewState viewStateFromBundle(@Nullable Bundle b) {
        if(b == null) {
            return null;
        } else {
            byte[] tagCode = b.getByteArray(NfcAdapter.EXTRA_ID);
            byte tagType = b.getByte(EXTRA_TAG_TYPE, TagTypes.TAG_UNKNOWN);
            Parcelable[] messageParcels = b.getParcelableArray(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage message = null;
            if(messageParcels != null && messageParcels.length > 0) {
                message = (NdefMessage) messageParcels[0];
            }

            if(tagCode != null || message != null) {
                return new TagResultView.ViewState(tagCode,tagType,message);
            } else {
                return null;
            }
        }
    }

    private void writeToBundle(@NonNull Bundle bundle, @Nullable TagResultView.ViewState state) {
        if(state != null) {
            bundle.putByteArray(NfcAdapter.EXTRA_ID,state.getTagCode());
            bundle.putByte(EXTRA_TAG_TYPE, state.getTagType());
            NdefMessage msg = state.getNdefMessage();
            if(msg != null) {
                bundle.putParcelableArray(NfcAdapter.EXTRA_NDEF_MESSAGES,new NdefMessage[]{msg});
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(TAG,"Received new intent");

//        byte[] tagCode = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
//        byte tagType = intent.getByteExtra(EXTRA_TAG_TYPE, TagTypes.TAG_UNKNOWN);
//        Parcelable[] messageParcels = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
//        NdefMessage message = null;
//        if(messageParcels.length > 0) {
//            message = (NdefMessage) messageParcels[0];
//        }
//
//        if(tagCode != null || message != null) {
//            currentViewState = new TagResultView.ViewState(tagCode,tagType,message);
//        } else {
//            currentViewState = null;
//        }

        currentViewState = viewStateFromBundle(intent.getExtras());

        reset();
    }

    private void reset() {
        resultView.setViewState(currentViewState);
    }
}
