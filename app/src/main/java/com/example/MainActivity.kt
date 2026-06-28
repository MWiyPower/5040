package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.Context
import android.content.IntentFilter
import android.content.ClipboardManager
import android.content.ClipData
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.webkit.JavascriptInterface
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme

private const val JS_BYPASS_WARNINGS = "javascript:(function() { " +
    "try { " +
    "  var meta = document.createElement('meta'); " +
    "  meta.name = 'viewport'; " +
    "  meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, shrink-to-fit=no'; " +
    "  document.head.appendChild(meta); " +
    "} catch(e) {} " +
    "})();"

private const val JS_NOTIFICATION_AND_THEME = "javascript:(function() { " +
    "try { " +
    "  var sendNotify = function(title, options) { " +
    "    try { " +
    "      var bodyText = ''; " +
    "      if (options && options.body) { bodyText = options.body; } " +
    "      if (window.AndroidApp) { " +
    "        if (window.AndroidApp.onNewMessageWithDetails) { " +
    "          window.AndroidApp.onNewMessageWithDetails(title, bodyText); " +
    "        } else if (window.AndroidApp.onNewMessage) { " +
    "          window.AndroidApp.onNewMessage(); " +
    "        } " +
    "      } " +
    "    } catch(e) {} " +
    "  }; " +
    "  if (!window.Notification) { " +
    "    window.Notification = function(title, options) { " +
    "      sendNotify(title, options); " +
    "    }; " +
    "    window.Notification.permission = 'granted'; " +
    "    window.Notification.requestPermission = function(cb) { if (cb) cb('granted'); return Promise.resolve('granted'); }; " +
    "  } else { " +
    "    var OrgNotification = window.Notification; " +
    "    window.Notification = function(title, options) { " +
    "      sendNotify(title, options); " +
    "      try { return new OrgNotification(title, options); } catch(e) { return {}; } " +
    "    }; " +
    "    Object.assign(window.Notification, OrgNotification); " +
    "  } " +
    "  if (navigator.serviceWorker) { " +
    "    navigator.serviceWorker.ready.then(function(reg) { " +
    "      var orgShow = reg.showNotification; " +
    "      reg.showNotification = function(title, options) { " +
    "        sendNotify(title, options); " +
    "        if (orgShow) { return orgShow.apply(reg, arguments); } " +
    "        return Promise.resolve(); " +
    "      }; " +
    "    }).catch(function(e){}); " +
    "  } " +
    "  var target = document.querySelector('title') || document.head; " +
    "  if (target) { " +
    "    var observer = new MutationObserver(function(mutations) { " +
    "      var title = document.title; " +
    "      var match = title.match(/\\((\\d+)\\)/); " +
    "      if (match && parseInt(match[1]) > 0) { " +
    "        if (window.AndroidApp) { " +
    "          if (window.AndroidApp.onNewMessageWithDetails) { " +
    "            window.AndroidApp.onNewMessageWithDetails('پیام جدید', 'شما پیام‌های خوانده نشده دارید'); " +
    "          } else if (window.AndroidApp.onNewMessage) { " +
    "            window.AndroidApp.onNewMessage(); " +
    "          } " +
    "        } " +
    "      } " +
    "    }); " +
    "    observer.observe(target, { subtree: true, characterData: true, childList: true }); " +
    "  } " +
    "  try { " +
    "    var currentTheme = 'light'; " +
    "    var pData = localStorage.getItem('private-data'); " +
    "    if (pData) { " +
    "      var pValue = JSON.parse(pData); " +
    "      if (pValue && pValue.settings && pValue.settings.theme) { " +
    "        currentTheme = pValue.settings.theme; " +
    "      } " +
    "    } " +
    "    if (document.querySelector('link[href*=\"dark\"]')) { " +
    "      currentTheme = 'dark'; " +
    "    } " +
    "    if (window.AndroidApp && window.AndroidApp.onWebThemeDetected) { " +
    "      window.AndroidApp.onWebThemeDetected(currentTheme); " +
    "    } " +
    "  } catch(e) {} " +
    "  try { " +
    "    var addFocusListener = function() { " +
    "      var inputs = document.querySelectorAll('input[type=\"password\"]'); " +
    "      for (var i = 0; i < inputs.length; i++) { " +
    "        var input = inputs[i]; " +
    "        if (!input.dataset.hasCredentialsListener) { " +
    "          input.dataset.hasCredentialsListener = \"true\"; " +
    "          input.addEventListener('focus', function() { " +
    "            if (window.AndroidApp && window.AndroidApp.showCredentials) { " +
    "              window.AndroidApp.showCredentials(); " +
    "            } " +
    "          }); " +
    "        } " +
    "      } " +
    "    }; " +
    "    addFocusListener(); " +
    "    var focusObserver = new MutationObserver(addFocusListener); " +
    "    focusObserver.observe(document.documentElement, { childList: true, subtree: true }); " +
    "  } catch(e) {} " +
    "  try { " +
    "    var checkProfile = function() { " +
    "      try { " +
    "        var hints = document.querySelectorAll('.panel-item-hint'); " +
    "        for (var i = 0; i < hints.length; i++) { " +
    "          var hint = hints[i]; " +
    "          if (hint.textContent && hint.textContent.trim().toLowerCase() === 'dark mode') { " +
    "            var parent = hint.closest('.panel-item'); " +
    "            if (parent) { " +
    "              parent.style.setProperty('display', 'none', 'important'); " +
    "            } " +
    "          } " +
    "        } " +
    "      } catch(e) {} " +
    "      var imgEl = document.querySelector('.avatar img'); " +
    "      var avatarUrl = ''; " +
    "      if (imgEl && imgEl.src) { " +
    "        avatarUrl = imgEl.src; " +
    "      } else { " +
    "        var avDiv = document.querySelector('.avatar'); " +
    "        if (avDiv) { " +
    "          var bg = avDiv.style.backgroundImage; " +
    "          if (bg && bg.indexOf('url') !== -1) { " +
    "            var match = bg.match(/url\\s*\\(\\s*['\"]?([^'\"]+)['\"]?\\s*\\)/); " +
    "            if (match && match[1]) { " +
    "              avatarUrl = match[1]; " +
    "            } " +
    "          } " +
    "        } " +
    "      } " +
    "      var nameEl = document.querySelector('.welcomeName'); " +
    "      var fullName = ''; " +
    "      if (nameEl) { " +
    "        fullName = nameEl.textContent || nameEl.innerText || ''; " +
    "      } " +
    "      if (window.AndroidApp && window.AndroidApp.onUserProfileDetected && (fullName || avatarUrl)) { " +
    "        window.AndroidApp.onUserProfileDetected(fullName, avatarUrl); " +
    "      } " +
    "    }; " +
    "    checkProfile(); " +
    "    var profileObserver = new MutationObserver(checkProfile); " +
    "    profileObserver.observe(document.documentElement, { childList: true, subtree: true, characterData: true }); " +
    "  } catch(e) {} " +
    "  var styleMobile = document.createElement('style'); " +
    "  styleMobile.type = 'text/css'; " +
    "  styleMobile.innerHTML = 'html, body { max-width: 100% !important; overflow-x: hidden !important; } .container, .main-container { width: 100% !important; max-width: 100% !important; }'; " +
    "  document.head.appendChild(styleMobile); " +
    "} catch(e) {} " +
    "})();"

private const val JS_PERSIST_SESSION = "javascript:(function() { " +
    "try { " +
    "  for (var i = 0; i < localStorage.length; i++) { " +
    "    var key = localStorage.key(i); " +
    "    if (key && key.indexOf('_ss_backup_') === 0) { " +
    "      var origKey = key.substring(11); " +
    "      if (!sessionStorage.getItem(origKey)) { " +
    "        sessionStorage.setItem(origKey, localStorage.getItem(key)); " +
    "      } " +
    "    } " +
    "  } " +
    "} catch(e) {} " +
    "try { " +
    "  var orgSet = sessionStorage.setItem; " +
    "  sessionStorage.setItem = function(k, v) { " +
    "    orgSet.apply(this, arguments); " +
    "    try { localStorage.setItem('_ss_backup_' + k, v); } catch(e) {} " +
    "  }; " +
    "  var orgRem = sessionStorage.removeItem; " +
    "  sessionStorage.removeItem = function(k) { " +
    "    orgRem.apply(this, arguments); " +
    "    try { localStorage.removeItem('_ss_backup_' + k); } catch(e) {} " +
    "  }; " +
    "  var orgClr = sessionStorage.clear; " +
    "  sessionStorage.clear = function() { " +
    "    orgClr.apply(this, arguments); " +
    "    try { " +
    "      var toDel = []; " +
    "      for (var i = 0; i < localStorage.length; i++) { " +
    "        var k = localStorage.key(i); " +
    "        if (k && k.indexOf('_ss_backup_') === 0) { toDel.push(k); } " +
    "      } " +
    "      for (var j = 0; j < toDel.length; j++) { localStorage.removeItem(toDel[j]); } " +
    "    } catch(e) {} " +
    "  }; " +
    "} catch(e) {} " +
    "})();"

class MainActivity : ComponentActivity() {

  companion object {
    var isAppInForeground = false
  }

  private var filePathCallback: ValueCallback<Array<Uri>>? = null
  private var permissionRequestCallback: PermissionRequest? = null
  private var simCallReceiver: SimCallReceiver? = null
  lateinit var voipManager: VoipManager

  private val filePickerLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val uris = if (result.resultCode == Activity.RESULT_OK) {
      val data = result.data
      var results: Array<Uri>? = null
      if (data != null) {
        val dataString = data.dataString
        val clipData = data.clipData
        if (clipData != null) {
          results = Array(clipData.itemCount) { i ->
            clipData.getItemAt(i).uri
          }
        } else if (dataString != null) {
          results = arrayOf(Uri.parse(dataString))
        }
      }
      results
    } else {
      null
    }
    filePathCallback?.onReceiveValue(uris)
    filePathCallback = null
  }

  private val permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val grantedList = ArrayList<String>()
    permissions.forEach { (permission, isGranted) ->
      if (isGranted) {
        if (permission == android.Manifest.permission.CAMERA) {
          grantedList.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
        }
        if (permission == android.Manifest.permission.RECORD_AUDIO) {
          grantedList.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        }
      }
    }
    if (grantedList.isNotEmpty()) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        permissionRequestCallback?.grant(grantedList.toTypedArray())
      }
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        permissionRequestCallback?.deny()
      }
    }
    permissionRequestCallback = null
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    voipManager = VoipManager(applicationContext)
    handleVoipIntent(intent)

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          MainScreen(
            voipManager = voipManager,
            onOpenFilePicker = { callback, acceptTypes, allowMultiple ->
              filePathCallback?.onReceiveValue(null)
              filePathCallback = callback
              val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                if (acceptTypes != null && acceptTypes.isNotEmpty()) {
                  putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes)
                }
                if (allowMultiple) {
                  putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
              }
              filePickerLauncher.launch(Intent.createChooser(intent, "انتخاب فایل"))
            },
            onPermissionRequest = { request ->
              permissionRequestCallback = request
              val androidPermissions = ArrayList<String>()
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (resource in request.resources) {
                  if (resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                    androidPermissions.add(android.Manifest.permission.CAMERA)
                  }
                  if (resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE) {
                    androidPermissions.add(android.Manifest.permission.RECORD_AUDIO)
                  }
                }
              }
              if (androidPermissions.isNotEmpty()) {
                permissionLauncher.launch(androidPermissions.toTypedArray())
              } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                  request.grant(request.resources)
                }
              }
            }
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleVoipIntent(intent)
  }

  private fun handleVoipIntent(intent: Intent?) {
    if (intent == null) return
    if (intent.action == "com.example.VOIP_ACCEPT") {
      voipManager.acceptIncomingCall()
    } else if (intent.action == "com.example.VOIP_REJECT") {
      voipManager.rejectIncomingCall()
    } else if (intent.hasExtra("trigger_voip_incoming")) {
      val num = intent.getStringExtra("trigger_voip_incoming") ?: "SERVICE"
      voipManager.triggerIncomingCall(num)
    }
  }

  override fun onResume() {
    super.onResume()
    isAppInForeground = true
    if (::voipManager.isInitialized) {
      voipManager.isAppInForeground = true
    }
    try {
      if (simCallReceiver == null) {
        simCallReceiver = SimCallReceiver()
      }
      val filter = IntentFilter(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(simCallReceiver, filter, Context.RECEIVER_EXPORTED)
      } else {
        registerReceiver(simCallReceiver, filter)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onPause() {
    super.onPause()
    isAppInForeground = false
    if (::voipManager.isInitialized) {
      voipManager.isAppInForeground = false
    }
    try {
      simCallReceiver?.let {
        unregisterReceiver(it)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    try {
      android.webkit.CookieManager.getInstance().flush()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}

private fun showSystemNotification(context: Context, title: String, body: String) {
  val channelId = "chat_notifications_channel"
  val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

  if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
    val channel = android.app.NotificationChannel(
      channelId,
      "اعلان‌های چت",
      android.app.NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = "اعلان پیام‌های جدید چت پشتیبانی"
    }
    notificationManager.createNotificationChannel(channel)
  }

  val intent = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
  }
  val pendingIntent = android.app.PendingIntent.getActivity(
    context,
    101,
    intent,
    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
  )

  val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
    .setContentTitle(title)
    .setContentText(body)
    .setSmallIcon(com.example.R.mipmap.ic_launcher)
    .setAutoCancel(true)
    .setContentIntent(pendingIntent)
    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
    .build()

  notificationManager.notify(102, notification)
}

class CircularRevealShape(
  private val progress: Float,
  private val centerOffset: Offset
) : Shape {
  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density
  ): Outline {
    if (progress <= 0f) {
      return Outline.Generic(Path())
    }
    if (progress >= 0.96f) {
      return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
    }
    val center = if (centerOffset == Offset.Unspecified || centerOffset == Offset.Zero) {
      Offset(size.width * 0.85f, size.height * 0.85f)
    } else {
      centerOffset
    }
    val maxRadius = kotlin.math.hypot(
      kotlin.math.max(center.x, size.width - center.x),
      kotlin.math.max(center.y, size.height - center.y)
    )
    val radius = maxRadius * progress
    val path = Path().apply {
      addOval(
        Rect(
          left = center.x - radius,
          top = center.y - radius,
          right = center.x + radius,
          bottom = center.y + radius
        )
      )
    }
    return Outline.Generic(path)
  }
}

private fun isOnline(context: Context): Boolean {
  val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    val nw = cm.activeNetwork ?: return false
    val actNw = cm.getNetworkCapabilities(nw) ?: return false
    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
  } else {
    @Suppress("DEPRECATION")
    val nwInfo = cm.activeNetworkInfo ?: return false
    @Suppress("DEPRECATION")
    nwInfo.isConnected
  }
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureWebViewSettings(webView: WebView) {
  webView.apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
    }
    settings.apply {
      javaScriptEnabled = true
      domStorageEnabled = true
      databaseEnabled = true
      allowFileAccess = true
      allowContentAccess = true
      javaScriptCanOpenWindowsAutomatically = true
      saveFormData = true
      
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
      }
      cacheMode = WebSettings.LOAD_DEFAULT
      setSupportZoom(true)
      builtInZoomControls = true
      displayZoomControls = false
      textZoom = 100
      useWideViewPort = true
      loadWithOverviewMode = true
      userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    CookieManager.getInstance().apply {
      setAcceptCookie(true)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        setAcceptThirdPartyCookies(webView, true)
      }
    }
  }
}

