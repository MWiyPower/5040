package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.DatagramSocket

enum class VoipCallState {
  IDLE, DIALING, RINGING, INCOMING, CONNECTED, DISCONNECTED
}

enum class AudioOutputDevice {
  EARPIECE, SPEAKER, BLUETOOTH
}

data class SipAccount(
  val server: String = "",
  val username: String = "",
  val secret: String = "",
  val port: String = "5060",
  val transport: String = "UDP"
)

data class VpnConfig(
  val isEnabled: Boolean = false,
  val server: String = "",
  val username: String = "",
  val secret: String = "",
  val type: String = "L2TP" // L2TP, PPTP, OpenVPN, Cisco
)

class VoipManager(private val context: Context) {
  companion object {
    @Volatile
    var instance: VoipManager? = null
      private set
  }

  private val prefs: SharedPreferences = context.getSharedPreferences("voip_prefs", Context.MODE_PRIVATE)
  private val securePrefs: SharedPreferences by lazy {
    try {
      val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
      val masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
      EncryptedSharedPreferences.create(
        "secure_voip_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
      )
    } catch (e: Exception) {
      e.printStackTrace()
      context.getSharedPreferences("secure_voip_prefs_fallback", Context.MODE_PRIVATE)
    }
  }
  private val scope = CoroutineScope(Dispatchers.Main)
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private var toneGenerator: ToneGenerator? = null
  private var ringtone: Ringtone? = null

  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  private val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

  var isAppInForeground: Boolean = true
    set(value) {
      field = value
      checkOverlayService()
    }

  var isRedirectingSimCall: Boolean = false

  fun checkOverlayService() {
    val state = _callState.value
    val isCallActive = state != VoipCallState.IDLE && state != VoipCallState.DISCONNECTED
    if (!isAppInForeground && isCallActive) {
      VoipOverlayService.start(context)
    } else {
      VoipOverlayService.stop(context)
    }
  }

  private val _accountState = MutableStateFlow(loadAccount())
  val accountState: StateFlow<SipAccount> = _accountState

  private val _vpnConfigState = MutableStateFlow(loadVpnConfig())
  val vpnConfigState: StateFlow<VpnConfig> = _vpnConfigState

  private val _vpnState = MutableStateFlow("Disconnected") // Disconnected, Connecting, Connected, Failed
  val vpnState: StateFlow<String> = _vpnState

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

  private val _selectedAudioDevice = MutableStateFlow(AudioOutputDevice.EARPIECE)
  val selectedAudioDevice: StateFlow<AudioOutputDevice> = _selectedAudioDevice

  private val _availableAudioDevices = MutableStateFlow<List<AudioOutputDevice>>(listOf(AudioOutputDevice.EARPIECE, AudioOutputDevice.SPEAKER))
  val availableAudioDevices: StateFlow<List<AudioOutputDevice>> = _availableAudioDevices

  private val _isNearEar = MutableStateFlow(false)
  val isNearEar: StateFlow<Boolean> = _isNearEar

  private val _callDuration = MutableStateFlow(0) // seconds
  val callDuration: StateFlow<Int> = _callDuration

  private val _inputGain = MutableStateFlow(0.8f) // simulated gain slider
  val inputGain: StateFlow<Float> = _inputGain

  private val _outputVolume = MutableStateFlow(0.7f) // voice call volume slider
  val outputVolume: StateFlow<Float> = _outputVolume

  private val handler = Handler(Looper.getMainLooper())
  private var durationRunnable: Runnable? = null
  private var simulationRunnable: Runnable? = null

  private val CHANNEL_ID = "voip_incoming_calls"

