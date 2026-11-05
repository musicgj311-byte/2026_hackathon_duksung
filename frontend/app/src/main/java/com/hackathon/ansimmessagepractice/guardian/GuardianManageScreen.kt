package com.hackathon.ansimmessagepractice.guardian

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimBottomSheet
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimOutlineButton
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimPrimaryButton
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimTopBar
import com.hackathon.ansimmessagepractice.ui.theme.*
import java.util.Calendar

/**
 * 최종 시안 15p '보호자 관리' + 연결 해제 하단 시트.
 * 시연 연결(DemoPairing)과 이 휴대폰에 저장한 전화번호를 구분해서 표시합니다.
 * 저장한 번호만 있는 경우 '연결 해제'는 이 휴대폰의 번호만 지우며 서버 연결은 건드리지 않습니다.
 */
@Composable
fun GuardianManageScreen(onBack: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE) }
    var name by remember { mutableStateOf(GuardianCall.savedName(context)) }
    var phone by remember { mutableStateOf(GuardianCall.savedNumber(context)) }
    var relation by remember { mutableStateOf(prefs.getString("guardian_relationship", "").orEmpty()) }
    var savedAt by remember { mutableLongStateOf(prefs.getLong("guardian_contact_saved_at", 0L)) }
    var demoLinked by remember { mutableStateOf(DemoPairing.approved(context)) }
    var confirming by remember { mutableStateOf(false) }
    val hasPhone = GuardianCall.validNumber(phone)
    val hasGuardian = hasPhone || demoLinked

    val displayName = if (hasPhone) name.ifBlank { "가족" } + if (relation.isNotBlank()) " ($relation)" else ""
    else DemoPairing.GUARDIAN_NAME
    val displayPhone = if (hasPhone) formatGuardianPhone(phone) else DemoPairing.GUARDIAN_PHONE
    val linkedAt = if (hasPhone) savedAt else DemoPairing.approvedAt(context)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
            AnsimTopBar("보호자 관리", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp)) {
                if (hasGuardian) {
                    HorizontalDivider(color = AnsimInk, thickness = 2.dp)
                    ContactInfo("이름", displayName)
                    ContactInfo("전화번호", displayPhone)
                    ContactInfo(if (hasPhone) "등록일" else "연결일", formatKoreanDate(linkedAt))
                    Spacer(Modifier.height(16.dp))
                    AnsimOutlineButton("보호자 변경", onEdit, height = 52.dp)
                    Spacer(Modifier.height(16.dp))
                    AnsimOutlineButton("연결 해제", { confirming = true }, textColor = AnsimDangerText, height = 52.dp)
                } else {
                    Text("등록된 보호자가 없어요", style = ansimStyle(20, 28, FontWeight.Bold), color = AnsimInk)
                    Spacer(Modifier.height(10.dp))
                    Text("등록하면 위험 경고 화면에서 안전하게 가족에게 전화할 수 있어요.",
                        style = AnsimType.BodyLong, color = AnsimMuted)
                    Spacer(Modifier.height(24.dp))
                    AnsimPrimaryButton("보호자 등록", onEdit)
                }
            }
        }
        if (confirming) AnsimBottomSheet(onDismiss = { confirming = false }) {
            Text("보호자 연결을 해제할까요?", style = ansimStyle(20, 28, FontWeight.Bold), color = AnsimInk)
            Spacer(Modifier.height(10.dp))
            Text(if (hasPhone) "이 휴대폰에 저장된 보호자 번호가 지워지고, 경고 화면에서 바로 전화할 수 없어요."
            else "해제하면 위험 문자 알림이 전송되지 않고, 경고 화면에서 바로 전화할 수 없어요.",
                style = AnsimType.BodyLong, color = AnsimMuted)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnsimOutlineButton("취소", { confirming = false }, Modifier.weight(1f), height = 52.dp)
                AnsimPrimaryButton("해제", {
                    if (hasPhone) {
                        prefs.edit().remove("guardian_name").remove("guardian_phone")
                            .remove("guardian_relationship").remove("guardian_contact_saved_at").apply()
                        name = ""; phone = ""; relation = ""; savedAt = 0L
                    } else {
                        DemoPairing.clear(context)
                        demoLinked = false
                    }
                    confirming = false
                }, modifier = Modifier.weight(1f), container = AnsimDanger, height = 52.dp,
                    textStyle = AnsimType.ButtonSmall)
            }
        }
    }
}

@Composable
private fun ContactInfo(label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(101.dp), style = AnsimType.Row, color = AnsimMuted)
        Text(value, Modifier.weight(1f), style = ansimStyle(17, 24, FontWeight.Bold), color = AnsimInk)
    }
    HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
}

/** 시안 표기: 2026. 11. 2. */
internal fun formatKoreanDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return "이전 등록"
    val c = Calendar.getInstance().apply { timeInMillis = epochMillis }
    return "${c.get(Calendar.YEAR)}. ${c.get(Calendar.MONTH) + 1}. ${c.get(Calendar.DAY_OF_MONTH)}."
}

internal fun formatGuardianPhone(raw: String): String {
    val d = raw.filter(Char::isDigit)
    return when (d.length) {
        11 -> "${d.take(3)}-${d.substring(3, 7)}-${d.takeLast(4)}"
        10 -> if (d.startsWith("02")) "02-${d.substring(2, 6)}-${d.takeLast(4)}" else "${d.take(3)}-${d.substring(3, 6)}-${d.takeLast(4)}"
        else -> raw.trim()
    }
}
