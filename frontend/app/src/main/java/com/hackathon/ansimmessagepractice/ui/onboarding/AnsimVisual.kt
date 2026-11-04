package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.theme.*

/** 공통 버튼/선택 행 — 중복 디자인을 방지합니다. */
@Composable
internal fun AnsimPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(7.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AnsimGreen, contentColor = Color.White)
    ) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}

@Composable
internal fun AnsimChoiceRow(text: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(9.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = if (selected) Color(0xFFF5F8FD) else Color.White),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) AnsimGreen else Color(0xFFDFE2E8))
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 53.dp).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), color = AnsimInk, fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
            RadioButton(selected = selected, onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = AnsimGreen), modifier = Modifier.size(34.dp))
        }
    }
}