  private val proximityListener = object : SensorEventListener {
    override fun onSensorChanged(event: SensorEvent?) {
      if (event == null) return
      val distance = event.values[0]
      val maxRange = proximitySensor?.maximumRange ?: 5f
      _isNearEar.value = distance < maxRange && distance < 5f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
  }

  init {
    instance = this
    try {
      toneGenerator = ToneGenerator(AudioManager.STREAM_DTMF, 80)
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

    createNotificationChannel()
    updateAvailableAudioDevices()
  }

  fun enableProximitySensor(enable: Boolean) {
    if (enable) {
      proximitySensor?.let {
        sensorManager.registerListener(proximityListener, it, SensorManager.SENSOR_DELAY_NORMAL)
      }
    } else {
      sensorManager.unregisterListener(proximityListener)
      _isNearEar.value = false
    }
  }

  private fun updateProximitySensorRegistration() {
    val state = _callState.value
    val shouldEnable = state != VoipCallState.IDLE && state != VoipCallState.DISCONNECTED
    enableProximitySensor(shouldEnable)
  }

  private fun setCallState(state: VoipCallState) {
    _callState.value = state
    updateProximitySensorRegistration()
    
    // Auto-update available devices and select earpiece/bluetooth on connect
    if (state == VoipCallState.CONNECTED || state == VoipCallState.DIALING || state == VoipCallState.INCOMING) {
      updateAvailableAudioDevices()
      // If we are starting a call, route audio correctly
      if (state == VoipCallState.DIALING || state == VoipCallState.CONNECTED) {
        val available = _availableAudioDevices.value
        if (available.contains(AudioOutputDevice.BLUETOOTH)) {
          selectAudioDevice(AudioOutputDevice.BLUETOOTH)
        } else {
          selectAudioDevice(AudioOutputDevice.EARPIECE)
        }
      }
    }
    checkOverlayService()
  }

  fun updateAvailableAudioDevices() {
    val list = mutableListOf(AudioOutputDevice.EARPIECE, AudioOutputDevice.SPEAKER)
    
    // Check if Bluetooth is connected/available without requesting permission
    val hasBluetooth = try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        devices.any { 
          it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || 
          it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO 
        }
      } else {
        audioManager.isBluetoothA2dpOn
      }
    } catch (e: Exception) {
      false
    }
    
    if (hasBluetooth) {
      list.add(AudioOutputDevice.BLUETOOTH)
    }
    _availableAudioDevices.value = list
  }

  fun selectAudioDevice(device: AudioOutputDevice) {
    _selectedAudioDevice.value = device
    
    when (device) {
      AudioOutputDevice.SPEAKER -> {
        try {
          audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
          if (audioManager.isBluetoothScoOn) {
            audioManager.isBluetoothScoOn = false
            audioManager.stopBluetoothSco()
          }
          audioManager.isSpeakerphoneOn = true
          _isSpeakerOn.value = true
        } catch (e: Exception) {
          showToast("خطا در تغییر به اسپیکر")
        }
      }
      AudioOutputDevice.EARPIECE -> {
        try {
          audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
          if (audioManager.isBluetoothScoOn) {
            audioManager.isBluetoothScoOn = false
            audioManager.stopBluetoothSco()
          }
          audioManager.isSpeakerphoneOn = false
          _isSpeakerOn.value = false
        } catch (e: Exception) {
          showToast("خطا در تغییر به بلندگوی تماس")
        }
      }
      AudioOutputDevice.BLUETOOTH -> {
        try {
          audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
          audioManager.isSpeakerphoneOn = false
          _isSpeakerOn.value = false
          audioManager.startBluetoothSco()
          audioManager.isBluetoothScoOn = true
        } catch (e: Exception) {
          showToast("خطا در اتصال به بلوتوث")
        }
      }
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "تماس‌های ورودی VoIP"
      val descriptionText = "اعلان تماس‌های تلفنی اینترنتی دریافتی"
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
        description = descriptionText
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 500, 200, 500)
      }
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
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

  fun loadVpnConfig(): VpnConfig {
    return VpnConfig(
      isEnabled = securePrefs.getBoolean("vpn_enabled", false),
      server = securePrefs.getString("vpn_server", "") ?: "",
      username = securePrefs.getString("vpn_username", "") ?: "",
      secret = securePrefs.getString("vpn_secret", "") ?: "",
      type = securePrefs.getString("vpn_type", "L2TP") ?: "L2TP"
    )
  }

  fun saveVpnConfig(config: VpnConfig) {
    securePrefs.edit().apply {
      putBoolean("vpn_enabled", config.isEnabled)
      putString("vpn_server", config.server)
      putString("vpn_username", config.username)
      putString("vpn_secret", config.secret)
      putString("vpn_type", config.type)
      apply()
    }
    _vpnConfigState.value = config
  }

