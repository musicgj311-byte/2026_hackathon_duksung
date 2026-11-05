package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hackathon.ansimmessagepractice.guardian.DemoPairing
import com.hackathon.ansimmessagepractice.ui.theme.*

/** 최종 시안 27p: 알림 방식 라디오 4줄 + 기본 정보(언어·보호자·권한) + 하단 저장 버튼. */
@Composable
fun NotificationSettingsScreen(currentType: String, currentLanguage: String,
                               onBackClick: () -> Unit, onSaveClick: (String, String) -> Unit,
                               onPermissionClick: () -> Unit = {}, onGuardianClick: () -> Unit = {},
                               onChangeRoleClick: () -> Unit = {}, onBackendClick: () -> Unit = {}) {
    val context = LocalContext.current
    val savedGuardianName = com.hackathon.ansimmessagepractice.guardian.GuardianCall.savedName(context)
    val savedGuardianRelation = context.getSharedPreferences("ansim_settings", android.content.Context.MODE_PRIVATE)
        .getString("guardian_relationship", "").orEmpty()
    val displayedGuardian = savedGuardianName.takeIf { it.isNotBlank() }?.let {
        it + savedGuardianRelation.takeIf(String::isNotBlank)?.let { relation -> " ($relation)" }.orEmpty()
    } ?: if (DemoPairing.approved(context)) DemoPairing.GUARDIAN_NAME else "등록 안 됨"
    val lifecycleOwner = LocalLifecycleOwner.current
    var canReadNotifications by remember { mutableStateOf(PermissionStatus.notificationAccess(context)) }
    var canDrawOverlay by remember { mutableStateOf(PermissionStatus.overlay(context)) }
    var canPostNotifications by remember { mutableStateOf(PermissionStatus.postNotifications(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canReadNotifications = PermissionStatus.notificationAccess(context)
                canDrawOverlay = PermissionStatus.overlay(context)
                canPostNotifications = PermissionStatus.postNotifications(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val allPermissions = canReadNotifications && canDrawOverlay && canPostNotifications
    val permissionsLabel = if (allPermissions) "모두 허용됨" else "설정 필요"
    var draftType by remember(currentType) { mutableStateOf(currentType) }
    var draftLanguage by remember(currentLanguage) { mutableStateOf(currentLanguage) }
    var languagesVisible by remember { mutableStateOf(false) }
    var developerTapCount by remember { mutableIntStateOf(0) }
    var showDeveloperTools by remember { mutableStateOf(false) }
    val choices = listOf(
        "기본 알림" to "큰 글씨와 소리로 알려주세요",
        "화면 안내 중심" to "소리보다는 화면과 진동으로",
        "쉬운 안내" to "단순한 게 좋아요",
        "음성 안내 중심" to "화면보다는 소리가 편해요"
    )
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        AnsimTopBar("설정", onBackClick)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(26.dp))
            Text("알림 방식", style = AnsimType.Section, color = AnsimSubtle)
            Spacer(Modifier.height(7.dp))
            choices.forEach { (value, label) ->
                val selected = draftType == value
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .clickable(role = Role.RadioButton) { draftType = value },
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f), color = AnsimInk,
                        style = ansimStyle(17, 24, if (selected) FontWeight.Bold else FontWeight.Normal))
                    AnsimRadio(selected)
                }
                HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
            }
            Spacer(Modifier.height(22.dp))
            // 기본 정보 제목을 연속 5번 누르면 이 화면에만 개발 도구가 나타납니다.
            Text("기본 정보", style = AnsimType.Section, color = AnsimSubtle,
                modifier = Modifier.clickable(interactionSource = null, indication = null) {
                    developerTapCount++
                    if (developerTapCount >= 5) {
                        showDeveloperTools = !showDeveloperTools
                        developerTapCount = 0
                    }
                })
            Spacer(Modifier.height(7.dp))
            SettingsDetailRow("언어", draftLanguage, onClick = { languagesVisible = !languagesVisible })
            if (languagesVisible) {
                listOf("한국어", "English", "Tiếng Việt", "中文").forEach { lang ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { draftLanguage = lang; languagesVisible = false }
                        .padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(lang, Modifier.weight(1f), style = AnsimType.Body, color = AnsimInk)
                        AnsimRadio(draftLanguage == lang)
                    }
                }
                HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
            }
            // 보호자 서버 연결을 조회하기 전에는 전화번호만으로 '연결됨'이라 표시하지 않습니다.
            SettingsDetailRow("보호자", displayedGuardian, onGuardianClick)
            SettingsDetailRow("권한", permissionsLabel, onPermissionClick,
                valueColor = if (allPermissions) AnsimProtected else AnsimDangerText, valueBold = true)
            // 시안에는 없지만 2p의 '설정에서 언제든 바꿀 수 있어요' 약속을 지키기 위해 역할 변경 줄을 둡니다.
            SettingsDetailRow("사용 역할", "보호받는 분", onChangeRoleClick)
            if (showDeveloperTools) {
                SettingsDetailRow("개발자 도구", "서버 연결 테스트", onBackendClick)
            }
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            AnsimPrimaryButton("저장", { onSaveClick(draftType, draftLanguage) })
        }
    }
}

@Composable
private fun SettingsDetailRow(
    title: String, value: String, onClick: () -> Unit,
    valueColor: Color = AnsimMuted, valueBold: Boolean = false
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = AnsimType.Row, color = AnsimInk)
        Text(value, modifier = Modifier.widthIn(max = 200.dp), color = valueColor,
            style = ansimStyle(15, 22, if (valueBold) FontWeight.Bold else FontWeight.Normal), textAlign = TextAlign.End)
        Spacer(Modifier.width(2.dp))
        AnsimIcon(AnsimIcons.ChevronRight, valueColor, 15.dp)
    }
    HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
}
