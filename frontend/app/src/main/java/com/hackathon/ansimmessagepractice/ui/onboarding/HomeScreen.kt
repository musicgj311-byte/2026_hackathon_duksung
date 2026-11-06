package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.hackathon.ansimmessagepractice.guardian.DemoPairing
import com.hackathon.ansimmessagepractice.guardian.GuardianCall
import com.hackathon.ansimmessagepractice.guardian.rememberGuardianCallAction
import com.hackathon.ansimmessagepractice.detection.AlertStore
import com.hackathon.ansimmessagepractice.detection.AlertRecord
import java.util.concurrent.TimeUnit
import com.hackathon.ansimmessagepractice.ui.theme.*

/**
 * guardianConnected=null은 백엔드 조회 전이며, 로컬 전화번호로 서버 연결을 추측하지 않습니다.
 * 상단 '모두안심' 로고를 5번 누르면 프론트엔드 확인용 시연 메뉴가 열립니다(평상시 숨김).
 */
@Composable
fun HomeScreen(
    selectedType: String, selectedLanguage: String,
    onTestWarningClick: (String) -> Unit, onSettingsClick: () -> Unit,
    onPermissionClick: () -> Unit, onGuardianManageClick: () -> Unit,
    onGuardianPreviewClick: () -> Unit, onTestOverlayClick: (String) -> Unit,
    onAlertDetailClick: (String) -> Unit = {},
    onRecentAlertClick: (AlertRecord) -> Unit = {},
    guardianConnected: Boolean? = null, guardianDisplayName: String? = null,
    onManualCallClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val callGuardian = rememberGuardianCallAction()
    val lifecycleOwner = LocalLifecycleOwner.current
    var notification by remember { mutableStateOf(PermissionStatus.notificationAccess(context)) }
    var overlay by remember { mutableStateOf(PermissionStatus.overlay(context)) }
    var postNotifications by remember { mutableStateOf(PermissionStatus.postNotifications(context)) }
    var recentAlerts by remember { mutableStateOf(AlertStore.list(context)) }
    var showDemo by remember { mutableStateOf(false) }
    var demoTapCount by remember { mutableIntStateOf(0) }
    var savedPhone by remember { mutableStateOf(GuardianCall.savedNumber(context)) }
    var savedName by remember { mutableStateOf(GuardianCall.savedName(context)) }
    var savedRelation by remember { mutableStateOf(context.getSharedPreferences("ansim_settings", android.content.Context.MODE_PRIVATE)
        .getString("guardian_relationship", "").orEmpty()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notification = PermissionStatus.notificationAccess(context)
                overlay = PermissionStatus.overlay(context)
                postNotifications = PermissionStatus.postNotifications(context)
                recentAlerts = AlertStore.list(context)
                savedPhone = GuardianCall.savedNumber(context)
                savedName = GuardianCall.savedName(context)
                savedRelation = context.getSharedPreferences("ansim_settings", android.content.Context.MODE_PRIVATE)
                    .getString("guardian_relationship", "").orEmpty()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(context) {
        val shared = context.getSharedPreferences("ansim_local_alerts", android.content.Context.MODE_PRIVATE)
        val callback = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "recent_v1") recentAlerts = AlertStore.list(context)
        }
        shared.registerOnSharedPreferenceChangeListener(callback)
        onDispose { shared.unregisterOnSharedPreferenceChangeListener(callback) }
    }
    val ready = notification && overlay && postNotifications
    val demo = DemoPairing.enabled(context)
    val demoLinked = DemoPairing.approved(context)
    val linked = guardianConnected == true || demoLinked
    val showExampleAlerts = demo || showDemo
    val hasPhone = GuardianCall.validNumber(savedPhone)
    val hasGuardian = hasPhone || linked
    val family = savedName.ifBlank { "가족" } + if (savedRelation.isNotBlank()) " ($savedRelation)" else ""
    val guardianName = when {
        demoLinked -> DemoPairing.GUARDIAN_NAME
        linked -> guardianDisplayName?.takeIf { it.isNotBlank() } ?: family
        else -> family
    }
    val guardianPhone = if (demoLinked && !hasPhone) DemoPairing.GUARDIAN_PHONE else formatPhone(savedPhone)

    Column(Modifier.fillMaxSize().background(AnsimCream).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.fillMaxWidth().background(Color.White)) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    // 개발 미리보기 버튼은 시안의 홈 화면에 노출되지 않습니다.
                    // 테스트가 필요할 때만 로고를 연속 5번 눌러 숨겨진 메뉴를 엽니다.
                    Modifier.clickable {
                        demoTapCount += 1
                        if (demoTapCount >= 5) { showDemo = !showDemo; demoTapCount = 0 }
                    }.padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnsimIcon(AnsimIcons.ShieldCheck, AnsimBlue, 24.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("모두안심", style = ansimStyle(18, 26, FontWeight.ExtraBold), color = AnsimInk)
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(48.dp).clickable(role = Role.Button, onClick = onSettingsClick),
                    contentAlignment = Alignment.Center) {
                    AnsimIcon(AnsimIcons.Sun, AnsimInk, 26.dp, contentDescription = "설정")
                }
                Spacer(Modifier.width(6.dp))
            }
            HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1) 보호 상태 카드
            when {
                !ready -> HomeCard(background = AnsimDangerCard, border = AnsimDanger, borderWidth = 2.dp,
                    vertical = 18.dp, bottom = 21.dp) {
                    StatusHeader(AnsimIcons.TriangleAlert, AnsimDangerText, "보호 중단", AnsimDangerText,
                        "꺼진 권한이 있어 일부 경고를 표시할 수 없어요")
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFF1E5E5), thickness = 1.dp)
                    StatusRow("알림 읽기", notification)
                    StatusRow("다른 앱 위에 표시", overlay)
                    StatusRow("경고 알림 표시", postNotifications)
                    Spacer(Modifier.height(14.dp))
                    AnsimPrimaryButton("권한 다시 설정", onPermissionClick, container = AnsimDanger,
                        height = 48.dp, textStyle = AnsimType.ButtonSmall)
                }
                !hasGuardian -> HomeCard(background = AnsimProtectedTint, border = AnsimProtected, borderWidth = 2.dp) {
                    StatusHeader(AnsimIcons.CircleCheck, AnsimProtected, "보호 중", AnsimInk,
                        "문자와 카카오톡을 확인하고 있어요")
                }
                else -> HomeCard {
                    StatusHeader(AnsimIcons.CircleCheck, AnsimProtected, "보호 중", AnsimProtected,
                        "문자와 카카오톡을 확인하고 있어요")
                }
            }

            // 2) 보호자 카드
            if (hasGuardian) {
                HomeCard(vertical = 17.dp, bottom = 18.dp) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("보호자", style = AnsimType.Body, color = AnsimSubtle)
                            Text(guardianName, style = ansimStyle(17, 24, FontWeight.Bold), color = AnsimInk)
                            if (ready && guardianPhone.isNotBlank()) {
                                Text(guardianPhone, style = AnsimType.Body, color = AnsimMuted)
                            }
                            if (!linked && DemoPairing.SHOW_DEMO_NOTICES) Text("전화번호만 저장됨 · 자동 알림 연결 상태는 확인 전",
                                style = ansimStyle(13, 19), color = AnsimSubtle)
                        }
                        Text("관리", Modifier.clickable(role = Role.Button, onClick = onGuardianManageClick)
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            style = ansimStyle(15, 22, FontWeight.SemiBold), color = AnsimBlue,
                            textDecoration = TextDecoration.Underline)
                    }
                }
            } else {
                HomeCard(vertical = 18.dp, bottom = 20.dp) {
                    Text("등록된 보호자가 없어요", style = ansimStyle(17, 24, FontWeight.Bold), color = AnsimInk)
                    Spacer(Modifier.height(10.dp))
                    Text("문자 확인은 계속되지만, 위험할 때 가족에게 알림이 전송되지 않아요.",
                        style = AnsimType.BodyLong, color = AnsimMuted)
                    Spacer(Modifier.height(16.dp))
                    AnsimOutlineButton("보호자 등록", onGuardianManageClick, textColor = AnsimBlue,
                        borderColor = AnsimBlue, height = 52.dp)
                }
            }

            // 3) 최근 알림 — 권한이 꺼지면 숨깁니다. 개발용 미리보기에서는 Mock 경고를 계속 볼 수 있습니다.
            if (ready || showDemo) {
                HomeCard(vertical = 17.dp, bottom = 6.dp) {
                    Text("최근 알림", style = ansimStyle(15, 22, FontWeight.Bold), color = AnsimInk)
                    Spacer(Modifier.height(7.dp))
                    HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
                    if (showExampleAlerts && hasGuardian) {
                        if (DemoPairing.SHOW_DEMO_NOTICES) Text("시연 예시 · 실제 수신 기록이 아닙니다",
                            Modifier.padding(top = 6.dp), style = ansimStyle(13, 18), color = AnsimSubtle)
                        AlertLine("위험", "가족 사칭 문자", "어제") { onAlertDetailClick("danger") }
                        HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
                        AlertLine("주의", "택배 사칭 의심 문자", "3일 전") { onAlertDetailClick("caution") }
                    } else if (recentAlerts.isEmpty()) {
                        Text("최근 받은 경고가 없어요", Modifier.padding(top = 15.dp, bottom = 16.dp),
                            style = AnsimType.Body, color = AnsimMuted)
                    } else {
                        recentAlerts.take(5).forEachIndexed { index, alert ->
                            if (index > 0) HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
                            val elapsed = System.currentTimeMillis() - alert.at
                            val whenText = when {
                                elapsed < TimeUnit.MINUTES.toMillis(1) -> "방금"
                                elapsed < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(elapsed)}분 전"
                                elapsed < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(elapsed)}시간 전"
                                elapsed < TimeUnit.DAYS.toMillis(2) -> "어제"
                                else -> "${TimeUnit.MILLISECONDS.toDays(elapsed)}일 전"
                            }
                            AlertLine(if (alert.level == "danger") "위험" else "주의",
                                "${if (alert.source == "kakao") "카카오톡 · " else ""}${alert.title}",
                                whenText) { onRecentAlertClick(alert) }
                        }
                    }
                }
            }
            if (showDemo) {
                Text("시연 전용 · 모두안심 로고를 5번 누르면 닫혀요", style = ansimStyle(13, 18), color = AnsimSubtle)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DemoChip("위험", Modifier.weight(1f)) { onTestWarningClick("danger") }
                    DemoChip("주의", Modifier.weight(1f)) { onTestWarningClick("caution") }
                    DemoChip("지연", Modifier.weight(1f)) { onTestWarningClick("timeout") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DemoChip("확인 중", Modifier.weight(1f)) { onTestOverlayClick("checking") }
                    DemoChip("오버레이", Modifier.weight(1f)) { onTestOverlayClick("danger") }
                    DemoChip("보호자", Modifier.weight(1f), onGuardianPreviewClick)
                }
            }
        }
        if (ready && hasGuardian) {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                AnsimPrimaryButton("보호자에게 전화", { if (hasPhone) callGuardian() else onManualCallClick() },
                    icon = AnsimIcons.Phone)
            }
        }
    }
}

