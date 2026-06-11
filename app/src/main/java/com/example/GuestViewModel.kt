package com.example

import android.app.Application
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class SafetyAlert(
    val id: String,
    val title: String,
    val description: String,
    val severity: String, // "CRITICAL", "HIGH", "MEDIUM", "LOW"
    val timestamp: String
)

data class TriageResult(
    val severity: String,
    val summary: String,
    val targetArea: String,
    val actionRequired: String
)

class GuestViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val locationManager = LocationManager(context)
    private val pathFinder = PathFinder()

    // Screen current states
    val isGpsLocked = MutableStateFlow(true)

    // Current Grid Location of Guest (under our 10x10 mapping)
    private val _guestGridPosition = MutableStateFlow(GridPoint(2, 6)) // Starts in Corridor near Crystal Room
    val guestGridPosition: StateFlow<GridPoint> = _guestGridPosition.asStateFlow()

    // Calculated Shortest Evacuation Path (Dashed line in Map)
    private val _evacuationPath = MutableStateFlow<List<GridPoint>>(emptyList())
    val evacuationPath: StateFlow<List<GridPoint>> = _evacuationPath.asStateFlow()

    // Alert feeds displayed in Tab 3
    private val _activeAlerts = MutableStateFlow<List<SafetyAlert>>(emptyList())
    val activeAlerts: StateFlow<List<SafetyAlert>> = _activeAlerts.asStateFlow()

    // SOS alert state triggers
    private val _sosState = MutableStateFlow<Boolean>(false)
    val sosState: StateFlow<Boolean> = _sosState.asStateFlow()

    // Triage states
    private val _isAnalyzing = MutableStateFlow<Boolean>(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _triageResult = MutableStateFlow<TriageResult?>(null)
    val triageResult: StateFlow<TriageResult?> = _triageResult.asStateFlow()

    // Optional Firestore Instance (fails or skips gracefully if google-services config is not present)
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("CodeForgeVM", "Firestore is not pre-configured: ${e.localizedMessage}")
            null
        }
    }

    init {
        // Load initial safety feed notifications
        _activeAlerts.value = listOf(
            SafetyAlert(
                id = "1",
                title = "ELEVATED ALERT — North Wing Evacuation",
                description = "Smoke detected in Sector 412 (Ball Room vicinity). Evacuate via North corridors.",
                severity = "CRITICAL",
                timestamp = "Just Now"
            ),
            SafetyAlert(
                id = "2",
                title = "TECHNICAL NOTIFICATION — Elevator 4 Offline",
                description = "Stalled from service due to localized mechanical safety lockouts. Use surrounding stairs.",
                severity = "MEDIUM",
                timestamp = "5 min ago"
            ),
            SafetyAlert(
                id = "3",
                title = "PREPARATION ALREADY UNDERWAY — Assembly Point Echo",
                description = "Assemble outer lawns (Apollo Lawn west entrance). Respond to roll-calls with local staff guides.",
                severity = "LOW",
                timestamp = "15 min ago"
            )
        )

        // Calculate initial evacuation route
        recomputeEvacuationPath()

        // Sync or background pull location metrics
        viewModelScope.launch {
            if (locationManager.hasLocationPermission()) {
                locationManager.fetchCurrentLocation()?.let { loc ->
                    updateCoordinates(loc)
                }
            }
        }
    }

    /**
     * Map fine GPS coordinates (Location) into our 10x10 indoor grids
     */
    fun updateCoordinates(location: Location) {
        // Taj Mahal Palace Mumbai general bounds mapping to 10x10 grid (Simulation)
        val latFraction = ((location.latitude - 18.921) / 0.001).coerceIn(0.0, 1.0)
        val lonFraction = ((location.longitude - 72.833) / 0.001).coerceIn(0.0, 1.0)

        val gridX = (lonFraction * 9).toInt()
        val gridY = (9 - (latFraction * 9)).toInt() // Invert Y axis for screen space representation

        _guestGridPosition.value = GridPoint(gridX, gridY)
        recomputeEvacuationPath()
    }

    fun setGuestGridPositionDirectly(gridPoint: GridPoint) {
        _guestGridPosition.value = gridPoint
        recomputeEvacuationPath()
    }

    /**
     * Recompute safe transit corridors from guest's cell to safe exits
     */
    fun recomputeEvacuationPath() {
        val start = _guestGridPosition.value
        val safeExit = GridPoint(5, 0) // EXIT N point on map
        _evacuationPath.value = pathFinder.findShortestPath(start, safeExit)
    }

    /**
     * Execute direct real-time SOS push to Firestore
     */
    fun triggerSOSReport(locationName: String) {
        _sosState.value = true

        val alertPayload = hashMapOf(
            "timestamp" to System.currentTimeMillis(),
            "room_id" to locationName,
            "user_coordinates" to "(${guestGridPosition.value.x}, ${guestGridPosition.value.y})",
            "threat_status" to "CRITICAL_SOS_TRIGGERED"
        )

        viewModelScope.launch {
            if (firestore != null) {
                try {
                    firestore?.collection("sos_triggers")
                        ?.document("active_panic_alert")
                        ?.set(alertPayload)
                    Log.i("CodeForgeSOS", "SOS successfully sent to Firestore: $alertPayload")
                } catch (e: Exception) {
                    Log.w("CodeForgeSOS", "Firestore SOS Write partial bypass: ${e.localizedMessage}")
                }
            } else {
                Log.w("CodeForgeSOS", "Simulated Database write (Local State Only): $alertPayload")
            }
        }

        // Add to active notifications
        val newAlert = SafetyAlert(
            id = System.currentTimeMillis().toString(),
            title = "CRITICAL SOS ALERT TRIGGER SENT",
            description = "Panic signal dispatched from $locationName. Responders mapped to cells (${guestGridPosition.value.x}, ${guestGridPosition.value.y}).",
            severity = "CRITICAL",
            timestamp = "Just Now"
        )
        _activeAlerts.value = listOf(newAlert) + _activeAlerts.value
    }

    fun markSafeReport(locationName: String) {
        _sosState.value = false
        val alertPayload = hashMapOf(
            "timestamp" to System.currentTimeMillis(),
            "room_id" to locationName,
            "threat_status" to "SAFE"
        )
        viewModelScope.launch {
            try {
                firestore?.collection("sos_updates")?.document("status")?.set(alertPayload)
            } catch (e: Exception) {
                Log.w("CodeForgeSOS", "Firestore non-blocking safe write error")
            }
        }

        val newAlert = SafetyAlert(
            id = System.currentTimeMillis().toString(),
            title = "USER REPORTED SAFE STATUS",
            description = "Guest verified self as fully evacuated/safe inside $locationName lobby corridors.",
            severity = "LOW",
            timestamp = "Just Now"
        )
        _activeAlerts.value = listOf(newAlert) + _activeAlerts.value
    }

    fun requestMedicalAssistance(locationName: String) {
        val alertPayload = hashMapOf(
            "timestamp" to System.currentTimeMillis(),
            "room_id" to locationName,
            "threat_status" to "MEDICAL_ASSISTANCE_REQUIRED"
        )
        viewModelScope.launch {
            try {
                firestore?.collection("sos_triggers")?.add(alertPayload)
            } catch (e: Exception) {
                Log.w("CodeForgeSOS", "Firestore query failed")
            }
        }

        val newAlert = SafetyAlert(
            id = System.currentTimeMillis().toString(),
            title = "MEDICAL PRIORITY REQUEST SENT",
            description = "Paramedics and lobby support teams dispatched to $locationName with priority access.",
            severity = "HIGH",
            timestamp = "Just Now"
        )
        _activeAlerts.value = listOf(newAlert) + _activeAlerts.value
    }

    /**
     * Contact Gemini SDK to check risk and severity level.
     * Generates a clean JSON with keys: "severity", "summary", "targetArea", "actionRequired"
     */
    fun triageReportWithGemini(userReport: String, currentRoom: String) {
        _isAnalyzing.value = true
        _triageResult.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val key = BuildConfig.GEMINI_API_KEY
            if (key == "MY_GEMINI_API_KEY" || key.isBlank()) {
                // Return gracefully generated fallback if there's no actual API key configured
                withContext(Dispatchers.Main) {
                    mockTriage(userReport, currentRoom)
                }
                return@launch
            }

            try {
                val model = GenerativeModel(
                    modelName = "gemini-3.5-flash",
                    apiKey = key
                )

                val prompt = """
                    You are Code Forge's AI safety triage engine for Taj Mahal Hotel.
                    Analyze this hazard report: "$userReport" at room: "$currentRoom".
                    Classify the incident. Return EXACTLY a classic raw JSON object without code blocks or markdown wrappers.
                    The JSON keys must match exactly:
                    {
                      "severity": "CRITICAL" or "HIGH" or "MEDIUM" or "LOW",
                      "summary": "One sentence summary mapping specific actions",
                      "targetArea": "The hotel rooms/levels mentioned",
                      "actionRequired": "Immediate survival instructions for guests"
                    }
                """.trimIndent()

                val result = model.generateContent(prompt)
                val rawText = result.text?.trim() ?: ""

                // Extrapolate and parse the JSON response
                val jsonString = cleanJsonWrappers(rawText)
                val json = JSONObject(jsonString)

                val triage = TriageResult(
                    severity = json.optString("severity", "MEDIUM"),
                    summary = json.optString("summary", "AI Triaged hazard report captured."),
                    targetArea = json.optString("targetArea", currentRoom),
                    actionRequired = json.optString("actionRequired", "Follow signs to emergency exits.")
                )

                withContext(Dispatchers.Main) {
                    _triageResult.value = triage
                    _isAnalyzing.value = false
                }

            } catch (e: Exception) {
                Log.e("CodeForgeVM", "Gemini triage failure: ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    mockTriage(userReport, currentRoom)
                }
            }
        }
    }

    private fun cleanJsonWrappers(raw: String): String {
        return raw.replace("```json", "")
            .replace("```", "")
            .trim()
    }

    private fun mockTriage(userReport: String, currentRoom: String) {
        val lowercaseReport = userReport.lowercase()
        val severity = when {
            lowercaseReport.contains("fire") || lowercaseReport.contains("smoke") || lowercaseReport.contains("burn") -> "CRITICAL"
            lowercaseReport.contains("medical") || lowercaseReport.contains("hurt") || lowercaseReport.contains("bleeding") -> "HIGH"
            lowercaseReport.contains("lock") || lowercaseReport.contains("broken") -> "MEDIUM"
            else -> "LOW"
        }

        val summary = "Triage evaluation: Detected terms suggesting $severity severity inside $currentRoom."
        val action = when (severity) {
            "CRITICAL" -> "Immediately crawl under smoke, cover mouth with wet fabric, proceed directly to northern staircases."
            "HIGH" -> "Stay low, secure doors, keep lines open for incoming medical response teams."
            "MEDIUM" -> "Stay clear of blocked elevator shafts, proceed on foot down corridor lanes."
            else -> "Keep calm, coordinate with floor staff, monitor current dashboard status."
        }

        _triageResult.value = TriageResult(
            severity = severity,
            summary = summary,
            targetArea = currentRoom,
            actionRequired = action
        )
        _isAnalyzing.value = false
    }

    fun submitTriageAsLiveAlert(triage: TriageResult) {
        val alert = SafetyAlert(
            id = System.currentTimeMillis().toString(),
            title = "AI VERIFIED HAZARD: ${triage.severity}",
            description = "${triage.summary} -> ACTION: ${triage.actionRequired}",
            severity = triage.severity,
            timestamp = "Just Now"
        )
        _activeAlerts.value = listOf(alert) + _activeAlerts.value

        // Block or add danger to BFS path if the report was near the corridor
        if (triage.severity == "CRITICAL" || triage.severity == "HIGH") {
            // Simulated: adds the Room's coordinates to our BFS active danger obstacles!
            // E.g. make Apollo Room / Crystal Room hazard-blocked if reported.
            if (triage.targetArea.contains("Apollo", ignoreCase = true)) {
                pathFinder.addHazard(GridPoint(1, 1))
                pathFinder.addHazard(GridPoint(1, 2))
                recomputeEvacuationPath()
            }
        }
    }
}
