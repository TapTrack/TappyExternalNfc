package com.taptrack.roaring;

import android.content.Context;
import android.nfc.NdefMessage;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.taptrack.tcmptappy.commlink.CommunicatorStatusChangeListener;
import com.taptrack.tcmptappy.commlink.TcmpMessageListener;
import com.taptrack.tcmptappy.commlink.UnparsablePacketListener;
import com.taptrack.tcmptappy.commlink.ble.TappyBleCommunicator;
import com.taptrack.tcmptappy.tappy.ble.TappyBleDeviceDefinition;
import com.taptrack.tcmptappy.tappy.ble.TappyBleDeviceStatus;
import com.taptrack.tcmptappy.tcmp.MalformedPayloadException;
import com.taptrack.tcmptappy.tcmp.TCMPMessage;
import com.taptrack.tcmptappy.tcmp.commandfamilies.basicnfc.BasicNfcCommandLibrary;
import com.taptrack.tcmptappy.tcmp.commandfamilies.basicnfc.commands.DispatchTagsCommand;
import com.taptrack.tcmptappy.tcmp.commandfamilies.basicnfc.commands.StopCommand;
import com.taptrack.tcmptappy.tcmp.commandfamilies.basicnfc.responses.NdefFoundResponse;
import com.taptrack.tcmptappy.tcmp.commandfamilies.basicnfc.responses.TagFoundResponse;
import com.taptrack.tcmptappy.tcmp.commandfamilies.systemfamily.SystemCommandLibrary;
import com.taptrack.tcmptappy.tcmp.commandfamilies.systemfamily.commands.PingCommand;
import com.taptrack.tcmptappy.tcmp.commandfamilies.systemfamily.responses.PingResponse;
import com.taptrack.tcmptappy.tcmp.common.CommandFamilyMessageResolver;
import com.taptrack.tcmptappy.tcmp.common.FamilyCodeNotSupportedException;
import com.taptrack.tcmptappy.tcmp.common.ResponseCodeNotSupportedException;

import timber.log.Timber;

