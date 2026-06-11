package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppEntry()
            }
        }
    }
}

@Composable
fun MainAppEntry() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }

    // Setup permission launchers
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Run checking
        val needsRequest = permissionsToRequest.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsRequest) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            hasLocationPermission = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            CyberpunkLoginScreen(
                onLoginCompleted = { role, user ->
                    Toast.makeText(context, "Protocol Authorized: $user", Toast.LENGTH_SHORT).show()
                    if (role == "guest") {
                        navController.navigate("guest_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("staff_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("guest_dashboard") {
            GuestDashboardScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("guest_dashboard") { inclusive = true }
                    }
                }
            )
        }

        composable("staff_dashboard") {
            StaffDashboardScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("staff_dashboard") { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun CyberpunkLoginScreen(
    onLoginCompleted: (role: String, user: String) -> Unit
) {
    val context = LocalContext.current
    var isGuestSelected by remember { mutableStateOf(true) }
    var usernameField by remember { mutableStateOf("") }
    var passwordField by remember { mutableStateOf("") }
    var isErrorVisible by remember { mutableStateOf(false) }

    val blackThemeBackground = Color(0xFF111318)
    val containerDeepDark = Color(0xFF0C0E12)
    val highAlertCoral = Color(0xFFFF5545)
    val safeNeonGreen = Color(0xFF53E16F)
    val mutedBorderHex = Color(0xFF333539)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(blackThemeBackground)
            .padding(24.dp)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Aesthetic backgrounds
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(300.dp)
                .clip(RoundedCornerShape(150.dp))
                .background(highAlertCoral.copy(alpha = 0.04f))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(highAlertCoral.copy(alpha = 0.15f))
                        .border(1.dp, highAlertCoral.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Code Forge",
                        tint = highAlertCoral,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "CODE FORGE",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "TAJ MAHAL HOTEL SAFETY PROTOCOL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selector Tabs (Guest vs Staff)
            Surface(
                color = containerDeepDark,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, mutedBorderHex),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    Button(
                        onClick = { isGuestSelected = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGuestSelected) highAlertCoral else Color.Transparent
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            "GUEST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGuestSelected) Color.White else Color.White.copy(alpha = 0.4f)
                        )
                    }

                    Button(
                        onClick = { isGuestSelected = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isGuestSelected) highAlertCoral else Color.Transparent
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            "STAFF COMMAND",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isGuestSelected) Color.White else Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Info box based on chosen tab
            Surface(
                color = if (isGuestSelected) safeNeonGreen.copy(alpha = 0.05f) else highAlertCoral.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, if (isGuestSelected) safeNeonGreen.copy(alpha = 0.2f) else highAlertCoral.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isGuestSelected) {
                        "PUBLIC SECURITY ACCESS: Enter any local room code or ID (e.g. ROOM-412) below with security authorization password 'guest123'."
                    } else {
                        "RESTRICTED COMMAND CENTER ACCESS: Authenticate badge with your system authority password 'staff2024'."
                    },
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.62f),
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 13.sp
                )
            }

            // Inputs
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(
                        "ROOM & BADGE ID",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = usernameField,
                        onValueChange = { usernameField = it },
                        placeholder = {
                            Text(
                                if (isGuestSelected) "e.g. ROOM-412" else "e.g. BADGE-01",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.3f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = highAlertCoral,
                            unfocusedBorderColor = mutedBorderHex,
                            focusedContainerColor = containerDeepDark,
                            unfocusedContainerColor = containerDeepDark
                        ),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                }

                Column {
                    Text(
                        "PASSWORD PROTOCOL",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = passwordField,
                        onValueChange = { passwordField = it },
                        placeholder = {
                            Text(
                                "Enter pass code",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.3f)
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = highAlertCoral,
                            unfocusedBorderColor = mutedBorderHex,
                            focusedContainerColor = containerDeepDark,
                            unfocusedContainerColor = containerDeepDark
                        ),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                }
            }

            if (isErrorVisible) {
                Text(
                    "❌ ERROR: Authorization rejected. Invalid keys.",
                    color = highAlertCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // LOGIN SUBMIT BUTTON
            Button(
                onClick = {
                    val actualUser = usernameField.trim().ifEmpty {
                        if (isGuestSelected) "ROOM-CORRIDOR" else "BADGE-DEFAULT"
                    }
                    val cleanPass = passwordField.trim()

                    if (isGuestSelected) {
                        if (cleanPass == "guest123") {
                            isErrorVisible = false
                            onLoginCompleted("guest", actualUser)
                        } else {
                            isErrorVisible = true
                        }
                    } else {
                        if (cleanPass == "staff2024") {
                            isErrorVisible = false
                            onLoginCompleted("staff", actualUser)
                        } else {
                            isErrorVisible = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = highAlertCoral),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    "INITIALIZE SYSTEMS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
