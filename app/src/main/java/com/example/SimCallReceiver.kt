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
            Log.d("SimCallReceiver", "Phone state changed: $state")
            
            val voipManager = VoipManager.instance ?: VoipManager(context.applicationContext)

            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                Log.d("SimCallReceiver", "Incoming call from: $incomingNumber")
                if (incomingNumber != null) {
                    val normalized = incomingNumber.replace("\\s".toRegex(), "").replace("-", "")
                    if (normalized.endsWith("2191005040") || normalized.endsWith("2191089020")) {
                        // 1. Set redirection flag BEFORE ending cellular call to prevent premature hangup in EXTRA_STATE_IDLE
                        voipManager.isRedirectingSimCall = true

                        // 2. Silence/mute the system ringer
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        try {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                        } catch (e: Exception) {
                            try {
                                audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
                            } catch (ex: Exception) {}
                        }
                        
                        // 3. Programmatically end the system cellular call so it doesn't show standard call UI
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                            try {
                                @Suppress("MissingPermission")
                                telecomManager?.endCall()
                            } catch (e: Exception) {
                                Log.e("SimCallReceiver", "Failed to end cellular call", e)
                            }
                        }
                        
                        // 4. Trigger VoIP call inside our app
                        voipManager.triggerIncomingCall("SERVICE")
                    }
                }
            } else if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                // If a cellular/SIM call is answered/offhook, terminate any active VoIP call session in the app
                val currentCallState = voipManager.callState.value
                if (currentCallState != VoipCallState.IDLE && currentCallState != VoipCallState.DISCONNECTED) {
                    Log.d("SimCallReceiver", "SIM call went OFFHOOK. Hanging up VoIP app call.")
                    voipManager.endCall()
                }
            } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                // If the SIM/cellular call is ended/idle, synchronize and end the app's call screen/ringing
                if (voipManager.isRedirectingSimCall) {
                    Log.d("SimCallReceiver", "SIM call went IDLE but it was our redirect. Ignoring.")
                    voipManager.isRedirectingSimCall = false
                } else {
                    val currentCallState = voipManager.callState.value
                    if (currentCallState != VoipCallState.IDLE && currentCallState != VoipCallState.DISCONNECTED) {
                        Log.d("SimCallReceiver", "SIM call went IDLE. Hanging up VoIP app call.")
                        voipManager.endCall()
                    }
                }
            }
        }
    }
}
