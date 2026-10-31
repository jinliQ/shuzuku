package com.example.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ConsoleCard
import com.example.ui.components.CurlGuideCard
import com.example.ui.components.RequestLogsCard
import com.example.ui.components.SecurityCard
import com.example.ui.components.ServerStatusCard
import com.example.ui.components.ShizukuStatusCard
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldConnected
import com.example.ui.theme.RedDanger
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SlateTextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BridgeScreen(
    viewModel: BridgeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val status by viewModel.status.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val autoStart by viewModel.autoStart.collectAsState()
    val testState by viewModel.testState.collectAsState()

    // Notification permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.refreshStatus()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = SlateBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyanContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Shizuku HTTP Bridge",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = "127.0.0.1:19870",
                                fontSize = 11.sp,
                                color = SlateTextSecondary
                            )
                        }
                    }
                },
                actions = {
                    // Status Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (status.serverRunning) EmeraldConnected.copy(alpha = 0.15f) else SlateBorder
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (status.serverRunning) EmeraldConnected else RedDanger)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (status.serverRunning) "ONLINE" else "OFFLINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (status.serverRunning) EmeraldConnected else SlateTextTertiary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.refreshStatus() },
                        modifier = Modifier.testTag("app_bar_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = SlateTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateBackground
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("main_scrollable_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Box(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                    ServerStatusCard(
                        status = status,
                        autoStart = autoStart,
                        onToggleService = { viewModel.toggleService(context) },
                        onToggleAutoStart = { viewModel.setAutoStart(it) }
                    )
                }
            }

            item {
                Box(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                    ShizukuStatusCard(
                        status = status,
                        onRequestPermission = {
                            activity?.let { viewModel.requestShizukuPermission(it) }
                        },
                        onRefresh = { viewModel.refreshStatus() }
                    )
                }
            }

            item {
                Box(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                    SecurityCard(
                        status = status,
                        onToggleTokenAuth = { viewModel.setTokenAuthEnabled(it) },
                        onRegenerateToken = { viewModel.regenerateToken() }
                    )
                }
            }

            item {
                Box(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                    ConsoleCard(
                        testState = testState,
                        onSelectTab = { viewModel.selectTestTab(it) },
                        onUpdateCommand = { viewModel.updateCommand(it) },
                        onUpdateTapCoordinates = { x, y -> viewModel.updateTapCoordinates(x, y) },
                        onRunTest = { viewModel.runTest() }
                    )
                }
            }

            item {
                Box(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                    CurlGuideCard(status = status)
                }
            }

            item {
                Box(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                    RequestLogsCard(
                        logs = logs,
                        onClearLogs = { viewModel.clearLogs() }
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Shizuku HTTP Bridge v1.0",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateTextTertiary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Bound strictly to 127.0.0.1 • Zero remote exposure",
                        fontSize = 10.sp,
                        color = SlateTextTertiary
                    )
                }
            }
        }
    }
}
