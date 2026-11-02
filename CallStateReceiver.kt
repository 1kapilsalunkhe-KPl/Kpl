package com.pixel6a.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Listens for phone call state transitions.
 * When a call is picked up (OFFHOOK), starts InCallFloatingOverlayService so the
 * user has an immediate one-tap Record button on their screen.
 */
class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

            Log.d("CallStateReceiver", "Phone state changed: $stateStr")

            // Check if overlay permission is granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                Log.w("CallStateReceiver", "Overlay permission not granted. Cannot show in-call button.")
                return
            }

            when (stateStr) {
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Call is answered or dialed out: show in-call manual record button!
                    val serviceIntent = Intent(context, InCallFloatingOverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    // Call finished: stop overlay and auto-save recording
                    val serviceIntent = Intent(context, InCallFloatingOverlayService::class.java)
                    context.stopService(serviceIntent)
                }
            }
        }
    }
}
