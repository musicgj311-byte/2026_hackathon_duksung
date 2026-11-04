package com.hackathon.ansimmessagepractice.guardian

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimPrimaryButton
import com.hackathon.ansimmessagepractice.ui.theme.*
import java.text.DateFormat
import java.util.Date

/** 홈의 '관리' 화면. 로컬 번호 삭제를 실제 서버 연결 해제로 오인하지 않도록 구분. */
@Composable
fun GuardianManageScreen(onBack: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE) }
    var name by remember { mutableStateOf(GuardianCall.savedName(context)) }
    var phone by remember { mutableStateOf(GuardianCall.savedNumber(context)) }
    var relation by remember { mutableStateOf(prefs.getString("guardian_relationship", "").orEmpty()) }
    var savedAt by remember { mutableLongStateOf(prefs.getLong("guardian_contact_saved_at", 0L)) }
    var confirming by remember { mutableStateOf(false) }
    val hasPhone = GuardianCall.validNumber(phone)

    if (confirming) AlertDialog(onDismissRequest = { confirming = false },
        title = { Text("가족 연락처를 삭제할까요?") },
        text = { Text("이 휴대폰에 저장된 전화번호만 지워져요. 서버의 보호자 연결까지 해제되지는 않으며, 이후 경고 화면에서 바로 전화할 수 없어요.") },
        confirmButton = { TextButton(onClick = {
            prefs.edit().remove("guardian_name").remove("guardian_phone")
                .remove("guardian_relationship").remove("guardian_contact_saved_at").apply()
            name = ""; phone = ""; relation = ""; savedAt = 0L; confirming = false
        }) { Text("삭제", color = AnsimDanger) } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text("취소") } })

    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = 14.dp)) {
        TextButton(onClick = onBack) { Text("‹  보호자 관리", fontSize = 18.sp, color = AnsimInk,
            fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(22.dp))
        if (hasPhone) {
            HorizontalDivider(color = AnsimInk)
            ContactInfo("이름", name.ifBlank { "가족" } + if (relation.isNotBlank()) " ($relation)" else "")
            ContactInfo("전화번호", phone)
            ContactInfo("등록일", if (savedAt > 0) DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(savedAt)) else "이전 등록")
            Spacer(Modifier.height(20.dp))
            Text("전화할 가족 연락처가 기기에 저장되어 있어요.", color = AnsimProtected, fontSize = 13.sp)
        } else {
            Text("등록된 보호자 연락처가 없어요", fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold, color = AnsimInk)
            Spacer(Modifier.height(8.dp))
            Text("등록하면 위험 경고 화면에서 안전하게 가족에게 전화할 수 있어요.",
                color = AnsimMuted, fontSize = 14.sp, lineHeight = 21.sp)
        }
        Spacer(Modifier.height(17.dp))
        Surface(color = AnsimMint, shape = RoundedCornerShape(9.dp)) {
            Text("보호자 자동 알림 연결은 별도의 서버 페어링이 필요해요. 이 화면은 기기에 저장한 연락처를 관리해요.",
                Modifier.fillMaxWidth().padding(13.dp), color = AnsimGreenDark, fontSize = 12.sp, lineHeight = 19.sp)
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth().height(51.dp),
            shape = RoundedCornerShape(8.dp)) {
            Text(if (hasPhone) "보호자 변경" else "보호자 등록", color = AnsimInk, fontWeight = FontWeight.Bold)
        }
        if (hasPhone) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { confirming = true }, modifier = Modifier.fillMaxWidth().height(51.dp),
                shape = RoundedCornerShape(8.dp)) { Text("연락처 삭제", color = AnsimDanger) }
        }
        Spacer(Modifier.weight(1f))
        AnsimPrimaryButton("홈으로", onBack)
    }
}

@Composable
private fun ContactInfo(label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 57.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.width(95.dp), color = AnsimMuted, fontSize = 14.sp)
        Text(value, Modifier.weight(1f), color = AnsimInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = Color(0xFFEDF0F2))
}
