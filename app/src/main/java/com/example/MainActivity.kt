package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.Context
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.theme.MyApplicationTheme

private const val JS_BYPASS_WARNINGS = "javascript:(function() { " +
    "window.alert = function() { return true; }; " +
    "window.confirm = function() { return true; }; " +
    "window.prompt = function() { return null; }; " +
    "var style = document.createElement('style'); " +
    "style.type = 'text/css'; " +
    "style.innerHTML = '.old-browser, #old-browser, .browser-upgrade, .outdated-browser, .browser-alert, [class*=\\'update-browser\\'], [class*=\\'old-browser\\'], [id*=\\'update-browser\\'], [id*=\\'old-browser\\'], [class*=\\'unsupported\\'], [id*=\\'unsupported\\'], [class*=\\'warning\\'], [id*=\\'warning\\'] { display: none !important; visibility: hidden !important; height: 0 !important; opacity: 0 !important; pointer-events: none !important; }'; " +
    "document.head.appendChild(style); " +
    "var meta = document.createElement('meta'); " +
    "meta.name = 'viewport'; " +
    "meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, shrink-to-fit=no'; " +
    "document.head.appendChild(meta); " +
    "document.addEventListener('gesturestart', function(e) { e.preventDefault(); }, { passive: false }); " +
    "document.addEventListener('gesturechange', function(e) { e.preventDefault(); }, { passive: false }); " +
    "document.addEventListener('touchmove', function(e) { if (e.scale !== 1) { e.preventDefault(); } }, { passive: false }); " +
    "var lastTouchEnd = 0; " +
    "document.addEventListener('touchend', function(e) { " +
    "  var now = (new Date()).getTime(); " +
    "  if (now - lastTouchEnd <= 300) { " +
    "    e.preventDefault(); " +
    "  } " +
    "  lastTouchEnd = now; " +
    "}, false); " +
    "var checkAndRemove = function() { " +
    "  var rx = /\\u0642\\u062f\\u06cc\\u0645\\u06cc|\\u0628\\u0631\\u0648\\u0632\\u0631\\u0633\\u0627\\u0646\\u06cc|\\u0622\\u067e\\u062f\\u06cc\\u062a|\\u0646\\u0633\\u062e\\u0647|browser|update|support|old|outdated/i; " +
    "  var elems = document.querySelectorAll('div, section, dialog, p, span, h1, h2, h3'); " +
    "  for (var i = 0; i < elems.length; i++) { " +
    "    var text = elems[i].textContent || elems[i].innerText || ''; " +
    "    if (rx.test(text)) { " +
    "      var pos = window.getComputedStyle(elems[i]).position; " +
    "      if (pos === 'fixed' || pos === 'absolute' || elems[i].className.indexOf('warning') !== -1 || elems[i].id.indexOf('warning') !== -1 || elems[i].className.indexOf('alert') !== -1 || elems[i].id.indexOf('alert') !== -1) { " +
    "        elems[i].style.setProperty('display', 'none', 'important'); " +
    "      } " +
    "    } " +
    "  } " +
    "}; " +
    "checkAndRemove(); " +
    "setInterval(checkAndRemove, 500); " +
    "})()"

class MainActivity : ComponentActivity() {

  private var filePathCallback: ValueCallback<Array<Uri>>? = null
  private var permissionRequestCallback: PermissionRequest? = null

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
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          MainScreen(
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
      setSupportZoom(false)
      builtInZoomControls = false
      displayZoomControls = false
      textZoom = 100
      useWideViewPort = true
      loadWithOverviewMode = true
      userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    setOnTouchListener { _, event ->
      event.pointerCount > 1
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
  onOpenFilePicker: (ValueCallback<Array<Uri>>?, Array<String>?, Boolean) -> Unit,
  onPermissionRequest: (PermissionRequest) -> Unit
) {
  var isMainLoaded by remember { mutableStateOf(false) }
  var isChatLoaded by remember { mutableStateOf(false) }

  var chatExpanded by remember { mutableStateOf(false) }
  var chatInitialized by remember { mutableStateOf(false) }

  var isOnlineState by remember { mutableStateOf(true) }
  var hasWebLoadError by remember { mutableStateOf(false) }

  var fabPosition by remember { mutableStateOf(Offset(900f, 1800f)) }
  var showCredentialsSheet by remember { mutableStateOf(false) }

  val context = LocalContext.current

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
    }
  }

  var mainWebViewRef by remember { mutableStateOf<WebView?>(null) }
  var chatWebViewRef by remember { mutableStateOf<WebView?>(null) }
  val mainWebView = remember {
    WebView(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      configureWebViewSettings(this)
      
      webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
          super.onPageFinished(view, url)
          isMainLoaded = true
          CookieManager.getInstance().flush()
          view?.loadUrl(JS_BYPASS_WARNINGS)
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
          result?.confirm()
          return true
        }

        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
          result?.confirm()
          return true
        }

        override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
          result?.confirm()
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
      
      webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
          super.onPageFinished(view, url)
          isChatLoaded = true
          CookieManager.getInstance().flush()
          view?.loadUrl(JS_BYPASS_WARNINGS)
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
          result?.confirm()
          return true
        }

        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
          result?.confirm()
          return true
        }

        override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
          result?.confirm()
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
            .background(Color(0xFF6750A4))
            .then(inputModifier)
        ) {
          AndroidView(
            factory = { chatWebView },
            modifier = Modifier.fillMaxSize()
          )

          AnimatedVisibility(
            visible = !isChatLoaded,
            enter = fadeIn(),
            exit = fadeOut()
          ) {
            LoadingScreen(
              message = "در حال ورود به چت...",
              backgroundColor = Color(0xFF6750A4),
              contentColor = Color.White
            )
          }
        }
      }

      // Credential Manager FAB (Bottom-Left Side, completely Circular absolute)
      Box(
        modifier = Modifier
          .align(AbsoluteAlignment.BottomLeft)
          .absolutePadding(bottom = 24.dp, left = 24.dp)
          .size(56.dp)
          .shadow(elevation = 6.dp, shape = CircleShape)
          .background(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = CircleShape
          )
          .clip(CircleShape)
          .clickable { showCredentialsSheet = true },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Filled.Lock,
          contentDescription = "مدیریت رمزها",
          tint = MaterialTheme.colorScheme.onTertiaryContainer,
          modifier = Modifier.size(24.dp)
        )
      }

      // Floating Action Button Overlay (Completely Circular, bottom-right side absolute)
      Box(
        modifier = Modifier
          .align(AbsoluteAlignment.BottomRight)
          .absolutePadding(bottom = 24.dp, right = 24.dp)
          .offset(y = fabYOffset)
          .size(56.dp)
          .shadow(elevation = 6.dp, shape = CircleShape)
          .background(
            color = if (chatExpanded) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape
          )
          .clip(CircleShape)
          .clickable { chatExpanded = !chatExpanded }
          .onGloballyPositioned { coordinates ->
            if (!chatExpanded) {
              val localPosition = coordinates.positionInParent()
              val size = coordinates.size
              fabPosition = Offset(
                x = localPosition.x + size.width / 2f,
                y = localPosition.y + size.height / 2f
              )
            }
          },
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
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
          } else {
            ChatLogoIcon(
              modifier = Modifier.size(24.dp),
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      }

      if (showCredentialsSheet) {
        CredentialsDialog(
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
          }
        )
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