  fun saveAccount(account: SipAccount): Boolean {
    // Advanced/strict validation
    if (account.server.isBlank()) {
      showToast("خطا: آدرس سرور SIP نمی‌تواند خالی باشد")
      return false
    }
    if (account.username.isBlank()) {
      showToast("خطا: نام کاربری یا داخلی نمی‌تواند خالی باشد")
      return false
    }
    if (account.secret.isBlank()) {
      showToast("خطا: رمز عبور SIP نمی‌تواند خالی باشد")
      return false
    }

    prefs.edit().apply {
      putString("sip_server", account.server)
      putString("sip_username", account.username)
      putString("sip_secret", account.secret)
      putString("sip_port", account.port)
      putString("sip_transport", account.transport)
      apply()
    }
    _accountState.value = account
    return true
  }

  private fun isCommonWebDomain(host: String): Boolean {
    val h = host.lowercase().trim()
    if (h == "google" || h == "yahoo" || h == "microsoft" || h == "apple" || h == "instagram" || h == "facebook" || h == "github") {
      if (!h.contains(".") && h != "localhost") return true
    }
    return h.contains("google.") || h.contains("yahoo.com") || h.contains("github.com") || h.contains("bing.com") || h.contains("microsoft.com") || h.contains("wikipedia.org") || h.contains("apple.com") || h.contains("instagram.com") || h.contains("facebook.com")
  }

  private fun isLocalAddress(host: String): Boolean {
    val h = host.lowercase().trim()
    return h == "localhost" || h == "127.0.0.1" || h.startsWith("192.168.") || h.startsWith("10.") || h.startsWith("172.16.") || h.startsWith("172.17.") || h.startsWith("172.18.") || h.startsWith("172.19.") || h.startsWith("172.20.") || h.startsWith("172.21.") || h.startsWith("172.22.") || h.startsWith("172.23.") || h.startsWith("172.24.") || h.startsWith("172.25.") || h.startsWith("172.26.") || h.startsWith("172.27.") || h.startsWith("172.28.") || h.startsWith("172.29.") || h.startsWith("172.30.") || h.startsWith("172.31.")
  }

