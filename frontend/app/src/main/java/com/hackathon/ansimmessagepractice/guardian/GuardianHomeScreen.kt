package com.hackathon.ansimmessagepractice.guardian

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.hackathon.ansimmessagepractice.data.api.BackendConfig
import com.hackathon.ansimmessagepractice.data.api.GuardianApi
import com.hackathon.ansimmessagepractice.data.api.GuardianNotificationRecord
import com.hackathon.ansimmessagepractice.data.api.formatGuardianEventTime
import com.hackathon.ansimmessagepractice.data.api.guardianScamTypeName
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimAppIcon
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimOutlineButton
import com.hackathon.ansimmessagepractice.ui.onboarding.AlertLine
import com.hackathon.ansimmessagepractice.ui.onboarding.LevelBadge
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import com.hackathon.ansimmessagepractice.ui.theme.*
import kotlinx.coroutines.launch

/** 최종 시안 p.34. 원문은 표시하지 않으며 실제 서버 알림만 목록에 표시합니다. */
@Composable
fun GuardianHomeScreen(onInputCode: () -> Unit, onManualCall: () -> Unit,
                       onChangeRole: () -> Unit, onNotificationSettings: () -> Unit,
                       onBackendSettings: () -> Unit, onAlertClick: (GuardianNotificationRecord) -> Unit,
                       onDemoAlertClick: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val demo = DemoPairing.enabled(context)
    var confirmed by remember { mutableStateOf(if (demo) DemoPairing.approved(context) else prefs.getBoolean("guardian_link_confirmed", false)) }
    var protectedId by remember { mutableStateOf(prefs.getString("guardian_link_protected_id", "").orEmpty()) }
    var ownId by remember { mutableStateOf(prefs.getString("guardian_link_guardian_id", "").orEmpty()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var debugTap by remember { mutableIntStateOf(0) }
    var showDebug by remember { mutableStateOf(false) }
    var confirmDisconnect by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var records by remember { mutableStateOf<List<GuardianNotificationRecord>>(emptyList()) }
    var notificationError by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    // 연결 정보/서버 응답이 바뀔 때 사용자가 다시 방문해도 최신 기록 조회
    LaunchedEffect(confirmed, refresh) {
        if (demo || !BackendConfig.isConfigured(context)) return@LaunchedEffect
        loading = true
        runCatching { GuardianApi.notifications(context) }
            .onSuccess { records = it; notificationError = "" }
            .onFailure { notificationError = "알림을 불러오지 못했어요. 다시 시도해 주세요." }
        loading = false
    }
    val dangerRecords = records.filter { it.level.equals("danger", ignoreCase = true) }
    val protectedName = if (demo && confirmed) DemoPairing.PROTECTED_NAME else
        dangerRecords.firstNotNullOfOrNull { it.protectedName?.takeIf(String::isNotBlank) } ?: "연결된 가족"
    if (confirmDisconnect) AlertDialog(
        onDismissRequest = { confirmDisconnect = false },
        title = { Text("가족 연결을 해제할까요?") },
        text = { Text("해제하면 가족의 위험 알림을 받을 수 없어요.") },
        confirmButton = { TextButton(onClick = {
            confirmDisconnect = false
            if (demo) {
                DemoPairing.clear(context)
                confirmed = false
                error = ""
            } else if (!PairingIntegration.isConfigured || protectedId.isBlank() || ownId.isBlank()) {
                error = "연결 서버가 확인되지 않아 연결을 해제할 수 없어요."
            } else {
                busy = true
                scope.launch {
                    runCatching { PairingIntegration.backend.unlinkGuardian(protectedId, ownId) }
                        .onSuccess {
                            prefs.edit().remove("guardian_link_protected_id").remove("guardian_link_guardian_id")
                                .putBoolean("guardian_link_confirmed", false).apply()
                            confirmed = false; protectedId = ""; ownId = ""; records = emptyList(); error = ""
                        }.onFailure { error = it.message ?: "연결 해제에 실패했어요." }
                    busy = false
                }
            }
        }) { Text("해제", color = AnsimDanger) } },
        dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text("취소") } }
    )

    Column(Modifier.fillMaxSize().background(AnsimCream).statusBarsPadding().navigationBarsPadding()) {
        // 최종 시안 34p 상단: 흰 바탕 56dp + 구분선, 앱 아이콘 + '안심문자', 오른쪽 '보호자' 꼬리표(메뉴)
        Column(Modifier.fillMaxWidth().background(Color.White)) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 20.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.clickable(interactionSource = null, indication = null) {
                    debugTap += 1
                    if (debugTap >= 5) { showDebug = !showDebug; debugTap = 0 }
                }, verticalAlignment = Alignment.CenterVertically) {
                    AnsimAppIcon(size = 24.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("안심문자", style = ansimStyle(18, 26, FontWeight.ExtraBold), color = AnsimInk)
                }
                Spacer(Modifier.weight(1f))
                Box {
                    Box(Modifier.heightIn(min = 28.dp).background(AnsimBlueSoft, RoundedCornerShape(14.dp))
                        .clickable { menuExpanded = true }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center) {
                        Text("보호자", style = ansimStyle(15, 20, FontWeight.Bold), color = AnsimBlueText)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("알림 권한 확인") }, onClick = {
                            menuExpanded = false; onNotificationSettings()
                        })
                        DropdownMenuItem(text = { Text("사용 역할 변경") }, onClick = {
                            menuExpanded = false; onChangeRole()
                        })
                        if (confirmed) DropdownMenuItem(text = { Text("가족 연결 해제", color = AnsimDangerText) },
                            onClick = { menuExpanded = false; confirmDisconnect = true })
                        if (showDebug) DropdownMenuItem(text = { Text("개발자 도구 · 서버 연결 테스트") },
                            onClick = { menuExpanded = false; onBackendSettings() })
                    }
                }
            }
            HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 16.dp)) {
            Text("지키고 있는 가족", style = AnsimType.Section, color = AnsimMuted,
                modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.height(11.dp))
            val shape = RoundedCornerShape(16.dp)
            Row(Modifier.fillMaxWidth().background(Color.White, shape).border(1.dp, AnsimDivider, shape)
                .padding(start = 21.dp, end = 20.dp, top = 18.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).background(AnsimBlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                    if (confirmed) Text(protectedName.firstOrNull()?.toString() ?: "가",
                        style = ansimStyle(18, 24, FontWeight.Bold), color = AnsimBlueText)
                    else AnsimIcon(AnsimIcons.Plus, AnsimBlueText, 20.dp)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (confirmed) protectedName else "연결된 가족이 없어요",
                        style = ansimStyle(18, 25, FontWeight.Bold), color = AnsimInk)
                    if (confirmed) Row(verticalAlignment = Alignment.CenterVertically) {
                        AnsimIcon(AnsimIcons.Check, AnsimProtected, 15.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("보호 중", style = ansimStyle(15, 22, FontWeight.SemiBold), color = AnsimProtected)
                    } else Text("6자리 코드를 입력해 주세요", style = AnsimType.Body, color = AnsimMuted)
                }
                if (confirmed) LevelBadge("위험", "위험 ${if (demo) 1 else dangerRecords.size}건")
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().background(Color.White, shape).border(1.dp, AnsimDivider, shape)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("최근 위험 알림", Modifier.weight(1f), style = ansimStyle(15, 22, FontWeight.Bold), color = AnsimInk)
                    if (!demo && BackendConfig.isConfigured(context)) Text("새로고침",
                        Modifier.clickable { refresh++ }.padding(4.dp), style = AnsimType.Body, color = AnsimBlue)
                }
                Spacer(Modifier.height(7.dp))
                HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
                when {
                    demo && confirmed -> {
                        AlertLine("위험", "엄마 · 가족 사칭 문자", "어제", onDemoAlertClick)
                        if (DemoPairing.SHOW_DEMO_NOTICES) Text("시연 예시 · 실제 알림이 전송된 것은 아니에요.",
                            style = ansimStyle(13, 18), color = AnsimSubtle)
                    }
                    demo -> GuardianEmptyText("시연용 연결 코드를 입력하면 가족 카드와 위험 알림 예시를 확인할 수 있어요.")
                    !BackendConfig.isConfigured(context) -> GuardianEmptyText("알림을 받으려면 서버 연결이 필요해요.")
                    loading -> GuardianEmptyText("최근 알림을 확인하고 있어요…")
                    notificationError.isNotBlank() -> GuardianEmptyText(notificationError, AnsimCaution)
                    dangerRecords.isEmpty() -> GuardianEmptyText("최근 받은 위험 알림이 없어요")
                    else -> dangerRecords.take(5).forEachIndexed { index, record ->
                        if (index > 0) HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
                        AlertLine("위험",
                            "${record.protectedName?.takeIf(String::isNotBlank) ?: "가족"} · ${guardianScamTypeName(record.scamType)} 문자",
                            formatGuardianEventTime(record.sentAt)) { onAlertClick(record) }
                    }
                }
            }
            Spacer(Modifier.height(11.dp))
            Text("위험 판정일 때만 알림이 와요. 받은 문자 내용은 보이지 않아요.",
                style = AnsimType.BodyLong, color = AnsimMuted, modifier = Modifier.padding(horizontal = 4.dp))
            if (error.isNotBlank()) Text(error, Modifier.padding(top = 8.dp), style = AnsimType.Body, color = AnsimDangerText)
        }
        Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            AnsimOutlineButton("가족 추가하기", onInputCode, enabled = !busy, icon = AnsimIcons.Plus)
        }
    }
}

@Composable
private fun GuardianEmptyText(text: String, color: Color = AnsimMuted) {
    Text(text, Modifier.padding(top = 15.dp, bottom = 12.dp), style = AnsimType.Body, color = color)
}
