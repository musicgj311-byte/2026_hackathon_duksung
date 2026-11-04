package com.hackathon.ansimmessagepractice.guardian

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimPrimaryButton
import com.hackathon.ansimmessagepractice.data.api.BackendConfig
import com.hackathon.ansimmessagepractice.data.api.GuardianApi
import com.hackathon.ansimmessagepractice.data.api.GuardianNotificationRecord
import com.hackathon.ansimmessagepractice.data.api.formatGuardianEventTime
import com.hackathon.ansimmessagepractice.data.api.guardianScamTypeName
import com.hackathon.ansimmessagepractice.ui.theme.*
import kotlinx.coroutines.launch

/** 보호자 홈: 연결 상태는 실제 서버 성공 응답으로 저장된 경우에만 '연결됨'이라고 표시. */
@Composable
fun GuardianHomeScreen(onInputCode: () -> Unit, onManualCall: () -> Unit,
                       onChangeRole: () -> Unit, onNotificationSettings: () -> Unit,
                       onBackendSettings: () -> Unit, onAlertClick: (GuardianNotificationRecord) -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var confirmed by remember { mutableStateOf(prefs.getBoolean("guardian_link_confirmed", false)) }
    var protectedId by remember { mutableStateOf(prefs.getString("guardian_link_protected_id", "").orEmpty()) }
    var ownId by remember { mutableStateOf(prefs.getString("guardian_link_guardian_id", "").orEmpty()) }
    var confirmDisconnect by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var developerTapCount by remember { mutableIntStateOf(0) }
    var showDeveloperTools by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var notificationRecords by remember { mutableStateOf<List<GuardianNotificationRecord>>(emptyList()) }
    var notificationLoadError by remember { mutableStateOf("") }
    var notificationLoading by remember { mutableStateOf(false) }
    var refreshSerial by remember { mutableIntStateOf(0) }
    LaunchedEffect(refreshSerial, confirmed) {
        if (!BackendConfig.isConfigured(context)) return@LaunchedEffect
        notificationLoading = true
        runCatching { GuardianApi.notifications(context) }
            .onSuccess { notificationRecords = it; notificationLoadError = "" }
            .onFailure { notificationLoadError = "서버 알림 이력을 불러오지 못했어요." }
        notificationLoading = false
    }
    if (confirmDisconnect) AlertDialog(
        onDismissRequest = { confirmDisconnect = false },
        title = { Text("가족 연결을 해제할까요?") },
        text = { Text("연결을 해제하면 가족의 위험 알림을 받을 수 없어요.") },
        confirmButton = {
            TextButton(onClick = {
                confirmDisconnect = false
                if (!PairingIntegration.isConfigured || protectedId.isBlank() || ownId.isBlank()) {
                    error = "서버가 연결되지 않아 해제할 수 없어요."
                } else {
                    busy = true
                    scope.launch {
                        runCatching { PairingIntegration.backend.unlinkGuardian(protectedId, ownId) }
                            .onSuccess {
                                prefs.edit().remove("guardian_link_protected_id")
                                    .remove("guardian_link_guardian_id")
                                    .putBoolean("guardian_link_confirmed", false).apply()
                                confirmed = false; protectedId = ""; ownId = ""; error = ""
                            }.onFailure { error = it.message ?: "연결 해제에 실패했어요." }
                        busy = false
                    }
                }
            }) { Text("해제", color = AnsimDanger) }
        },
        dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text("취소") } }
    )
    Column(Modifier.fillMaxSize().background(AnsimCream).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 17.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                // 보호자 홈 제목 5번 탭: 숨긴 서버 연결 진단 메뉴 표시/숨김.
                Text("안심문자", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold,
                    color = AnsimInk, modifier = Modifier.clickable {
                        developerTapCount++
                        if (developerTapCount >= 5) {
                            showDeveloperTools = !showDeveloperTools
                            developerTapCount = 0
                        }
                    })
                Text("가족 보호하기", color = AnsimGreen, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }
            Box {
                TextButton(onClick = { menuExpanded = true }) { Text("설정", color = AnsimInk) }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("알림 설정") }, onClick = {
                        menuExpanded = false; onNotificationSettings()
                    })
                    if (showDeveloperTools) {
                        DropdownMenuItem(text = { Text("개발자 도구 · 서버 테스트") }, onClick = {
                            menuExpanded = false; onBackendSettings()
                        })
                    }
                    DropdownMenuItem(text = { Text("사용 역할 변경") }, onClick = {
                        menuExpanded = false; onChangeRole()
                    })
                    if (confirmed) DropdownMenuItem(text = { Text("가족 연결 해제", color = AnsimDanger) },
                        onClick = { menuExpanded = false; confirmDisconnect = true })
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Surface(color = Color.White, shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (confirmed) "✓ 가족과 연결되었어요" else "아직 연결된 가족이 없어요",
                        color = if (confirmed) AnsimProtected else AnsimCaution,
                        fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (confirmed) "연결 코드를 확인했어요. 실제 알림 수신은 푸시 연동 후 가능해요."
                    else "보호받는 가족에게 6자리 코드를 받아 입력해 주세요.",
                        color = AnsimMuted, fontSize = 14.sp, lineHeight = 21.sp)
                    if (confirmed) Text("연결 상태는 마지막 서버 연결 결과를 기준으로 표시돼요.",
                        color = AnsimMuted, fontSize = 12.sp)
                }
            }
            Surface(color = Color.White, shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("최근 가족 알림", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = AnsimInk)
                        if (BackendConfig.isConfigured(context)) TextButton(onClick = { refreshSerial++ }) {
                            Text("새로고침", fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = AnsimCream)
                    if (!BackendConfig.isConfigured(context)) {
                        Text("알림 이력은 서버 연결 후 확인할 수 있어요.", color = AnsimMuted, fontSize = 14.sp)
                    } else if (notificationLoading) {
                        Text("알림 기록을 확인하고 있어요...", color = AnsimMuted, fontSize = 13.sp)
                    } else if (notificationLoadError.isNotBlank()) {
                        Text(notificationLoadError, color = AnsimCaution, fontSize = 13.sp)
                    } else if (notificationRecords.isEmpty()) {
                        Text("서버에 기록된 가족 알림이 없어요.", color = AnsimMuted, fontSize = 13.sp)
                    } else {
                        notificationRecords.take(5).forEach { record ->
                            Column(Modifier.fillMaxWidth().clickable { onAlertClick(record) }
                                .padding(vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("${if (record.level == "danger") "위험" else "주의"} · ${guardianScamTypeName(record.scamType)}",
                                    color = AnsimInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("알림 전송 시각: ${formatGuardianEventTime(record.sentAt)} · 상세 보기 ›",
                                    color = AnsimMuted, fontSize = 12.sp)
                            }
                            HorizontalDivider(color = AnsimCream)
                        }
                    }
                    Text("이는 서버의 FCM 전송 기록이며, 휴대폰 수신 성공을 보장하지 않아요.",
                        color = AnsimMuted, fontSize = 12.sp)
                }
            }
            if (error.isNotBlank()) Text(error, color = AnsimDanger, fontSize = 13.sp)
        }
        AnsimPrimaryButton(if (confirmed) "가족에게 전화하기" else "연결 코드 입력하기",
            if (confirmed) onManualCall else onInputCode)

    }
}