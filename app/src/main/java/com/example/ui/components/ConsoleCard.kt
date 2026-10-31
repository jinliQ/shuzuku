package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TestTab
import com.example.ui.TestUiState
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldConnected
import com.example.ui.theme.RedDanger
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SlateTextTertiary
import com.example.ui.theme.TerminalCodeBg
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen

@Composable
fun ConsoleCard(
    testState: TestUiState,
    onSelectTab: (TestTab) -> Unit,
    onUpdateCommand: (String) -> Unit,
    onUpdateTapCoordinates: (String, String) -> Unit,
    onRunTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val quickPresets = listOf(
        "whoami && id",
        "getprop ro.product.model",
        "pm list packages -3 | head -n 5",
        "dumpsys battery | grep level",
        "uptime"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("console_card"),
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
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Interactive Test Console",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = testState.selectedTab.ordinal,
                containerColor = SlateSurfaceVariant,
                contentColor = CyanPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[testState.selectedTab.ordinal]),
                        color = CyanPrimary
                    )
                },
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = testState.selectedTab == TestTab.EXEC,
                    onClick = { onSelectTab(TestTab.EXEC) },
                    text = { Text("POST /exec", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_exec")
                )
                Tab(
                    selected = testState.selectedTab == TestTab.INPUT,
                    onClick = { onSelectTab(TestTab.INPUT) },
                    text = { Text("POST /input", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_input")
                )
                Tab(
                    selected = testState.selectedTab == TestTab.STATUS,
                    onClick = { onSelectTab(TestTab.STATUS) },
                    text = { Text("GET /status", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_status")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Content
            when (testState.selectedTab) {
                TestTab.EXEC -> {
                    Text(
                        text = "Shell Command",
                        fontSize = 12.sp,
                        color = SlateTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = testState.command,
                        onValueChange = onUpdateCommand,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("command_input_field"),
                        placeholder = { Text("e.g. ls -la /data/data", color = SlateTextTertiary) },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = SlateTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = SlateBorder,
                            focusedContainerColor = TerminalCodeBg,
                            unfocusedContainerColor = TerminalCodeBg
                        ),
                        singleLine = false,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickPresets.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SlateSurfaceElevated)
                                    .clickable { onUpdateCommand(preset) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TerminalCyan
                                )
                            }
                        }
                    }
                }
                TestTab.INPUT -> {
                    Text(
                        text = "Action: tap",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = testState.tapX,
                            onValueChange = { onUpdateTapCoordinates(it, testState.tapY) },
                            label = { Text("X coordinate") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tap_x_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = SlateTextPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = SlateBorder,
                                focusedContainerColor = TerminalCodeBg,
                                unfocusedContainerColor = TerminalCodeBg
                            )
                        )

                        OutlinedTextField(
                            value = testState.tapY,
                            onValueChange = { onUpdateTapCoordinates(testState.tapX, it) },
                            label = { Text("Y coordinate") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tap_y_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = SlateTextPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = SlateBorder,
                                focusedContainerColor = TerminalCodeBg,
                                unfocusedContainerColor = TerminalCodeBg
                            )
                        )
                    }
                }
                TestTab.STATUS -> {
                    Text(
                        text = "Query the current server and Shizuku status endpoint directly.",
                        fontSize = 12.sp,
                        color = SlateTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Execute Button
            Button(
                onClick = onRunTest,
                enabled = !testState.isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("run_test_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                if (testState.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Executing...", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Execute", fontWeight = FontWeight.SemiBold)
                }
            }

            // Output Terminal Box
            AnimatedVisibility(visible = testState.output.isNotBlank()) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Response Output",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateTextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (testState.isSuccess != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (testState.isSuccess == true) EmeraldConnected.copy(alpha = 0.2f) else RedDanger.copy(alpha = 0.2f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (testState.isSuccess == true) "200 OK" else "ERROR",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (testState.isSuccess == true) EmeraldConnected else RedDanger
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Response", testState.output)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Output copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp).testTag("copy_output_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Response",
                                tint = SlateTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TerminalCodeBg)
                            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = testState.output,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = if (testState.isSuccess == true) TerminalGreen else SlateTextPrimary
                        )
                    }
                }
            }
        }
    }
}