@Composable
fun MainScreen(
  voipManager: VoipManager,
  onOpenFilePicker: (ValueCallback<Array<Uri>>?, Array<String>?, Boolean) -> Unit,
  onPermissionRequest: (PermissionRequest) -> Unit
) {
  val systemDark = isSystemInDarkTheme()
  var isMainLoaded by remember { mutableStateOf(false) }
  var isChatLoaded by remember { mutableStateOf(false) }

  var chatExpanded by remember { mutableStateOf(false) }
  var chatInitialized by remember { mutableStateOf(false) }
  var hasNewChatMessage by remember { mutableStateOf(false) }

  var isOnlineState by remember { mutableStateOf(true) }
  var hasWebLoadError by remember { mutableStateOf(false) }
  var detectedWebTheme by remember { mutableStateOf("light") }

  var fabPosition by remember { mutableStateOf(Offset(900f, 1800f)) }
  var showCredentialsSheet by remember { mutableStateOf(false) }
  var showFullPasswordManager by remember { mutableStateOf(false) }
  var showVoipSheet by remember { mutableStateOf(false) }
  var isDrawerOpen by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var userFullName by remember { mutableStateOf<String?>(null) }
  var userAvatarUrl by remember { mutableStateOf<String?>(null) }

  val context = LocalContext.current
  val voipCallState by voipManager.callState.collectAsState()
  val voipCallDuration by voipManager.callDuration.collectAsState()

  LaunchedEffect(context) {
    isOnlineState = isOnline(context)
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCallback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        isOnlineState = true
      }
      override fun onLost(network: Network) {
        isOnlineState = false
      }
    }
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
      } else {
        val builder = NetworkRequest.Builder()
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
      }
    } catch (e: Exception) {
      // safe fallback
    }
  }

  val bubbleProgress by animateFloatAsState(
    targetValue = if (chatExpanded) 1f else 0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "BubbleReveal"
  )

  val iconRotation = 0f

  val fabYOffset by animateDpAsState(
    targetValue = if (chatExpanded) (-48).dp else 0.dp,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "FabYOffset"
  )

  LaunchedEffect(chatExpanded) {
    if (chatExpanded) {
      chatInitialized = true
      hasNewChatMessage = false
    }
  }

  var mainWebViewRef by remember { mutableStateOf<WebView?>(null) }
  var chatWebViewRef by remember { mutableStateOf<WebView?>(null) }

  val coroutineScope = rememberCoroutineScope()
  var appUpdateState by remember { mutableStateOf<AppUpdateState>(AppUpdateState.CheckingConfig) }

  val startupPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissionsMap ->
    coroutineScope.launch {
      try {
        appUpdateState = AppUpdateState.CheckingConfig
        val config = fetchConfigs()
        if (config != null && config.update) {
          val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
          val currentVersion = packageInfo.versionName ?: "1.0.0"
          
          if (isVersionOlder(currentVersion, config.updateVersion)) {
            appUpdateState = AppUpdateState.Downloading(0L, 0L)
            
            val apkFile = downloadApk(context, config.updateUrl) { downloaded, total ->
              appUpdateState = AppUpdateState.Downloading(downloaded, total)
            }
            
            if (apkFile != null) {
              appUpdateState = AppUpdateState.ReadyToInstall(apkFile)
              installApk(context, apkFile)
            } else {
              appUpdateState = AppUpdateState.Idle
            }
          } else {
            appUpdateState = AppUpdateState.Idle
          }
        } else {
          appUpdateState = AppUpdateState.Idle
        }
      } catch (e: Exception) {
        e.printStackTrace()
        appUpdateState = AppUpdateState.Idle
      }
    }
  }

  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner, appUpdateState) {
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
      if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        val state = appUpdateState
        if (state is AppUpdateState.ReadyToInstall) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context.packageManager.canRequestPackageInstalls()) {
              installApk(context, state.apkFile)
            }
          } else {
            installApk(context, state.apkFile)
          }
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  LaunchedEffect(Unit) {
    try {
      // Background loop to periodically flush cookies so they persist across updates/kills
      coroutineScope.launch {
        while (isActive) {
          try {
            android.webkit.CookieManager.getInstance().flush()
          } catch (e: Exception) {
            e.printStackTrace()
          }
          delay(5000)
        }
      }

      val needed = mutableListOf<String>().apply {
        add(android.Manifest.permission.READ_PHONE_STATE)
        add(android.Manifest.permission.RECORD_AUDIO)
        add(android.Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          add(android.Manifest.permission.READ_CALL_LOG)
          add(android.Manifest.permission.ANSWER_PHONE_CALLS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
      }
      val missing = needed.filter { androidx.core.content.ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
      
      if (missing.isNotEmpty()) {
        appUpdateState = AppUpdateState.PermissionsRequired
      } else {
        appUpdateState = AppUpdateState.CheckingConfig
        
        val config = fetchConfigs()
        if (config != null && config.update) {
          val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
          val currentVersion = packageInfo.versionName ?: "1.0.0"
          
          if (isVersionOlder(currentVersion, config.updateVersion)) {
            appUpdateState = AppUpdateState.Downloading(0L, 0L)
            
            val apkFile = downloadApk(context, config.updateUrl) { downloaded, total ->
              appUpdateState = AppUpdateState.Downloading(downloaded, total)
            }
            
            if (apkFile != null) {
              appUpdateState = AppUpdateState.ReadyToInstall(apkFile)
              installApk(context, apkFile)
            } else {
              appUpdateState = AppUpdateState.Idle
            }
          } else {
            appUpdateState = AppUpdateState.Idle
          }
        } else {
          appUpdateState = AppUpdateState.Idle
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
      appUpdateState = AppUpdateState.Idle
    }
  }

  DisposableEffect(context) {
    val smsReceiverObj = object : android.content.BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {
          val bundle = intent.extras
          if (bundle != null) {
            try {
              val pdus = bundle.get("pdus") as? Array<*>
              if (pdus != null) {
                for (pdu in pdus) {
                  val format = bundle.getString("format")
                  val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    android.telephony.SmsMessage.createFromPdu(pdu as ByteArray, format)
                  } else {
                    @Suppress("DEPRECATION")
                    android.telephony.SmsMessage.createFromPdu(pdu as ByteArray)
                  }
                  val sender = message.displayOriginatingAddress ?: ""
                  val body = message.displayMessageBody ?: ""
                  
                  if (sender.contains("50003784563")) {
                    val match = Regex("کد ورود به سیستم:\\s*(\\d+)").find(body)
                    val code = match?.groupValues?.getOrNull(1)
                    if (code != null) {
                      val js = String.format(
                        "javascript:(function() { " +
                        "var inputs = document.querySelectorAll('input'); " +
                        "for (var i = 0; i < inputs.length; i++) { " +
                        "  var input = inputs[i]; " +
                        "  var type = input.getAttribute('type') || ''; " +
                        "  if (type === 'text' || type === 'number' || type === 'tel' || type === '') { " +
                        "    if (!input.value || input.value.trim() === '') { " +
                        "      input.value = '%s'; " +
                        "      input.dispatchEvent(new Event('input', { bubbles: true })); " +
                        "      input.dispatchEvent(new Event('change', { bubbles: true })); " +
                        "      break; " +
                        "    } " +
                        "  } " +
                        "} " +
                        "})()",
                        code
                      )
                      mainWebViewRef?.post {
                        mainWebViewRef?.loadUrl(js)
                        Toast.makeText(context, "کد تایید به صورت خودکار وارد شد: $code", Toast.LENGTH_LONG).show()
                      }
                    }
                  }
                }
              }
            } catch (e: Exception) {
              e.printStackTrace()
            }
          }
        }
      }
    }
    val filter = android.content.IntentFilter("android.provider.Telephony.SMS_RECEIVED")
    context.registerReceiver(smsReceiverObj, filter)
    onDispose {
      try {
        context.unregisterReceiver(smsReceiverObj)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
  val mainWebView = remember {
    WebView(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      configureWebViewSettings(this)
      
      addJavascriptInterface(WebAppInterface(
        onShowCredentials = {
          (context as? Activity)?.runOnUiThread {
            showCredentialsSheet = true
          }
        },
        onNewMessage = {
          (context as? Activity)?.runOnUiThread {
            if (!chatExpanded) {
              hasNewChatMessage = true
            }
          }
        },
        onNewMessageWithDetails = { title, body ->
          (context as? Activity)?.runOnUiThread {
            if (!chatExpanded) {
              hasNewChatMessage = true
            }
            if (!MainActivity.isAppInForeground) {
              showSystemNotification(context, title, body)
            }
          }
        },
        onWebThemeDetected = { theme ->
          (context as? Activity)?.runOnUiThread {
            detectedWebTheme = theme
          }
        },
        onUserProfileDetected = { name, avatar ->
          (context as? Activity)?.runOnUiThread {
            if (name.isNotEmpty()) {
              userFullName = name
            }
            if (avatar.isNotEmpty()) {
              userAvatarUrl = avatar
            }
          }
        }
      ), "AndroidApp")
      
      webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
          super.onPageStarted(view, url, favicon)
          view?.loadUrl(JS_PERSIST_SESSION)
          
          val isSystemDark = systemDark
          val earlyThemeJs = "javascript:(function() { " +
              "try { " +
              "  var wantDark = $isSystemDark; " +
              "  var changed = false; " +
              "  var pData = localStorage.getItem('private-data') || '{}'; " +
              "  var pValue = JSON.parse(pData) || {}; " +
              "  if (!pValue.settings) pValue.settings = {}; " +
              "  var current = pValue.settings.theme || 'light'; " +
              "  if (wantDark && current !== 'dark') { " +
              "    pValue.settings.theme = 'dark'; " +
              "    localStorage.setItem('private-data', JSON.stringify(pValue)); " +
              "    changed = true; " +
              "  } else if (!wantDark && current === 'dark') { " +
              "    pValue.settings.theme = 'light'; " +
              "    localStorage.setItem('private-data', JSON.stringify(pValue)); " +
              "    changed = true; " +
              "  } " +
              "  var directTheme = localStorage.getItem('theme'); " +
              "  if (wantDark && directTheme !== 'dark') { " +
              "    localStorage.setItem('theme', 'dark'); " +
              "    changed = true; " +
              "  } else if (!wantDark && directTheme === 'dark') { " +
              "    localStorage.setItem('theme', 'light'); " +
              "    changed = true; " +
              "  } " +
              "  if (wantDark) { " +
              "    document.documentElement.classList.add('dark'); " +
              "    document.documentElement.setAttribute('data-theme', 'dark'); " +
              "  } else { " +
              "    document.documentElement.classList.remove('dark'); " +
              "    document.documentElement.setAttribute('data-theme', 'light'); " +
              "  } " +
              "  if (changed) { " +
              "    window.location.reload(); " +
              "  } " +
              "} catch(e) {} " +
              "})();"
          view?.loadUrl(earlyThemeJs)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
          super.onPageFinished(view, url)
          isMainLoaded = true
          CookieManager.getInstance().flush()
          
          // Sync system dark theme to WebView localStorage
          val isSystemDark = systemDark
          val themeJs = "javascript:(function() { " +
              "try { " +
              "  var wantDark = $isSystemDark; " +
              "  var changed = false; " +
              "  var pData = localStorage.getItem('private-data') || '{}'; " +
              "  var pValue = JSON.parse(pData) || {}; " +
              "  if (!pValue.settings) pValue.settings = {}; " +
              "  var current = pValue.settings.theme || 'light'; " +
              "  if (wantDark && current !== 'dark') { " +
              "    pValue.settings.theme = 'dark'; " +
              "    localStorage.setItem('private-data', JSON.stringify(pValue)); " +
              "    changed = true; " +
              "  } else if (!wantDark && current === 'dark') { " +
              "    pValue.settings.theme = 'light'; " +
              "    localStorage.setItem('private-data', JSON.stringify(pValue)); " +
              "    changed = true; " +
              "  } " +
              "  var directTheme = localStorage.getItem('theme'); " +
              "  if (wantDark && directTheme !== 'dark') { " +
              "    localStorage.setItem('theme', 'dark'); " +
              "    changed = true; " +
              "  } else if (!wantDark && directTheme === 'dark') { " +
              "    localStorage.setItem('theme', 'light'); " +
              "    changed = true; " +
              "  } " +
              "  if (wantDark) { " +
              "    document.documentElement.classList.add('dark'); " +
              "    document.documentElement.setAttribute('data-theme', 'dark'); " +
              "  } else { " +
              "    document.documentElement.classList.remove('dark'); " +
              "    document.documentElement.setAttribute('data-theme', 'light'); " +
              "  } " +
              "  if (changed) { " +
              "    window.location.reload(); " +
              "  } " +
              "} catch(e) {} " +
              "})();"
          view?.loadUrl(themeJs)
          
          view?.loadUrl(JS_PERSIST_SESSION)
          view?.loadUrl(JS_BYPASS_WARNINGS)
          view?.loadUrl(JS_NOTIFICATION_AND_THEME)
        }

        override fun onReceivedError(
          view: WebView?,
          errorCode: Int,
          description: String?,
          failingUrl: String?
        ) {
          super.onReceivedError(view, errorCode, description, failingUrl)
          if (failingUrl?.startsWith("https://panel.5040.me") == true || view?.url == failingUrl) {
            hasWebLoadError = true
          }
        }

        override fun onReceivedError(
          view: WebView?,
          request: WebResourceRequest?,
          error: WebResourceError?
        ) {
          super.onReceivedError(view, request, error)
          if (request?.isForMainFrame == true) {
            hasWebLoadError = true
          }
        }

        override fun onReceivedHttpError(
          view: WebView?,
          request: WebResourceRequest?,
          errorResponse: WebResourceResponse?
        ) {
          super.onReceivedHttpError(view, request, errorResponse)
          if (request?.isForMainFrame == true) {
            val status = errorResponse?.statusCode ?: 200
            if (status >= 400) {
              hasWebLoadError = true
            }
          }
        }
      }
      webChromeClient = object : WebChromeClient() {
        override fun onShowFileChooser(
          webView: WebView?,
          filePathCallback: ValueCallback<Array<Uri>>?,
          fileChooserParams: FileChooserParams?
        ): Boolean {
          val acceptTypes = fileChooserParams?.acceptTypes
          val allowMultiple = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
          } else {
            false
          }
          onOpenFilePicker(filePathCallback, acceptTypes, allowMultiple)
          return true
        }

        override fun onPermissionRequest(request: PermissionRequest) {
          onPermissionRequest(request)
        }

        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
          view?.context?.let { ctx ->
            android.app.AlertDialog.Builder(ctx)
              .setMessage(message)
              .setPositiveButton("تایید") { _, _ -> result?.confirm() }
              .setOnCancelListener { result?.cancel() }
              .show()
          } ?: result?.confirm()
          return true
        }

        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
          view?.context?.let { ctx ->
            android.app.AlertDialog.Builder(ctx)
              .setMessage(message)
              .setPositiveButton("بله") { _, _ -> result?.confirm() }
              .setNegativeButton("خیر") { _, _ -> result?.cancel() }
              .setOnCancelListener { result?.cancel() }
              .show()
          } ?: result?.cancel()
          return true
        }

        override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
          view?.context?.let { ctx ->
            val input = android.widget.EditText(ctx).apply {
              setText(defaultValue)
            }
            android.app.AlertDialog.Builder(ctx)
              .setMessage(message)
              .setView(input)
              .setPositiveButton("تایید") { _, _ -> result?.confirm(input.text.toString()) }
              .setNegativeButton("انصراف") { _, _ -> result?.cancel() }
              .setOnCancelListener { result?.cancel() }
              .show()
          } ?: result?.cancel()
          return true
        }
      }
      
      mainWebViewRef = this
      loadUrl("https://panel.5040.me")
    }
  }

  val chatWebView = remember {
    WebView(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      configureWebViewSettings(this)
      
      addJavascriptInterface(WebAppInterface(
        onShowCredentials = {
          (context as? Activity)?.runOnUiThread {
            showCredentialsSheet = true
          }
        },
        onNewMessage = {
          (context as? Activity)?.runOnUiThread {
            if (!chatExpanded) {
              hasNewChatMessage = true
            }
          }
        },
        onNewMessageWithDetails = { title, body ->
          (context as? Activity)?.runOnUiThread {
            if (!chatExpanded) {
              hasNewChatMessage = true
            }
            if (!MainActivity.isAppInForeground) {
              showSystemNotification(context, title, body)
            }
          }
        },
        onWebThemeDetected = { theme ->
          (context as? Activity)?.runOnUiThread {
            detectedWebTheme = theme
          }
        },
        onUserProfileDetected = { name, avatar ->
          (context as? Activity)?.runOnUiThread {
            if (name.isNotEmpty()) {
              userFullName = name
            }
            if (avatar.isNotEmpty()) {
              userAvatarUrl = avatar
            }
          }
        }
      ), "AndroidApp")
      
      webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
          super.onPageStarted(view, url, favicon)
          view?.loadUrl(JS_PERSIST_SESSION)
          
          val isSystemDark = systemDark
          val earlyThemeJs = "javascript:(function() { " +
              "try { " +
              "  var wantDark = $isSystemDark; " +
              "  var changed = false; " +
              "  var pData = localStorage.getItem('private-data') || '{}'; " +
              "  var pValue = JSON.parse(pData) || {}; " +
              "  if (!pValue.settings) pValue.settings = {}; " +
              "  var current = pValue.settings.theme || 'light'; " +
              "  if (wantDark && current !== 'dark') { " +
              "    pValue.settings.theme = 'dark'; " +
              "    localStorage.setItem('private-data', JSON.stringify(pValue)); " +
              "    changed = true; " +
              "  } else if (!wantDark && current === 'dark') { " +
              "    pValue.settings.theme = 'light'; " +
              "    localStorage.setItem('private-data', JSON.stringify(pValue)); " +
              "    changed = true; " +
              "  } " +
              "  var directTheme = localStorage.getItem('theme'); " +
              "  if (wantDark && directTheme !== 'dark') { " +
              "    localStorage.setItem('theme', 'dark'); " +
              "    changed = true; " +
              "  } else if (!wantDark && directTheme === 'dark') { " +
              "    localStorage.setItem('theme', 'light'); " +
              "    changed = true; " +
              "  } " +
              "  if (wantDark) { " +
              "    document.documentElement.classList.add('dark'); " +
              "    document.documentElement.setAttribute('data-theme', 'dark'); " +
              "  } else { " +
              "    document.documentElement.classList.remove('dark'); " +
              "    document.documentElement.setAttribute('data-theme', 'light'); " +
              "  } " +
              "  if (changed) { " +
              "    window.location.reload(); " +
              "  } " +
              "} catch(e) {} " +
              "})();"
          view?.loadUrl(earlyThemeJs)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
          super.onPageFinished(view, url)
          isChatLoaded = true
          CookieManager.getInstance().flush()
          
          // Sync system dark theme to WebView localStorage
          val isSystemDark = systemDark
          val themeJs = "javascript:(function() { " +
              "try { " +
              "  var wantDark = $isSystemDark; " +
              "  var changed = false; " +
              "  var pData = localStorage.getItem('private-data') || '{}'; " +
              "  var pValue = JSON.parse(pData) || {}; " +
              "  if (!pValue.settings) pValue.settings = {}; " +
              "  var current = pValue.settings.theme || 'light'; " +
              "  if (wantDark && current !== 'dark') { " +
              "    pValue.settings.theme = 'dark'; " +
              "    localStorage.setItem('private-data', JSON.stringify(pValue)); " +
              "    changed = true; " +
              "  } else if (!wantDark && current === 'dark') { " +
              "    pValue.settings.theme = 'light'; " +
              "    localStorage.setItem('private-data', JSON.stringify(pValue)); " +
              "    changed = true; " +
              "  } " +
              "  var directTheme = localStorage.getItem('theme'); " +
              "  if (wantDark && directTheme !== 'dark') { " +
              "    localStorage.setItem('theme', 'dark'); " +
              "    changed = true; " +
              "  } else if (!wantDark && directTheme === 'dark') { " +
              "    localStorage.setItem('theme', 'light'); " +
              "    changed = true; " +
              "  } " +
              "  if (wantDark) { " +
              "    document.documentElement.classList.add('dark'); " +
              "    document.documentElement.setAttribute('data-theme', 'dark'); " +
              "  } else { " +
              "    document.documentElement.classList.remove('dark'); " +
              "    document.documentElement.setAttribute('data-theme', 'light'); " +
              "  } " +
              "  if (changed) { " +
              "    window.location.reload(); " +
              "  } " +
              "} catch(e) {} " +
              "})();"
          view?.loadUrl(themeJs)
          
          view?.loadUrl(JS_PERSIST_SESSION)
          view?.loadUrl(JS_BYPASS_WARNINGS)
          view?.loadUrl(JS_NOTIFICATION_AND_THEME)
        }

        override fun onReceivedError(
          view: WebView?,
          errorCode: Int,
          description: String?,
          failingUrl: String?
        ) {
          super.onReceivedError(view, errorCode, description, failingUrl)
          if (failingUrl?.startsWith("https://chat.5040.me") == true || view?.url == failingUrl) {
            hasWebLoadError = true
          }
        }

        override fun onReceivedError(
          view: WebView?,
          request: WebResourceRequest?,
          error: WebResourceError?
        ) {
          super.onReceivedError(view, request, error)
          if (request?.isForMainFrame == true) {
            hasWebLoadError = true
          }
        }

        override fun onReceivedHttpError(
          view: WebView?,
          request: WebResourceRequest?,
          errorResponse: WebResourceResponse?
        ) {
          super.onReceivedHttpError(view, request, errorResponse)
          if (request?.isForMainFrame == true) {
            val status = errorResponse?.statusCode ?: 200
            if (status >= 400) {
              hasWebLoadError = true
            }
          }
        }
      }
      webChromeClient = object : WebChromeClient() {
        override fun onShowFileChooser(
          webView: WebView?,
          filePathCallback: ValueCallback<Array<Uri>>?,
          fileChooserParams: FileChooserParams?
        ): Boolean {
          val acceptTypes = fileChooserParams?.acceptTypes
          val allowMultiple = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
          } else {
            false
          }
          onOpenFilePicker(filePathCallback, acceptTypes, allowMultiple)
          return true
        }

        override fun onPermissionRequest(request: PermissionRequest) {
          onPermissionRequest(request)
        }

        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
          view?.context?.let { ctx ->
            android.app.AlertDialog.Builder(ctx)
              .setMessage(message)
              .setPositiveButton("تایید") { _, _ -> result?.confirm() }
              .setOnCancelListener { result?.cancel() }
              .show()
          } ?: result?.confirm()
          return true
        }

        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
          view?.context?.let { ctx ->
            android.app.AlertDialog.Builder(ctx)
              .setMessage(message)
              .setPositiveButton("بله") { _, _ -> result?.confirm() }
              .setNegativeButton("خیر") { _, _ -> result?.cancel() }
              .setOnCancelListener { result?.cancel() }
              .show()
          } ?: result?.cancel()
          return true
        }

        override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
          view?.context?.let { ctx ->
            val input = android.widget.EditText(ctx).apply {
              setText(defaultValue)
            }
            android.app.AlertDialog.Builder(ctx)
              .setMessage(message)
              .setView(input)
              .setPositiveButton("تایید") { _, _ -> result?.confirm(input.text.toString()) }
              .setNegativeButton("انصراف") { _, _ -> result?.cancel() }
              .setOnCancelListener { result?.cancel() }
              .show()
          } ?: result?.cancel()
          return true
        }
      }
      
      chatWebViewRef = this
      loadUrl("https://chat.5040.me")
    }
  }

  BackHandler(enabled = chatExpanded || (mainWebViewRef?.canGoBack() == true)) {
    if (chatExpanded) {
      if (chatWebViewRef?.canGoBack() == true) {
        chatWebViewRef?.goBack()
      } else {
        chatExpanded = false
      }
    } else {
      mainWebViewRef?.goBack()
    }
  }

  val isOfflineOrError = !isOnlineState || hasWebLoadError

  Box(
    modifier = Modifier
      .fillMaxSize()
  ) {
    when (val state = appUpdateState) {
      is AppUpdateState.PermissionsRequired -> {
        PermissionsRequiredScreen(
          onGrantClick = {
            val needed = mutableListOf<String>().apply {
              add(android.Manifest.permission.READ_PHONE_STATE)
              add(android.Manifest.permission.RECORD_AUDIO)
              add(android.Manifest.permission.RECEIVE_SMS)
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(android.Manifest.permission.READ_CALL_LOG)
                add(android.Manifest.permission.ANSWER_PHONE_CALLS)
              }
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
              }
            }
            startupPermissionLauncher.launch(needed.toTypedArray())
          }
        )
      }
      is AppUpdateState.CheckingConfig -> {
        SplashLoadingScreen()
      }
      is AppUpdateState.Downloading -> {
        UpdateProgressScreen(
          downloadedBytes = state.downloadedBytes,
          totalBytes = state.totalBytes
        )
      }
      is AppUpdateState.ReadyToInstall -> {
        ReadyToInstallScreen(
          onInstallClick = {
            installApk(context, state.apkFile)
          }
        )
      }
      is AppUpdateState.Idle -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
        ) {
    if (isOfflineOrError) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xFFB3261E)) // Royal deep red
          .clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.padding(32.dp)
        ) {
          Canvas(modifier = Modifier.size(80.dp)) {
            val width = size.width
            val height = size.height
            val strokeWidthPx = 4.dp.toPx()
            val path = Path().apply {
              moveTo(width / 2f, height * 0.15f)
              lineTo(width * 0.15f, height * 0.85f)
              lineTo(width * 0.85f, height * 0.85f)
              close()
            }
            drawPath(
              path = path,
              color = Color.White,
              style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidthPx,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
              )
            )
            
            drawRoundRect(
              color = Color.White,
              topLeft = Offset(width / 2f - 2.5.dp.toPx(), height * 0.40f),
              size = Size(5.dp.toPx(), height * 0.25f),
              cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            drawCircle(
              color = Color.White,
              radius = 3.dp.toPx(),
              center = Offset(width / 2f, height * 0.73f)
            )
          }

          Spacer(modifier = Modifier.height(28.dp))
          
          Text(
            text = "اینترنت وصل نیست",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ),
            color = Color.White,
            textAlign = TextAlign.Center
          )
          
          Spacer(modifier = Modifier.height(10.dp))
          
          Text(
            text = "لطفاً اتصال دستگاه خود را بررسی کرده و دوباره تلاش کنید.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFFFDAD9),
            textAlign = TextAlign.Center
          )
          
          Spacer(modifier = Modifier.height(32.dp))
          
          Button(
            onClick = {
              hasWebLoadError = false
              isMainLoaded = false
              isChatLoaded = false
              val online = isOnline(context)
              isOnlineState = online
              mainWebViewRef?.loadUrl("https://panel.5040.me")
              chatWebViewRef?.loadUrl("https://chat.5040.me")
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = Color.White,
              contentColor = Color(0xFFB3261E)
            ),
            shape = MaterialTheme.shapes.medium,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
          ) {
            Text(
              "تلاش مجدد",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
              )
            )
          }
        }
      }
    } else {
      // Main Panel WebView
      Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
          factory = { mainWebView },
          modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
          visible = !isMainLoaded,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          LoadingScreen(
            message = "در حال ورود به پنل...",
            backgroundColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
          )
        }
      }

      // Chat WebView Overlay (Bubble Reveal shape animated)
      if (chatInitialized) {
        val inputModifier = if (chatExpanded) {
          Modifier.pointerInput(Unit) {}
        } else {
          Modifier
        }

        val isChatDark = systemDark
        val chatBgColor = if (isChatDark) Color(0xFF1E1E1E) else Color(0xFF6750A4)

        Box(
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
              alpha = if (bubbleProgress > 0f) 1f else 0f
              clip = bubbleProgress < 0.98f
              if (clip) {
                shape = CircularRevealShape(bubbleProgress, fabPosition)
              }
            }
            .background(chatBgColor)
            .then(inputModifier)
        ) {
          AndroidView(
            factory = { chatWebView },
            modifier = Modifier
              .fillMaxSize()
              .navigationBarsPadding()
              .imePadding()
          )

          AnimatedVisibility(
            visible = !isChatLoaded,
            enter = fadeIn(),
            exit = fadeOut()
          ) {
            LoadingScreen(
              message = "در حال ورود به چت...",
              backgroundColor = chatBgColor,
              contentColor = Color.White
            )
          }
        }
      }

      // Floating Action Button Overlay (Completely Circular, bottom-right side absolute)
      val isChatDark = systemDark
      val fabBgColor = if (isChatDark) {
        if (chatExpanded) Color(0xFF2D2D2D) else Color(0xFF1E1E1E)
      } else {
        if (chatExpanded) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
      }
      val fabIconColor = if (isChatDark) {
        Color.White
      } else {
        if (chatExpanded) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
      }

      Box(
        modifier = Modifier
          .align(AbsoluteAlignment.BottomRight)
          .absolutePadding(bottom = 24.dp, right = 24.dp)
          .offset(y = fabYOffset)
          .size(56.dp)
          .onGloballyPositioned { coordinates ->
            if (!chatExpanded) {
              val localPosition = coordinates.positionInParent()
              val size = coordinates.size
              fabPosition = Offset(
                x = localPosition.x + size.width / 2f,
                y = localPosition.y + size.height / 2f
              )
            }
          }
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .shadow(elevation = 6.dp, shape = CircleShape)
            .background(
              color = fabBgColor,
              shape = CircleShape
            )
            .clip(CircleShape)
            .clickable { chatExpanded = !chatExpanded },
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier.graphicsLayer {
              rotationZ = iconRotation
            }
          ) {
            if (chatExpanded) {
              PanelLogoIcon(
                modifier = Modifier.size(24.dp),
                color = fabIconColor
              )
            } else {
              ChatLogoIcon(
                modifier = Modifier.size(24.dp),
                color = fabIconColor
              )
            }
          }
        }

        if (hasNewChatMessage && !chatExpanded) {
          val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "blink")
          val blinkAlpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1.0f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
              animation = androidx.compose.animation.core.tween(durationMillis = 600, easing = androidx.compose.animation.core.LinearEasing),
              repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "blink_alpha"
          )
          Box(
            modifier = Modifier
              .size(14.dp)
              .align(Alignment.TopEnd)
              .offset(x = 2.dp, y = (-2).dp)
              .background(androidx.compose.ui.graphics.Color.Red.copy(alpha = blinkAlpha), shape = CircleShape)
              .border(1.5.dp, androidx.compose.ui.graphics.Color.White, CircleShape)
          )
        }
      }

      // VoIP Call FAB (Bottom-Left Side) temporarily removed as requested


      // Top-Left Floating Menu Button (smaller than bottom ones, opens the right drawer)
      Box(
        modifier = Modifier
          .align(AbsoluteAlignment.TopLeft)
          .absolutePadding(top = 56.dp, left = 16.dp)
          .size(44.dp)
          .shadow(elevation = 6.dp, shape = CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape)
          .clip(CircleShape)
          .clickable { isDrawerOpen = true },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Filled.Menu,
          contentDescription = "منوی اصلی",
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(20.dp)
        )
      }

      // Persistent connection status indicator capsule temporarily removed as requested


      if (showVoipSheet) {
        VoipDialog(
          voipManager = voipManager,
          onDismiss = { showVoipSheet = false }
        )
      }

      if (showSettingsDialog) {
        SettingsDialog(
          voipManager = voipManager,
          onDismiss = { showSettingsDialog = false }
        )
      }

      // Right Sliding Drawer Menu Overlay
      AnimatedVisibility(
        visible = isDrawerOpen,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { isDrawerOpen = false }
        ) {
          AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.TopStart)
          ) {
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(enabled = true, onClick = {}) // prevent click-through
                .padding(16.dp)
            ) {
              Column(
                modifier = Modifier.fillMaxSize()
              ) {
                Column(
                  modifier = Modifier.weight(1f)
                ) {
                  if (userFullName == null) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                      )
                      Spacer(modifier = Modifier.width(12.dp))
                      Text(
                        text = "در حال بارگذاری اطلاعات...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  } else {
                    // User info parse logic
                    val rawName = userFullName ?: ""
                    val trimmedName = rawName.trim()
                    var suffix = ""
                    var displayName = trimmedName

                    if (trimmedName.endsWith("RW", ignoreCase = true)) {
                        suffix = "RW"
                        displayName = trimmedName.substring(0, trimmedName.length - 2).trim()
                    } else if (trimmedName.endsWith("NP", ignoreCase = true)) {
                        suffix = "NP"
                        displayName = trimmedName.substring(0, trimmedName.length - 2).trim()
                    } else if (trimmedName.endsWith("VP", ignoreCase = true)) {
                        suffix = "VP"
                        displayName = trimmedName.substring(0, trimmedName.length - 2).trim()
                    }

                    val roleText = when (suffix.uppercase()) {
                        "RW" -> "دور کار"
                        "NP" -> "حضوری"
                        "VP" -> "لیدر"
                        else -> ""
                    }

                    val displayTitle = if (displayName.isNotEmpty()) displayName else "کاربر گرامی"
                    val displaySubtitle = if (roleText.isNotEmpty()) roleText else "منوی دسترسی سریع"

                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Box(
                        modifier = Modifier
                          .size(40.dp)
                          .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                        contentAlignment = Alignment.Center
                      ) {
                        if (!userAvatarUrl.isNullOrEmpty()) {
                          AsyncImage(
                            model = userAvatarUrl,
                            contentDescription = "تصویر پروفایل",
                            modifier = Modifier
                              .size(40.dp)
                              .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                          )
                        } else {
                          Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                          )
                        }
                      }
                      Spacer(modifier = Modifier.width(12.dp))
                      Column {
                        Text(
                          text = displayTitle,
                          style = MaterialTheme.typography.titleMedium,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                          text = displaySubtitle,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                  }

                  Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))

                  // VoIP features are temporarily disabled


                  // Credentials Item
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(MaterialTheme.shapes.medium)
                      .clickable {
                        showFullPasswordManager = true
                        isDrawerOpen = false
                      }
                      .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Lock,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(24.dp)
                    )
                    Text(
                      text = "مدیریت رمز های عبور",
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }
                }

                // Drawer Footer with Version text
                Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "V1.1.2",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                  )
                }
              }
            }
          }
        }
      }

      if (showCredentialsSheet) {
        Box(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 96.dp)
            .fillMaxWidth(0.95f)
        ) {
          FloatingCredentialsMenu(
            onDismiss = { showCredentialsSheet = false },
            onInject = { cred ->
              val activeWebView = if (chatExpanded) chatWebViewRef else mainWebViewRef
              activeWebView?.let { webView ->
                val js = String.format(
                  "javascript:(function() { " +
                  "var inputs = document.querySelectorAll('input'); " +
                  "var userField = null; " +
                  "var passField = null; " +
                  "for (var i = 0; i < inputs.length; i++) { " +
                  "  var type = inputs[i].getAttribute('type') || ''; " +
                  "  var name = inputs[i].getAttribute('name') || ''; " +
                  "  var id = inputs[i].getAttribute('id') || ''; " +
                  "  var placeholder = inputs[i].getAttribute('placeholder') || ''; " +
                  "  if (type === 'password') { " +
                  "    passField = inputs[i]; " +
                  "  } else if (type === 'email' || type === 'text' || name.indexOf('user') !== -1 || name.indexOf('email') !== -1 || id.indexOf('user') !== -1 || id.indexOf('email') !== -1 || placeholder.indexOf('نام کاربری') !== -1 || placeholder.indexOf('ایمیل') !== -1 || placeholder.indexOf('تلفن') !== -1) { " +
                  "    if (!userField && type !== 'submit' && type !== 'button') { " +
                  "      userField = inputs[i]; " +
                  "    } " +
                  "  } " +
                  "} " +
                  "if (userField) { " +
                  "  userField.value = '%s'; " +
                  "  userField.dispatchEvent(new Event('input', { bubbles: true })); " +
                  "  userField.dispatchEvent(new Event('change', { bubbles: true })); " +
                  "} " +
                  "if (passField) { " +
                  "  passField.value = '%s'; " +
                  "  passField.dispatchEvent(new Event('input', { bubbles: true })); " +
                  "  passField.dispatchEvent(new Event('change', { bubbles: true })); " +
                  "} " +
                  "})()",
                  cred.username.replace("'", "\\'").replace("\"", "\\\""),
                  cred.password.replace("'", "\\'").replace("\"", "\\\"")
                )
                webView.loadUrl(js)
                showCredentialsSheet = false
                Toast.makeText(context, "اطلاعات با موفقیت درج شد", Toast.LENGTH_SHORT).show()
              } ?: run {
                Toast.makeText(context, "خطا: صفحه فعال یافت نشد", Toast.LENGTH_SHORT).show()
              }
            },
            onOpenFullManager = {
              showCredentialsSheet = false
              showFullPasswordManager = true
            }
          )
        }
      }

      if (showFullPasswordManager) {
        CredentialsDialog(
          onDismiss = { showFullPasswordManager = false },
          onInject = { cred ->
            val activeWebView = if (chatExpanded) chatWebViewRef else mainWebViewRef
            activeWebView?.let { webView ->
              val js = String.format(
                "javascript:(function() { " +
                "var inputs = document.querySelectorAll('input'); " +
                "var userField = null; " +
                "var passField = null; " +
                "for (var i = 0; i < inputs.length; i++) { " +
                "  var type = inputs[i].getAttribute('type') || ''; " +
                "  var name = inputs[i].getAttribute('name') || ''; " +
                "  var id = inputs[i].getAttribute('id') || ''; " +
                "  var placeholder = inputs[i].getAttribute('placeholder') || ''; " +
                "  if (type === 'password') { " +
                "    passField = inputs[i]; " +
                "  } else if (type === 'email' || type === 'text' || name.indexOf('user') !== -1 || name.indexOf('email') !== -1 || id.indexOf('user') !== -1 || id.indexOf('email') !== -1 || placeholder.indexOf('نام کاربری') !== -1 || placeholder.indexOf('ایمیل') !== -1 || placeholder.indexOf('تلفن') !== -1) { " +
                "    if (!userField && type !== 'submit' && type !== 'button') { " +
                "      userField = inputs[i]; " +
                "    } " +
                "  } " +
                "} " +
                "if (userField) { " +
                "  userField.value = '%s'; " +
                "  userField.dispatchEvent(new Event('input', { bubbles: true })); " +
                "  userField.dispatchEvent(new Event('change', { bubbles: true })); " +
                "} " +
                "if (passField) { " +
                "  passField.value = '%s'; " +
                "  passField.dispatchEvent(new Event('input', { bubbles: true })); " +
                "  passField.dispatchEvent(new Event('change', { bubbles: true })); " +
                "} " +
                "})()",
                cred.username.replace("'", "\\'").replace("\"", "\\\""),
                cred.password.replace("'", "\\'").replace("\"", "\\\"")
              )
              webView.loadUrl(js)
              showFullPasswordManager = false
              Toast.makeText(context, "اطلاعات با موفقیت درج شد", Toast.LENGTH_SHORT).show()
            } ?: run {
              Toast.makeText(context, "خطا: صفحه فعال یافت نشد", Toast.LENGTH_SHORT).show()
            }
          }
        )
      }
    }
    }
    }
    }
  }
}