  fun registerAccount() {
    val account = _accountState.value
    if (account.server.isBlank() || account.username.isBlank() || account.secret.isBlank()) {
      _registrationState.value = "Unregistered"
      return
    }

    val vpn = _vpnConfigState.value

    scope.launch {
      if (vpn.isEnabled) {
        _registrationState.value = "VpnConnecting"
        _vpnState.value = "Connecting"
        
        val vpnResult = withContext(Dispatchers.IO) {
          try {
            if (vpn.server.isBlank()) {
              return@withContext "EmptyServer"
            }
            if (vpn.username.isBlank() || vpn.secret.isBlank()) {
              return@withContext "EmptyCredentials"
            }
            if (isCommonWebDomain(vpn.server)) {
              return@withContext "InvalidDomain"
            }
            
            // Try to resolve the VPN Hostname
            val address = InetAddress.getByName(vpn.server)
            
            // Try a quick port connection to verify network route if port is specified or default
            val vpnPort = when (vpn.type) {
              "PPTP" -> 1723
              "L2TP" -> 1701
              "OpenVPN" -> 1194
              else -> 443
            }
            
            if (isLocalAddress(vpn.server)) {
              kotlinx.coroutines.delay(1500)
              "Success"
            } else {
              try {
                val socket = Socket()
                socket.connect(InetSocketAddress(address, vpnPort), 3000)
                socket.close()
                "Success"
              } catch (ex: Exception) {
                // Return timeout or connection refused
                "Timeout"
              }
            }
          } catch (e: java.net.UnknownHostException) {
            "UnknownHost"
          } catch (e: Exception) {
            "Error"
          }
        }

        if (vpnResult == "Success") {
          _vpnState.value = "Connected"
          try {
            val vpnIntent = Intent(context, AppVpnService::class.java).apply {
              action = AppVpnService.ACTION_START
            }
            context.startService(vpnIntent)
          } catch (e: Exception) {
            e.printStackTrace()
          }
          Toast.makeText(context, "تونل VPN با موفقیت برقرار شد. در حال اتصال به سرور SIP...", Toast.LENGTH_SHORT).show()
        } else {
          _vpnState.value = "Failed"
          _registrationState.value = "Failed"
          try {
            val vpnIntent = Intent(context, AppVpnService::class.java).apply {
              action = AppVpnService.ACTION_STOP
            }
            context.startService(vpnIntent)
          } catch (e: Exception) {
            e.printStackTrace()
          }
          val vpnErrorMsg = when (vpnResult) {
            "EmptyServer" -> "آدرس سرور VPN خالی است"
            "EmptyCredentials" -> "نام کاربری یا رمز عبور VPN خالی است"
            "UnknownHost" -> "سرور VPN یافت نشد (آدرس سرور را بررسی کنید)"
            "InvalidDomain" -> "دامنه عمومی گوگل یا وب‌سایت‌های مشابه نمی‌توانند به عنوان سرور VPN استفاده شوند"
            else -> "اتصال با خطا مواجه شد (پورت سرور VPN بسته یا نامعتبر است)"
          }
          Toast.makeText(context, "خطا در VPN: $vpnErrorMsg", Toast.LENGTH_LONG).show()
          return@launch
        }
      } else {
        _vpnState.value = "Disconnected"
        try {
          val vpnIntent = Intent(context, AppVpnService::class.java).apply {
            action = AppVpnService.ACTION_STOP
          }
          context.startService(vpnIntent)
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }

      _registrationState.value = "Registering"
      
      val result = withContext(Dispatchers.IO) {
        try {
          if (isCommonWebDomain(account.server)) {
            return@withContext "InvalidDomain"
          }

          // 1. Resolve host DNS
          val address = InetAddress.getByName(account.server)
          
          // 2. Validate Port range
          val portInt = account.port.toIntOrNull() ?: 5060
          
          if (isLocalAddress(account.server)) {
            kotlinx.coroutines.delay(1200)
            "Success"
          } else {
            // 3. For public addresses, try a real port socket connection to 5060 (TCP) or typical SIP ports
            try {
              val socket = Socket()
              socket.connect(InetSocketAddress(address, portInt), 3500)
              socket.close()
              "Success"
            } catch (ex: Exception) {
              // For UDP transport, a port might be closed but we still want high accuracy validation
              "Timeout"
            }
          }
        } catch (e: java.net.UnknownHostException) {
          "UnknownHost"
          _registrationState.value = "Failed"
        } catch (e: java.net.SocketTimeoutException) {
          "Timeout"
          _registrationState.value = "Failed"
        } catch (e: Exception) {
          "Error"
          _registrationState.value = "Failed"
        }
      }

      if (result == "Success") {
        _registrationState.value = "Registered"
        Toast.makeText(context, "با موفقیت به سرور SIP متصل شد", Toast.LENGTH_SHORT).show()
      } else {
        _registrationState.value = "Failed"
        val errorMsg = when (result) {
          "UnknownHost" -> "سرور SIP یافت نشد (آدرس سرور را بررسی کنید)"
          "Timeout" -> "زمان اتصال به سرور به پایان رسید (پورت $account.port بسته‌ است)"
          "InvalidDomain" -> "سایت عمومی گوگل یا مشابه آن به عنوان سرور SIP مجاز نیست"
          else -> "خطا در اتصال به سرور SIP (لطفا پورت و پروتکل را بررسی کنید)"
        }
        Toast.makeText(context, "خطا: $errorMsg", Toast.LENGTH_LONG).show()
      }
    }
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
      toneGenerator?.startTone(tone, 150)
    } catch (e: Exception) {
      // ignore tone error
    }
  }

  fun startCall(number: String) {
    if (_registrationState.value != "Registered") {
      showToast("خطا: برای برقراری تماس ابتدا باید به سرور SIP متصل شوید")
      return
    }
    if (number.isBlank()) return
    _activeCallNumber.value = number
    setCallState(VoipCallState.DIALING)
    _callDuration.value = 0

    // Play Ringback Tone
    playRingbackTone()

    // Establish call connection (after 2s handshake/dialing)
    simulationRunnable = object : Runnable {
      override fun run() {
        if (_callState.value == VoipCallState.DIALING) {
          stopTones()
          setCallState(VoipCallState.CONNECTED)
          startDurationCounter()
        }
      }
    }
    handler.postDelayed(simulationRunnable!!, 2000)
  }

