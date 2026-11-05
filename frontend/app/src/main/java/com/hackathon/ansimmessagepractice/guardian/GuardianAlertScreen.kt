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
    val name = GuardianCall.savedName(context).ifBlank { "가족" }
    // 이 기기에 등록된 번호는 보호 대상자 -> 보호자로 거는 번호입니다.
    // 보호자 모드의 실제 기록에서 보호 대상자의 번호로 오인하여 사용하면 안 됩니다.
    val canCallSavedGuardian = isPreview && GuardianCall.validNumber(GuardianCall.savedNumber(context))
    val callGuardian = rememberGuardianCallAction()
    val scamType = guardianScamTypeName(record?.scamType ?: "family_impersonation")
    val recipient = record?.protectedName?.takeIf { it.isNotBlank() } ?: "보호 대상자"
    val isDanger = record == null || record.level.equals("danger", ignoreCase = true)
    val isCaution = record?.level.equals("caution", ignoreCase = true)
    val levelText = if (isDanger) "위험" else if (isCaution) "주의" else "확인 필요"
    val levelColor = if (isDanger) AnsimDanger else if (isCaution) AnsimCaution else AnsimMuted
    val explanation = if (isPreview) {
        "가족을 사칭해 송금을 요구하는 문자예요. 원래 휴대폰 번호로 직접 전화해 보세요."
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

    Column(Modifier.fillMaxSize().background(AnsimCream).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp)) {
        TextButton(onClick = onBackClick) { Text("‹  보호자 알림", color = AnsimInk,
            fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(13.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Surface(shape = RoundedCornerShape(11.dp), color = Color.White, shadowElevation = 1.dp) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = AnsimGreen, shape = RoundedCornerShape(8.dp)) {
                        Text("☑", color = Color.White, fontSize = 21.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isPreview) "안심문자 · 경고 미리보기" else "안심문자 · 보호자 알림",
                                color = AnsimMuted, fontSize = 11.sp)
                            Text(if (isPreview) "시연" else "전송: ${formatGuardianEventTime(record?.sentAt)}",
                                color = AnsimMuted, fontSize = 11.sp)
                        }
                        Text(if (isPreview) "가족에게 위험 문자가 도착했어요" else "$recipient 님에게 의심 문자가 도착했어요",
                            color = AnsimInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(if (isPreview) "가족 사칭 문자예요. 지금 전화해서 확인해 주세요."
                        else "문자를 확인하기 전에 가족에게 먼저 전화해 주세요.",
                            color = AnsimMuted, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
            Surface(shape = RoundedCornerShape(11.dp), color = Color.White, shadowElevation = 1.dp) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(Modifier.fillMaxHeight().width(4.dp).background(levelColor))
                    Column(Modifier.weight(1f).padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$recipient · $scamType", modifier = Modifier.weight(1f), fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold, color = AnsimInk)
                            Surface(color = if (isDanger) Color(0xFFFDE8EB) else if (isCaution) Color(0xFFFFF1D9) else AnsimCream,
                                shape = RoundedCornerShape(6.dp)) {
                                Text(if (isDanger) "⚠ $levelText" else levelText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    color = levelColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Text(explanation, color = AnsimInk, fontSize = 14.sp, lineHeight = 22.sp)
                        HorizontalDivider(color = Color(0xFFEEF0F3))
                        GuardianAlertRow("위험 문자 수신", formatGuardianEventTime(record?.incidentCreatedAt))
                        GuardianAlertRow("경고 화면 표시", formatGuardianEventTime(record?.warningShownAt))
                    }
                }
            }
            Text(if (isPreview)
                "개인정보 보호를 위해 문자 원문을 표시하지 않아요. 현재 화면은 시연 예시이며 실제 사건 시각이 없어요."
            else "가족의 개인정보를 위해 문자 원문은 보여주지 않아요. 날짜가 없는 항목은 확인되지 않은 정보예요.",
                color = AnsimMuted, fontSize = 12.sp, lineHeight = 19.sp)
            if (!isPreview) {
                Text("알림 상태: ${guardianNotificationStatusText(record?.status)}",
                    color = AnsimMuted, fontSize = 12.sp, lineHeight = 18.sp)
                Text("알림을 보낸 기록이며 실제 휴대폰 수신 여부는 확인되지 않았어요.",
                    color = AnsimMuted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { if (canCallSavedGuardian) callGuardian() else onManualNumberClick() },
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(7.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AnsimGreen)) {
            Text(if (canCallSavedGuardian) "☎  ${name}에게 전화" else "☎  전화할 가족 번호 직접 입력",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
        }
        if (isPreview) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showPreviewInfo = true },
                modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(7.dp)) {
                Text("시연 정보 보기", color = AnsimInk, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GuardianAlertRow(label: String, status: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), color = AnsimInk, fontSize = 13.sp)
        Text(status, color = AnsimMuted, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

private fun guardianNotificationStatusText(status: String?): String = when (status?.lowercase()) {
    "sent", "success" -> "발송 처리됨"
    "failed", "error" -> "전송 실패"
    "pending", "queued" -> "전송 대기"
    else -> "확인 필요"
}