package com.example

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.content.Intent
import android.util.Log
import java.io.IOException

class AppVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    companion object {
        const val ACTION_START = "com.example.START_VPN"
        const val ACTION_STOP = "com.example.STOP_VPN"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
        }
        return START_NOT_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) {
            Log.d("AppVpnService", "VPN is already running")
            return
        }
        try {
            val builder = Builder()
                .setSession("AppSpecificVoipVpn")
                .addAddress("10.8.0.2", 24)
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)

            // Make the VPN only active for this application (Per-app VPN)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                try {
                    builder.addAllowedApplication(packageName)
                    Log.d("AppVpnService", "Successfully configured VPN for exclusive app use: $packageName")
                } catch (e: Exception) {
                    Log.e("AppVpnService", "Error setting addAllowedApplication", e)
                }
            }

            vpnInterface = builder.establish()
            Log.d("AppVpnService", "VPN Interface established successfully")
        } catch (e: Exception) {
            Log.e("AppVpnService", "Failed to establish VPN Interface", e)
        }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            Log.d("AppVpnService", "VPN Interface closed")
        } catch (e: IOException) {
            Log.e("AppVpnService", "Error closing VPN Interface", e)
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
