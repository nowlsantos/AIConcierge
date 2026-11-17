package com.nowlsantos.aiconcierge.presentation

/**
 * PRODUCTION METADATA CONSTANTS & DEFAULTS FOR ARIS-GEMINI
 */
internal enum class ActionStatus(val value: String) {
    PENDING("PENDING"),
    CONFIRMED("CONFIRMED"),
    SCHEDULED("SCHEDULED"),
    RESERVED("RESERVED"),
    SPA_RESERVED("SPA_RESERVED"),
    PRIORITY_DISPATCH("PRIORITY_DISPATCH"),
    FAILED("FAILED")
}

internal enum class SystemStateKey(val key: String) {
    SUBTITLE("subtitle"),
    STATUS("status"),
    SCENE("scene"),
    DND("dnd")
}

internal enum class DevicePowerState(val value: String) {
    ON("ON"),
    OFF("OFF"),
    SHOW("SHOW")
}

internal enum class SectionNodes(val value: String) {
    ROOM_CONTROLS("Room Controls"),
    MEDIA("Media"),
    IN_ROOM_DINING("In-Room Dining Ordering"),
    SPA_AND_WELLNESS("Spa and Wellness"),
    SPA_TREATMENTS("Menu"),
    PEN_ACADEMY("Pen Academy"),
    TRANSPORTATION("Transportation"),
    RESTAURANTS("Restaurants and Bars")
}

internal enum class Commands(val value: String) {
    SELECT("Select"),
    PLAY("Play"),
    FILTER("Filter")
}

internal enum class ActionDefaultText(val value: String) {
    SHOE_SHINE("Shoe Shine"),
    MAKEUP_ROOM("Makeup Room"),
    VALET_BOX_PICKUP("Valet Box Pickup"),
    ORDER_SUMMARY("Order Summary")
}

internal object ActionDefaults {
    val SLEEP_KEYWORDS = listOf("night", "sleep", "standby", "bed", "hold", "shutdown", "quit", "pause", "stop")
}

/**
 * PURE FACTUAL TOOL FEEDBACK FOR ARIS-GEMINI
 * Concise, factual status confirmations to prevent model repetition and latency.
 */
internal object ArisFeedback {
    fun navSuccess(name: String, content: String, prevName: String? = null) =
        "Status: SUCCESS. Displayed '$name'."
    fun alreadyOnPage(name: String) =
        "Status: ALREADY_ACTIVE. Currently viewing '$name'."
    fun navNotFound(target: String) =
        "Status: NOT_FOUND. Could not locate '$target'."
    fun selectionSuccess(group: String, option: String) =
        "Status: SUCCESS. Selected '$option' for '$group'."
    fun roomUpdate(cat: String, target: String, value: String) =
        "Status: SUCCESS. Adjusted $cat in $target to $value."
    fun mediaPlaying(value: String, category: String) =
        "Status: SUCCESS. Displayed $value on $category."
    fun orderSubmitted() =
        "Status: SUCCESS. Order summary ready."
    fun serviceInfo(topic: String, content: String) =
        "Status: SUCCESS. Displaying $topic."
    fun returningTo(name: String) =
        "Status: SUCCESS. Returned to $name."
    fun returningHome() =
        "Status: SUCCESS. Returned to Home screen."
    fun alreadyHome() =
        "Status: ALREADY_HOME."
    fun housekeepingDispatched(items: String) =
        "Status: SUCCESS. Housekeeping request for $items dispatched."
    fun dndUpdated(enabled: Boolean, untilTime: String) =
        "Status: SUCCESS. Do Not Disturb set to ${if (enabled) "ON" else "OFF"}."
    fun valetScheduled(type: String) =
        "Status: SUCCESS. Valet pickup for $type scheduled."
    fun experienceBooked(exp: String, party: Int, date: String) =
        "Status: SUCCESS. Booked $exp for $party guests on $date."
    fun spaBooked(treatment: String, party: Int, dateTime: String) =
        "Status: SUCCESS. Booked $treatment at $dateTime."
    fun transportBooked(dest: String, time: String, vehicle: String) =
        "Status: SUCCESS. Booked $vehicle to $dest at $time."
    fun tableReserved(rest: String, party: Int, time: String) =
        "Status: SUCCESS. Reserved table at $rest."
    fun sceneActivated(scene: String) =
        "Status: SUCCESS. Activated $scene scene."
    fun penChatSent(recipient: String) =
        "Status: SUCCESS. Message sent to $recipient."
    fun orderConfirmed() =
        "Status: SUCCESS. Order confirmed."
    fun weatherReport(weather: String) =
        "Status: SUCCESS. London weather: $weather."
}

internal enum class MediaCategory(
    val nodeName: String,
    val sectionTitle: String,
    val filterCategories: List<String>
) {
    TV(
        nodeName = "TV",
        sectionTitle = "TV CHANNELS",
        filterCategories = listOf("All", "Local", "News", "Entertainment", "Family", "Sports")
    ),
    RADIO(
        nodeName = "Radio",
        sectionTitle = "RADIO STATIONS",
        filterCategories = listOf("All", "Top 40", "Pop", "News", "Dance & Electronic", "Relaxing")
    ),
    MOVIES(
        nodeName = "Movies",
        sectionTitle = "MOVIES & CINEMA",
        filterCategories = listOf("All", "Action", "Comedy", "Drama", "Family", "Sci-Fi")
    );

    companion object {
        fun from(name: String): MediaCategory {
            return entries.find { it.nodeName.equals(name.trim(), ignoreCase = true) } ?: TV
        }
    }
}
