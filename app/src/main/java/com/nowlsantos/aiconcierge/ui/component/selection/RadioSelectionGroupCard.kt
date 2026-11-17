package com.nowlsantos.aiconcierge.ui.component.selection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.R
import com.nowlsantos.aiconcierge.ui.theme.PeninsulaGold
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.common.DocumentNode

/**
 * 1. RADIO CONTROL: Single choice selection per group
 */
@Composable
fun RadioSelectionGroupCard(
    group: DocumentNode,
    selectedOption: String?,
    onOptionClick: (String) -> Unit
) {
    val isSatisfied = selectedOption != null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(
            width = if (isSatisfied) 1.dp else 2.dp,
            color = if (isSatisfied) Color.White.copy(alpha = 0.1f) else PeninsulaGold.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.name,
                    style = sfTypography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )
                if (!isSatisfied) {
                    Text(
                        text = stringResource(R.string.required_field),
                        style = sfTypography.labelSmall,
                        color = PeninsulaGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            group.children.forEach { option ->
                val isSelected = selectedOption?.trim().equals(option.name.trim(), ignoreCase = true)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            onClick = { onOptionClick(option.name) }
                        )
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .border(2.dp, if (isSelected) PeninsulaGold else Color.White.copy(alpha = 0.3f), CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(PeninsulaGold, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.name,
                            style = sfTypography.titleMedium,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (option.content.isNotEmpty()) {
                            Text(
                                text = option.content,
                                style = sfTypography.labelLarge,
                                color = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    val optionPrice = option.price
                    if (optionPrice != null && optionPrice > 0) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "+€%.2f".format(optionPrice),
                            style = sfTypography.titleMedium,
                            color = PeninsulaGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
