package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BridgeStatus
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.TerminalCodeBg
import com.example.ui.theme.TerminalCyan

@Composable
fun CurlGuideCard(
    status: BridgeStatus,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokenSuffix = if (status.tokenAuthEnabled) "?token=${status.token}" else ""
    val hostPort = "http://127.0.0.1:${status.port}"

    val curlStatus = "curl -s $hostPort/status$tokenSuffix"
    val curlExec = "curl -s -X POST $hostPort/exec$tokenSuffix \\\n  -H 'Content-Type: application/json' \\\n  -d '{\"command\": \"whoami\"}'"
    val curlInputTap = "curl -s -X POST $hostPort/input$tokenSuffix \\\n  -H 'Content-Type: application/json' \\\n  -d '{\"action\": \"tap\", \"x\": 540, \"y\": 1600}'"
    val curlInputSwipe = "curl -s -X POST $hostPort/input$tokenSuffix \\\n  -H 'Content-Type: application/json' \\\n  -d '{\"action\": \"swipe\", \"x1\": 540, \"y1\": 1800, \"x2\": 540, \"y2\": 900, \"duration_ms\": 300}'"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("curl_guide_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(SlateBorder)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.IntegrationInstructions,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HTTP API & cURL Reference",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            CurlSnippet(
                title = "1. Check Status (GET /status)",
                curl = curlStatus,
                context = context,
                testTag = "copy_curl_status"
            )

            Spacer(modifier = Modifier.height(12.dp))

            CurlSnippet(
                title = "2. Execute Shell Command (POST /exec)",
                curl = curlExec,
                context = context,
                testTag = "copy_curl_exec"
            )

            Spacer(modifier = Modifier.height(12.dp))

            CurlSnippet(
                title = "3. Tap Simulation (POST /input)",
                curl = curlInputTap,
                context = context,
                testTag = "copy_curl_tap"
            )

            Spacer(modifier = Modifier.height(12.dp))

            CurlSnippet(
                title = "4. Swipe Simulation (POST /input)",
                curl = curlInputSwipe,
                context = context,
                testTag = "copy_curl_swipe"
            )
        }
    }
}

@Composable
private fun CurlSnippet(
    title: String,
    curl: String,
    context: Context,
    testTag: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateTextSecondary
            )

            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("cURL Command", curl)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(28.dp).testTag(testTag)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Command",
                    tint = CyanPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalCodeBg)
                .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                .padding(10.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = curl,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = TerminalCyan
            )
        }
    }
}
