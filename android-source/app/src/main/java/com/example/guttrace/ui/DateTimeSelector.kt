package com.example.guttrace.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
fun DateTimeSelector(
    selectedDateTime: LocalDateTime,
    onDateTimeSelected: (LocalDateTime) -> Unit
) {
    val context = LocalContext.current

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year: Int, month: Int, day: Int ->
            val updatedDate = selectedDateTime.withYear(year).withMonth(month + 1).withDayOfMonth(day)
            
            // Create time picker here to capture the updated date
            val timePickerDialog = TimePickerDialog(
                context,
                { _, hour: Int, minute: Int ->
                    onDateTimeSelected(updatedDate.withHour(hour).withMinute(minute))
                },
                selectedDateTime.hour,
                selectedDateTime.minute,
                true
            )
            timePickerDialog.show()
        },
        selectedDateTime.year,
        selectedDateTime.monthValue - 1,
        selectedDateTime.dayOfMonth
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("Horário do Evento:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { datePickerDialog.show() }) {
            val isNow = selectedDateTime.toLocalDate() == LocalDateTime.now().toLocalDate() &&
                    java.time.Duration.between(selectedDateTime, LocalDateTime.now()).toMinutes() < 2
            val label = if (isNow) "Agora" else selectedDateTime.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
            Text(label, color = Color(0xFF7C83FD))
        }
    }
}
