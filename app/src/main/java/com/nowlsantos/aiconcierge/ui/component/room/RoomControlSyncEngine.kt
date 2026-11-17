package com.nowlsantos.aiconcierge.ui.component.room

/**
 * ROOM CONTROL SYNC ENGINE
 * Parses incoming roomStates key-value pairs (Climate, Lighting, Fan, Curtains, Presets, Master/Night switches)
 * and updates state maps for target room zones (BEDROOM, LIVING ROOM, SITTING ROOM).
 */
object RoomControlSyncEngine {

    fun syncRoomStates(
        roomStates: Map<String, String>,
        zones: List<String>,
        activeZone: String,
        thermostatMap: Map<String, ThermostatState>,
        masterLightMap: Map<String, MasterLightState>,
        lightSliderMap: Map<String, LightSliderState>,
        fanMap: Map<String, RotaryFanState>,
        curtainMap: Map<String, CurtainSliderState>,
        selectedMoodMap: Map<String, String>,
        onStateUpdated: (
            newActiveZone: String,
            newThermostatMap: Map<String, ThermostatState>,
            newMasterLightMap: Map<String, MasterLightState>,
            newLightSliderMap: Map<String, LightSliderState>,
            newFanMap: Map<String, RotaryFanState>,
            newCurtainMap: Map<String, CurtainSliderState>,
            newSelectedMoodMap: Map<String, String>
        ) -> Unit
    ) {
        if (roomStates.isEmpty()) return

        var updatedActiveZone = activeZone

        // 1. Find the MOST RECENT room zone targeted by AI (checking explicit ROOM CONTROLS - navigation keys first)
        val explicitZoneKey = roomStates.keys.reversed().firstNotNullOfOrNull { key ->
            if (key.uppercase().startsWith("ROOM CONTROLS-")) {
                val parts = key.split("-")
                if (parts.size >= 2) {
                    val z = parts[1].trim().uppercase()
                    if (z in zones) z else null
                } else null
            } else null
        }

        val latestTargetZone = explicitZoneKey ?: roomStates.keys.reversed().firstNotNullOfOrNull { key ->
            val parts = key.split("-")
            if (parts.size >= 2) {
                val zone = parts[1].trim().uppercase()
                if (zone in zones) zone else null
            } else null
        }

        if (latestTargetZone != null && latestTargetZone != activeZone) {
            updatedActiveZone = latestTargetZone
        }

        var tMap = thermostatMap
        var mLMap = masterLightMap
        var lSMap = lightSliderMap
        var fMap = fanMap
        var cMap = curtainMap
        var sMMap = selectedMoodMap

        // Partition entries: Process Preset Scenes FIRST, explicit device controls SECOND
        val presetEntries = roomStates.filter { (k, _) ->
            val upper = k.uppercase()
            upper.contains("MOOD") || upper.contains("PRESET") || upper.contains("SCENE")
        }
        val deviceEntries = roomStates.filter { (k, _) ->
            val upper = k.uppercase()
            !upper.contains("MOOD") && !upper.contains("PRESET") && !upper.contains("SCENE")
        }

        val orderedEntries = presetEntries.entries + deviceEntries.entries

        // 2. Sync component values
        orderedEntries.forEach { (key, value) ->
            val parts = key.split("-")
            if (parts.size >= 2) {
                val category = parts[0].trim().uppercase()
                val targetZone = parts[1].trim().uppercase()
                val targetZones = if (targetZone == "ALL") zones else listOf(targetZone)
                val fullKeyUpper = key.uppercase()
                val cleanValue = value.substringBefore("-").trim()
                val valLower = cleanValue.lowercase()

                targetZones.forEach { z ->
                    when {
                        category.contains("CLIMATE") || category.contains("TEMP") || category.contains("THERMOSTAT") -> {
                            val tempNumber = Regex("\\d+").find(cleanValue)?.value?.toIntOrNull()
                            val current = tMap[z] ?: ThermostatState()
                            val newTempC = when {
                                tempNumber != null -> {
                                    if (cleanValue.contains("°F") || cleanValue.contains("F")) {
                                        ((tempNumber - 32) * 5 / 9)
                                    } else {
                                        tempNumber
                                    }
                                }

                                valLower.contains("warm") || valLower.contains("cool") || valLower.contains("lower") || valLower.contains("down") -> {
                                    current.targetTempC - 2
                                }

                                valLower.contains("raise") || valLower.contains("up") || valLower.contains("increase") -> {
                                    current.targetTempC + 2
                                }

                                else -> {
                                    current.targetTempC
                                }
                            }.coerceIn(16, 30)

                            tMap = tMap + (z to current.copy(targetTempC = newTempC))
                        }

                        category.contains("LIGHT") || category.contains("DIMMER") || category.contains("ILLUMINATION") || category.contains("CONTROL") || fullKeyUpper.contains("MASTER") || fullKeyUpper.contains("NIGHT") -> {
                            val isNight = fullKeyUpper.contains("NIGHT") || valLower.contains("night")
                            val isMasterExplicit = fullKeyUpper.contains("MASTER") || valLower.contains("master")

                            if (isNight) {
                                val isOff = valLower.contains("off") || valLower.contains("disable") || valLower.contains("deactivate")
                                val isOn = !isOff && (valLower.contains("on") || valLower.contains("enable") || valLower.contains("activate"))
                                val currentMaster = mLMap[z] ?: MasterLightState()
                                mLMap = mLMap + (z to currentMaster.copy(nightLightOn = isOn))
                            } else if (isMasterExplicit) {
                                val isOff = valLower.contains("off") || valLower.contains("disable") || valLower.contains("deactivate")
                                val isOn = !isOff && (valLower.contains("on") || valLower.contains("enable") || valLower.contains("activate"))
                                val currentMaster = mLMap[z] ?: MasterLightState()
                                mLMap = mLMap + (z to currentMaster.copy(masterLightOn = isOn))
                            } else {
                                val current = lSMap[z] ?: LightSliderState()
                                val numericLevel = Regex("\\d+").find(value)?.value?.toIntOrNull()?.coerceIn(0, 5)

                                val newLevel = when {
                                    numericLevel != null -> numericLevel
                                    valLower.contains("off") || valLower.contains("disable") || valLower.contains("deactivate") -> 0
                                    valLower.contains("dim") || valLower.contains("lower") || valLower.contains("down") || valLower.contains("soft") || valLower.contains("less") -> (current.lightLevel - 2).coerceAtLeast(1)
                                    valLower.contains("illuminate") || valLower.contains("bright") || valLower.contains("increase") || valLower.contains("up") || valLower.contains("more") || valLower.contains("higher") -> (current.lightLevel + 2).coerceAtMost(5)
                                    valLower.contains("on") -> 4
                                    else -> (current.lightLevel - 1).coerceAtLeast(1)
                                }

                                lSMap = lSMap + (z to current.copy(lightLevel = newLevel))

                                if (valLower.contains("off") || valLower.contains("disable") || newLevel == 0) {
                                    val currentMaster = mLMap[z] ?: MasterLightState()
                                    mLMap = mLMap + (z to currentMaster.copy(masterLightOn = false))
                                } else if (valLower.contains("on") || newLevel > 0) {
                                    val currentMaster = mLMap[z] ?: MasterLightState()
                                    mLMap = mLMap + (z to currentMaster.copy(masterLightOn = true))
                                }
                            }
                        }

                        category.contains("FAN") || category.contains("VENTILATION") || category.contains("BLOWER") -> {
                            val current = fMap[z] ?: RotaryFanState()
                            val speedIndex = when {
                                valLower.contains("off") -> 0
                                valLower.contains("low") || valLower.contains("1") -> 1
                                valLower.contains("mid") || valLower.contains("medium") || valLower.contains("2") -> 2
                                valLower.contains("high") || valLower.contains("3") -> 3
                                valLower.contains("max") || valLower.contains("4") -> 4
                                valLower.contains("on") -> 1
                                else -> Regex("\\d+").find(value)?.value?.toIntOrNull()?.coerceIn(0, 4) ?: current.fanSpeedIndex
                            }
                            fMap = fMap + (z to current.copy(fanSpeedIndex = speedIndex))
                        }

                        category.contains("CURTAIN") || category.contains("DRAPE") || category.contains("BLIND") -> {
                            val current = cMap[z] ?: CurtainSliderState()
                            val numbers = Regex("\\d+").findAll(value).map { it.value.toFloat() / 100f }.toList()
                            val newRange = if (numbers.size >= 2) {
                                numbers[0].coerceIn(0f, 1f)..numbers[1].coerceIn(0f, 1f)
                            } else {
                                val isClosed = valLower.contains("close") || valLower.contains("off")
                                val isOpen = valLower.contains("open") || valLower.contains("on")
                                if (isClosed) 0f..0.52f else if (isOpen) 0.47f..1f else current.curtainRange
                            }
                            cMap = cMap + (z to current.copy(curtainRange = newRange))
                        }

                        category.contains("MOOD") || category.contains("PRESET") || category.contains("SCENE") -> {
                            val moodName = value.trim()
                            sMMap = sMMap + (z to moodName)
                            when {
                                moodName.contains("Relax", ignoreCase = true) -> {
                                    lSMap = lSMap + (z to LightSliderState(lightLevel = 2))
                                    cMap = cMap + (z to CurtainSliderState(curtainRange = 0f..0.52f))
                                    fMap = fMap + (z to RotaryFanState(fanSpeedIndex = 1))
                                    tMap = tMap + (z to ThermostatState(targetTempC = 21))
                                    mLMap = mLMap + (z to MasterLightState(masterLightOn = true, nightLightOn = false))
                                }
                                moodName.contains("Cinema", ignoreCase = true) -> {
                                    lSMap = lSMap + (z to LightSliderState(lightLevel = 1))
                                    cMap = cMap + (z to CurtainSliderState(curtainRange = 0f..0.52f))
                                    fMap = fMap + (z to RotaryFanState(fanSpeedIndex = 2))
                                    tMap = tMap + (z to ThermostatState(targetTempC = 20))
                                    mLMap = mLMap + (z to MasterLightState(masterLightOn = true, nightLightOn = false))
                                }
                                moodName.contains("Daylight", ignoreCase = true) -> {
                                    lSMap = lSMap + (z to LightSliderState(lightLevel = 5))
                                    cMap = cMap + (z to CurtainSliderState(curtainRange = 0.47f..1f))
                                    fMap = fMap + (z to RotaryFanState(fanSpeedIndex = 1))
                                    tMap = tMap + (z to ThermostatState(targetTempC = 20))
                                    mLMap = mLMap + (z to MasterLightState(masterLightOn = true, nightLightOn = false))
                                }
                                moodName.contains("Lounge", ignoreCase = true) -> {
                                    lSMap = lSMap + (z to LightSliderState(lightLevel = 3))
                                    cMap = cMap + (z to CurtainSliderState(curtainRange = 0.47f..1f))
                                    fMap = fMap + (z to RotaryFanState(fanSpeedIndex = 2))
                                    tMap = tMap + (z to ThermostatState(targetTempC = 22))
                                    mLMap = mLMap + (z to MasterLightState(masterLightOn = true, nightLightOn = false))
                                }
                            }
                        }
                    }
                }
            }
        }

        onStateUpdated(
            updatedActiveZone,
            tMap,
            mLMap,
            lSMap,
            fMap,
            cMap,
            sMMap
        )
    }
}
