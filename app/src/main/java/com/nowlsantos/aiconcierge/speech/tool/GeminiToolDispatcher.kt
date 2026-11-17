package com.nowlsantos.aiconcierge.speech.tool

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * GEMINI TOOL DISPATCHER
 * Encapsulates the tool callback lambdas and JSON parameter extraction logic.
 */
class GeminiToolDispatcher {
    var onSpokenTextReceived: ((String) -> Unit)? = null
    var onNavigate: (suspend (String) -> String)? = null
    var onScrollPage: (suspend (String) -> String)? = null
    var onSetTheme: ((Boolean) -> Unit)? = null
    var onRoomControl: (suspend (String, String, String) -> String)? = null
    var onMediaControl: (suspend (String, String, String) -> String)? = null
    var onGetServiceInfo: (suspend (String) -> String)? = null
    var onSelectOption: (suspend (String, String) -> String)? = null
    var onSubmitOrder: (suspend () -> String)? = null
    var onConfirmOrder: (suspend () -> String)? = null
    var onCancelOrder: (suspend (String) -> String)? = null
    var onBookSpa: (suspend (String, Int, String) -> String)? = null
    var onRequestHousekeeping: (suspend (String, Boolean) -> String)? = null
    var onSetDoNotDisturb: (suspend (Boolean, String) -> String)? = null
    var onScheduleValet: (suspend (String) -> String)? = null
    var onBookExperience: (suspend (String, Int, String) -> String)? = null
    var onRequestTransportation: (suspend (String, String, String) -> String)? = null
    var onReserveTable: (suspend (String, Int, String) -> String)? = null
    var onSetMasterScene: (suspend (String) -> String)? = null
    var onSendPenChat: (suspend (String, String) -> String)? = null
    var onGetWeather: (suspend () -> String)? = null
    var onCloseModal: (suspend () -> String)? = null
    var onStandby: (() -> Unit)? = null

