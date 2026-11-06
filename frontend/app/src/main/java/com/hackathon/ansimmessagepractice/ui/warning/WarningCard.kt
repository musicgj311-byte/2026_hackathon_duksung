package com.hackathon.ansimmessagepractice.ui.warning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WarningCard(
    riskLevel: String,
    onClose: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val danger = riskLevel == "danger"
    val accent = if (danger) Color(0xFFC32335) else Color(0xFFAA781D)
    val title = if (danger) "위험 · 사기 의심 문자" else "주의 · 사기 의심 문자"
    val body = if (danger) "방금 받은 문자의 링크를 누르거나 송금하지 마세요." else "사기일 수 있어요. 링크를 누르지 마세요."
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp, color = Color.White) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Surface(color = accent) {
                Text("⚠  $title", Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text(body, Modifier.padding(horizontal = 17.dp, vertical = 16.dp),
                color = Color(0xFF1B1D23), fontWeight = FontWeight.Bold,
                fontSize = 18.sp, lineHeight = 25.sp)
            Row(Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onDetails, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                    Text("자세히 보기")
                }
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(7.dp)) { Text("닫기") }
            }
        }
    }
}
