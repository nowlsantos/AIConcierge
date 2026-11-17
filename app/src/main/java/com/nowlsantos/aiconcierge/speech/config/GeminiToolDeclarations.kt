package com.nowlsantos.aiconcierge.speech.config

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class GeminiSchemaType(val wireType: String) {
    STRING("STRING"),
    BOOLEAN("BOOLEAN"),
    INTEGER("INTEGER")
}

data class GeminiParameterSchema(
    val type: GeminiSchemaType,
    val description: String,
    val required: Boolean = true
)

data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Map<String, GeminiParameterSchema> = emptyMap()
) {
    fun toJsonObject(): JsonObject = buildJsonObject {
        put("name", name)
        put("description", description)
        if (parameters.isNotEmpty()) {
            put("parameters", buildJsonObject {
                put("type", "OBJECT")
                put("properties", buildJsonObject {
                    parameters.forEach { (paramName, schema) ->
                        put(paramName, buildJsonObject {
                            put("type", schema.type.wireType)
                            put("description", schema.description)
                        })
                    }
                })
                val requiredKeys = parameters.filterValues { it.required }.keys
                if (requiredKeys.isNotEmpty()) {
                    put("required", buildJsonArray {
                        requiredKeys.forEach { add(JsonPrimitive(it)) }
                    })
                }
            })
        }
    }
}

