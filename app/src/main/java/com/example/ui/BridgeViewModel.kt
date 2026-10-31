package com.example.ui

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BridgeManager
import com.example.model.BridgeStatus
import com.example.model.LogEntry
import com.example.service.BridgeService
import com.example.shizuku.ShizukuExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class TestTab {
    STATUS, EXEC, INPUT
}

data class TestUiState(
    val selectedTab: TestTab = TestTab.EXEC,
    val command: String = "whoami && id",
    val tapX: String = "540",
    val tapY: String = "1600",
    val swipeX1: String = "540",
    val swipeY1: String = "1800",
    val swipeX2: String = "540",
    val swipeY2: String = "900",
    val swipeDurationMs: String = "300",
    val isRunning: Boolean = false,
    val output: String = "",
    val isSuccess: Boolean? = null
)

class BridgeViewModel : ViewModel() {

    val status: StateFlow<BridgeStatus> = BridgeManager.status
    val logs: StateFlow<List<LogEntry>> = BridgeManager.logs
    val autoStart: StateFlow<Boolean> = BridgeManager.autoStart

    private val _testState = MutableStateFlow(TestUiState())
    val testState: StateFlow<TestUiState> = _testState.asStateFlow()

    fun toggleService(context: Context) {
        if (status.value.serverRunning) {
            BridgeService.stop(context)
        } else {
            BridgeService.start(context)
        }
    }

    fun refreshStatus() {
        BridgeManager.refreshShizukuStatus()
    }

    fun requestShizukuPermission(activity: Activity) {
        BridgeManager.requestShizukuPermission(activity)
    }

    fun setTokenAuthEnabled(enabled: Boolean) {
        BridgeManager.setTokenAuthEnabled(enabled)
    }

    fun regenerateToken() {
        BridgeManager.regenerateToken()
    }

    fun setAutoStart(enabled: Boolean) {
        BridgeManager.setAutoStart(enabled)
    }

    fun clearLogs() {
        BridgeManager.clearLogs()
    }

    fun selectTestTab(tab: TestTab) {
        _testState.update { it.copy(selectedTab = tab) }
    }

    fun updateCommand(cmd: String) {
        _testState.update { it.copy(command = cmd) }
    }

    fun updateTapCoordinates(x: String, y: String) {
        _testState.update { it.copy(tapX = x, tapY = y) }
    }

    fun updateSwipeParameters(x1: String, y1: String, x2: String, y2: String, duration: String) {
        _testState.update {
            it.copy(
                swipeX1 = x1,
                swipeY1 = y1,
                swipeX2 = x2,
                swipeY2 = y2,
                swipeDurationMs = duration
            )
        }
    }

    fun runTest() {
        viewModelScope.launch {
            _testState.update { it.copy(isRunning = true, output = "Running test...") }
            val state = _testState.value

            when (state.selectedTab) {
                TestTab.STATUS -> {
                    BridgeManager.refreshShizukuStatus()
                    val s = BridgeManager.status.value
                    val json = JSONObject().apply {
                        put("shizuku_running", s.shizukuRunning)
                        put("authorized", s.shizukuAuthorized)
                        put("version", s.shizukuVersion.toString())
                        put("uid", s.shizukuUid)
                        put("server_running", s.serverRunning)
                        put("host", s.host)
                        put("port", s.port)
                        put("token_required", s.tokenAuthEnabled)
                    }
                    _testState.update {
                        it.copy(
                            isRunning = false,
                            output = json.toString(2),
                            isSuccess = true
                        )
                    }
                }
                TestTab.EXEC -> {
                    val result = ShizukuExecutor.execute(state.command)
                    val json = JSONObject().apply {
                        put("stdout", result.stdout)
                        put("stderr", result.stderr)
                        put("exit_code", result.exitCode)
                        put("duration_ms", result.durationMs)
                    }
                    _testState.update {
                        it.copy(
                            isRunning = false,
                            output = json.toString(2),
                            isSuccess = result.exitCode == 0
                        )
                    }
                }
                TestTab.INPUT -> {
                    val x = state.tapX.toIntOrNull() ?: 540
                    val y = state.tapY.toIntOrNull() ?: 1600
                    val result = ShizukuExecutor.executeInputTap(x, y)
                    val json = JSONObject().apply {
                        put("success", result.success)
                        put("message", result.message)
                    }
                    _testState.update {
                        it.copy(
                            isRunning = false,
                            output = json.toString(2),
                            isSuccess = result.success
                        )
                    }
                }
            }
        }
    }
}
