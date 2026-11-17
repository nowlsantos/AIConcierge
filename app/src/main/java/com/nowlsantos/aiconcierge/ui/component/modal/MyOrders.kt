package com.nowlsantos.aiconcierge.ui.component.modal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.presentation.OrderState
import com.nowlsantos.aiconcierge.presentation.PlacedOrder
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.RedPen
import com.nowlsantos.aiconcierge.ui.theme.WhitePen
import com.nowlsantos.aiconcierge.ui.theme.sfTypography

@Composable
fun MyOrdersModal(
    pastOrders: List<PlacedOrder>,
    onCancelOrder: (String) -> Unit
) {
    val scrollState = androidx.compose.runtime.key(pastOrders.size) { rememberScrollState() }
    androidx.compose.runtime.LaunchedEffect(pastOrders.size) {
        scrollState.scrollTo(0)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "My Orders",
            style = sfTypography.headlineMedium,
            color = PeninsulaGold,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (pastOrders.isNotEmpty()) {
            pastOrders.forEach { order ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WhitePen.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, PeninsulaGold.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = order.orderName,
                                style = sfTypography.titleMedium,
                                color = PeninsulaGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "€${"%.2f".format(order.total)}",
                                style = sfTypography.titleMedium,
                                color = PeninsulaGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Status: ${order.statusText}",
                            style = sfTypography.bodyMedium,
                            color = WhitePen.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        order.items.forEach { (itemName, itemPrice) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• $itemName",
                                    modifier = Modifier.width(500.dp),
                                    style = sfTypography.titleSmall,
                                    color = WhitePen.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = if (itemPrice > 0) "€${"%.2f".format(itemPrice)}" else "INCLUDED",
                                    style = sfTypography.bodyMedium,
                                    color = WhitePen.copy(alpha = 0.6f)
                                )
                            }
                        }

                        if (order.state == OrderState.CONFIRMED) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onCancelOrder(order.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RedPen.copy(alpha = 0.8f))
                            ) {
                                Text(
                                    text = "Cancel Order",
                                    style = sfTypography.titleSmall,
                                    color = WhitePen
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = "There are no active orders at this time.",
                style = sfTypography.bodyLarge,
                color = PeninsulaGold.copy(alpha = 0.85f),
                fontSize = 18.sp
            )
        }
    }
}
