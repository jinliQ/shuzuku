package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BridgeStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldConnected
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.RedDanger
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SlateTextTertiary

@Composable
fun ServerStatusCard(
    status: BridgeStatus,
    autoStart: Boolean,
    onToggleService: () -> Unit,
    onToggleAutoStart: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = status.serverRunning
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("server_status_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (isRunning) EmeraldConnected.copy(alpha = 0.6f) else SlateBorder)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRunning) EmeraldConnected.copy(alpha = pulseAlpha) else SlateTextTertiary
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "SERVER ACTIVE" else "SERVER STOPPED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = if (isRunning) EmeraldConnected else SlateTextTertiary
                    )
                }

                // Security Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyanContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Localhost Only",
                        modifier = Modifier.size(12.dp),
                        tint = CyanPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "127.0.0.1 ONLY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Address display
            Text(
                text = "Listening on",
                fontSize = 12.sp,
                color = SlateTextSecondary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "http://${status.host}:${status.port}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = SlateTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Action Button
            Button(
                onClick = onToggleService,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("toggle_server_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) RedDanger else CyanPrimary
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Stop Bridge Server" else "Start Bridge Server",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Auto-start switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlateSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Auto-start on launch",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "Keep listening in background via foreground service",
                        fontSize = 11.sp,
                        color = SlateTextSecondary
                    )
                }
                Switch(
                    checked = autoStart,
                    onCheckedChange = onToggleAutoStart,
                    modifier = Modifier.testTag("autostart_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyanPrimary,
                        checkedTrackColor = CyanContainer
                    )
                )
            }
        }
    }
}

@Composable
fun ShizukuStatusCard(
    status: BridgeStatus,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shizuku_status_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                when {
                    status.shizukuAuthorized -> EmeraldConnected.copy(alpha = 0.5f)
                    status.shizukuRunning -> AmberWarning.copy(alpha = 0.5f)
                    else -> SlateBorder
                }
            )
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Shizuku Framework",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp).testTag("refresh_shizuku_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Status",
                        tint = SlateTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status items
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Running State Chip
                StatusMetricBox(
                    label = "Service",
                    value = if (status.shizukuRunning) "Running" else "Stopped",
                    isGood = status.shizukuRunning,
                    modifier = Modifier.weight(1f)
                )

                // Auth State Chip
                StatusMetricBox(
                    label = "Permission",
                    value = if (status.shizukuAuthorized) "Authorized" else "Unauthorized",
                    isGood = status.shizukuAuthorized,
                    modifier = Modifier.weight(1f)
                )

                // UID / Version
                StatusMetricBox(
                    label = "Privilege",
                    value = when (status.shizukuUid) {
                        0 -> "Root (0)"
                        2000 -> "ADB (2000)"
                        -1 -> "N/A"
                        else -> "UID ${status.shizukuUid}"
                    },
                    isGood = status.shizukuUid >= 0,
                    modifier = Modifier.weight(1f)
                )
            }

            // Action banner if permission not granted
            AnimatedVisibility(visible = !status.shizukuAuthorized) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (status.shizukuRunning) AmberWarning.copy(alpha = 0.15f) else RedDanger.copy(alpha = 0.12f))
                            .border(
                                1.dp,
                                if (status.shizukuRunning) AmberWarning.copy(alpha = 0.4f) else RedDanger.copy(alpha = 0.3f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (status.shizukuRunning) Icons.Default.Warning else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (status.shizukuRunning) AmberWarning else RedDanger,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (status.shizukuRunning) {
                                    "Shizuku is running, but permission is not yet granted to this bridge app."
                                } else {
                                    "Shizuku service is not running. Please start Shizuku Manager via wireless debugging or root."
                                },
                                fontSize = 12.sp,
                                color = SlateTextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (status.shizukuRunning) {
                            Button(
                                onClick = onRequestPermission,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("request_permission_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                            ) {
                                Text("Grant Permission", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        OutlinedButton(
                            onClick = { launchShizukuApp(context) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("open_shizuku_app_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Open Shizuku App", color = SlateTextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMetricBox(
    label: String,
    value: String,
    isGood: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SlateSurfaceVariant)
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = SlateTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isGood) EmeraldConnected else RedDanger)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isGood) SlateTextPrimary else SlateTextSecondary
                )
            }
        }
    }
}

private fun launchShizukuApp(context: Context) {
    val pm = context.packageManager
    val launchIntent = pm.getLaunchIntentForPackage("moe.shizuku.privileged.api")
    if (launchIntent != null) {
        context.startActivity(launchIntent)
    } else {
        // Fallback: try opening settings or application details
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:moe.shizuku.privileged.api")
            }
            context.startActivity(intent)
        } catch (_: Throwable) {}
    }
}
