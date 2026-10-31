package com.example.server

import com.example.data.BridgeManager
import com.example.model.LogEntry
import com.example.shizuku.ShizukuExecutor
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.net.InetAddress

class BridgeHttpServer(
    host: String = "127.0.0.1",
    port: Int = BridgeManager.DEFAULT_PORT
) : NanoHTTPD(host, port) {

    override fun serve(session: IHTTPSession): Response {
        val startTime = System.currentTimeMillis()
        val clientIp = session.remoteIpAddress ?: "127.0.0.1"
        val method = session.method
        val uri = session.uri ?: "/"

        // Security Check 1: Restrict to localhost only
        if (!isLoopback(clientIp)) {
            val res = jsonResponse(
                Response.Status.FORBIDDEN,
                JSONObject().apply {
                    put("error", "Forbidden: Only localhost connections are permitted")
                }
            )
            logRequest(method.name, uri, clientIp, Response.Status.FORBIDDEN.requestStatus, "Rejected non-loopback IP", startTime)
            return res
        }

        // Handle CORS preflight
        if (method == Method.OPTIONS) {
            val response = newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "")
            addCorsHeaders(response)
            return response
        }

        // Security Check 2: Token verification if enabled
        val currentStatus = BridgeManager.status.value
        if (currentStatus.tokenAuthEnabled) {
            val tokenFromQuery = session.parms["token"]
            val authHeader = session.headers["authorization"]?.removePrefix("Bearer ")?.trim()
            val customHeader = session.headers["x-bridge-token"]?.trim()
            val providedToken = tokenFromQuery ?: authHeader ?: customHeader

            if (providedToken.isNullOrBlank() || providedToken != currentStatus.token) {
                val res = jsonResponse(
                    Response.Status.UNAUTHORIZED,
                    JSONObject().apply {
                        put("error", "Unauthorized: Valid token required via ?token=, Authorization header, or X-Bridge-Token")
                    }
                )
                logRequest(method.name, uri, clientIp, Response.Status.UNAUTHORIZED.requestStatus, "Invalid or missing token", startTime)
                return res
            }
        }

        // Route handling
        return try {
            when {
                method == Method.GET && (uri == "/status" || uri == "/") -> {
                    handleStatus(startTime, clientIp)
                }
                method == Method.POST && uri == "/exec" -> {
                    handleExec(session, startTime, clientIp)
                }
                method == Method.POST && uri == "/input" -> {
                    handleInput(session, startTime, clientIp)
                }
                else -> {
                    val notFoundObj = JSONObject().apply {
                        put("error", "Endpoint not found: ${method.name} $uri")
                        put("endpoints", org.json.JSONArray().apply {
                            put("GET /status")
                            put("POST /exec")
                            put("POST /input")
                        })
                    }
                    val res = jsonResponse(Response.Status.NOT_FOUND, notFoundObj)
                    logRequest(method.name, uri, clientIp, Response.Status.NOT_FOUND.requestStatus, "Not found", startTime)
                    res
                }
            }
        } catch (e: Throwable) {
            val errObj = JSONObject().apply {
                put("error", "Internal server error: ${e.message}")
            }
            val res = jsonResponse(Response.Status.INTERNAL_ERROR, errObj)
            logRequest(method.name, uri, clientIp, Response.Status.INTERNAL_ERROR.requestStatus, "Error: ${e.message}", startTime)
            res
        }
    }

    private fun handleStatus(startTime: Long, clientIp: String): Response {
        BridgeManager.refreshShizukuStatus()
        val s = BridgeManager.status.value
        val json = JSONObject().apply {
            put("shizuku_running", s.shizukuRunning)
            put("authorized", s.shizukuAuthorized)
            put("version", s.shizukuVersion.toString())
            put("uid", s.shizukuUid)
            put("server_running", true)
            put("host", s.host)
            put("port", s.port)
            put("token_required", s.tokenAuthEnabled)
        }
        val res = jsonResponse(Response.Status.OK, json)
        logRequest("GET", "/status", clientIp, 200, "Status queried", startTime)
        return res
    }

    private fun handleExec(session: IHTTPSession, startTime: Long, clientIp: String): Response {
        val bodyStr = getRequestBody(session)
        if (bodyStr.isBlank()) {
            val res = jsonResponse(
                Response.Status.BAD_REQUEST,
                JSONObject().apply { put("error", "Request body is empty. Expecting JSON: {\"command\": \"...\"}") }
            )
            logRequest("POST", "/exec", clientIp, 400, "Empty body", startTime)
            return res
        }

        val jsonInput = try {
            JSONObject(bodyStr)
        } catch (e: Exception) {
            val res = jsonResponse(
                Response.Status.BAD_REQUEST,
                JSONObject().apply { put("error", "Invalid JSON body: ${e.message}") }
            )
            logRequest("POST", "/exec", clientIp, 400, "JSON parse error", startTime)
            return res
        }

        val command = jsonInput.optString("command", "")
        if (command.isBlank()) {
            val res = jsonResponse(
                Response.Status.BAD_REQUEST,
                JSONObject().apply { put("error", "Missing required field 'command'") }
            )
            logRequest("POST", "/exec", clientIp, 400, "Missing command field", startTime)
            return res
        }

        val timeoutSec = jsonInput.optLong("timeout_sec", 30L)
        val result = runBlocking {
            ShizukuExecutor.execute(command, timeoutSec)
        }

        val responseJson = JSONObject().apply {
            put("stdout", result.stdout)
            put("stderr", result.stderr)
            put("exit_code", result.exitCode)
        }

        val res = jsonResponse(Response.Status.OK, responseJson)
        val summary = "cmd: '$command' -> exit: ${result.exitCode}"
        logRequest("POST", "/exec", clientIp, 200, summary, startTime)
        return res
    }

    private fun handleInput(session: IHTTPSession, startTime: Long, clientIp: String): Response {
        val bodyStr = getRequestBody(session)
        if (bodyStr.isBlank()) {
            val res = jsonResponse(
                Response.Status.BAD_REQUEST,
                JSONObject().apply { put("error", "Request body is empty") }
            )
            logRequest("POST", "/input", clientIp, 400, "Empty body", startTime)
            return res
        }

        val jsonInput = try {
            JSONObject(bodyStr)
        } catch (e: Exception) {
            val res = jsonResponse(
                Response.Status.BAD_REQUEST,
                JSONObject().apply { put("error", "Invalid JSON body: ${e.message}") }
            )
            logRequest("POST", "/input", clientIp, 400, "JSON parse error", startTime)
            return res
        }

        val action = jsonInput.optString("action", "").lowercase()
        val result = runBlocking {
            when (action) {
                "tap" -> {
                    val x = jsonInput.optInt("x", -1)
                    val y = jsonInput.optInt("y", -1)
                    if (x < 0 || y < 0) {
                        return@runBlocking Pair(false, "Invalid coordinates for tap: x=$x, y=$y")
                    }
                    val inputRes = ShizukuExecutor.executeInputTap(x, y)
                    Pair(inputRes.success, inputRes.message)
                }
                "swipe" -> {
                    val x1 = jsonInput.optInt("x1", -1)
                    val y1 = jsonInput.optInt("y1", -1)
                    val x2 = jsonInput.optInt("x2", -1)
                    val y2 = jsonInput.optInt("y2", -1)
                    val durationMs = jsonInput.optLong("duration_ms", 300L)
                    if (x1 < 0 || y1 < 0 || x2 < 0 || y2 < 0) {
                        return@runBlocking Pair(false, "Invalid coordinates for swipe: ($x1,$y1) -> ($x2,$y2)")
                    }
                    val inputRes = ShizukuExecutor.executeInputSwipe(x1, y1, x2, y2, durationMs)
                    Pair(inputRes.success, inputRes.message)
                }
                else -> {
                    Pair(false, "Unknown action: '$action'. Supported actions are 'tap' and 'swipe'")
                }
            }
        }

        val responseJson = JSONObject().apply {
            put("success", result.first)
            put("message", result.second)
        }

        val status = if (result.first) Response.Status.OK else Response.Status.BAD_REQUEST
        val res = jsonResponse(status, responseJson)
        logRequest("POST", "/input", clientIp, status.requestStatus, "action: $action -> ${result.second}", startTime)
        return res
    }

    private fun getRequestBody(session: IHTTPSession): String {
        val map = HashMap<String, String>()
        try {
            session.parseBody(map)
        } catch (_: Throwable) {}

        return map["postData"] ?: ""
    }

    private fun isLoopback(ip: String): Boolean {
        if (ip == "127.0.0.1" || ip == "localhost" || ip == "::1" || ip == "0:0:0:0:0:0:0:1") {
            return true
        }
        return try {
            val addr = InetAddress.getByName(ip)
            addr.isLoopbackAddress
        } catch (_: Throwable) {
            false
        }
    }

    private fun jsonResponse(status: Response.Status, json: JSONObject): Response {
        val response = newFixedLengthResponse(
            status,
            "application/json; charset=utf-8",
            json.toString(2)
        )
        addCorsHeaders(response)
        return response
    }

    private fun addCorsHeaders(response: Response) {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Bridge-Token")
    }

    private fun logRequest(
        method: String,
        path: String,
        clientIp: String,
        statusCode: Int,
        message: String,
        startTime: Long
    ) {
        val duration = System.currentTimeMillis() - startTime
        BridgeManager.addLog(
            LogEntry(
                method = method,
                path = path,
                clientIp = clientIp,
                statusCode = statusCode,
                message = message,
                durationMs = duration
            )
        )
    }
}
