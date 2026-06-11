package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GuestDashboardScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GuestViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(1) } // 1: MAP, 2: SOS, 3: STATUS/FEED

    // ViewModel State collectors
    val guestPosition by viewModel.guestGridPosition.collectAsState()
    val safetyPath by viewModel.evacuationPath.collectAsState()
    val activeAlerts by viewModel.activeAlerts.collectAsState()
    val isSisActive by viewModel.sosState.collectAsState()

    // Cyberpunk Color Palette
    val blackThemeBackground = Color(0xFF111318)
    val containerDeepDark = Color(0xFF0C0E12)
    val highAlertCoral = Color(0xFFFF5545)
    val safeNeonGreen = Color(0xFF53E16F)
    val mutedBorderHex = Color(0xFF333539)
    val statusOrange = Color(0xFFFFB874)

    // Local screen controls
    var showTriageDialog by remember { mutableStateOf(false) }
    var currentSelectedRoomInMap by remember { mutableStateOf("Taj Mahal Corridor") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                                imageVector = Icons.Default.Build,
                                contentDescription = "Forge Logo",
                                tint = highAlertCoral,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "CODE FORGE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            "TAJ MAHAL HOTEL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                    }

                    // Precise GPS State indicator
                    Row(
                        modifier = Modifier
                            .background(
                                color = safeNeonGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(100.dp)
                            )
                            .border(1.dp, safeNeonGreen.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(safeNeonGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "PRECISE GPS ACTIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = safeNeonGreen,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = containerDeepDark,
                border = BorderStroke(1.dp, mutedBorderHex),
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabItem(
                        imageVector = Icons.Default.LocationOn,
                        label = "FLOOR MAP",
                        isActive = selectedTab == 1,
                        activeColor = safeNeonGreen,
                        onClick = { selectedTab = 1 }
                    )
                    TabItem(
                        imageVector = Icons.Default.Warning,
                        label = "SOS TRIGGER",
                        isActive = selectedTab == 2,
                        activeColor = highAlertCoral,
                        onClick = { selectedTab = 2 }
                    )
                    TabItem(
                        imageVector = Icons.Default.Lock,
                        label = "SAFETY FEED",
                        isActive = selectedTab == 3,
                        activeColor = statusOrange,
                        onClick = { selectedTab = 3 }
                    )
                }
            }
        },
        containerColor = blackThemeBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                1 -> FloorMapTab(
                    guestPosition = guestPosition,
                    evacuationPath = safetyPath,
                    onCellSelected = { room, point ->
                        currentSelectedRoomInMap = room
                        viewModel.setGuestGridPositionDirectly(point)
                    },
                    selectedRoomName = currentSelectedRoomInMap,
                    onReportHazard = { showTriageDialog = true }
                )
                2 -> SosTriggerTab(
                    isSosTriggered = isSisActive,
                    currentRoom = currentSelectedRoomInMap,
                    onTriggerSos = { viewModel.triggerSOSReport(currentSelectedRoomInMap) },
                    onMarkSafe = { viewModel.markSafeReport(currentSelectedRoomInMap) },
                    onMedicalAssistance = { viewModel.requestMedicalAssistance(currentSelectedRoomInMap) }
                )
                3 -> SafetyFeedTab(
                    alertsList = activeAlerts
                )
            }

            // floating report trigger if not on map (makes reporting extremely handy)
            if (selectedTab != 1) {
                FloatingActionButton(
                    onClick = { showTriageDialog = true },
                    containerColor = highAlertCoral,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .size(56.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Trigger AI Triage"
                    )
                }
            }

            // Gemini AI Triage modal backdrop
            if (showTriageDialog) {
                GeminiTriageDialog(
                    roomName = currentSelectedRoomInMap,
                    viewModel = viewModel,
                    onClose = { showTriageDialog = false }
                )
            }
        }
    }
}