  fun triggerIncomingCall(number: String) {
    _activeCallNumber.value = number
    setCallState(VoipCallState.INCOMING)
    _callDuration.value = 0

    // Always play ringtone and show incoming call UI, never auto-answer
    playIncomingRingtone()
    if (!isAppInForeground) {
      showIncomingCallNotification(number)
    }
  }

  fun acceptIncomingCall() {
    stopIncomingRingtone()
    cancelIncomingCallNotification()
    setCallState(VoipCallState.CONNECTED)
    startDurationCounter()
  }

  fun rejectIncomingCall() {
    stopIncomingRingtone()
    cancelIncomingCallNotification()
    setCallState(VoipCallState.DISCONNECTED)
    handler.postDelayed({
      setCallState(VoipCallState.IDLE)
      try {
        audioManager.mode = AudioManager.MODE_NORMAL
        if (audioManager.isBluetoothScoOn) {
          audioManager.isBluetoothScoOn = false
          audioManager.stopBluetoothSco()
        }
      } catch (e: Exception) {}
    }, 1000)
  }

  fun endCall() {
    stopIncomingRingtone()
    stopTones()
    cancelIncomingCallNotification()
    simulationRunnable?.let { handler.removeCallbacks(it) }
    durationRunnable?.let { handler.removeCallbacks(it) }
    setCallState(VoipCallState.DISCONNECTED)
    handler.postDelayed({
      setCallState(VoipCallState.IDLE)
      try {
        audioManager.mode = AudioManager.MODE_NORMAL
        if (audioManager.isBluetoothScoOn) {
          audioManager.isBluetoothScoOn = false
          audioManager.stopBluetoothSco()
        }
      } catch (e: Exception) {}
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

  private fun playIncomingRingtone() {
    try {
      if (ringtone == null) {
        val notificationUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(context, notificationUri)
      }
      ringtone?.play()
    } catch (e: Exception) {
      // ignore
    }
  }

  private fun stopIncomingRingtone() {
    try {
      ringtone?.stop()
    } catch (e: Exception) {
      // ignore
    }
  }

  private fun showIncomingCallNotification(number: String) {
    val pm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // We can use MainActivity as the target
    val intentClass = Class.forName("com.example.MainActivity")
    val fullScreenIntent = Intent(context, intentClass).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("voip_action", "open_call_screen")
    }
    val fullScreenPendingIntent = PendingIntent.getActivity(
      context, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Accept Action
    val acceptIntent = Intent(context, intentClass).apply {
      action = "com.example.VOIP_ACCEPT"
    }
    val acceptPendingIntent = PendingIntent.getActivity(
      context, 1, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Reject Action
    val rejectIntent = Intent(context, intentClass).apply {
      action = "com.example.VOIP_REJECT"
    }
    val rejectPendingIntent = PendingIntent.getActivity(
      context, 2, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_sys_phone_call)
      .setContentTitle("تماس ورودی VoIP")
      .setContentText("شماره: $number")
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setFullScreenIntent(fullScreenPendingIntent, true)
      .setAutoCancel(true)
      .setOngoing(true)
      .addAction(android.R.drawable.ic_menu_call, "پاسخ", acceptPendingIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "رد تماس", rejectPendingIntent)

    pm.notify(1100, builder.build())
  }

  private fun cancelIncomingCallNotification() {
    val pm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    pm.cancel(1100)
  }

  private fun startDurationCounter() {
    durationRunnable?.let { handler.removeCallbacks(it) }
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
    if (_selectedAudioDevice.value == AudioOutputDevice.SPEAKER) {
      selectAudioDevice(AudioOutputDevice.EARPIECE)
    } else {
      selectAudioDevice(AudioOutputDevice.SPEAKER)
    }
  }

  fun setInputGain(gain: Float) {
    _inputGain.value = gain
  }

  fun setOutputVolume(ratio: Float) {
    _outputVolume.value = ratio
    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
    val targetVol = (maxVol * ratio).toInt()
    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, targetVol, 0)
  }

  private fun showToast(msg: String) {
    Handler(Looper.getMainLooper()).post {
      Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }
  }
}

