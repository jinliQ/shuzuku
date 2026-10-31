package com.example.shizuku

import android.content.pm.PackageManager
import com.example.model.ExecResult
import com.example.model.InputResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object ShizukuExecutor {

    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun isAuthorized(): Boolean {
        return try {
            if (!isShizukuRunning()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun getVersion(): Int {
        return try {
            if (isShizukuRunning()) Shizuku.getVersion() else 0
        } catch (_: Throwable) {
            0
        }
    }

    fun getUid(): Int {
        return try {
            if (isShizukuRunning()) Shizuku.getUid() else -1
        } catch (_: Throwable) {
            -1
        }
    }

    suspend fun execute(command: String, timeoutSec: Long = 30): ExecResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (!isShizukuRunning()) {
            return@withContext ExecResult(
                stdout = "",
                stderr = "Shizuku service is not running. Please start Shizuku first.",
                exitCode = -1,
                durationMs = System.currentTimeMillis() - startTime
            )
        }

        if (!isAuthorized()) {
            return@withContext ExecResult(
                stdout = "",
                stderr = "Shizuku permission not granted. Please authorize this app in Shizuku.",
                exitCode = -1,
                durationMs = System.currentTimeMillis() - startTime
            )
        }

        try {
            val cmdArray = arrayOf("sh", "-c", command)
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }

            val process = newProcessMethod.invoke(null, cmdArray, null, null) as Process

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val stdoutThread = Thread {
                try {
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            if (stdoutBuilder.isNotEmpty()) stdoutBuilder.append("\n")
                            stdoutBuilder.append(line)
                        }
                    }
                } catch (_: Throwable) {}
            }

            val stderrThread = Thread {
                try {
                    BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            if (stderrBuilder.isNotEmpty()) stderrBuilder.append("\n")
                            stderrBuilder.append(line)
                        }
                    }
                } catch (_: Throwable) {}
            }

            stdoutThread.start()
            stderrThread.start()

            val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext ExecResult(
                    stdout = stdoutBuilder.toString(),
                    stderr = "Process timed out after $timeoutSec seconds",
                    exitCode = 124,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }

            stdoutThread.join(1000)
            stderrThread.join(1000)

            val exitCode = process.exitValue()
            ExecResult(
                stdout = stdoutBuilder.toString(),
                stderr = stderrBuilder.toString(),
                exitCode = exitCode,
                durationMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Throwable) {
            ExecResult(
                stdout = "",
                stderr = "Execution error: ${e.message ?: e.javaClass.simpleName}",
                exitCode = -1,
                durationMs = System.currentTimeMillis() - startTime
            )
        }
    }

    suspend fun executeInputTap(x: Int, y: Int): InputResult {
        val result = execute("input tap $x $y")
        return if (result.exitCode == 0) {
            InputResult(success = true, message = "Tap executed at ($x, $y)")
        } else {
            InputResult(
                success = false,
                message = if (result.stderr.isNotBlank()) result.stderr else "Exit code: ${result.exitCode}"
            )
        }
    }

    suspend fun executeInputSwipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long): InputResult {
        val result = execute("input swipe $x1 $y1 $x2 $y2 $durationMs")
        return if (result.exitCode == 0) {
            InputResult(success = true, message = "Swipe executed from ($x1, $y1) to ($x2, $y2) in ${durationMs}ms")
        } else {
            InputResult(
                success = false,
                message = if (result.stderr.isNotBlank()) result.stderr else "Exit code: ${result.exitCode}"
            )
        }
    }
}
