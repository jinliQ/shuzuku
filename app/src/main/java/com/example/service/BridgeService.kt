package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.BridgeManager
import com.example.server.BridgeHttpServer
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BridgeService : Service() {

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        private const val CHANNEL_ID = "shizuku_bridge_channel"
        private const val NOTIFICATION_ID = 19870

        fun start(context: Context) {
            val intent = Intent(context, BridgeService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BridgeService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var httpServer: BridgeHttpServer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var statusObserverJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeStatusChanges()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopBridgeServer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundWithNotification()
                startBridgeServer()
                return START_STICKY
            }
        }
    }

    private fun startBridgeServer() {
        if (httpServer == null) {
            try {
                val server = BridgeHttpServer(
                    host = "127.0.0.1",
                    port = BridgeManager.status.value.port
                )
                server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                httpServer = server
                BridgeManager.setServerRunning(true)
                updateNotification()
            } catch (e: Throwable) {
                BridgeManager.setServerRunning(false)
            }
        }
    }

    private fun stopBridgeServer() {
        try {
            httpServer?.stop()
        } catch (_: Throwable) {}
        httpServer = null
        BridgeManager.setServerRunning(false)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Shizuku Bridge Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the local Shizuku HTTP bridge server running in background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BridgeService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val s = BridgeManager.status.value
        val shizukuText = when {
            !s.shizukuRunning -> "Shizuku: Not Running"
            !s.shizukuAuthorized -> "Shizuku: Unauthorized"
            else -> "Shizuku: Ready (UID ${s.shizukuUid})"
        }

        val contentText = "127.0.0.1:${s.port} • $shizukuText"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Shizuku HTTP Bridge Active")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun observeStatusChanges() {
        statusObserverJob?.cancel()
        statusObserverJob = scope.launch {
            BridgeManager.status.collect {
                if (httpServer != null) {
                    updateNotification()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        statusObserverJob?.cancel()
        stopBridgeServer()
    }
}
