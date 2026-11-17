package com.nowlsantos.aiconcierge.ui.component.modal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nowlsantos.aiconcierge.presentation.PlacedOrder
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography

enum class TopBarDialogType {
    NONE,
    LANGUAGE,
    MY_ORDERS,
    MESSAGES
}

/**
 * TOP BAR MODAL DIALOG
 * Custom dark in-window modal overlay for Language selector, My Orders, and Penchat / Messages.
 * Eliminates OS DialogWindow inset recalculations so background layout remains 100% stationary.
 */
@Composable
fun TopBarModalDialog(
    dialogType: TopBarDialogType,
    selections: Map<String, String> = emptyMap(),
    pastOrders: List<PlacedOrder> = emptyList(),
    onLanguageSelected: (String) -> Unit = {},
    onCancelOrder: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val activeLanguage = selections["Language"] ?: "English (Default)"

    AnimatedVisibility(
        visible = dialogType != TopBarDialogType.NONE,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .width(700.dp)
                    .heightIn(min = 480.dp, max = 680.dp)
                    .clickable(enabled = false) {}, // Consume clicks on card so backdrop click dismisses
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF161719),
                border = BorderStroke(1.dp, WhitePen.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    when (dialogType) {
                        TopBarDialogType.LANGUAGE -> LanguagesModal(
                            activeLanguage = activeLanguage,
                            onLanguageSelected = onLanguageSelected,
                            onDismiss = onDismiss
                        )
                        TopBarDialogType.MESSAGES -> MessagesModal()
                        TopBarDialogType.MY_ORDERS -> MyOrdersModal(
                            pastOrders = pastOrders,
                            onCancelOrder = onCancelOrder
                        )
                        else -> {}
                    }

                    // Close Pill Button
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, WhitePen.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .clickable { onDismiss() }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Close",
                                style = sfTypography.titleLarge,
                                color = WhitePen
                            )
                        }
                    }
                }
            }
        }
    }
}
