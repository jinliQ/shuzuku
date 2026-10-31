package com.example.model

data class BridgeStatus(
    val serverRunning: Boolean = false,
    val host: String = "127.0.0.1",
    val port: Int = 19870,
    val shizukuRunning: Boolean = false,
    val shizukuAuthorized: Boolean = false,
    val shizukuVersion: Int = 0,
    val shizukuUid: Int = -1,
    val tokenAuthEnabled: Boolean = false,
    val token: String = ""
)

data class ExecResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val durationMs: Long = 0
)

data class InputResult(
    val success: Boolean,
    val message: String
)

data class LogEntry(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val path: String,
    val clientIp: String,
    val statusCode: Int,
    val message: String,
    val durationMs: Long = 0
)
