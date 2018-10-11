package com.taptrack.echimamish;

import android.content.Context;
import android.nfc.NdefMessage;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.taptrack.tcmptappy.tappy.constants.TagTypes;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StyleRes;
import butterknife.BindView;
import butterknife.ButterKnife;

public class TagResultView extends FrameLayout {
    @BindView(R.id.tv_tag_code)
    TextView tvTagCode;
    @BindView(R.id.tv_tag_type)
    TextView tvTagType;
    @BindView(R.id.tv_ndef_none)
    TextView tvNdefNone;
    @BindView(R.id.dnv_display_ndef_view)
    DisplayNdefView dnvDisplayNdefView;

    public static class ViewState {
        @Nullable
        private final byte[] tagCode;
        private final byte tagType;
        @Nullable
        private final NdefMessage ndefMessage;

        public ViewState(@Nullable byte[] tagCode, byte tagType, NdefMessage ndefMessage) {
            this.tagCode = tagCode;
            this.tagType = tagType;
            this.ndefMessage = ndefMessage;
        }

        @Nullable
        public byte[] getTagCode() {
            return tagCode;
        }

        public byte getTagType() {
            return tagType;
        }

        @Nullable
        public NdefMessage getNdefMessage() {
            return ndefMessage;
        }
    }

    @Nullable
    ViewState currentViewState;

    public TagResultView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public TagResultView(@NonNull Context context,
                         @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TagResultView(@NonNull Context context,
                         @Nullable AttributeSet attrs,
                         @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TagResultView(@NonNull Context context,
                         @Nullable AttributeSet attrs,
                         @AttrRes int defStyleAttr,
                         @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.tag_result_view, this);
        ButterKnife.bind(this);

        reset();
    }

    public void setViewState(@Nullable final ViewState state) {
        post(new Runnable() {
            @Override
            public void run() {
                TagResultView.this.currentViewState = state;
                reset();
            }
        });
    }

    private void reset() {
        if(currentViewState == null) {
            tvTagCode.setText(R.string.no_data_present);
            tvTagType.setText(R.string.no_data_present);
            tvNdefNone.setVisibility(VISIBLE);
            dnvDisplayNdefView.setVisibility(INVISIBLE);
        } else {
            tvTagCode.setText(getFormattedTagSerial(currentViewState.getTagCode()));
            tvTagType.setText(parseTagType(currentViewState.getTagType()));
            NdefMessage msg = currentViewState.getNdefMessage();
            if(msg == null) {
                tvNdefNone.setVisibility(VISIBLE);
                dnvDisplayNdefView.setVisibility(INVISIBLE);
            } else {
                tvNdefNone.setVisibility(GONE);
                dnvDisplayNdefView.displayNdefMessage(currentViewState.getNdefMessage());
                dnvDisplayNdefView.setVisibility(VISIBLE);
            }
        }
    }

    private String getFormattedTagSerial(@Nullable byte[] serial) {
        if(serial != null && serial.length > 0) {
            StringBuilder builder = new StringBuilder((serial.length * 3) - 1);

            for (int i = 0; i < serial.length; i++) {
                builder.append(String.format("%02X:", serial[i]));
            }
            builder.setLength(builder.length() - 1);
            return builder.toString();
        } else {
            return getContext().getString(R.string.no_data_present);
        }
    }

    private String parseTagType(byte flag) {
        Context ctx = getContext();
        switch(flag) {
        case TagTypes.MIFARE_ULTRALIGHT: {
            return ctx.getString(R.string.ultralight_title);
        }
        case TagTypes.NTAG203: {
            return ctx.getString(R.string.ntag203_title);
        }
        case TagTypes.MIFARE_ULTRALIGHT_C: {
            return ctx.getString(R.string.ultralight_c_title);
        }
        case TagTypes.MIFARE_STD_1K: {
            return ctx.getString(R.string.std_1k_title);
        }
        case TagTypes.MIFARE_STD_4K: {
            return ctx.getString(R.string.std_4k_title);
        }
        case TagTypes.MIFARE_DESFIRE_EV1_2K: {
            return ctx.getString(R.string.desfire_ev1_2k_title);
        }
        case TagTypes.TYPE_2_TAG: {
            return ctx.getString(R.string.unk_type2_title);
        }
        case TagTypes.MIFARE_PLUS_2K_CL2: {
            return ctx.getString(R.string.plus_2k_title);
        }
        case TagTypes.MIFARE_PLUS_4K_CL2: {
            return ctx.getString(R.string.plus_4k_title);
        }
        case TagTypes.MIFARE_MINI: {
            return ctx.getString(R.string.mini_title);
        }
        case TagTypes.OTHER_TYPE4: {
            return ctx.getString(R.string.other_type4_title);
        }
        case TagTypes.MIFARE_DESFIRE_EV1_4K: {
            return ctx.getString(R.string.desfire_ev1_4k_title);
        }
        case TagTypes.MIFARE_DESFIRE_EV1_8K: {
            return ctx.getString(R.string.desfire_ev1_8k);
        }
        case TagTypes.MIFARE_DESFIRE: {
            return ctx.getString(R.string.desfire_title);
        }
        case TagTypes.TOPAZ_512: {
            return ctx.getString(R.string.topaz_512_title);
        }
        case TagTypes.NTAG_210: {
            return ctx.getString(R.string.ntag_210_title);
        }
        case TagTypes.NTAG_212: {
            return ctx.getString(R.string.ntag_212_title);
        }
        case TagTypes.NTAG_213: {
            return ctx.getString(R.string.ntag_213_title);
        }
        case TagTypes.NTAG_215: {
            return ctx.getString(R.string.ntag_215_title);
        }
        case TagTypes.NTAG_216: {
            return ctx.getString(R.string.ntag_216_title);
        }
        case TagTypes.NO_TAG: {
            return ctx.getString(R.string.no_tag_title);
        }
        case TagTypes.TAG_UNKNOWN:
        default: {
            return ctx.getString(R.string.unk_type_title);
        }
        }
    }
}