@Composable
fun LoadingScreen(
  message: String,
  backgroundColor: Color = MaterialTheme.colorScheme.background,
  contentColor: Color = MaterialTheme.colorScheme.onBackground
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(backgroundColor)
      .clickable(enabled = true, onClick = {}),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(24.dp)
    ) {
      CircularProgressIndicator(
        color = contentColor,
        strokeWidth = 4.dp,
        modifier = Modifier.size(56.dp)
      )
      Spacer(modifier = Modifier.height(24.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.titleMedium,
        color = contentColor,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun PanelLogoIcon(modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    
    // Top-Left Square (smaller)
    drawRect(
      color = color,
      topLeft = Offset(0f, 0f),
      size = Size(w * 0.35f, h * 0.45f)
    )
    
    // Bottom-Left Square (smaller)
    drawRect(
      color = color,
      topLeft = Offset(0f, h * 0.55f),
      size = Size(w * 0.35f, h * 0.45f)
    )
    
    // Top-Right Square (wider/larger right side)
    drawRect(
      color = color,
      topLeft = Offset(w * 0.45f, 0f),
      size = Size(w * 0.55f, h * 0.45f)
    )
    
    // Bottom-Right Square (wider/larger right side)
    drawRect(
      color = color,
      topLeft = Offset(w * 0.45f, h * 0.55f),
      size = Size(w * 0.55f, h * 0.45f)
    )
  }
}

@Composable
fun ChatLogoIcon(modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    
    // Main rectangle body (takes 75% height) with rounded corners
    drawRoundRect(
      color = color,
      topLeft = Offset(0f, 0f),
      size = Size(w, h * 0.75f),
      cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    
    // Tail triangle coming out from the bottom-right corner:
    val path = Path().apply {
      moveTo(w * 0.70f, h * 0.75f)
      lineTo(w * 0.95f, h * 0.75f)
      lineTo(w * 0.95f, h * 1.0f)
      close()
    }
    drawPath(path = path, color = color)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialsDialog(
  onDismiss: () -> Unit,
  onInject: (SavedCredential) -> Unit
) {
  val context = LocalContext.current
  val store = remember { CredentialStore(context) }
  var credentialsList by remember { mutableStateOf(store.getCredentials()) }
  
  var isAddingNew by remember { mutableStateOf(false) }
  var titleInput by remember { mutableStateOf("") }
  var usernameInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("بستن", style = MaterialTheme.typography.labelLarge)
      }
    },
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (isAddingNew) "افزودن رمز عبور جدید" else "مدیریت رمزهای ورود",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.primary
        )
        if (!isAddingNew) {
          IconButton(onClick = { isAddingNew = true }) {
            Icon(Icons.Filled.Add, contentDescription = "افزودن")
          }
        } else {
          IconButton(onClick = { isAddingNew = false }) {
            Icon(Icons.Filled.Close, contentDescription = "بازگشت")
          }
        }
      }
    },
    text = {
      Box(modifier = Modifier.width(320.dp).heightIn(max = 450.dp)) {
        if (isAddingNew) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            OutlinedTextField(
              value = titleInput,
              onValueChange = { titleInput = it },
              label = { Text("عنوان (مثلاً: پنل کاربری)") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true
            )
            OutlinedTextField(
              value = usernameInput,
              onValueChange = { usernameInput = it },
              label = { Text("نام کاربری / ایمیل / تلفن") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true
            )
            OutlinedTextField(
              value = passwordInput,
              onValueChange = { passwordInput = it },
              label = { Text("رمز عبور") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                  Text(if (passwordVisible) "پنهان" else "نمایش")
                }
              }
            )
            
            Button(
              onClick = {
                if (titleInput.isBlank() || usernameInput.isBlank() || passwordInput.isBlank()) {
                  Toast.makeText(context, "لطفاً تمامی فیلدها را پر کنید", Toast.LENGTH_SHORT).show()
                } else {
                  val newCred = SavedCredential(
                    title = titleInput,
                    username = usernameInput,
                    password = passwordInput,
                    siteType = "other"
                  )
                  store.addCredential(newCred)
                  credentialsList = store.getCredentials()
                  // Reset
                  titleInput = ""
                  usernameInput = ""
                  passwordInput = ""
                  isAddingNew = false
                  Toast.makeText(context, "رمز عبور با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                }
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("ذخیره رمز عبور")
            }
          }
        } else {
          if (credentialsList.isEmpty()) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
              )
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "هیچ رمز عبوری ذخیره نشده است.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(8.dp))
              TextButton(onClick = { isAddingNew = true }) {
                Text("افزودن اولین رمز عبور")
              }
            }
          } else {
            androidx.compose.foundation.lazy.LazyColumn(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(credentialsList.size) { index ->
                val cred = credentialsList[index]
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                  )
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(12.dp)
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = cred.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                          fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                      )
                      IconButton(
                        onClick = {
                          store.deleteCredential(cred.id)
                          credentialsList = store.getCredentials()
                          Toast.makeText(context, "رمز عبور حذف شد", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Filled.Delete,
                          contentDescription = "حذف",
                          tint = MaterialTheme.colorScheme.error,
                          modifier = Modifier.size(18.dp)
                        )
                      }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                      text = "نام کاربری: ${cred.username}",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Button(
                        onClick = { onInject(cred) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.primary
                        )
                      ) {
                        Text("درج در سایت", style = MaterialTheme.typography.bodySmall)
                      }
                      
                      OutlinedButton(
                        onClick = {
                          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                          val clip = ClipData.newPlainText("username", cred.username)
                          clipboard.setPrimaryClip(clip)
                          Toast.makeText(context, "نام کاربری کپی شد", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                      ) {
                        Text("کپی نام", style = MaterialTheme.typography.bodySmall)
                      }

                      OutlinedButton(
                        onClick = {
                          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                          val clip = ClipData.newPlainText("password", cred.password)
                          clipboard.setPrimaryClip(clip)
                          Toast.makeText(context, "رمز عبور کپی شد", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                      ) {
                        Text("کپی رمز", style = MaterialTheme.typography.bodySmall)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  )
}

@Composable
fun FloatingCredentialsMenu(
  onDismiss: () -> Unit,
  onInject: (SavedCredential) -> Unit,
  onOpenFullManager: () -> Unit
) {
  val context = LocalContext.current
  val store = remember { CredentialStore(context) }
  val credentialsList = remember { store.getCredentials() }

  Card(
    modifier = Modifier
      .padding(horizontal = 8.dp, vertical = 4.dp)
      .fillMaxWidth()
      .wrapContentHeight()
      .shadow(12.dp, shape = RoundedCornerShape(16.dp)),
    colors = CardDefaults.cardColors(
      containerColor = Color(0xFF0F172A) // Slate 900
    ),
    border = BorderStroke(1.dp, Color(0xFF334155)), // Slate 700
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "تکمیل خودکار اطلاعات",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White
          )
        }
        
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          TextButton(
            onClick = onOpenFullManager,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text(
              text = "مدیریت رمزها",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = "بستن",
              tint = Color.LightGray,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
      
      Spacer(modifier = Modifier.height(8.dp))
      
      if (credentialsList.isEmpty()) {
        Text(
          text = "هیچ رمز عبوری ذخیره نشده است.",
          style = MaterialTheme.typography.bodySmall,
          color = Color.Gray,
          modifier = Modifier.padding(vertical = 4.dp)
        )
      } else {
        androidx.compose.foundation.lazy.LazyRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(vertical = 4.dp)
        ) {
          items(credentialsList.size) { index ->
            val cred = credentialsList[index]
            Card(
              modifier = Modifier
                .clickable { onInject(cred) }
                .widthIn(max = 180.dp),
              colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B) // Slate 800
              ),
              border = BorderStroke(1.dp, Color(0xFF475569)), // Slate 600
              shape = RoundedCornerShape(12.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Filled.Person,
                  contentDescription = null,
                  tint = Color(0xFF94A3B8), // Slate 400
                  modifier = Modifier.size(14.dp)
                )
                Column {
                  Text(
                    text = cred.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1
                  )
                  Text(
                    text = cred.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8), // Slate 400
                    maxLines = 1
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

class WebAppInterface(
  private val onShowCredentials: () -> Unit,
  private val onNewMessage: (() -> Unit)? = null,
  private val onNewMessageWithDetails: ((String, String) -> Unit)? = null,
  private val onWebThemeDetected: ((String) -> Unit)? = null,
  private val onUserProfileDetected: ((String, String) -> Unit)? = null
) {
  @JavascriptInterface
  fun showCredentials() {
    onShowCredentials()
  }

  @JavascriptInterface
  fun onNewMessage() {
    onNewMessage?.invoke()
  }

  @JavascriptInterface
  fun onNewMessageWithDetails(title: String?, body: String?) {
    onNewMessageWithDetails?.invoke(title ?: "", body ?: "")
  }

  @JavascriptInterface
  fun onWebThemeDetected(theme: String?) {
    onWebThemeDetected?.invoke(theme ?: "light")
  }

  @JavascriptInterface
  fun onUserProfileDetected(name: String?, avatarUrl: String?) {
    onUserProfileDetected?.invoke(name ?: "", avatarUrl ?: "")
  }
}

@Composable
fun AudioWaveformVisualizer(isActive: Boolean) {
  val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "waveform")
  val barCount = 15
  val heights = remember { (0 until barCount).map { kotlin.random.Random.nextFloat() * 0.8f + 0.2f } }
  val durations = remember { (0 until barCount).map { kotlin.random.Random.nextInt(250, 450) } }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(60.dp)
      .padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically
  ) {
    for (i in 0 until barCount) {
      val targetVal = if (isActive) heights[i] else 0.1f
      val duration = durations[i]
      val animState = infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = targetVal,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
          animation = androidx.compose.animation.core.tween(
            durationMillis = duration,
            easing = androidx.compose.animation.core.LinearEasing
          ),
          repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "bar_$i"
      )
      Box(
        modifier = Modifier
          .width(5.dp)
          .fillMaxHeight(animState.value)
          .background(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.5.dp)
          )
      )
    }
  }
}

@Composable
fun VoipIcon(
  name: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  tint: Color = LocalContentColor.current
) {
  Canvas(modifier = modifier.size(24.dp)) {
    when (name) {
      "Bluetooth" -> {
        val path = Path().apply {
          moveTo(12f, 3f)
          lineTo(12f, 21f)
          lineTo(16.5f, 16.5f)
          lineTo(7.5f, 12f)
          lineTo(16.5f, 7.5f)
          lineTo(12f, 3f)
        }
        drawPath(path = path, color = tint, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
      }
      "VolumeUp" -> {
        val path = Path().apply {
          moveTo(4f, 9f)
          lineTo(8f, 9f)
          lineTo(13f, 5f)
          lineTo(13f, 19f)
          lineTo(8f, 15f)
          lineTo(4f, 15f)
          close()
        }
        drawPath(path = path, color = tint)
        drawArc(
          color = tint,
          startAngle = -45f,
          sweepAngle = 90f,
          useCenter = false,
          style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
          topLeft = androidx.compose.ui.geometry.Offset(15f, 8f),
          size = androidx.compose.ui.geometry.Size(6f, 8f)
        )
        drawArc(
          color = tint,
          startAngle = -45f,
          sweepAngle = 90f,
          useCenter = false,
          style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
          topLeft = androidx.compose.ui.geometry.Offset(13f, 5f),
          size = androidx.compose.ui.geometry.Size(12f, 14f)
        )
      }
      "VolumeDown" -> {
        val path = Path().apply {
          moveTo(4f, 9f)
          lineTo(8f, 9f)
          lineTo(13f, 5f)
          lineTo(13f, 19f)
          lineTo(8f, 15f)
          lineTo(4f, 15f)
          close()
        }
        drawPath(path = path, color = tint)
        drawArc(
          color = tint,
          startAngle = -45f,
          sweepAngle = 90f,
          useCenter = false,
          style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
          topLeft = androidx.compose.ui.geometry.Offset(15f, 8f),
          size = androidx.compose.ui.geometry.Size(6f, 8f)
        )
      }
      "Mic" -> {
        drawRoundRect(
          color = tint,
          topLeft = androidx.compose.ui.geometry.Offset(9f, 4f),
          size = androidx.compose.ui.geometry.Size(6f, 10f),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )
        drawArc(
          color = tint,
          startAngle = 0f,
          sweepAngle = 180f,
          useCenter = false,
          style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
          topLeft = androidx.compose.ui.geometry.Offset(6f, 8f),
          size = androidx.compose.ui.geometry.Size(12f, 8f)
        )
        drawLine(
          color = tint,
          start = androidx.compose.ui.geometry.Offset(12f, 16f),
          end = androidx.compose.ui.geometry.Offset(12f, 19f),
          strokeWidth = 2f
        )
        drawLine(
          color = tint,
          start = androidx.compose.ui.geometry.Offset(8f, 19f),
          end = androidx.compose.ui.geometry.Offset(16f, 19f),
          strokeWidth = 2f
        )
      }
      "MicOff" -> {
        drawRoundRect(
          color = tint.copy(alpha = 0.5f),
          topLeft = androidx.compose.ui.geometry.Offset(9f, 4f),
          size = androidx.compose.ui.geometry.Size(6f, 10f),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )
        drawArc(
          color = tint,
          startAngle = 0f,
          sweepAngle = 180f,
          useCenter = false,
          style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
          topLeft = androidx.compose.ui.geometry.Offset(6f, 8f),
          size = androidx.compose.ui.geometry.Size(12f, 8f)
        )
        drawLine(
          color = tint,
          start = androidx.compose.ui.geometry.Offset(12f, 16f),
          end = androidx.compose.ui.geometry.Offset(12f, 19f),
          strokeWidth = 2f
        )
        drawLine(
          color = tint,
          start = androidx.compose.ui.geometry.Offset(8f, 19f),
          end = androidx.compose.ui.geometry.Offset(16f, 19f),
          strokeWidth = 2f
        )
        drawLine(
          color = tint,
          start = androidx.compose.ui.geometry.Offset(4f, 4f),
          end = androidx.compose.ui.geometry.Offset(20f, 20f),
          strokeWidth = 2f
        )
      }
      "Dialpad" -> {
        val dotRadius = 2f
        val positions = listOf(6f, 12f, 18f)
        for (x in positions) {
          for (y in positions) {
            drawCircle(
              color = tint,
              radius = dotRadius,
              center = androidx.compose.ui.geometry.Offset(x, y)
            )
          }
        }
      }
      else -> {
        // Fallback dot
        drawCircle(color = tint, radius = 4f, center = androidx.compose.ui.geometry.Offset(12f, 12f))
      }
    }
  }
}

private fun formatVoipDuration(seconds: Int): String {
  val m = seconds / 60
  val s = seconds % 60
  return String.format("%02d:%02d", m, s)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VoipDialog(
  voipManager: VoipManager,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val callState by voipManager.callState.collectAsState()
  val activeCallNumber by voipManager.activeCallNumber.collectAsState()
  val isMuted by voipManager.isMuted.collectAsState()
  val isSpeakerOn by voipManager.isSpeakerOn.collectAsState()
  val callDuration by voipManager.callDuration.collectAsState()
  val inputGain by voipManager.inputGain.collectAsState()
  val outputVolume by voipManager.outputVolume.collectAsState()
  val registrationState by voipManager.registrationState.collectAsState()
  val accountState by voipManager.accountState.collectAsState()

  val isNearEar by voipManager.isNearEar.collectAsState()
  val selectedAudioDevice by voipManager.selectedAudioDevice.collectAsState()
  val availableAudioDevices by voipManager.availableAudioDevices.collectAsState()

  var dialedNumber by remember { mutableStateOf("") }
  var showAudioMenu by remember { mutableStateOf(false) }

  val callHistory by voipManager.callHistory.collectAsState(initial = emptyList())
  var selectedTab by remember { mutableStateOf(0) } // 0 = Dialer, 1 = History
  val coroutineScope = rememberCoroutineScope()

  val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    Settings.canDrawOverlays(context)
  } else {
    true
  }

  // Auto-register on open if not registered
  LaunchedEffect(Unit) {
    if (registrationState == "Unregistered" && accountState.server.isNotBlank()) {
      voipManager.registerAccount()
    }
  }

  if (callState != VoipCallState.IDLE) {
    // --- IMMERSIVE FULL-SCREEN CALL SCREEN ---
    Dialog(
      onDismissRequest = {}, // Lock dismiss actions to emulate system call screen
      properties = DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = false,
        dismissOnClickOutside = false
      )
    ) {
      if (isNearEar) {
        // Simulating the screen turning completely off when near the ear
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = false) {}
        )
      } else {
        // Immersive Dark Calling UI (like standard phone call screens)
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(Color(0xFF0F172A), Color(0xFF020617))
              )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
        ) {
          Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            // Top area: Call Info & Secure Label
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(top = 16.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                  .background(Color(0x1110B981), shape = CircleShape)
                  .padding(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .background(Color(0xFF10B981), shape = CircleShape)
                )
                Text(
                  text = "تماس امن و مستقیم (VoIP)",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF34D399)
                )
              }
              
              Spacer(modifier = Modifier.height(32.dp))
              
              // Pulsing avatar container
              Box(
                modifier = Modifier
                  .size(120.dp)
                  .background(Color(0x0AFFFFFF), shape = CircleShape)
                  .padding(12.dp),
                contentAlignment = Alignment.Center
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x15FFFFFF), shape = CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                text = activeCallNumber,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
              )
              
              Spacer(modifier = Modifier.height(8.dp))

              Text(
                text = when (callState) {
                  VoipCallState.DIALING -> "در حال شماره‌گیری..."
                  VoipCallState.RINGING -> "در حال زنگ خوردن..."
                  VoipCallState.CONNECTED -> "تماس برقرار شد • ${formatVoipDuration(callDuration)}"
                  VoipCallState.DISCONNECTED -> "تماس پایان یافت"
                  else -> ""
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (callState == VoipCallState.CONNECTED) Color(0xFF34D399) else Color(0xFF94A3B8)
              )
            }

            // Middle area: real-time visualizer
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
              contentAlignment = Alignment.Center
            ) {
              AudioWaveformVisualizer(isActive = callState == VoipCallState.CONNECTED)
            }

            // Bottom area: Call controls & Hangup
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(24.dp),
              modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
              // Custom Audio Gain Sliders inside a modern expandable panel
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0x0DFFFFFF), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                  .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                // Micro gain
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text("میکروفون", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    Text("${(inputGain * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                  }
                  Slider(
                    value = inputGain,
                    onValueChange = { voipManager.setInputGain(it) },
                    colors = SliderDefaults.colors(
                      activeTrackColor = Color.White,
                      inactiveTrackColor = Color(0x33FFFFFF),
                      thumbColor = Color.White
                    ),
                    enabled = !isMuted
                  )
                }
                
                // Volume gain
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text("بلندگو", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    Text("${(outputVolume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                  }
                  Slider(
                    value = outputVolume,
                    onValueChange = { voipManager.setOutputVolume(it) },
                    colors = SliderDefaults.colors(
                      activeTrackColor = Color.White,
                      inactiveTrackColor = Color(0x33FFFFFF),
                      thumbColor = Color.White
                    )
                  )
                }
              }

              // Control buttons row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Mute microphone
                IconButton(
                  onClick = { voipManager.toggleMute() },
                  modifier = Modifier
                    .size(56.dp)
                    .background(
                      color = if (isMuted) Color(0x44EF4444) else Color(0x15FFFFFF),
                      shape = CircleShape
                    )
                ) {
                  VoipIcon(
                    name = if (isMuted) "MicOff" else "Mic",
                    contentDescription = "بی‌صدا کردن",
                    tint = if (isMuted) Color(0xFFEF4444) else Color.White,
                    modifier = Modifier.size(24.dp)
                  )
                }

                // Audio Output Selection Menu
                Box {
                  IconButton(
                    onClick = { 
                      voipManager.updateAvailableAudioDevices()
                      showAudioMenu = true 
                    },
                    modifier = Modifier
                      .size(56.dp)
                      .background(
                        color = if (selectedAudioDevice != AudioOutputDevice.EARPIECE) Color(0xFF3B82F6) else Color(0x15FFFFFF),
                        shape = CircleShape
                      )
                  ) {
                    VoipIcon(
                      name = when (selectedAudioDevice) {
                        AudioOutputDevice.SPEAKER -> "VolumeUp"
                        AudioOutputDevice.BLUETOOTH -> "Bluetooth"
                        else -> "VolumeDown"
                      },
                      contentDescription = "خروجی صدا",
                      tint = Color.White,
                      modifier = Modifier.size(24.dp)
                    )
                  }

                  // Dropdown menu for selecting output devices
                  DropdownMenu(
                    expanded = showAudioMenu,
                    onDismissRequest = { showAudioMenu = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                  ) {
                    availableAudioDevices.forEach { device ->
                      DropdownMenuItem(
                        text = {
                          Text(
                            text = when (device) {
                              AudioOutputDevice.EARPIECE -> "📱 خروجی گوشی (Earpiece)"
                              AudioOutputDevice.SPEAKER -> "🔊 بلندگو (Speaker)"
                              AudioOutputDevice.BLUETOOTH -> "🎧 بلوتوث (Bluetooth)"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                          )
                        },
                        onClick = {
                          voipManager.selectAudioDevice(device)
                          showAudioMenu = false
                        }
                      )
                    }
                  }
                }

                // Hangup Button
                IconButton(
                  onClick = { voipManager.endCall() },
                  modifier = Modifier
                    .size(64.dp)
                    .background(color = Color(0xFFEF4444), shape = CircleShape)
                ) {
                  Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "قطع تماس",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  } else {
    // --- IDLE / DIALPAD DIALOG ---
    AlertDialog(
      onDismissRequest = onDismiss,
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = onDismiss) {
          Text("بستن و فعالیت در پس‌زمینه", style = MaterialTheme.typography.labelLarge)
        }
      },
      title = {
        Text(
          text = "سیستم تماس اینترنتی (VoIP)",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.fillMaxWidth(),
          textAlign = TextAlign.Center
        )
      },
      text = {
        Box(modifier = Modifier.width(340.dp).heightIn(max = 500.dp)) {
          Column(modifier = Modifier.fillMaxWidth()) {
            TabRow(
              selectedTabIndex = selectedTab,
              modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
              Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("شماره‌گیر", style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(20.dp)) }
              )
              Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("تاریخچه", style = MaterialTheme.typography.labelLarge) },
                icon = { Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp)) }
              )
            }

            if (selectedTab == 0) {
              if (!hasOverlayPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Card(
                  colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                  ),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                  modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                  Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Text(
                      text = "⚠️ برای نمایش پاپ‌آپ تماس هنگام خروج از برنامه، نیاز به مجوز «نمایش روی سایر برنامه‌ها» است.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onErrorContainer,
                      textAlign = TextAlign.Center
                    )
                    Button(
                      onClick = {
                        try {
                          val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                          )
                          context.startActivity(intent)
                        } catch (e: Exception) {
                          val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                          context.startActivity(intent)
                        }
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                      ),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Text("فعال‌سازی مجوز", style = MaterialTheme.typography.labelMedium)
                    }
                  }
                }
              }

              // Connection status banner
              Card(
                colors = CardDefaults.cardColors(
                  containerColor = when (registrationState) {
                    "Registered" -> Color(0xFFE8F5E9)
                    "Registering" -> Color(0xFFFFFDE7)
                    "VpnConnecting" -> Color(0xFFEDE7F6)
                    "Failed" -> Color(0xFFFFEBEE)
                    else -> Color(0xFFECEFF1)
                  }
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .background(
                        color = when (registrationState) {
                          "Registered" -> Color(0xFF4CAF50)
                          "Registering" -> Color(0xFFFFEB3B)
                          "VpnConnecting" -> Color(0xFF9C27B0)
                          "Failed" -> Color(0xFFE53935)
                          else -> Color(0xFF78909C)
                        },
                        shape = CircleShape
                      )
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = when (registrationState) {
                      "Registered" -> "متصل به سرور SIP (${accountState.username})"
                      "Registering" -> "در حال اتصال به سرور SIP..."
                      "VpnConnecting" -> "در حال برقراری اتصال به VPN اختصاصی..."
                      "Failed" -> "خطا در اتصال به سرور (تنظیمات SIP یا VPN را بررسی کنید)"
                      else -> "آفلاین - نیاز به تنظیمات SIP"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (registrationState) {
                      "Registered" -> Color(0xFF2E7D32)
                      "Registering" -> Color(0xFFF57F17)
                      "VpnConnecting" -> Color(0xFF6A1B9A)
                      "Failed" -> Color(0xFFC62828)
                      else -> Color(0xFF37474F)
                    }
                  )
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  // Display number being typed
                  OutlinedTextField(
                    value = dialedNumber,
                    onValueChange = { dialedNumber = it },
                    placeholder = { Text("شماره داخلی یا SIP URI") },
                    modifier = Modifier
                      .fillMaxWidth()
                      .focusProperties { canFocus = false },
                    singleLine = true,
                    readOnly = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                    trailingIcon = {
                      if (dialedNumber.isNotEmpty()) {
                        Box(
                          modifier = Modifier
                            .clip(CircleShape)
                            .combinedClickable(
                              onClick = {
                                if (dialedNumber.isNotEmpty()) {
                                  dialedNumber = dialedNumber.dropLast(1)
                                }
                              },
                              onLongClick = {
                                dialedNumber = ""
                              }
                            )
                            .padding(12.dp)
                        ) {
                          Icon(Icons.Filled.ArrowBack, contentDescription = "حذف")
                        }
                      }
                    }
                  )

                  // Grid of Numbers
                  val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("*", "0", "#")
                  )

                  Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    keys.forEach { rowKeys ->
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        rowKeys.forEach { key ->
                          Button(
                            onClick = {
                              dialedNumber += key
                              voipManager.playDtmf(key[0])
                            },
                            modifier = Modifier
                              .weight(1f)
                              .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                              containerColor = MaterialTheme.colorScheme.surfaceVariant,
                              contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                          ) {
                            Text(
                              text = key,
                              style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            )
                          }
                        }
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  // Call Button
                  Button(
                    onClick = {
                      if (dialedNumber.isBlank()) {
                        Toast.makeText(context, "لطفا شماره یا آدرس SIP را وارد کنید", Toast.LENGTH_SHORT).show()
                      } else {
                        voipManager.startCall(dialedNumber)
                      }
                    },
                    enabled = registrationState == "Registered",
                    modifier = Modifier
                      .size(64.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = Color(0xFF4CAF50),
                      contentColor = Color.White,
                      disabledContainerColor = Color(0xFFE0E0E0),
                      disabledContentColor = Color(0xFF9E9E9E)
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Call,
                      contentDescription = "برقراری تماس",
                      modifier = Modifier.size(32.dp)
                    )
                  }
                }
              }
            } else {
              // Call History Tab
              Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "تماس‌های اخیر",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  if (callHistory.isNotEmpty()) {
                    TextButton(
                      onClick = {
                        coroutineScope.launch {
                          voipManager.callRepository.clearHistory()
                        }
                      },
                      colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Icon(Icons.Filled.Delete, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                        Text("پاک کردن تاریخچه", style = MaterialTheme.typography.labelMedium)
                      }
                    }
                  }
                }

                if (callHistory.isEmpty()) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(250.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                      )
                      Text(
                        text = "هیچ تماسی یافت نشد",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                      )
                    }
                  }
                } else {
                  androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    items(callHistory) { record ->
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .clickable {
                            dialedNumber = record.number
                            selectedTab = 0
                          }
                          .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                      ) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                          Box(
                            modifier = Modifier
                              .size(40.dp)
                              .background(
                                color = when (record.type) {
                                  "OUTGOING" -> Color(0xFFE3F2FD)
                                  "INCOMING" -> Color(0xFFE8F5E9)
                                  else -> Color(0xFFFFEBEE)
                                },
                                shape = CircleShape
                              ),
                            contentAlignment = Alignment.Center
                          ) {
                            Icon(
                              imageVector = Icons.Filled.Call,
                              contentDescription = null,
                              tint = when (record.type) {
                                "OUTGOING" -> Color(0xFF1976D2)
                                "INCOMING" -> Color(0xFF388E3C)
                                else -> Color(0xFFD32F2F)
                              },
                              modifier = Modifier.size(20.dp)
                            )
                          }

                          Column {
                            Text(
                              text = record.number,
                              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                              color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                              Text(
                                text = when (record.type) {
                                  "OUTGOING" -> "خروجی"
                                  "INCOMING" -> "ورودی"
                                  else -> "بی‌پاسخ"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when (record.type) {
                                  "OUTGOING" -> Color(0xFF1976D2)
                                  "INCOMING" -> Color(0xFF388E3C)
                                  else -> Color(0xFFD32F2F)
                                }
                              )
                              Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                              )
                              Text(
                                text = formatVoipDuration(record.durationSeconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                              )
                            }
                          }
                        }

                        Column(
                          horizontalAlignment = Alignment.End
                        ) {
                          val dateString = remember(record.timestamp) {
                            try {
                              val now = System.currentTimeMillis()
                              val diff = now - record.timestamp
                              val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                              val timeStr = sdfTime.format(java.util.Date(record.timestamp))
                              if (diff < 24 * 60 * 60 * 1000L) {
                                timeStr
                              } else {
                                val sdfDate = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
                                sdfDate.format(java.util.Date(record.timestamp)) + " " + timeStr
                              }
                            } catch (e: Exception) {
                              ""
                            }
                          }
                          Text(
                            text = dateString,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                          )
                          Spacer(modifier = Modifier.height(4.dp))
                          IconButton(
                            onClick = {
                              voipManager.startCall(record.number)
                            },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Icon(
                              imageVector = Icons.Filled.Call,
                              contentDescription = "تماس مجدد",
                              tint = Color(0xFF4CAF50),
                              modifier = Modifier.size(16.dp)
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
  voipManager: VoipManager,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val accountState by voipManager.accountState.collectAsState()
  val registrationState by voipManager.registrationState.collectAsState()
  val vpnConfigState by voipManager.vpnConfigState.collectAsState()
  val vpnState by voipManager.vpnState.collectAsState()

  var sipServer by remember { mutableStateOf(accountState.server) }
  var sipUser by remember { mutableStateOf(accountState.username) }
  var sipSecret by remember { mutableStateOf(accountState.secret) }
  var sipPort by remember { mutableStateOf(accountState.port) }
  var sipTransport by remember { mutableStateOf(accountState.transport) }

  var vpnEnabled by remember { mutableStateOf(vpnConfigState.isEnabled) }
  var vpnServer by remember { mutableStateOf(vpnConfigState.server) }
  var vpnUser by remember { mutableStateOf(vpnConfigState.username) }
  var vpnSecret by remember { mutableStateOf(vpnConfigState.secret) }
  var vpnType by remember { mutableStateOf(vpnConfigState.type) }

  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("بستن", style = MaterialTheme.typography.labelLarge)
      }
    },
    title = {
      Text(
        text = "تنظیمات پیشرفته سیستم",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary
      )
    },
    text = {
      androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
          .width(320.dp)
          .heightIn(max = 500.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), shape = MaterialTheme.shapes.medium)
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "وضعیت لحظه‌ای ارتباطات",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(bottom = 4.dp)
            )
            
            // VPN Status indicator
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .background(
                      color = when (vpnState) {
                        "Connected" -> Color(0xFF4CAF50)
                        "Connecting" -> Color(0xFFFFC107)
                        "Failed" -> Color(0xFFF44336)
                        else -> Color(0xFF9E9E9E)
                      },
                      shape = CircleShape
                    )
                )
                Text("تونل VPN اختصاصی:", style = MaterialTheme.typography.bodySmall)
              }
              Text(
                text = when (vpnState) {
                  "Connected" -> "متصل (ایمن)"
                  "Connecting" -> "در حال برقراری..."
                  "Failed" -> "خطا در اتصال"
                  else -> "غیرفعال"
                },
                color = when (vpnState) {
                  "Connected" -> Color(0xFF2E7D32)
                  "Connecting" -> Color(0xFFF57F17)
                  "Failed" -> Color(0xFFC62828)
                  else -> Color(0xFF455A64)
                },
                style = MaterialTheme.typography.labelSmall
              )
            }

            // SIP Status indicator
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .background(
                      color = when (registrationState) {
                        "Registered" -> Color(0xFF4CAF50)
                        "Registering" -> Color(0xFFFFC107)
                        "VpnConnecting" -> Color(0xFF9C27B0)
                        "Failed" -> Color(0xFFF44336)
                        else -> Color(0xFF9E9E9E)
                      },
                      shape = CircleShape
                    )
                )
                Text("سرور تلفنی SIP (VoIP):", style = MaterialTheme.typography.bodySmall)
              }
              Text(
                text = when (registrationState) {
                  "Registered" -> "متصل و فعال"
                  "Registering" -> "در حال ثبت‌نام..."
                  "VpnConnecting" -> "در انتظار VPN..."
                  "Failed" -> "خطا در اتصال"
                  else -> "قطع ارتباط"
                },
                color = when (registrationState) {
                  "Registered" -> Color(0xFF2E7D32)
                  "Registering" -> Color(0xFFF57F17)
                  "VpnConnecting" -> Color(0xFF6A1B9A)
                  "Failed" -> Color(0xFFC62828)
                  else -> Color(0xFF455A64)
                },
                style = MaterialTheme.typography.labelSmall
              )
            }
          }
          androidx.compose.material3.Divider(modifier = Modifier.padding(vertical = 4.dp))
        }

        item {
          Text(
            text = "تنظیمات اتصال VoIP (SIP)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
          )
        }
        item {
          OutlinedTextField(
            value = sipServer,
            onValueChange = { sipServer = it },
            label = { Text("سرور SIP / آدرس دامنه") },
            placeholder = { Text("sip.mydomain.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }
        item {
          OutlinedTextField(
            value = sipUser,
            onValueChange = { sipUser = it },
            label = { Text("نام کاربری / داخلی") },
            placeholder = { Text("101") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }
        item {
          OutlinedTextField(
            value = sipSecret,
            onValueChange = { sipSecret = it },
            label = { Text("رمز عبور SIP") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
          )
        }
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = sipPort,
              onValueChange = { sipPort = it },
              label = { Text("پورت") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
            OutlinedTextField(
              value = sipTransport,
              onValueChange = { sipTransport = it },
              label = { Text("پروتکل") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
          }
        }

        item {
          Divider(modifier = Modifier.padding(vertical = 4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "اتصال به VPN اختصاصی",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "برای تماس‌های خارج از شبکه یا محافظت‌شده",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = vpnEnabled,
              onCheckedChange = { vpnEnabled = it }
            )
          }
        }

        if (vpnEnabled) {
          item {
            OutlinedTextField(
              value = vpnServer,
              onValueChange = { vpnServer = it },
              label = { Text("آدرس سرور VPN") },
              placeholder = { Text("192.168.1.100") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true
            )
          }
          item {
            OutlinedTextField(
              value = vpnUser,
              onValueChange = { vpnUser = it },
              label = { Text("نام کاربری VPN") },
              placeholder = { Text("vpn_user") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true
            )
          }
          item {
            OutlinedTextField(
              value = vpnSecret,
              onValueChange = { vpnSecret = it },
              label = { Text("رمز عبور VPN") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              visualTransformation = PasswordVisualTransformation()
            )
          }
          item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "نوع پروتکل VPN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                listOf("L2TP", "PPTP", "OpenVPN", "Cisco").forEach { type ->
                  val isSelected = vpnType == type
                  Button(
                    onClick = { vpnType = type },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                      contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  ) {
                    Text(type, style = MaterialTheme.typography.labelSmall)
                  }
                }
              }
            }
          }
        }

        item {
          Button(
            onClick = {
              val acc = SipAccount(sipServer, sipUser, sipSecret, sipPort, sipTransport)
              val vpnConf = VpnConfig(vpnEnabled, vpnServer, vpnUser, vpnSecret, vpnType)
              voipManager.saveVpnConfig(vpnConf)
              if (voipManager.saveAccount(acc)) {
                voipManager.registerAccount()
                Toast.makeText(context, "تنظیمات ذخیره شد و در حال اتصال است...", Toast.LENGTH_SHORT).show()
              } else {
                Toast.makeText(context, "خطا: اطلاعات حساب (آدرس سرور، داخلی، رمز عبور) نباید خالی باشد!", Toast.LENGTH_LONG).show()
              }
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("ذخیره و اتصال به سرور")
          }
        }
      }
    }
  )
}

data class ConfigData(
  val update: Boolean,
  val updateVersion: String,
  val updateUrl: String
)

sealed interface AppUpdateState {
  object Idle : AppUpdateState
  object PermissionsRequired : AppUpdateState
  object CheckingConfig : AppUpdateState
  data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : AppUpdateState
  data class ReadyToInstall(val apkFile: java.io.File) : AppUpdateState
}

fun isVersionOlder(current: String, latest: String): Boolean {
  val cleanCurr = current.replace("v", "").replace("V", "").trim()
  val cleanLat = latest.replace("v", "").replace("V", "").trim()
  val currParts = cleanCurr.split(".").map { it.toIntOrNull() ?: 0 }
  val latParts = cleanLat.split(".").map { it.toIntOrNull() ?: 0 }
  for (i in 0 until maxOf(currParts.size, latParts.size)) {
    val currVal = currParts.getOrNull(i) ?: 0
    val latVal = latParts.getOrNull(i) ?: 0
    if (latVal > currVal) return true
    if (currVal > latVal) return false
  }
  return false
}

suspend fun fetchConfigs(): ConfigData? = withContext(Dispatchers.IO) {
  try {
    var configUrl = "https://github.com/MWiyPower/5040DB/blob/main/configs.ini"
    if (configUrl.contains("github.com") && configUrl.contains("/blob/")) {
      configUrl = configUrl.replace("github.com", "raw.githubusercontent.com").replace("/blob/", "/")
    }
    val url = URL(configUrl)
    val conn = url.openConnection() as HttpURLConnection
    conn.connectTimeout = 8000
    conn.readTimeout = 8000
    conn.requestMethod = "GET"
    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
    
    if (conn.responseCode == 200) {
      val text = conn.inputStream.bufferedReader().use { it.readText() }
      
      val updateMatch = """AU_Update\s*=\s*"([^"]*)"""".toRegex().find(text)
      val versionMatch = """AU_UpdateVersion\s*=\s*"([^"]*)"""".toRegex().find(text)
      val urlMatch = """AU_UpdateURL\s*=\s*"([^"]*)"""".toRegex().find(text)
      
      val update = updateMatch?.groupValues?.getOrNull(1)?.trim()?.lowercase() == "true"
      val version = versionMatch?.groupValues?.getOrNull(1)?.trim() ?: "1.0.0"
      val updateUrl = urlMatch?.groupValues?.getOrNull(1)?.trim() ?: ""
      
      return@withContext ConfigData(update, version, updateUrl)
    }
  } catch (e: Exception) {
    e.printStackTrace()
  }
  return@withContext null
}

suspend fun downloadApk(
  context: Context, 
  downloadUrl: String, 
  onProgress: (Long, Long) -> Unit
): File? = withContext(Dispatchers.IO) {
  try {
    val url = URL(downloadUrl)
    val conn = url.openConnection() as HttpURLConnection
    conn.connectTimeout = 15000
    conn.readTimeout = 15000
    conn.connect()
    
    val lengthOfFile = conn.contentLength.toLong()
    val input = conn.inputStream
    
    val apkFile = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "update.apk")
    if (apkFile.exists()) {
      apkFile.delete()
    }
    
    val output = FileOutputStream(apkFile)
    val data = ByteArray(8192)
    var total: Long = 0
    var count: Int
    while (input.read(data).also { count = it } != -1) {
      total += count
      onProgress(total, lengthOfFile)
      output.write(data, 0, count)
    }
    output.flush()
    output.close()
    input.close()
    return@withContext apkFile
  } catch (e: Exception) {
    e.printStackTrace()
  }
  return@withContext null
}

fun installApk(context: Context, apkFile: File) {
  try {
    val authority = "${context.packageName}.fileprovider"
    val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      androidx.core.content.FileProvider.getUriForFile(context, authority, apkFile)
    } else {
      Uri.fromFile(apkFile)
    }
    
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(apkUri, "application/vnd.android.package-archive")
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (!context.packageManager.canRequestPackageInstalls()) {
        val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
          data = Uri.parse("package:${context.packageName}")
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(settingsIntent)
        Toast.makeText(context, "لطفاً اجازه نصب برنامه از این منبع را فعال کنید و سپس دکمه نصب را مجدد بزنید", Toast.LENGTH_LONG).show()
        return
      }
    }
    context.startActivity(intent)
  } catch (e: Exception) {
    e.printStackTrace()
    Toast.makeText(context, "خطا در نصب برنامه: ${e.message}", Toast.LENGTH_LONG).show()
  }
}

@Composable
fun SplashLoadingScreen() {
  val context = androidx.compose.ui.platform.LocalContext.current
  val view = androidx.compose.ui.platform.LocalView.current
  androidx.compose.runtime.SideEffect {
    val window = (context as? android.app.Activity)?.window
    if (window != null) {
      window.statusBarColor = android.graphics.Color.parseColor("#0F0F12")
      val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
      insetsController.isAppearanceLightStatusBars = false
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(androidx.compose.ui.graphics.Color(0xFF0F0F12)),
    contentAlignment = Alignment.Center
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E1E24)),
      elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
      modifier = Modifier
        .padding(32.dp)
        .widthIn(max = 300.dp)
    ) {
      Column(
        modifier = Modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        CircularProgressIndicator(
          color = androidx.compose.ui.graphics.Color(0xFF007AFF),
          strokeWidth = 4.dp,
          modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
          text = "در حال دریافت تنظیمات",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
          color = androidx.compose.ui.graphics.Color.White,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
fun UpdateProgressScreen(
  downloadedBytes: Long,
  totalBytes: Long
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val view = androidx.compose.ui.platform.LocalView.current
  androidx.compose.runtime.SideEffect {
    val window = (context as? android.app.Activity)?.window
    if (window != null) {
      window.statusBarColor = android.graphics.Color.parseColor("#0D6EFD")
      val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
      insetsController.isAppearanceLightStatusBars = false
    }
  }

  val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
  val downloadedMb = downloadedBytes.toFloat() / (1024 * 1024)
  val totalMb = totalBytes.toFloat() / (1024 * 1024)
  
  val progressText = if (totalBytes > 0) {
    String.format(java.util.Locale.US, "%.2fMB / %.2fMB", downloadedMb, totalMb)
  } else {
    String.format(java.util.Locale.US, "%.2fMB / --MB", downloadedMb)
  }
  
  val percentageText = "${(progress * 100).toInt()}%"

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        androidx.compose.ui.graphics.Brush.verticalGradient(
          colors = listOf(
            androidx.compose.ui.graphics.Color(0xFF0D6EFD), 
            androidx.compose.ui.graphics.Color(0xFF0A58CA)
          )
        )
      ),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(24.dp)
    ) {
      Text(
        text = "در حال دریافت بروزرسانی جدید",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
        color = androidx.compose.ui.graphics.Color.White,
        textAlign = TextAlign.Center
      )
      
      Spacer(modifier = Modifier.height(48.dp))
      
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp)
      ) {
        CircularProgressIndicator(
          progress = progress,
          color = androidx.compose.ui.graphics.Color.White,
          trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
          strokeWidth = 8.dp,
          modifier = Modifier.fillMaxSize()
        )
        Text(
          text = percentageText,
          style = MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
          color = androidx.compose.ui.graphics.Color.White
        )
      }
      
      Spacer(modifier = Modifier.height(32.dp))
      
      Text(
        text = progressText,
        style = MaterialTheme.typography.titleMedium.copy(
          fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
          fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        ),
        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
      )
    }
  }
}

