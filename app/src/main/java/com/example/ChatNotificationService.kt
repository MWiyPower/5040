package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat

class ChatNotificationService : Service() {

    private var webView: WebView? = null
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val NOTIFICATION_ID = 9982
        private const val CHANNEL_ID = "chat_notification_service_channel"

        fun start(context: Context) {
            val intent = Intent(context, ChatNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ChatNotificationService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("آماده دریافت پیام...", "برنامه در پس‌زمینه فعال است"))

        // Create WebView on the Main Thread
        handler.post {
            initWebView()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "سرویس پیام‌رسان",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "پایش پیام‌های جدید در پس‌زمینه"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, body: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_chat", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(com.example.R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)

        return builder.build()
    }

    private fun initWebView() {
        try {
            webView = WebView(applicationContext).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }

                CookieManager.getInstance().setAcceptCookie(true)

                addJavascriptInterface(object : Any() {
                    @android.webkit.JavascriptInterface
                    fun onNewMessageWithDetails(title: String, body: String) {
                        handler.post {
                            // If MainActivity is in foreground, we don't need to post a system notification
                            if (!MainActivity.isAppInForeground) {
                                showSystemNotification(applicationContext, title, body)
                            }
                        }
                    }
                    
                    @android.webkit.JavascriptInterface
                    fun onNewMessage() {
                        handler.post {
                            if (!MainActivity.isAppInForeground) {
                                showSystemNotification(applicationContext, "پیام جدید", "شما یک پیام خوانده‌نشده دارید")
                            }
                        }
                    }
                }, "AndroidApp")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Inject the notification polling script
                        view?.loadUrl(JS_NOTIFICATION_SCRIPT)
                    }
                }

                loadUrl("https://chat.5040.me")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        handler.post {
            webView?.destroy()
            webView = null
        }
        super.onDestroy()
    }
}

private const val JS_NOTIFICATION_SCRIPT = "javascript:(function() { " +
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
    "} catch(e) {} " +
    "})();"