/** 010-1234-5678 형태로 보기 좋게 표시합니다. 형식이 다르면 입력값을 그대로 둡니다. */
private fun formatPhone(raw: String): String {
    val digits = raw.filter(Char::isDigit)
    return when {
        digits.length == 11 -> "${digits.take(3)}-${digits.substring(3, 7)}-${digits.takeLast(4)}"
        digits.length == 10 && digits.startsWith("02") -> "02-${digits.substring(2, 6)}-${digits.takeLast(4)}"
        digits.length == 10 -> "${digits.take(3)}-${digits.substring(3, 6)}-${digits.takeLast(4)}"
        else -> raw.trim()
    }
}

@Composable
private fun HomeCard(
    background: Color = Color.White, border: Color = AnsimDivider, borderWidth: Dp = 1.dp,
    vertical: Dp = 19.dp, bottom: Dp = vertical,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().background(background, shape).border(borderWidth, border, shape)
        .padding(start = 20.dp, end = 20.dp, top = vertical, bottom = bottom), content = content)
}

@Composable
private fun StatusHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color,
                         title: String, titleColor: Color, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AnsimIcon(icon, iconColor, 26.dp)
        Spacer(Modifier.width(15.dp))
        Column {
            Text(title, style = ansimStyle(20, 28, FontWeight.Bold), color = titleColor)
            Text(subtitle, style = AnsimType.Body, color = AnsimMuted)
        }
    }
}

