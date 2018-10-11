package com.taptrack.experiments.rancheria.business

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.taptrack.roaring.R

class TappyNotificationManager() {
    companion object {
        private val CHANNEL_ID="ACTIVE_TAPPY_CHANNEL"

        @RequiresApi(Build.VERSION_CODES.O)
        fun getTappyNotificationId(): String {
            return CHANNEL_ID
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun createTappyNotificationChannel(context: Context){
            val name = context.getString(R.string.tappy_channel_name)
            val desc = context.getString(R.string.tappy_channel_description)

            val channel = NotificationChannel(CHANNEL_ID,name, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = desc
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                    ?.createNotificationChannel(channel)

        }

        fun createNotificationChannelIfOreo(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                TappyNotificationManager.createTappyNotificationChannel(context)
            }
        }

        fun createNotificationBuilder(context: Context): NotificationCompat.Builder {
            return NotificationCompat.Builder(context, CHANNEL_ID)
        }
    }
}

class TappyNotificationLocaleUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_LOCALE_CHANGED && context != null) {
            TappyNotificationManager.createNotificationChannelIfOreo(context)
        }
    }

}