@Composable
fun TabItem(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val alphaAnim by animateFloatAsState(targetValue = if (isActive) 1.0f else 0.5f)
    val scaleAnim by animateFloatAsState(targetValue = if (isActive) 1.05f else 0.95f)

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(12.dp)
            .width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = label,
            tint = if (isActive) activeColor else Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .size(22.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun FloorMapTab(
    guestPosition: GridPoint,
    evacuationPath: List<GridPoint>,
    selectedRoomName: String,
    onCellSelected: (String, GridPoint) -> Unit,
    onReportHazard: () -> Unit
) {
    val transition = rememberInfiniteTransition()
    val pulseRadius by transition.animateFloat(
        initialValue = 10f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Floor header details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFB874))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "TAJ MAHAL MAIN FLOOR PLAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                "GRID MODEL: 10 X 10",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.4f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactivity Instructions
        Surface(
            color = Color(0xFF0C0E12),
            border = BorderStroke(1.dp, Color(0xFF333539)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "💡 TAP ROOMS ON MAP TO TRIGGER INSTANT BFS SAFE Evacuation re-routing.",
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(10.dp),
                lineHeight = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Map Graphics Canvas & Selection Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(2.dp, Color(0xFF333539), RoundedCornerShape(4.dp))
                .background(Color(0xFF0C0E12))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val height = size.height
                            val cellW = width / 10f
                            val cellH = height / 10f

                            val gridX = (offset.x / cellW)
                                .toInt()
                                .coerceIn(0, 9)
                            val gridY = (offset.y / cellH)
                                .toInt()
                                .coerceIn(0, 9)

                            // Maps coordinates into named hotel rooms
                            val roomMatched = when {
                                gridX in 1..3 && gridY in 1..3 -> "Apollo Room"
                                gridX in 4..6 && gridY in 1..3 -> "Ball Room"
                                gridX in 1..3 && gridY in 6..8 -> "Crystal Room (North)"
                                gridX in 4..6 && gridY in 6..8 -> "Crystal Room (Central)"
                                gridX in 7..9 && gridY in 6..8 -> "Crystal Room (South)"
                                gridX == 5 && gridY == 0 -> "Exit N Corridor"
                                gridX == 5 && gridY == 9 -> "Exit S Corridor"
                                else -> "Floor Hallway Corridor"
                            }

                            onCellSelected(roomMatched, GridPoint(gridX, gridY))
                        }
                    }
            ) {
                val canvasW = size.width
                val canvasH = size.height
                val cellW = canvasW / 10f
                val cellH = canvasH / 10f

                // Draw standard grid guide background lines (Cyberpunk radar aesthetic)
                for (i in 0..10) {
                    // Vertical
                    drawLine(
                        color = Color(0xFF1E2024),
                        start = Offset(i * cellW, 0f),
                        end = Offset(i * cellW, canvasH),
                        strokeWidth = 1f
                    )
                    // Horizontal
                    drawLine(
                        color = Color(0xFF1E2024),
                        start = Offset(0f, i * cellH),
                        end = Offset(canvasW, i * cellH),
                        strokeWidth = 1f
                    )
                }

                // DRAW ROOM VECTOR SHAPES REPRESENTATIONS
                // 1. Apollo Room (cols 1..3, rows 1..3)
                drawRect(
                    color = Color(0xFF1E2024).copy(alpha = 0.8f),
                    topLeft = Offset(1 * cellW, 1 * cellH),
                    size = Size(3 * cellW, 3 * cellH)
                )
                drawRect(
                    color = Color(0xFF53E16F).copy(alpha = 0.3f), // safe neon border
                    topLeft = Offset(1 * cellW, 1 * cellH),
                    size = Size(3 * cellW, 3 * cellH),
                    style = Stroke(width = 2f)
                )

                // 2. Ball Room (Hazard obstacle! cols 4..6, rows 1..3 -> transparent red fire alert)
                drawRect(
                    color = Color(0xFFFF5545).copy(alpha = 0.25f),
                    topLeft = Offset(4 * cellW, 1 * cellH),
                    size = Size(3 * cellW, 3 * cellH)
                )
                drawRect(
                    color = Color(0xFFFF5545),
                    topLeft = Offset(4 * cellW, 1 * cellH),
                    size = Size(3 * cellW, 3 * cellH),
                    style = Stroke(width = 3f)
                )

                // 3. Crystal North (cols 1..3, rows 6..8)
                drawRect(
                    color = Color(0xFF1E2024),
                    topLeft = Offset(1 * cellW, 6 * cellH),
                    size = Size(3 * cellW, 3 * cellH)
                )
                drawRect(
                    color = Color(0xFF333539),
                    topLeft = Offset(1 * cellW, 6 * cellH),
                    size = Size(3 * cellW, 3 * cellH),
                    style = Stroke(width = 1.5f)
                )

                // 4. Crystal Central (cols 4..6, rows 6..8)
                drawRect(
                    color = Color(0xFF1E2024),
                    topLeft = Offset(4 * cellW, 6 * cellH),
                    size = Size(3 * cellW, 3 * cellH)
                )
                drawRect(
                    color = Color(0xFF333539),
                    topLeft = Offset(4 * cellW, 6 * cellH),
                    size = Size(3 * cellW, 3 * cellH),
                    style = Stroke(width = 1.5f)
                )

                // 5. Crystal South (cols 7..9, rows 6..8)
                drawRect(
                    color = Color(0xFF1E2024),
                    topLeft = Offset(7 * cellW, 6 * cellH),
                    size = Size(2.5f * cellW, 3 * cellH)
                )
                drawRect(
                    color = Color(0xFF333539),
                    topLeft = Offset(7 * cellW, 6 * cellH),
                    size = Size(2.5f * cellW, 3 * cellH),
                    style = Stroke(width = 1.5f)
                )

                // Draw Exits (North Exit (5,0) and South Exit (5,9))
                drawRect(
                    color = Color(0xFF53E16F), // Safe Exit Green
                    topLeft = Offset(5 * cellW, 0.1f * cellH),
                    size = Size(cellW, 0.5f * cellH)
                )
                drawRect(
                    color = Color(0xFF53E16F),
                    topLeft = Offset(5 * cellW, 9.4f * cellH),
                    size = Size(cellW, 0.5f * cellH)
                )

                // DRAW SAFE PATH (Dashed Green line linking current guest coordinates to EXIT N (5,0))
                if (evacuationPath.size > 1) {
                    for (i in 0 until evacuationPath.size - 1) {
                        val curr = evacuationPath[i]
                        val next = evacuationPath[i + 1]

                        drawLine(
                            color = Color(0xFF53E16F),
                            start = Offset((curr.x + 0.5f) * cellW, (curr.y + 0.5f) * cellH),
                            end = Offset((next.x + 0.5f) * cellW, (next.y + 0.5f) * cellH),
                            strokeWidth = 7f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                        )
                    }
                }

                // DRAW THE "YOU" LIVE PULSATING DOT
                val youX = (guestPosition.x + 0.5f) * cellW
                val youY = (guestPosition.y + 0.5f) * cellH

                // Pulsing safety ring
                drawCircle(
                    color = Color(0xFFFF5545).copy(alpha = pulseAlpha),
                    radius = pulseRadius * 2,
                    center = Offset(youX, youY)
                )

                // solid marker
                drawCircle(
                    color = Color(0xFFFF5545),
                    radius = 16f,
                    center = Offset(youX, youY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = Offset(youX, youY)
                )
            }

            // Simple Overlay Labels for display
            Box(modifier = Modifier.padding(12.dp).align(Alignment.TopStart)) {
                Text("APOLLO ROOM", color = Color.White.copy(alpha = 0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopCenter)
                    .offset(x = (-10).dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️ BALL ROOM", color = Color(0xFFFF5545), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text("SMOKE DETECTED", color = Color(0xFFFF5545), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-4).dp)
            ) {
                Text("EXIT NORTH", color = Color(0xFF53E16F), fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
            ) {
                Text("EXIT SOUTH", color = Color(0xFF53E16F), fontSize = 8.sp, fontWeight = FontWeight.Black)
            }

            // Live HUD overlay labels for Crystal Room blocks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp, start = 30.dp, end = 30.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("CRYSTAL N", color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text("CRYSTAL CENTRAL", color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text("CRYSTAL S", color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Cell Interactive Panel
        Surface(
            color = Color(0xFF1E2024),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, Color(0xFF333539)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "SELECTED ZONE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        selectedRoomName.uppercase(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Grid coordinates: X:${guestPosition.x}, Y:${guestPosition.y}",
                        fontSize = 10.sp,
                        color = Color(0xFF53E16F),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = onReportHazard,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5545)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Contact Forge AI",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("REPORT HAZARD", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SosTriggerTab(
    isSosTriggered: Boolean,
    currentRoom: String,
    onTriggerSos: () -> Unit,
    onMarkSafe: () -> Unit,
    onMedicalAssistance: () -> Unit
) {
    var holdTriggerTime by remember { mutableStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // long press circular simulation animation
    val holdPercentage = (holdTriggerTime / 2000f).coerceIn(0f, 1f)

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val startTime = System.currentTimeMillis()
            while (isHolding && holdTriggerTime < 2000f) {
                holdTriggerTime = (System.currentTimeMillis() - startTime).toFloat()
                delay(20)
            }
            if (holdTriggerTime >= 2000f) {
                onTriggerSos()
                holdTriggerTime = 0f
                isHolding = false
            }
        } else {
            holdTriggerTime = 0f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "CENTRAL COMMAND EMERGENCY TRIGGER",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "SOS BROADCAST PROTOCOL",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        // Circular holding SOS trigger with pulsating ring and progress radial border
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isHolding = true
                            tryAwaitRelease()
                            isHolding = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Draw background glow rings
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        BorderStroke(
                            2.dp,
                            if (isHolding) Color(0xFFFF5545).copy(alpha = 0.5f) else Color(0xFF333539)
                        ),
                        CircleShape
                    )
            )

            // Progress Radial Ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color(0xFFFF5545),
                    startAngle = -90f,
                    sweepAngle = holdPercentage * 360f,
                    useCenter = false,
                    style = Stroke(width = 16f)
                )
            }

            // Middle primary circle
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFE02B1B), Color(0xFF690003))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "SOS Button",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isHolding) "HOLD FORING..." else "PRESS & HOLD\nFOR SOS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    if (isHolding) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${(holdPercentage * 100).toInt()}% READY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB874)
                        )
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Current reporting point: $currentRoom",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Hold gesture triggers immediate security dispatch and Firestore localization.",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.35f),
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
        }

        // Action grid below: "I AM SAFE" and "MEDICAL ASSISTANCE"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onMarkSafe,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF53E16F).copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, Color(0xFF53E16F)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Safe Button",
                    tint = Color(0xFF53E16F),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "I AM SAFE",
                    color = Color(0xFF53E16F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Button(
                onClick = onMedicalAssistance,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB874).copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, Color(0xFFFFB874)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Medical Assistance Button",
                    tint = Color(0xFFFFB874),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "MEDICAL",
                    color = Color(0xFFFFB874),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun SafetyFeedTab(
    alertsList: List<SafetyAlert>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Alert feed icon",
                    tint = Color(0xFFFF5545),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "URGENT REAL-TIME FEED",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                "${alertsList.size} ALERTS ACTIVE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF5545)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (alertsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No critical risk alerts currently active.\nSystem green.",
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(alertsList) { alert ->
                    // Set severity colors
                    val stripeColor = when (alert.severity) {
                        "CRITICAL" -> Color(0xFFFF5545)
                        "HIGH" -> Color(0xFFFFB874)
                        "MEDIUM" -> Color(0xFF4A90E2)
                        else -> Color(0xFF53E16F)
                    }

                    Surface(
                        color = Color(0xFF1E2024),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, Color(0xFF333539)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Left border layout stripe
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
                                    .padding(14.dp)
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
                                        color = stripeColor,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        alert.timestamp,
                                        fontSize = 8.sp,
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    alert.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    alert.description,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    lineHeight = 14.sp
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
fun GeminiTriageDialog(
    roomName: String,
    viewModel: GuestViewModel,
    onClose: () -> Unit
) {
    var userText by remember { mutableStateOf("") }
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val triageResult by viewModel.triageResult.collectAsState()

    Surface(
        color = Color(0x99000000), // Semi-transparent overlay backdrop
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClose() },
        contentColor = Color.White
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color(0xFF1E2024),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF333539)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // block click propagation
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "AI Triage Robot",
                                tint = Color(0xFFFF5545),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "GEMINI THREAT TRIAGE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        "Report specific emergency hazard conditions observed around $roomName. Gemini will run threat analysis instantly.",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 13.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = userText,
                        onValueChange = { userText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "e.g. A localized fuse spark has caused small flames on the staircase near the central lobby...",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.3f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF5545),
                            unfocusedBorderColor = Color(0xFF333539),
                            focusedContainerColor = Color(0xFF0C0E12),
                            unfocusedContainerColor = Color(0xFF0C0E12)
                        ),
                        maxLines = 3,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triageReportWithGemini(userText, roomName) },
                            enabled = !isAnalyzing && userText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333539)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Analysis auto",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("RUN TRIAGE", fontSize = 10.sp)
                            }
                        }
                    }

                    // Triage results displaying area
                    if (triageResult != null) {
                        val colorMap = when (triageResult?.severity) {
                            "CRITICAL" -> Color(0xFFFF5545)
                            "HIGH" -> Color(0xFFFFB874)
                            "MEDIUM" -> Color(0xFF4A90E2)
                            else -> Color(0xFF53E16F)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = Color(0xFF0C0E12),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, colorMap.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "TRIAGE CONFIRMED SEVERITY:",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        triageResult?.severity ?: "UNKNOWN",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = colorMap
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    triageResult?.summary ?: "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "🚨 CRITICAL SURVIVAL ACTION REQUIRED:",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFB874)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    triageResult?.actionRequired ?: "",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    lineHeight = 13.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        triageResult?.let { viewModel.submitTriageAsLiveAlert(it) }
                                        onClose()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5545)),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("SUBMIT TRIAGE TO COMMAND CENTER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
