package com.nowlsantos.aiconcierge.ui.component.widget

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * ADVANCED DEBOUNCED CLICK MODIFIER (AGENTS.MD Compliant)
 * Extension modifier that suppresses rapid double-tapping, adversarial mashing,
 * and accidental multi-clicks while fully retaining standard clickable capabilities
 * (ripples, accessibility semantics, interaction source, indication, role).
 */
@Composable
fun Modifier.debouncedClick(
    debounceTimeMs: Long = 100L,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onClick: () -> Unit
): Modifier {
    var lastClickTimestamp by remember { mutableLongStateOf(0L) }
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    return this.clickable(
        interactionSource = resolvedInteractionSource,
        indication = indication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTimestamp >= debounceTimeMs) {
            lastClickTimestamp = currentTime
            onClick()
        }
    }
}
