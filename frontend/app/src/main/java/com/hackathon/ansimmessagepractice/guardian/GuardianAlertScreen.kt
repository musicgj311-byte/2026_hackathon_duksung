package com.hackathon.ansimmessagepractice.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.data.api.GuardianNotificationRecord
import com.hackathon.ansimmessagepractice.data.api.formatGuardianEventTime
import com.hackathon.ansimmessagepractice.data.api.guardianScamTypeName
import com.hackathon.ansimmessagepractice.ui.theme.*
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimOutlineButton
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimPrimaryButton
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimShieldBadge
import androidx.compose.foundation.border

/**
 * record == null: 보호 대상자 홈에서 열어보는 시연용 미리보기.
 * record != null: 보호자 홈의 서버 알림 목록에서 선택한 실제 알림.
 * 실제 사건/이벤트 시각을 받지 못하면 반드시 '기록 없음'으로 표시합니다.
 */
@Composable
fun GuardianAlertScreen(
    onBackClick: () -> Unit,
    onManualNumberClick: () -> Unit = {},
    record: GuardianNotificationRecord? = null
) {
    val context = LocalContext.current
    val isPreview = record == null
    val demo = isPreview && DemoPairing.enabled(context)
    val name = GuardianCall.savedName(context).ifBlank { "가족" }
    // 이 기기에 등록된 번호는 보호 대상자 -> 보호자로 거는 번호입니다.
    // 보호자 모드의 실제 기록에서 보호 대상자의 번호로 오인하여 사용하면 안 됩니다.
    val canCallSavedGuardian = isPreview && GuardianCall.validNumber(GuardianCall.savedNumber(context))
    val callGuardian = rememberGuardianCallAction()
    val scamType = guardianScamTypeName(record?.scamType ?: "family_impersonation")
    val recipient = record?.protectedName?.takeIf { it.isNotBlank() }
        ?: if (demo) DemoPairing.PROTECTED_NAME else "보호 대상자"
    val isDanger = record == null || record.level.equals("danger", ignoreCase = true)
    val isCaution = record?.level.equals("caution", ignoreCase = true)
    val levelText = if (isDanger) "위험" else if (isCaution) "주의" else "확인 필요"
    val levelColor = if (isDanger) AnsimDangerText else if (isCaution) AnsimCaution else AnsimMuted
    val explanation = if (isPreview) {
        "자녀를 사칭해 송금을 요구하는 문자예요. 엄마 휴대폰에 경고가 표시되었어요."
    } else record?.explanation?.takeIf { it.isNotBlank() } ?: when (scamType) {
        "가족 사칭" -> "가족을 사칭하는 문자로 탐지됐어요. 평소에 알고 있던 번호로 직접 연락해 확인해 주세요."
        "택배 사칭" -> "택배를 사칭하는 문자로 탐지됐어요. 문자에 있는 링크를 바로 열지 마세요."
        else -> "의심스러운 문자가 탐지됐어요. 문자 속 링크나 번호 대신 평소에 알고 있던 방법으로 확인해 주세요."
    }
    var showPreviewInfo by remember { mutableStateOf(false) }
    if (showPreviewInfo) AlertDialog(
        onDismissRequest = { showPreviewInfo = false },
        title = { Text("시연 화면 안내") },
        text = { Text("이 화면은 예시예요. 실제 알림이 오면 확인된 정보만 보여드립니다.") },
        confirmButton = { TextButton(onClick = { showPreviewInfo = false }) { Text("확인") } }
    )

    androidx.activity.compose.BackHandler(onBack = onBackClick)
    Column(Modifier.fillMaxSize().background(AnsimCream).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(start = 13.dp, end = 13.dp, top = 15.dp, bottom = 16.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(start = 17.dp, end = 16.dp, top = 15.dp, bottom = 15.dp)) {
                    AnsimShieldBadge(38.dp)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text("모두안심", Modifier.weight(1f), style = AnsimType.Body, color = AnsimSubtle)
                            Text(if (isPreview) "방금" else formatGuardianEventTime(record?.sentAt),
                                style = AnsimType.Body, color = AnsimSubtle)
                        }
                        Text(if (isPreview) "엄마에게 위험 문자가 도착했어요" else "$recipient 님에게 의심 문자가 도착했어요",
                            style = ansimStyle(15, 22, FontWeight.Bold), color = AnsimInk)
                        Text(if (isPreview) "가족 사칭 문자예요. 지금 전화해서 확인해 주세요."
                        else "문자를 확인하기 전에 가족에게 먼저 전화해 주세요.",
                            style = ansimStyle(15, 22.5), color = AnsimMuted)
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
            val cardShape = RoundedCornerShape(16.dp)
            Column(Modifier.padding(horizontal = 4.dp).fillMaxWidth().background(Color.White, cardShape)
                .border(1.dp, AnsimDivider, cardShape)
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(recipient, Modifier.weight(1f), style = ansimStyle(20, 28, FontWeight.Bold), color = AnsimInk)
                    Box(Modifier.heightIn(min = 25.dp).background(if (isDanger) AnsimDangerTint else if (isCaution) AnsimCautionTint else AnsimCream,
                        RoundedCornerShape(6.dp)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                        Text("$levelText · $scamType", style = ansimStyle(15, 20, FontWeight.Bold), color = levelColor)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(explanation, style = AnsimType.BodyLong, color = AnsimMuted)
                Spacer(Modifier.height(15.dp))
                HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
                GuardianAlertRow("위험 문자 수신", if (isPreview) "18:14" else formatGuardianEventTime(record?.incidentCreatedAt))
                HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
                // warning_shown 이벤트 시각. 링크 열기·송금(link_opened, transfer_attempted)은 이번 범위에서 제외합니다.
                GuardianAlertRow("경고 화면 표시", if (isPreview) "18:14" else formatGuardianEventTime(record?.warningShownAt))
            }
            Spacer(Modifier.height(10.dp))
            Text(if (isPreview && DemoPairing.SHOW_DEMO_NOTICES)
                "개인정보 보호를 위해 문자 원문을 표시하지 않아요. 날짜와 상태는 시연 예시이며 실제 기록이 아니에요."
            else "개인정보 보호를 위해 받은 문자 내용은 표시하지 않아요.",
                Modifier.padding(horizontal = 3.dp), style = AnsimType.BodyLong, color = AnsimSubtle)
            if (!isPreview) {
                Spacer(Modifier.height(6.dp))
                Text("알림 상태: ${guardianNotificationStatusText(record?.status)} · 실제 휴대폰 수신 여부는 확인되지 않았어요.",
                    Modifier.padding(horizontal = 3.dp), style = ansimStyle(13, 19), color = AnsimSubtle)
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            AnsimPrimaryButton(if (canCallSavedGuardian) "${name}에게 전화" else if (demo) "엄마에게 전화" else "전화할 가족 번호 직접 입력",
                { if (canCallSavedGuardian) callGuardian() else onManualNumberClick() }, icon = AnsimIcons.Phone)
            Spacer(Modifier.height(12.dp))
            AnsimOutlineButton("알림 기록 보기", { if (isPreview && DemoPairing.SHOW_DEMO_NOTICES) showPreviewInfo = true else onBackClick() },
                height = 52.dp)
        }
    }
}

@Composable
private fun GuardianAlertRow(label: String, status: String, valueColor: Color = AnsimSubtle, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = AnsimType.Body, color = AnsimInk)
        Text(status, style = ansimStyle(15, 22, if (bold) FontWeight.Bold else FontWeight.Normal), color = valueColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

private fun guardianNotificationStatusText(status: String?): String = when (status?.lowercase()) {
    "sent", "success" -> "발송 처리됨"
    "failed", "error" -> "전송 실패"
    "pending", "queued" -> "전송 대기"
    else -> "확인 필요"
}