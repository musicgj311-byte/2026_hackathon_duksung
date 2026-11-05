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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.theme.*

/** 라디오 4개 + 기본 정보 세 줄 + 하단 저장 버튼. */
@Composable
fun NotificationSettingsScreen(currentType: String, currentLanguage: String,
                               onBackClick: () -> Unit, onSaveClick: (String, String) -> Unit,
                               onPermissionClick: () -> Unit = {}, onGuardianClick: () -> Unit = {},
                               onChangeRoleClick: () -> Unit = {}, onBackendClick: () -> Unit = {}) {
    val context = LocalContext.current
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
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = 15.dp)) {
        TextButton(onClick = onBackClick) { Text("‹   설정", color = AnsimInk, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(14.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("알림 방식", color = AnsimMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(5.dp))
            choices.forEach { (value, label) ->
                Row(Modifier.fillMaxWidth().heightIn(min = 54.dp)
                    .clickable { draftType = value }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f), color = AnsimInk, fontSize = 15.sp,
                        fontWeight = if (draftType == value) FontWeight.Bold else FontWeight.Medium)
                    RadioButton(selected = draftType == value, onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = AnsimGreen))
                }
                HorizontalDivider(color = Color(0xFFEEF0F3))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (draftType == "기본 알림") "이 방식을 저장하면 앱 글씨가 자동으로 커져요."
                else "이 방식은 기본 글씨 크기로 표시돼요.",
                color = AnsimMuted, fontSize = 13.sp, lineHeight = 19.sp
            )
            HorizontalDivider(color = Color(0xFFEEF0F3))
            Spacer(Modifier.height(18.dp))
            // 기본 정보 제목을 연속 5번 누르면 이 화면에만 개발 도구가 나타납니다.
            Text("기본 정보", color = AnsimMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.clickable {
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
                    Row(Modifier.fillMaxWidth().height(40.dp).clickable { draftLanguage = lang; languagesVisible = false }
                        .padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(lang, Modifier.weight(1f), color = AnsimInk, fontSize = 14.sp)
                        if (draftLanguage == lang) Text("✓", color = AnsimGreen)
                    }
                }
                HorizontalDivider(color = Color(0xFFEEF0F3))
            }
            // 보호자 서버 연결을 조회하기 전에는 전화번호만으로 '연결됨'이라 표시하지 않습니다.
            SettingsDetailRow("보호자", "연결 상태 확인 전", onGuardianClick)
            SettingsDetailRow("권한", permissionsLabel, onPermissionClick,
                valueColor = if (allPermissions) AnsimProtected else AnsimDanger)
            SettingsDetailRow("사용 역할", "변경하기", onChangeRoleClick)
            if (showDeveloperTools) {
                SettingsDetailRow("개발자 도구", "서버 연결 테스트", onBackendClick)
            }
        }
        Text("저장하지 않고 나가면 변경 내용이 취소돼요.", color = AnsimMuted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        AnsimPrimaryButton("저장", { onSaveClick(draftType, draftLanguage) })
    }
}

@Composable
private fun SettingsDetailRow(
    title: String, value: String, onClick: () -> Unit,
    valueColor: Color = AnsimMuted
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 51.dp).clickable(onClick = onClick)
        .padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = AnsimInk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(value, modifier = Modifier.widthIn(max = 168.dp), color = valueColor,
            fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Spacer(Modifier.width(8.dp))
        Text("›", color = AnsimMuted, fontSize = 22.sp)
    }
    HorizontalDivider(color = Color(0xFFEEF0F3))
}