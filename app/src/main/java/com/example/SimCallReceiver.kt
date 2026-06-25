package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log

class SimCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Temporarily disabled as requested
        return
    }
}