object GeminiToolDeclarations {
    val ALL_DECLARATIONS: List<GeminiFunctionDeclaration> = listOf(
        GeminiFunctionDeclaration(
            name = "navigate_to",
            description = "MANDATORY: MUST be called EVERY time the guest mentions, asks about, wants to view, see, open, or navigate to any section, menu, category, dining option, media channel, room control, or item page on screen. NEVER call 'navigate_to' when the guest selects, orders, or chooses an individual item, wine, dish, or option on an active selection/ordering screen—use 'select_option' exclusively for item selections.",
            parameters = mapOf(
                "target" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "The exact destination section, menu, category, or item page name to show on screen from the hotel directory (e.g., Spa and Wellness, Housekeeping, Laundry, Dry Cleaning, Pressing, Shoe Shining, Pen Academy, About Peninsula Academy, Guest Programmes, Our City, Our Favourite Spots, Our Team's City Secrets, The Royal Parks, Breakfast Sets, All-Day Dining, In-Room Dining, Media, TV, Radio, Movies, Room Controls, Shops at the Pen, Shopping, Languages, My Orders, Messages, Penchat)."
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "scroll_page",
            description = "Scrolls the current active list, grid, or detail page vertically UP or DOWN to reveal more items or content when available.",
            parameters = mapOf(
                "direction" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Scroll direction: 'DOWN' (scroll down / see more items), 'UP' (scroll up), 'TOP' (scroll to top), 'BOTTOM' (scroll to bottom)"
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "update_room_control",
            description = "Control room systems (Lighting, Master Light, Night Light, Climate, Curtains, Fans, Preset Mood / Scenes e.g. Relax, Cinema, Daylight, Lounge). Specify target room zone e.g. BEDROOM, LIVING ROOM, SITTING ROOM, or ALL.",
            parameters = mapOf(
                "category" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Lighting, Master Light, Night Light, Climate, Curtains, Fans, Preset Mood, Scene"
                ),
                "target" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Target room zone: BEDROOM, LIVING ROOM, SITTING ROOM, or ALL",
                    required = false
                ),
                "value" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Setting: ON, OFF, Level 1 to 5, lower, dim, illuminate, 21°C, OPEN, CLOSED, LOW, MID, HIGH, MAX, Relax, Cinema, Daylight, Lounge"
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "update_media_state",
            description = "Control media playback or filter grid by genre chip e.g. News, Sports, Local, Entertainment, Family, Action, Comedy, Pop, Sci-Fi.",
            parameters = mapOf(
                "category" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "TV, Radio, Movies"
                ),
                "action" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Play, Stop, Pause, Filter"
                ),
                "value" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Channel or Title name (e.g. CNN, Sky Sports) OR Genre Filter name (e.g. Sports, News, Local, Action)",
                    required = false
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "get_service_info",
            description = "Retrieve factual hotel information, history, services, or policies (e.g. check-in times, Wi-Fi info) that do not require navigating to a dedicated screen.",
            parameters = mapOf(
                "topic" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Topic or question about hotel services"
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "select_option",
            description = "MANDATORY: MUST be called whenever the guest selects, chooses, or orders any specific item, wine, dish, beverage, or option on a menu selection or ordering screen (e.g., 'I'll have the 2015 Cristal', 'Give me congee'). NEVER call 'submit_order' automatically right after 'select_option'.",
            parameters = mapOf(
                "group" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Category or group name",
                    required = false
                ),
                "option" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Specific item to select"
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "submit_order",
            description = "Submit the current selections and proceed to the order summary screen. MANDATORY MANDATE: NEVER call 'submit_order' automatically right after selecting an item. You MUST ONLY call 'submit_order' when the guest speaks an explicit completion phrase ('That will be all', 'That's it', 'Show me my order summary', 'Complete order', 'Checkout', 'No that is all', 'Finished')."
        ),
        GeminiFunctionDeclaration(
            name = "confirm_order",
            description = "Confirm and dispatch the reviewed order summary to the kitchen"
        ),
        GeminiFunctionDeclaration(
            name = "cancel_order",
            description = "Cancel an active or recently placed order",
            parameters = mapOf(
                "order_name" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Optional order name to cancel",
                    required = false
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "request_housekeeping_service",
            description = "ONLY call if the guest explicitly commands to REQUEST OR DISPATCH extra towels, pillows, water, dental kits, or cleaning. DO NOT call when the guest simply asks to VIEW, SEE, OR OPEN HouseKeeping Request, Laundry, Dry Cleaning, Pressing, or Shoe Shining on screen—call 'navigate_to' instead.",
            parameters = mapOf(
                "items" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Requested items comma separated"
                ),
                "urgent" to GeminiParameterSchema(
                    type = GeminiSchemaType.BOOLEAN,
                    description = "Whether the request is urgent",
                    required = false
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "set_do_not_disturb",
            description = "Enable or disable Do Not Disturb / Privacy Please mode",
            parameters = mapOf(
                "enabled" to GeminiParameterSchema(
                    type = GeminiSchemaType.BOOLEAN,
                    description = "True to turn on DND"
                ),
                "until_time" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Optional time until when DND stays active",
                    required = false
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "schedule_valet_pickup",
            description = "ONLY call if the guest explicitly asks to SCHEDULE OR PICK UP a valet box for shoe shine, laundry, or pressing. DO NOT call when the guest asks to VIEW, SEE, OR SHOW Laundry, Dry Cleaning, Pressing, or Shoe Shining on screen—call 'navigate_to' instead.",
            parameters = mapOf(
                "service_type" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Shoe shine, Laundry, or Pressing"
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "book_pen_academy_experience",
            description = "Book an exclusive Pen Academy tour or experience",
            parameters = mapOf(
                "experience_name" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Name of the Pen Academy experience"
                ),
                "party_size" to GeminiParameterSchema(
                    type = GeminiSchemaType.INTEGER,
                    description = "Number of guests",
                    required = false
                ),
                "date" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Requested date",
                    required = false
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "book_spa_treatment",
            description = "Book a spa treatment, massage, facial, or salon service with our partners like Errol Douglas",
            parameters = mapOf(
                "treatment_name" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Name of the massage, facial, or salon treatment"
                ),
                "party_size" to GeminiParameterSchema(
                    type = GeminiSchemaType.INTEGER,
                    description = "Number of guests",
                    required = false
                ),
                "date_time" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Requested date and time",
                    required = false
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "request_transportation",
            description = "Book Rolls-Royce or luxury fleet transportation",
            parameters = mapOf(
                "destination" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Destination location"
                ),
                "pickup_time" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Requested pickup time",
                    required = false
                ),
                "vehicle_type" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Vehicle type, e.g. Rolls-Royce",
                    required = false
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "reserve_restaurant_table",
            description = "Reserve a table at hotel restaurants like Brooklands or Canton Blue",
            parameters = mapOf(
                "restaurant_name" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Restaurant name"
                ),
                "party_size" to GeminiParameterSchema(
                    type = GeminiSchemaType.INTEGER,
                    description = "Number of guests",
                    required = false
                ),
                "time" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Requested reservation time",
                    required = false
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "set_master_scene",
            description = "Activate ambient master scenes like Relax, Cinema, Daylight, Lounge, Good Night, or Good Morning. Target room zone defaults to active room.",
            parameters = mapOf(
                "scene_name" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Relax, Cinema, Daylight, Lounge, Good Night, Good Morning, Work"
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "standby_system",
            description = "Puts Aris and the concierge voice session into standby, sleep, pause, or rest mode. Call this immediately when the guest says or asks for 'Standby', 'Stand by', 'Sleep', 'Go to sleep', 'Good night', 'Goodbye', 'Pause', 'Rest', 'Dismiss', or 'That will be all'."
        ),
        GeminiFunctionDeclaration(
            name = "send_penchat_message",
            description = "Send a message to Front Desk or Concierge via PenChat",
            parameters = mapOf(
                "recipient" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Recipient e.g. Front Desk",
                    required = false
                ),
                "message_text" to GeminiParameterSchema(
                    type = GeminiSchemaType.STRING,
                    description = "Message body"
                )
            )
        ),
        GeminiFunctionDeclaration(
            name = "get_live_weather",
            description = "Get current local weather conditions for London"
        ),
        GeminiFunctionDeclaration(
            name = "close_modal",
            description = "Close or dismiss any active top bar modal dialog, overlay, or popup currently open on screen (e.g. Messages, Penchat, Languages, My Orders)"
        ),
        GeminiFunctionDeclaration(
            name = "set_theme",
            description = "Switch between Dark Mode and Light Mode theme on the guest interface",
            parameters = mapOf(
                "is_dark" to GeminiParameterSchema(
                    type = GeminiSchemaType.BOOLEAN,
                    description = "True for dark mode, false for light mode"
                )
            )
        )
    )

    fun toToolsJsonArray(declarations: List<GeminiFunctionDeclaration> = ALL_DECLARATIONS): JsonArray =
        buildJsonArray {
            add(buildJsonObject {
                put("functionDeclarations", buildJsonArray {
                    declarations.forEach { add(it.toJsonObject()) }
                })
            })
        }
}
