package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun StaffDashboardScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GuestViewModel = viewModel()
) {
    val activeAlerts by viewModel.activeAlerts.collectAsState()

    val blackThemeBackground = Color(0xFF111318)
    val containerDeepDark = Color(0xFF0C0E12)
    val highAlertCoral = Color(0xFFFF5545)
    val safeNeonGreen = Color(0xFF53E16F)
    val statusOrange = Color(0xFFFFB874)
    val mutedBorderHex = Color(0xFF333539)

    var broadcastText by remember { mutableStateOf("") }
    var activeBroadcastingNotice by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Surface(
                color = blackThemeBackground,
                border = BorderStroke(1.dp, mutedBorderHex),
                modifier = Modifier.statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock",
                                tint = highAlertCoral,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "FORGE COMMAND",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            "STAFF BADGE CONSOLE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                    }

                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = containerDeepDark),
                        border = BorderStroke(1.dp, mutedBorderHex),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXIT", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        },
        containerColor = blackThemeBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "TOTAL ONSITE",
                    value = "1,420",
                    tint = Color.White,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "VERIFIED SAFE",
                    value = "1,398",
                    tint = safeNeonGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "UNACCOUNTED",
                    value = "${1420 - 1398}",
                    tint = highAlertCoral,
                    modifier = Modifier.weight(1f)
                )
            }

            // Broadcast box
            Surface(
                color = containerDeepDark,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, mutedBorderHex),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "BROADCAST GLOBAL SAFETY NOTICE & EVAC MAP GUIDELINES",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = broadcastText,
                            onValueChange = { broadcastText = it },
                            placeholder = {
                                Text(
                                    "e.g. Sector 2 corridor is safe. Proceed immediately...",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.3f)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = highAlertCoral,
                                unfocusedBorderColor = mutedBorderHex,
                                focusedContainerColor = blackThemeBackground,
                                unfocusedContainerColor = blackThemeBackground
                            ),
                            maxLines = 1,
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                        )

                        Button(
                            onClick = {
                                if (broadcastText.isNotBlank()) {
                                    val triage = TriageResult(
                                        severity = "HIGH",
                                        summary = "COMMAND BROADCAST: $broadcastText",
                                        targetArea = "Central Corridors",
                                        actionRequired = "Move along evacuation paths calculated on your floor plan."
                                    )
                                    viewModel.submitTriageAsLiveAlert(triage)
                                    activeBroadcastingNotice = broadcastText
                                    broadcastText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = highAlertCoral),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Text("SEND", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    AnimatedVisibility(visible = activeBroadcastingNotice != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "✓ Sent active safety notification to all hotel guest screens successfully.",
                            color = safeNeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // List Title
            Text(
                "ACTIVE THREAT REPORTS & AI TRIAGE queue",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            // Threat List queue
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeAlerts) { alert ->
                    val stripeColor = when (alert.severity) {
                        "CRITICAL" -> highAlertCoral
                        "HIGH" -> statusOrange
                        "MEDIUM" -> Color(0xFF4A90E2)
                        else -> safeNeonGreen
                    }

                    Surface(
                        color = containerDeepDark,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, mutedBorderHex),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .fillMaxHeight()
                                    .align(Alignment.CenterVertically)
                                    .background(stripeColor)
                                    .size(width = 6.dp, height = 75.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        alert.severity,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = stripeColor
                                    )
                                    Text(
                                        "ACTIVE ALERT",
                                        fontSize = 8.sp,
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    alert.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    alert.description,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0C0E12),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, Color(0xFF333539)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = tint
            )
        }
    }
}