    suspend fun executeFallbackTool(toolName: String, args: Map<String, JsonElement>): String {
        return when (toolName) {
            "navigate_to" -> {
                val target = extractStringArg(
                    args,
                    "target", "destination", "page", "section", "target_section",
                    "targetSection", "category", "name", "topic"
                )
                onNavigate?.invoke(target) ?: "Service unavailable"
            }
            "scroll_page" -> {
                val direction = extractStringArg(args, "direction", "dir", "side").ifBlank { "DOWN" }.uppercase()
                onScrollPage?.invoke(direction) ?: "Scroll unavailable"
            }
            "update_room_control" -> {
                val category = extractStringArg(args, "category", "type", "system", "device")
                val target = extractStringArg(args, "target", "zone", "room", "target_zone", "targetZone")
                val value = extractStringArg(args, "value", "setting", "state", "level", "action")
                onRoomControl?.invoke(category, target.ifBlank { "All" }, value) ?: "Room control unavailable"
            }
            "update_media_state" -> {
                val category = extractStringArg(args, "category", "type", "media_type", "mediaType")
                val action = extractStringArg(args, "action", "command", "state")
                val value = extractStringArg(args, "value", "channel", "title", "genre", "target")
                onMediaControl?.invoke(category, action, value) ?: "Media control unavailable"
            }
            "get_service_info" -> {
                val topic = extractStringArg(args, "topic", "service", "section", "category", "name", "target")
                onGetServiceInfo?.invoke(topic) ?: "Service info unavailable"
            }
            "select_option" -> {
                val group = extractStringArg(args, "group", "category", "section")
                val option = extractStringArg(args, "option", "item", "selection", "choice")
                onSelectOption?.invoke(group, option) ?: "Selection unavailable"
            }
            "submit_order" -> onSubmitOrder?.invoke() ?: "Order submission unavailable"
            "confirm_order" -> onConfirmOrder?.invoke() ?: "Order confirmation unavailable"
            "cancel_order" -> {
                val orderName = extractStringArg(args, "order_name", "orderName", "name", "order")
                onCancelOrder?.invoke(orderName) ?: "Order cancellation unavailable"
            }
            "request_housekeeping_service" -> {
                val items = extractStringArg(args, "items", "item", "request", "service")
                val urgent = extractStringArg(args, "urgent", "priority").toBoolean()
                onRequestHousekeeping?.invoke(items, urgent) ?: "Housekeeping unavailable"
            }
            "set_do_not_disturb" -> {
                val enabled = extractStringArg(args, "enabled", "active", "state").ifBlank { "true" }.toBoolean()
                val until = extractStringArg(args, "until_time", "untilTime", "time")
                onSetDoNotDisturb?.invoke(enabled, until) ?: "DND unavailable"
            }
            "schedule_valet_pickup" -> {
                val serviceType = extractStringArg(args, "service_type", "serviceType", "type", "service").ifBlank { "Valet" }
                onScheduleValet?.invoke(serviceType) ?: "Valet service unavailable"
            }
            "book_pen_academy_experience" -> {
                val exp = extractStringArg(args, "experience_name", "experienceName", "experience", "tour", "name")
                val party = extractStringArg(args, "party_size", "partySize", "guests", "people").toIntOrNull() ?: 1
                val date = extractStringArg(args, "date", "day", "time").ifBlank { "Today" }
                onBookExperience?.invoke(exp, party, date) ?: "Booking unavailable"
            }
            "book_spa_treatment" -> {
                val treatment = extractStringArg(args, "treatment_name", "treatmentName", "treatment", "service", "massage")
                val party = extractStringArg(args, "party_size", "partySize", "guests", "people").toIntOrNull() ?: 1
                val dateTime = extractStringArg(args, "date_time", "dateTime", "time", "date").ifBlank { "Today" }
                onBookSpa?.invoke(treatment, party, dateTime) ?: "Spa booking unavailable"
            }
            "request_transportation" -> {
                val dest = extractStringArg(args, "destination", "dest", "location", "to")
                val time = extractStringArg(args, "pickup_time", "pickupTime", "time").ifBlank { "Now" }
                val vehicle = extractStringArg(args, "vehicle_type", "vehicleType", "car", "vehicle").ifBlank { "Rolls-Royce" }
                onRequestTransportation?.invoke(dest, time, vehicle) ?: "Transportation unavailable"
            }
            "reserve_restaurant_table" -> {
                val rest = extractStringArg(args, "restaurant_name", "restaurantName", "restaurant", "venue")
                val party = extractStringArg(args, "party_size", "partySize", "guests", "people").toIntOrNull() ?: 2
                val time = extractStringArg(args, "time", "reservation_time", "reservationTime").ifBlank { "7:00 PM" }
                onReserveTable?.invoke(rest, party, time) ?: "Reservation unavailable"
            }
            "standby_system" -> {
                onStandby?.invoke()
                "Status: SUCCESS. Entering standby mode. MANDATORY: You MUST speak a warm, brief British farewell aloud right now (e.g. 'Certainly, entering standby mode. Have a wonderful evening.') before concluding."
            }
            "set_master_scene" -> {
                val scene = extractStringArg(args, "scene_name", "sceneName", "scene", "mode", "ambient").ifBlank { "Relax" }
                onSetMasterScene?.invoke(scene) ?: "Scene service unavailable"
            }
            "send_penchat_message" -> {
                val recipient = extractStringArg(args, "recipient", "to", "department").ifBlank { "Front Desk" }
                val text = extractStringArg(args, "message_text", "messageText", "text", "message", "body")
                onSendPenChat?.invoke(recipient, text) ?: "Messaging unavailable"
            }
            "get_live_weather" -> onGetWeather?.invoke() ?: "London weather: 18°C, Partly Cloudy."
            "close_modal" -> onCloseModal?.invoke() ?: "Modal closed"
            "set_theme" -> {
                val isDark = extractStringArg(args, "is_dark", "isDark", "dark").ifBlank { "true" }.toBoolean()
                onSetTheme?.invoke(isDark)
                "Theme updated"
            }
            else -> "Success"
        }
    }

    private fun extractStringArg(args: Map<String, JsonElement>, vararg keys: String): String {
        for (key in keys) {
            val element = args[key] ?: continue
            when (element) {
                is JsonPrimitive -> {
                    val content = element.content.trim()
                    if (content.isNotEmpty() && content != "null") return content
                }
                is JsonObject -> {
                    val name = element["name"]?.let { (it as? JsonPrimitive)?.content }
                        ?: element["target"]?.let { (it as? JsonPrimitive)?.content }
                        ?: element["title"]?.let { (it as? JsonPrimitive)?.content }
                    if (!name.isNullOrBlank()) return name.trim()
                }
                else -> {
                    val str = element.toString().replace("\"", "").trim()
                    if (str.isNotEmpty() && str != "null") return str
                }
            }
        }
        return ""
    }
}
