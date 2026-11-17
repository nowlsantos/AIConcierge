package com.nowlsantos.aiconcierge.ui.component.widget

import android.app.Activity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nowlsantos.aiconcierge.ui.theme.DarkPen
import com.nowlsantos.aiconcierge.ui.theme.GoldPen

@Composable
fun ShowExitDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.onPrimary,
        textContentColor = MaterialTheme.colorScheme.onBackground,
        title = {
            Text(
                text = "Exit AiConcierge?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        },
        text = {
            Text(
                text = "Are you sure you want to close the app?",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    (context as? Activity)?.finishAndRemoveTask()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPen,
                    contentColor = DarkPen
                )
            ) {
                Text("Exit")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPen,
                    contentColor = DarkPen
                )
            ) {
                Text("Cancel")
            }
        }
    )
}
