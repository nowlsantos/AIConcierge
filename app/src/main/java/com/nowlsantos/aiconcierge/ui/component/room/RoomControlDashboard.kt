package com.nowlsantos.aiconcierge.ui.component.room

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.ui.component.widget.debouncedClick
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGreyCard
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.Logger
import com.nowlsantos.common.DocumentNode
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RoomControlDashboardView(
    node: DocumentNode,
    roomStates: Map<String, String> = emptyMap(),
    onRoomControlChange: (category: String, target: String, value: String) -> Unit = { _, _, _ -> },
    onActiveZoneChange: (String) -> Unit = {}
) {
    Logger.d("RoomControlDashboardView rendering node '${node.name}'", "ROOM_DASHBOARD")

    val zones = listOf("BEDROOM", "LIVING ROOM", "SITTING ROOM")

    // Determine active zone from roomStates if targeted by AI navigation, defaulting to BEDROOM
    val initialZone = remember(roomStates) {
        roomStates.keys.reversed().firstNotNullOfOrNull { key ->
            val parts = key.split("-")
            if (parts.size >= 2) {
                val targetZone = parts[1].trim().uppercase()
                if (targetZone in zones) targetZone else null
            } else null
        } ?: "BEDROOM"
    }
    var activeZone by remember(initialZone) { mutableStateOf(initialZone) }

    // Persistent component state maps remembered across tab selection switches!
    var masterLightMap by remember {
        mutableStateOf(
            mapOf(
                "BEDROOM" to MasterLightState(masterLightOn = false, nightLightOn = false),
                "LIVING ROOM" to MasterLightState(masterLightOn = false, nightLightOn = false),
                "SITTING ROOM" to MasterLightState(masterLightOn = false, nightLightOn = false)
            )
        )
    }

    var thermostatMap by remember {
        mutableStateOf(
            mapOf(
                "BEDROOM" to ThermostatState(targetTempC = 20, isCelsius = true),
                "LIVING ROOM" to ThermostatState(targetTempC = 20, isCelsius = true),
                "SITTING ROOM" to ThermostatState(targetTempC = 20, isCelsius = true)
            )
        )
    }

    var lightSliderMap by remember {
        mutableStateOf(
            mapOf(
                "BEDROOM" to LightSliderState(lightLevel = 0),
                "LIVING ROOM" to LightSliderState(lightLevel = 0),
                "SITTING ROOM" to LightSliderState(lightLevel = 0)
            )
        )
    }

    var fanMap by remember {
        mutableStateOf(
            mapOf(
                "BEDROOM" to RotaryFanState(fanSpeedIndex = 0),
                "LIVING ROOM" to RotaryFanState(fanSpeedIndex = 0),
                "SITTING ROOM" to RotaryFanState(fanSpeedIndex = 0)
            )
        )
    }

    var curtainMap by remember {
        mutableStateOf(
            mapOf(
                "BEDROOM" to CurtainSliderState(curtainRange = 0f..1f),
                "LIVING ROOM" to CurtainSliderState(curtainRange = 0f..1f),
                "SITTING ROOM" to CurtainSliderState(curtainRange = 0f..1f)
            )
        )
    }

    var selectedMoodMap by remember {
        mutableStateOf(
            mapOf(
                "BEDROOM" to "",
                "LIVING ROOM" to "",
                "SITTING ROOM" to ""
            )
        )
    }

    // AI Voice / Hardware Event Feedback Pulse State
    var aiPulseCategory by remember { mutableStateOf<String?>(null) }

    // Sync roomStates from Gemini AI tool calls to local component maps via Sync Engine
    LaunchedEffect(roomStates) {
        if (roomStates.isNotEmpty()) {
            val lastKey = roomStates.keys.lastOrNull()?.uppercase() ?: ""
            aiPulseCategory = lastKey
        }

        RoomControlSyncEngine.syncRoomStates(
            roomStates = roomStates,
            zones = zones,
            activeZone = activeZone,
            thermostatMap = thermostatMap,
            masterLightMap = masterLightMap,
            lightSliderMap = lightSliderMap,
            fanMap = fanMap,
            curtainMap = curtainMap,
            selectedMoodMap = selectedMoodMap,
            onStateUpdated = { newActiveZone, newTMap, newMLMap, newLSMap, newFMap, newCMap, newSMMap ->
                activeZone = newActiveZone
                onActiveZoneChange(newActiveZone)
                thermostatMap = newTMap
                masterLightMap = newMLMap
                lightSliderMap = newLSMap
                fanMap = newFMap
                curtainMap = newCMap
                selectedMoodMap = newSMMap
            }
        )

        if (roomStates.isNotEmpty()) {
            delay(2000L.milliseconds)
            aiPulseCategory = null
        }
    }

    LaunchedEffect(activeZone) {
        onActiveZoneChange(activeZone)
    }

    val currentMasterLight = masterLightMap[activeZone] ?: MasterLightState()
    val currentThermostat = thermostatMap[activeZone] ?: ThermostatState()
    val currentLightSlider = lightSliderMap[activeZone] ?: LightSliderState()
    val currentFan = fanMap[activeZone] ?: RotaryFanState()
    val currentCurtain = curtainMap[activeZone] ?: CurtainSliderState()

    // Haptic touch feedback provider for hardware controls
    val haptic = LocalHapticFeedback.current

    // AI Feedback Pulse Detectors
    val col1Pulse = aiPulseCategory?.contains("CLIMATE") == true ||
                    aiPulseCategory?.contains("TEMP") == true ||
                    aiPulseCategory?.contains("THERMOSTAT") == true ||
                    aiPulseCategory?.contains("LIGHT") == true ||
                    aiPulseCategory?.contains("DIMMER") == true

    val col2Pulse = aiPulseCategory?.contains("MASTER") == true ||
                    aiPulseCategory?.contains("NIGHT") == true ||
                    aiPulseCategory?.contains("MOOD") == true ||
                    aiPulseCategory?.contains("PRESET") == true

    val col3Pulse = aiPulseCategory?.contains("FAN") == true ||
                    aiPulseCategory?.contains("VENTILATION") == true ||
                    aiPulseCategory?.contains("CURTAIN") == true ||
                    aiPulseCategory?.contains("DRAPE") == true ||
                    aiPulseCategory?.contains("BLIND") == true

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // HEADER TITLE
            Text(
                text = "ROOM CONTROLS",
                style = sfTypography.titleLarge,
                color = WhitePen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // ZONE TABS (BEDROOM | LIVING ROOM | SITTING ROOM) WITH 400MS ADVERSARIAL TOUCH LOCKOUT
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                zones.forEach { zone ->
                    val isSelected = activeZone.equals(zone, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) WhitePen.copy(alpha = 0.1f) else Color.Transparent,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) PeninsulaGold else WhitePen.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .height(42.dp)
                            .debouncedClick(debounceTimeMs = 400L) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                activeZone = zone
                                onRoomControlChange("Room Control Tab", zone, "SHOW")
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = zone,
                                style = sfTypography.titleSmall,
                                color = if (isSelected) PeninsulaGold else WhitePen.copy(alpha = 0.6f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 3-COLUMN CONTROL PANEL
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(1f)
                    .padding(start = 50.dp, top = 0.dp, end = 50.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // COLUMN 1: CLIMATE THERMOSTAT DIAL & LIGHTING DIMMER
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PeninsulaGreyCard.copy(alpha = 0.6f)),
                    border = BorderStroke(
                        width = if (col1Pulse) 2.dp else 1.dp,
                        color = if (col1Pulse) PeninsulaGold else WhitePen.copy(alpha = 0.10f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        ClimateThermostat(
                            state = currentThermostat,
                            onTempChange = { newTempC ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val activeThermostat = thermostatMap[activeZone] ?: ThermostatState()
                                val updatedState = activeThermostat.copy(targetTempC = newTempC)
                                thermostatMap = thermostatMap + (activeZone to updatedState)
                                val displayStr = if (updatedState.isCelsius) "$newTempC°C" else "${updatedState.targetTempF}°F"
                                onRoomControlChange("Climate", activeZone, displayStr)
                            },
                            onUnitToggle = { newIsCelsius ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val activeThermostat = thermostatMap[activeZone] ?: ThermostatState()
                                thermostatMap = thermostatMap + (activeZone to activeThermostat.copy(isCelsius = newIsCelsius))
                            }
                        )

                        RoomLightSlider(
                            state = currentLightSlider,
                            activeZone = activeZone,
                            onLightLevelChange = { newLevel ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lightSliderMap = lightSliderMap + (activeZone to currentLightSlider.copy(lightLevel = newLevel))
                                onRoomControlChange("Lighting", activeZone, "Level $newLevel")
                            }
                        )
                    }
                }

                // COLUMN 2: MASTER QUICK SWITCHES & DETAILED PRESET SCENES
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PeninsulaGreyCard.copy(alpha = 0.6f)),
                    border = BorderStroke(
                        width = if (col2Pulse) 2.dp else 1.dp,
                        color = if (col2Pulse) PeninsulaGold else WhitePen.copy(alpha = 0.10f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Row: Master & Night Light Switches
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LightSwitch(
                                title = if (currentMasterLight.masterLightOn) "Master Light ON" else "Master Light OFF",
                                isOn = currentMasterLight.masterLightOn,
                                onToggle = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val newMasterState = !currentMasterLight.masterLightOn
                                    masterLightMap = masterLightMap + (activeZone to currentMasterLight.copy(masterLightOn = newMasterState))
                                    onRoomControlChange("Master Light", activeZone, if (newMasterState) "ON" else "OFF")
                                },
                                buttonSize = 60.dp,
                                iconSize = 30.dp
                            )

                            LightSwitch(
                                title = if (currentMasterLight.nightLightOn) "Night Light ON" else "Night Light OFF",
                                isOn = currentMasterLight.nightLightOn,
                                onToggle = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val newNightState = !currentMasterLight.nightLightOn
                                    masterLightMap = masterLightMap + (activeZone to currentMasterLight.copy(nightLightOn = newNightState))
                                    onRoomControlChange("Night Light", activeZone, if (newNightState) "ON" else "OFF")
                                },
                                buttonSize = 60.dp,
                                iconSize = 30.dp
                            )
                        }

                        HorizontalDivider(
                            color = WhitePen.copy(alpha = 0.2f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Bottom Section: Detailed Preset Scenes
                        ZoneMoodPresets(
                            selectedMood = selectedMoodMap[activeZone] ?: "",
                            onMoodSelect = { moodName ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedMoodMap = selectedMoodMap + (activeZone to moodName)
                                when (moodName) {
                                    "Relax" -> {
                                        lightSliderMap = lightSliderMap + (activeZone to LightSliderState(lightLevel = 2))
                                        curtainMap = curtainMap + (activeZone to CurtainSliderState(curtainRange = 0f..0.52f))
                                        fanMap = fanMap + (activeZone to RotaryFanState(fanSpeedIndex = 1))
                                        thermostatMap = thermostatMap + (activeZone to ThermostatState(targetTempC = 21))
                                        masterLightMap = masterLightMap + (activeZone to MasterLightState(masterLightOn = true, nightLightOn = false))
                                    }
                                    "Cinema" -> {
                                        lightSliderMap = lightSliderMap + (activeZone to LightSliderState(lightLevel = 1))
                                        curtainMap = curtainMap + (activeZone to CurtainSliderState(curtainRange = 0f..0.52f))
                                        fanMap = fanMap + (activeZone to RotaryFanState(fanSpeedIndex = 2))
                                        thermostatMap = thermostatMap + (activeZone to ThermostatState(targetTempC = 20))
                                        masterLightMap = masterLightMap + (activeZone to MasterLightState(masterLightOn = true, nightLightOn = false))
                                    }
                                    "Daylight" -> {
                                        lightSliderMap = lightSliderMap + (activeZone to LightSliderState(lightLevel = 5))
                                        curtainMap = curtainMap + (activeZone to CurtainSliderState(curtainRange = 0.47f..1f))
                                        fanMap = fanMap + (activeZone to RotaryFanState(fanSpeedIndex = 1))
                                        thermostatMap = thermostatMap + (activeZone to ThermostatState(targetTempC = 20))
                                        masterLightMap = masterLightMap + (activeZone to MasterLightState(masterLightOn = true, nightLightOn = false))
                                    }
                                    "Lounge" -> {
                                        lightSliderMap = lightSliderMap + (activeZone to LightSliderState(lightLevel = 3))
                                        curtainMap = curtainMap + (activeZone to CurtainSliderState(curtainRange = 0.47f..1f))
                                        fanMap = fanMap + (activeZone to RotaryFanState(fanSpeedIndex = 2))
                                        thermostatMap = thermostatMap + (activeZone to ThermostatState(targetTempC = 22))
                                        masterLightMap = masterLightMap + (activeZone to MasterLightState(masterLightOn = true, nightLightOn = false))
                                    }
                                }
                                onRoomControlChange("Preset Mood", activeZone, moodName)
                            }
                        )
                    }
                }

                // COLUMN 3: ROTARY FAN KNOB & CURTAIN SLIDER
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PeninsulaGreyCard.copy(alpha = 0.6f)),
                    border = BorderStroke(
                        width = if (col3Pulse) 2.dp else 1.dp,
                        color = if (col3Pulse) PeninsulaGold else WhitePen.copy(alpha = 0.10f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RotaryFan(
                            state = currentFan,
                            onFanSpeedChange = { newIndex ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                fanMap = fanMap + (activeZone to currentFan.copy(fanSpeedIndex = newIndex))
                                val levels = listOf("OFF", "LOW", "MID", "HIGH", "MAX")
                                onRoomControlChange("Fan", activeZone, levels[newIndex])
                            }
                        )

                        CurtainSlider(
                            state = currentCurtain,
                            activeZone = activeZone,
                            onCurtainRangeChange = { newRange ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                curtainMap = curtainMap + (activeZone to currentCurtain.copy(curtainRange = newRange))
                                val leftPct = (newRange.start * 100).toInt()
                                val rightPct = (newRange.endInclusive * 100).toInt()
                                onRoomControlChange("Curtains", activeZone, "Left $leftPct% - Right $rightPct%")
                            }
                        )
                    }
                }
            }
        }
    }
}


