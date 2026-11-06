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
import com.hackathon.ansimmessagepractice.guardian.DemoPairing
import com.hackathon.ansimmessagepractice.guardian.GuardianCall
import com.hackathon.ansimmessagepractice.guardian.rememberGuardianCallAction
import com.hackathon.ansimmessagepractice.detection.AlertRecord
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimPrimaryButton
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimTopBar
import com.hackathon.ansimmessagepractice.ui.onboarding.LevelBadge
import java.util.Calendar
import com.hackathon.ansimmessagepractice.ui.theme.*

/**
 * record == null: 홈의 시연 예시(위험/주의). record != null: 이 기기에 저장된 실제 경고 기록
 * (원문·발신 번호는 저장하지 않으므로 표시하지 않습니다).
 */
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
    val guardianLabel = GuardianCall.savedName(context).ifBlank {
        DemoPairing.GUARDIAN_NAME.replace(" (", "(")
    }
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        AnsimTopBar("알림 상세", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp)) {
            LevelBadge(if (danger) "위험" else "주의")
            Spacer(Modifier.height(10.dp))
            Text(type, style = ansimStyle(22, 30, FontWeight.Bold), color = AnsimInk)
            Spacer(Modifier.height(22.dp))
            HorizontalDivider(color = AnsimInk, thickness = 2.dp)
            DetailRow("일시", record?.let { formatDateTime(it.at) } ?: "2026. 11. 2. 18:14")
            if (record == null) {
                DetailRow("발신", sender)
                DetailRow("보호자", if (danger) "$guardianLabel 님에게 전송 완료" else "보내지 않음 (주의 단계)",
                    valueColor = if (danger) AnsimProtected else AnsimMuted, bold = danger)
                Spacer(Modifier.height(20.dp))
                Column(Modifier.fillMaxWidth().background(AnsimCream, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 13.dp)) {
                    Text("받은 문자", style = ansimStyle(15, 22, FontWeight.SemiBold), color = AnsimSubtle)
                    Spacer(Modifier.height(5.dp))
                    Text(sample, style = AnsimType.BodyLong, color = AnsimInk)
                }
                if (DemoPairing.SHOW_DEMO_NOTICES) Text("이 화면은 시연 데이터입니다.",
                    Modifier.padding(top = 12.dp), style = AnsimType.Body, color = AnsimSubtle)
            } else {
                DetailRow("출처", if (record.source == "kakao") "카카오톡" else "문자")
                DetailRow("보호자", "전송 상태 확인되지 않음", valueColor = AnsimMuted)
                Spacer(Modifier.height(20.dp))
                Text("개인정보 보호를 위해 이 기기의 알림 목록에는 문자 원문과 발신 번호를 저장하지 않았어요.",
                    style = AnsimType.BodyLong, color = AnsimMuted)
            }
        }
        Box(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            AnsimPrimaryButton(if (hasCall) "보호자에게 전화" else "전화번호 직접 입력",
                { if (hasCall) callGuardian() else onManualNumber() }, icon = AnsimIcons.Phone)
        }
    }
}

private fun formatDateTime(epochMillis: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = epochMillis }
    return "${c.get(Calendar.YEAR)}. ${c.get(Calendar.MONTH) + 1}. ${c.get(Calendar.DAY_OF_MONTH)}. " +
            "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
}

@Composable
private fun DetailRow(key: String, value: String, valueColor: Color = AnsimInk, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(key, Modifier.width(97.dp), style = AnsimType.Row, color = AnsimMuted)
        Text(value, Modifier.weight(1f), style = ansimStyle(17, 24, if (bold) FontWeight.Bold else FontWeight.Normal),
            color = valueColor)
    }
    HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
}