@Composable
fun PermissionsRequiredScreen(
  onGrantClick: () -> Unit
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val view = androidx.compose.ui.platform.LocalView.current
  androidx.compose.runtime.SideEffect {
    val window = (context as? android.app.Activity)?.window
    if (window != null) {
      window.statusBarColor = android.graphics.Color.parseColor("#0F0F12")
      val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
      insetsController.isAppearanceLightStatusBars = false
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(androidx.compose.ui.graphics.Color(0xFF0F0F12)),
    contentAlignment = Alignment.Center
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E1E24)),
      elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
      modifier = Modifier
        .padding(24.dp)
        .widthIn(max = 320.dp)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = androidx.compose.material.icons.Icons.Default.Lock,
          contentDescription = null,
          tint = androidx.compose.ui.graphics.Color(0xFF007AFF),
          modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "مجوزهای دسترسی مورد نیاز",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
          color = androidx.compose.ui.graphics.Color.White,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "برای عملکرد صحیح و خودکارسازی فرآیندها، لطفاً مجوزهای زیر را تأیید کنید:",
          style = MaterialTheme.typography.bodyMedium,
          color = androidx.compose.ui.graphics.Color.Gray,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        PermissionDescItem("ضبط صدا", "جهت برقراری تماس‌های صوتی VoIP")
        PermissionDescItem("وضعیت تلفن", "جهت مدیریت تماس‌های تلفنی و خطوط")
        PermissionDescItem("دریافت پیامک", "جهت پردازش پیام‌های خودکار")
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
          onClick = onGrantClick,
          colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF007AFF)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
          Text(
            text = "تأیید و اعطای مجوزها",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = androidx.compose.ui.graphics.Color.White
          )
        }
      }
    }
  }
}

