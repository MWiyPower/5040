package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

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
    if (progress >= 1f) {
      return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
    }
    val maxRadius = kotlin.math.hypot(
      kotlin.math.max(centerOffset.x, size.width - centerOffset.x),
      kotlin.math.max(centerOffset.y, size.height - centerOffset.y)
    )
    val radius = maxRadius * progress
    val path = Path().apply {
      addOval(
        Rect(
          left = centerOffset.x - radius,
          top = centerOffset.y - radius,
          right = centerOffset.x + radius,
          bottom = centerOffset.y + radius
        )
      )
    }
    return Outline.Generic(path)
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
      useWideViewPort = true
      loadWithOverviewMode = true
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

  var containerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
  var fabPosition by remember { mutableStateOf(Offset(1000f, 2000f)) }

  val bubbleProgress by animateFloatAsState(
    targetValue = if (chatExpanded) 1f else 0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "BubbleReveal"
  )

  LaunchedEffect(chatExpanded) {
    if (chatExpanded) {
      chatInitialized = true
    }
  }

  var mainWebViewRef by remember { mutableStateOf<WebView?>(null) }
  var chatWebViewRef by remember { mutableStateOf<WebView?>(null) }

  val context = LocalContext.current
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

  Box(
    modifier = Modifier
      .fillMaxSize()
      .onGloballyPositioned { containerCoordinates = it }
  ) {
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
        LoadingScreen(message = "در حال ورود به پنل...")
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
          }
          .clip(CircularRevealShape(bubbleProgress, fabPosition))
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
          LoadingScreen(message = "در حال ورود به چت...")
        }
      }
    }

    // Floating Action Button Overlay (Completely Circular, bottom-right aligned in RTL)
    Box(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(bottom = 24.dp, start = 24.dp)
        .size(56.dp)
        .shadow(elevation = 6.dp, shape = CircleShape)
        .background(
          color = if (chatExpanded) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
          shape = CircleShape
        )
        .clip(CircleShape)
        .clickable { chatExpanded = !chatExpanded }
        .onGloballyPositioned { coordinates ->
          containerCoordinates?.let { parent ->
            val localPosition = parent.localPositionOf(coordinates, Offset.Zero)
            val size = coordinates.size
            fabPosition = Offset(
              x = localPosition.x + size.width / 2f,
              y = localPosition.y + size.height / 2f
            )
          }
        },
      contentAlignment = Alignment.Center
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
}

@Composable
fun LoadingScreen(message: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .clickable(enabled = true, onClick = {}),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(24.dp)
    ) {
      CircularProgressIndicator(
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 4.dp,
        modifier = Modifier.size(56.dp)
      )
      Spacer(modifier = Modifier.height(24.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Right
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
