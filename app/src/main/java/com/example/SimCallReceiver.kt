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
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                Log.d("SimCallReceiver", "Incoming call from: $incomingNumber")
                if (incomingNumber != null) {
                    val normalized = incomingNumber.replace("\\s".toRegex(), "").replace("-", "")
                    if (normalized.endsWith("2191005040") || normalized.endsWith("2191089020")) {
                        // 1. Silence/mute the system ringer
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        try {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                        } catch (e: Exception) {
                            try {
                                audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
                            } catch (ex: Exception) {}
                        }
                        
                        // 2. Programmatically end the system cellular call so it doesn't show standard call UI
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                            try {
                                @Suppress("MissingPermission")
                                telecomManager?.endCall()
                            } catch (e: Exception) {
                                Log.e("SimCallReceiver", "Failed to end cellular call", e)
                            }
                        }

                        // 3. Trigger VoIP call inside our app
                        val voipManager = VoipManager.instance
                        if (voipManager != null) {
                            voipManager.triggerIncomingCall("SERVICE")
                        } else {
                            val appIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("trigger_voip_incoming", "SERVICE")
                            }
                            context.startActivity(appIntent)
                        }
                    }
                }
            }
        }
    }
}