@Composable
fun PermissionDescItem(title: String, desc: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = androidx.compose.material.icons.Icons.Default.Info,
      contentDescription = null,
      tint = androidx.compose.ui.graphics.Color(0xFF007AFF),
      modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
        color = androidx.compose.ui.graphics.Color.White
      )
      Text(
        text = desc,
        style = MaterialTheme.typography.bodySmall,
        color = androidx.compose.ui.graphics.Color.Gray
      )
    }
  }
}

@Composable
fun ReadyToInstallScreen(
  onInstallClick: () -> Unit
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val view = androidx.compose.ui.platform.LocalView.current
  androidx.compose.runtime.SideEffect {
    val window = (context as? android.app.Activity)?.window
    if (window != null) {
      window.statusBarColor = android.graphics.Color.parseColor("#0D6EFD")
      val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
      insetsController.isAppearanceLightStatusBars = false
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        androidx.compose.ui.graphics.Brush.verticalGradient(
          colors = listOf(
            androidx.compose.ui.graphics.Color(0xFF0D6EFD), 
            androidx.compose.ui.graphics.Color(0xFF0A58CA)
          )
        )
      ),
    contentAlignment = Alignment.Center
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E1E24)),
      elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
      modifier = Modifier
        .padding(24.dp)
        .widthIn(max = 320.dp)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = androidx.compose.material.icons.Icons.Default.Info,
          contentDescription = null,
          tint = androidx.compose.ui.graphics.Color(0xFF0D6EFD),
          modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "بروزرسانی آماده نصب است",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
          color = androidx.compose.ui.graphics.Color.White,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "نسخه جدید با موفقیت دانلود شد. جهت اعمال تغییرات و نصب برنامه روی دکمه زیر کلیک کنید.",
          style = MaterialTheme.typography.bodyMedium,
          color = androidx.compose.ui.graphics.Color.Gray,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
          onClick = onInstallClick,
          colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF0D6EFD)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
          Text(
            text = "نصب بروزرسانی",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = androidx.compose.ui.graphics.Color.White
          )
        }
      }
    }
  }
}