@Composable
private fun StatusRow(title: String, granted: Boolean) {
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = AnsimType.Body, color = AnsimInk)
        Text(if (granted) "허용됨" else "꺼짐", style = ansimStyle(15, 22, FontWeight.Bold),
            color = if (granted) AnsimProtected else AnsimDangerText)
    }
    HorizontalDivider(color = Color(0xFFF1E5E5), thickness = 1.dp)
}

@Composable
internal fun AlertLine(level: String, title: String, time: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically) {
        LevelBadge(level)
        Spacer(Modifier.width(13.dp))
        Text(title, Modifier.weight(1f), style = ansimStyle(17, 24), color = AnsimInk, maxLines = 1)
        Text(time, style = AnsimType.Body, color = AnsimSubtle)
    }
}

@Composable
internal fun LevelBadge(level: String, text: String = level) {
    val danger = level == "위험"
    Box(Modifier.heightIn(min = 23.dp).background(if (danger) AnsimDangerTint else AnsimCautionTint, RoundedCornerShape(6.dp))
        .padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
        Text(text, style = ansimStyle(15, 20, FontWeight.Bold), color = if (danger) AnsimDangerText else AnsimCaution)
    }
}

@Composable
private fun DemoChip(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.heightIn(min = 40.dp).background(Color.White, CircleShape).border(1.dp, AnsimBorder, CircleShape)
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, style = ansimStyle(13, 18, FontWeight.Medium), color = AnsimInk)
    }
}