public class TappyBleManagementDelegate implements CommunicatorStatusChangeListener,
        TcmpMessageListener, UnparsablePacketListener {
    private static final String TAG = TappyBleManagementDelegate.class.getName();

    private final TappyBleDeviceDefinition deviceDefinition;
    private final Context context;
    private final TappyBleCommunicator communicator;
    private final DeviceStatusListener statusListener;
    private final TagScanListener tagListener;
    private final CommandFamilyMessageResolver messageResolver;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean isClosing = false;
    private boolean isSilent = false;

    private boolean hasReceivedPing = false;

    private int lastState = TappyBleDeviceStatus.DISCONNECTED;
    private long lastReconnectAttempted = 0;
    private static final int RECONNECT_DEBOUNCE_INTERVAL = 2000;

    // This is so we can detect when the tappy has finished booting to start polling
    private static final int SEND_PING_INTERVAL = 1000;
    private final Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            if(!isClosing && !hasReceivedPing) {
                if(communicator.getState() == TappyBleDeviceStatus.READY) {
                    communicator.sendTcmpMessage(new PingCommand());
                }

                handler.postDelayed(pingRunnable,SEND_PING_INTERVAL);
            }
        }
    };


    // throttle the poll requests we send
    private static final int POLL_REQUEST_THROTTLE = 100;
    private long lastPollRequestSent = 0;

    private final TCMPMessage message;

    public interface DeviceStatusListener {
        void newDeviceStatus(TappyBleDeviceDefinition deviceDefinition, int status);
    }

    public interface TagScanListener {
        void tagFound(byte[] uid, byte tagType);
        void ndefFound(byte[] uid, byte tagType, NdefMessage message);
    }

    public TappyBleManagementDelegate(TappyBleDeviceDefinition deviceDefinition,
                                      Context context,
                                      DeviceStatusListener statusListener,
                                      TagScanListener tagListener) {
        this(deviceDefinition,context,statusListener,tagListener,new DispatchTagsCommand((byte) 0));
    }

    public TappyBleManagementDelegate(TappyBleDeviceDefinition deviceDefinition,
                                      Context context,
                                      DeviceStatusListener statusListener,
                                      TagScanListener tagListener,
                                      TCMPMessage message) {
        this.deviceDefinition = deviceDefinition;
        this.context = context;
        this.statusListener = statusListener;
        this.tagListener = tagListener;
        this.message = message;

        messageResolver = new CommandFamilyMessageResolver();
        messageResolver.registerCommandLibrary(new BasicNfcCommandLibrary());
        messageResolver.registerCommandLibrary(new SystemCommandLibrary());

        communicator = new TappyBleCommunicator(context,deviceDefinition);
        communicator.registerMessageReceivedListener(this);
        communicator.registerStatusChangedListener(this);
        communicator.registerUnparsableMessageReceivedListener(this);
    }

    public boolean activate() {
        return communicator.initialize() && communicator.connect();
    }

    @Override
    public void onStatusChanged(int newStatus) {
        Timber.v(String.format("Received TappyBleState: %d, TappyBleDeviceStatus: %d",newStatus,communicator.getState()));
        if(!isClosing || !isSilent) {
            // requesting the state because the callback state is a TappyBleState,
            // not a TappyBleDeviceStatus, which is what we want
            statusListener.newDeviceStatus(deviceDefinition, communicator.getState());
        }

        if(isClosing) {
            if (newStatus == TappyBleDeviceStatus.DISCONNECTED) {
                communicator.close();
            }
        } else {
            switch (newStatus) {
            case TappyBleDeviceStatus.READY:
                Timber.v("Attempting to start polling.");
                // delaying initial polling so the tappy has time to boot
                initiatePinging();
                break;
            case TappyBleDeviceStatus.DISCONNECTED:
                final long disconnectReceived = SystemClock.uptimeMillis();
                handler.postDelayed(new Runnable() {
                                 @Override
                                 public void run() {
                                     // make sure we aren't reviving a closed connection by
                                     // accident
                                     if(!isClosing) {
                                         // check to see if a reconnect was attempted since we were made
                                         if (disconnectReceived >= (lastReconnectAttempted+RECONNECT_DEBOUNCE_INTERVAL)) {
                                             // at least debounce time has passed
                                             // since a reconnect was attempted
                                             if(communicator.getState() == TappyBleDeviceStatus.DISCONNECTED) {
                                                 // we're still disconnected
                                                 lastReconnectAttempted = SystemClock.uptimeMillis();
                                                 communicator.connect();
                                             }
                                         }
                                     }
                                 }
                             }
                    ,RECONNECT_DEBOUNCE_INTERVAL+10); // adding ten ms to this just in case the clock is slightly off
                break;
            }

            lastState = newStatus;
        }
    }

    private void initiatePinging() {
        hasReceivedPing = false;
        handler.postDelayed(pingRunnable,SEND_PING_INTERVAL);
    }

    public boolean hasStartedClosing() {
        return isClosing;
    }

    public void goSilent() {
        this.isSilent = true;
    }

    private void startPolling() {
        final long thisPollRequested = SystemClock.uptimeMillis();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isClosing) {
                    if(thisPollRequested >= (lastPollRequestSent+POLL_REQUEST_THROTTLE)) {
                        lastPollRequestSent = SystemClock.uptimeMillis();
                        communicator.sendTcmpMessage(message);
                    }
                }
            }
        },POLL_REQUEST_THROTTLE+10); // adding ten ms to this just in case the clock is slightly off
    }

    @Override
    public void onNewTcmpMessage(TCMPMessage tcmpMessage) {
        TCMPMessage resolved = null;
        try {
            resolved = messageResolver.parseResponse(tcmpMessage);
        } catch (FamilyCodeNotSupportedException | ResponseCodeNotSupportedException | MalformedPayloadException e) {
            Timber.w("Error on resolving Tappy response",e);
        }

        if(resolved instanceof NdefFoundResponse) {
            Timber.v("NDEF response received");
            NdefFoundResponse ndefFoundResponse = (NdefFoundResponse) resolved;
            tagListener.ndefFound(ndefFoundResponse.getTagCode(),ndefFoundResponse.getTagType(),ndefFoundResponse.getMessage());
        } else if (resolved instanceof TagFoundResponse) {
            Timber.v("Tag response received");
            TagFoundResponse tagFoundResponse = (TagFoundResponse) resolved;
            tagListener.tagFound(tagFoundResponse.getTagCode(), tagFoundResponse.getTagType());
        } else if (resolved instanceof PingResponse) {
            Timber.v("Ping response received");
            hasReceivedPing = true;
            startPolling();
        } else {
            // The tappy has responded with something, lets just try to start polling
            startPolling();
        }
    }

    @Override
    public void onUnparsablePacket(byte[] packet) {
        startPolling();
    }

    public void close(boolean silently) {
        isClosing = true;
        isSilent = silently;
        communicator.sendTcmpMessage(new StopCommand());

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                communicator.disconnect();
            }
        },100);
    }
}
