package com.example

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class VoipCallState {
  IDLE, DIALING, RINGING, CONNECTED, DISCONNECTED
}

data class SipAccount(
  val server: String = "",
  val username: String = "",
  val secret: String = "",
  val port: String = "5060",
  val transport: String = "UDP"
)

class VoipManager(private val context: Context) {
  private val prefs: SharedPreferences = context.getSharedPreferences("voip_prefs", Context.MODE_PRIVATE)
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private var toneGenerator: ToneGenerator? = null

  private val _accountState = MutableStateFlow(loadAccount())
  val accountState: StateFlow<SipAccount> = _accountState

  private val _registrationState = MutableStateFlow("Unregistered") // Unregistered, Registering, Registered
  val registrationState: StateFlow<String> = _registrationState

  private val _callState = MutableStateFlow(VoipCallState.IDLE)
  val callState: StateFlow<VoipCallState> = _callState

  private val _activeCallNumber = MutableStateFlow("")
  val activeCallNumber: StateFlow<String> = _activeCallNumber

  private val _isMuted = MutableStateFlow(false)
  val isMuted: StateFlow<Boolean> = _isMuted

  private val _isSpeakerOn = MutableStateFlow(false)
  val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn

  private val _callDuration = MutableStateFlow(0) // seconds
  val callDuration: StateFlow<Int> = _callDuration

  private val _inputGain = MutableStateFlow(0.8f) // simulated gain slider
  val inputGain: StateFlow<Float> = _inputGain

  private val _outputVolume = MutableStateFlow(0.7f) // voice call volume slider
  val outputVolume: StateFlow<Float> = _outputVolume

  private val handler = Handler(Looper.getMainLooper())
  private var durationRunnable: Runnable? = null
  private var simulationRunnable: Runnable? = null

  init {
    try {
      toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
    } catch (e: Exception) {
      // safe fallback
    }
    // Set initial speaker state
    _isSpeakerOn.value = audioManager.isSpeakerphoneOn
    _isMuted.value = audioManager.isMicrophoneMute
    
    // Read actual system voice call volume ratio
    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL).toFloat()
    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL).toFloat()
    if (maxVol > 0f) {
      _outputVolume.value = currentVol / maxVol
    }
  }

  fun loadAccount(): SipAccount {
    return SipAccount(
      server = prefs.getString("sip_server", "") ?: "",
      username = prefs.getString("sip_username", "") ?: "",
      secret = prefs.getString("sip_secret", "") ?: "",
      port = prefs.getString("sip_port", "5060") ?: "5060",
      transport = prefs.getString("sip_transport", "UDP") ?: "UDP"
    )
  }

  fun saveAccount(account: SipAccount) {
    prefs.edit().apply {
      putString("sip_server", account.server)
      putString("sip_username", account.username)
      putString("sip_secret", account.secret)
      putString("sip_port", account.port)
      putString("sip_transport", account.transport)
      apply()
    }
    _accountState.value = account
  }

  fun registerAccount() {
    val account = _accountState.value
    if (account.server.isBlank() || account.username.isBlank()) {
      _registrationState.value = "Unregistered"
      return
    }

    _registrationState.value = "Registering"
    
    // Simulate registration delays and handshakes
    handler.postDelayed({
      _registrationState.value = "Registered"
    }, 1800)
  }

  fun playDtmf(digit: Char) {
    val tone = when (digit) {
      '1' -> ToneGenerator.TONE_DTMF_1
      '2' -> ToneGenerator.TONE_DTMF_2
      '3' -> ToneGenerator.TONE_DTMF_3
      '4' -> ToneGenerator.TONE_DTMF_4
      '5' -> ToneGenerator.TONE_DTMF_5
      '6' -> ToneGenerator.TONE_DTMF_6
      '7' -> ToneGenerator.TONE_DTMF_7
      '8' -> ToneGenerator.TONE_DTMF_8
      '9' -> ToneGenerator.TONE_DTMF_9
      '0' -> ToneGenerator.TONE_DTMF_0
      '*' -> ToneGenerator.TONE_DTMF_S
      '#' -> ToneGenerator.TONE_DTMF_P
      else -> return
    }
    try {
      toneGenerator?.startTone(tone, 120)
    } catch (e: Exception) {
      // ignore tone error
    }
  }

  fun startCall(number: String) {
    if (number.isBlank()) return
    _activeCallNumber.value = number
    _callState.value = VoipCallState.DIALING
    _callDuration.value = 0

    // Play Ringback Tone
    playRingbackTone()

    // Phase 1: Dialing (3s) -> Ringing (3s) -> Connected
    simulationRunnable = object : Runnable {
      override fun run() {
        when (_callState.value) {
          VoipCallState.DIALING -> {
            _callState.value = VoipCallState.RINGING
            handler.postDelayed(this, 3000)
          }
          VoipCallState.RINGING -> {
            stopTones()
            _callState.value = VoipCallState.CONNECTED
            startDurationCounter()
          }
          else -> {}
        }
      }
    }
    handler.postDelayed(simulationRunnable!!, 3000)
  }

  fun endCall() {
    stopTones()
    simulationRunnable?.let { handler.removeCallbacks(it) }
    durationRunnable?.let { handler.removeCallbacks(it) }
    _callState.value = VoipCallState.DISCONNECTED
    handler.postDelayed({
      _callState.value = VoipCallState.IDLE
    }, 1500)
  }

  private fun playRingbackTone() {
    try {
      toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE)
    } catch (e: Exception) {
      // ignore
    }
  }

  private fun stopTones() {
    try {
      toneGenerator?.stopTone()
    } catch (e: Exception) {
      // ignore
    }
  }

  private fun startDurationCounter() {
    durationRunnable = object : Runnable {
      override fun run() {
        if (_callState.value == VoipCallState.CONNECTED) {
          _callDuration.value += 1
          handler.postDelayed(this, 1000)
        }
      }
    }
    handler.post(durationRunnable!!)
  }

  fun toggleMute() {
    val next = !_isMuted.value
    _isMuted.value = next
    audioManager.isMicrophoneMute = next
  }

  fun toggleSpeaker() {
    val next = !_isSpeakerOn.value
    _isSpeakerOn.value = next
    audioManager.isSpeakerphoneOn = next
  }

  fun setInputGain(gain: Float) {
    _inputGain.value = gain
    // In real VoIP this adjusts mic capture sensitivity / software multiplier
  }

  fun setOutputVolume(ratio: Float) {
    _outputVolume.value = ratio
    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
    val targetVol = (maxVol * ratio).toInt()
    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, targetVol, 0)
  }
}
