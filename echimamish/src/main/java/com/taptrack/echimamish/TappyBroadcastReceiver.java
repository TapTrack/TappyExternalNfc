package com.taptrack.echimamish;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TappyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startActivityIntent = new Intent(context,MainActivity.class);
        startActivityIntent.replaceExtras(intent);
        startActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        context.startActivity(startActivityIntent);
    }
}
