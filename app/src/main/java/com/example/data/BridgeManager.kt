package com.example.data

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import com.example.model.BridgeStatus
import com.example.model.LogEntry
import com.example.shizuku.ShizukuExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.util.UUID

object BridgeManager {

    private const val PREFS_NAME = "shizuku_bridge_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_TOKEN_ENABLED = "token_auth_enabled"
    private const val KEY_PORT = "server_port"
    private const val KEY_AUTO_START = "auto_start_server"

    const val DEFAULT_PORT = 19870
    const val REQUEST_CODE_SHIZUKU_PERMISSION = 1001

    private val scope = CoroutineScope(Dispatchers.Main)
    private var prefs: SharedPreferences? = null

    private val _status = MutableStateFlow(BridgeStatus())
    val status: StateFlow<BridgeStatus> = _status.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _autoStart = MutableStateFlow(true)
    val autoStart: StateFlow<Boolean> = _autoStart.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshShizukuStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        refreshShizukuStatus()
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_SHIZUKU_PERMISSION) {
            refreshShizukuStatus()
        }
    }

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p

        var savedToken = p.getString(KEY_TOKEN, null)
        if (savedToken.isNullOrBlank()) {
            savedToken = UUID.randomUUID().toString().replace("-", "").take(16)
            p.edit().putString(KEY_TOKEN, savedToken).apply()
        }
        val tokenEnabled = p.getBoolean(KEY_TOKEN_ENABLED, false)
        val port = p.getInt(KEY_PORT, DEFAULT_PORT)
        val autoStartPref = p.getBoolean(KEY_AUTO_START, true)
        _autoStart.value = autoStartPref

        _status.update {
            it.copy(
                port = port,
                tokenAuthEnabled = tokenEnabled,
                token = savedToken
            )
        }

        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (_: Throwable) {}

        refreshShizukuStatus()
    }

    fun refreshShizukuStatus() {
        val isRunning = ShizukuExecutor.isShizukuRunning()
        val isAuthorized = ShizukuExecutor.isAuthorized()
        val version = ShizukuExecutor.getVersion()
        val uid = ShizukuExecutor.getUid()

        _status.update {
            it.copy(
                shizukuRunning = isRunning,
                shizukuAuthorized = isAuthorized,
                shizukuVersion = version,
                shizukuUid = uid
            )
        }
    }

    fun setServerRunning(running: Boolean) {
        _status.update { it.copy(serverRunning = running) }
    }

    fun setTokenAuthEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_TOKEN_ENABLED, enabled)?.apply()
        _status.update { it.copy(tokenAuthEnabled = enabled) }
    }

    fun regenerateToken(): String {
        val newToken = UUID.randomUUID().toString().replace("-", "").take(16)
        prefs?.edit()?.putString(KEY_TOKEN, newToken)?.apply()
        _status.update { it.copy(token = newToken) }
        return newToken
    }

    fun setAutoStart(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_AUTO_START, enabled)?.apply()
        _autoStart.value = enabled
    }

    fun requestShizukuPermission(activity: Activity) {
        try {
            if (Shizuku.getVersion() < 11) {
                // Pre v11 is not supported
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                refreshShizukuStatus()
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU_PERMISSION)
            } else {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU_PERMISSION)
            }
        } catch (_: Throwable) {
            refreshShizukuStatus()
        }
    }

    fun addLog(log: LogEntry) {
        scope.launch {
            _logs.update { current ->
                (listOf(log) + current).take(100) // keep last 100 entries
            }
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
