package com.hackathon.ansimmessagepractice.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.guardian.GuardianCall
import com.hackathon.ansimmessagepractice.guardian.rememberGuardianCallAction
import com.hackathon.ansimmessagepractice.detection.AlertRecord
import java.text.DateFormat
import java.util.Date
import com.hackathon.ansimmessagepractice.ui.theme.*

/** 위험 상세의 레이아웃. 이 버전의 값은 실제 기록이 아닌 Mock 데이터. */
@Composable
fun AlertDetailScreen(level: String, onBack: () -> Unit, onManualNumber: () -> Unit,
                      record: AlertRecord? = null) {
    val context = LocalContext.current
    val callGuardian = rememberGuardianCallAction()
    val danger = level == "danger"
    val type = record?.title ?: if (danger) "가족을 사칭해 송금을 요구한 문자" else "택배 사칭이 의심되는 문자"
    val sender = if (danger) "010-****-2741 (저장 안 됨)" else "1588-****"
    val sample = if (danger)
        "엄마 나 폰 액정 깨져서 임시폰으로 연락해. 50만원만 이 계좌로 보내줄 수 있어?"
    else "고객님 택배가 미수령 상태입니다. 재배송 신청 http://post-redeliver.example/k2"
    val hasCall = GuardianCall.validNumber(GuardianCall.savedNumber(context))
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = 14.dp)) {
        TextButton(onClick = onBack) { Text("‹   알림 상세", color = AnsimInk, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(18.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(if (danger) "⚠ 위험" else "⚠ 주의", color = if (danger) AnsimDanger else AnsimCaution,
                fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(type, color = AnsimInk, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold,
                lineHeight = 31.sp)
            HorizontalDivider(color = AnsimInk)
            DetailRow("일시", record?.let { DateFormat.getDateTimeInstance().format(Date(it.at)) } ?: "시연 예시")
            HorizontalDivider(color = Color(0xFFEEF0F2))
            DetailRow("출처", record?.let { if (it.source == "kakao") "카카오톡" else "문자" } ?: "시연")
            if (record == null) DetailRow("발신", sender)
            HorizontalDivider(color = Color(0xFFEEF0F2))
            DetailRow("보호자", "전송 상태 확인되지 않음")
            Spacer(Modifier.height(6.dp))
            if (record == null) {
                Surface(color = Color(0xFFF2F3F6), shape = RoundedCornerShape(7.dp)) {
                    Column(Modifier.fillMaxWidth().padding(15.dp)) {
                        Text("받은 문자 · 시연 예시", color = AnsimMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(sample, color = AnsimInk, fontSize = 15.sp, lineHeight = 23.sp)
                    }
                }
                Text("이 화면은 시연 데이터입니다.", color = AnsimMuted, fontSize = 12.sp)
            } else {
                Text("개인정보 보호를 위해 이 기기의 알림 목록에는 문자 원문과 발신 번호를 저장하지 않았어요.",
                    color = AnsimMuted, fontSize = 13.sp, lineHeight = 20.sp)
            }
            Text("보호자 전송 여부는 백엔드에서 확인해야 해요.", color = AnsimMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { if (hasCall) callGuardian() else onManualNumber() },
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(7.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AnsimGreen)) {
            Text(if (hasCall) "☎  보호자에게 전화" else "☎  전화번호 직접 입력", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DetailRow(key: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 26.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(key, Modifier.width(105.dp), color = AnsimMuted, fontSize = 14.sp)
        Text(value, Modifier.weight(1f), color = AnsimInk, fontSize = 14.sp, lineHeight = 20.sp)
    }
}