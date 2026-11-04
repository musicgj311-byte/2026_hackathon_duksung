package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.hackathon.ansimmessagepractice.guardian.GuardianCall
import com.hackathon.ansimmessagepractice.guardian.rememberGuardianCallAction
import com.hackathon.ansimmessagepractice.detection.AlertStore
import com.hackathon.ansimmessagepractice.detection.AlertRecord
import java.util.concurrent.TimeUnit
import com.hackathon.ansimmessagepractice.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 정상일 때 홈 / 필수 권한이 꺼졌을 때 보호 중단 홈.
 * guardianConnected=null은 백엔드 조회 전이며, 로컬 전화번호로 서버 연결을 추측하지 않습니다.
 * 화면 제목을 누르면 프론트엔드 확인용 시연 메뉴가 열립니다(평상시 숨김).
 */
@Composable
fun HomeScreen(
    selectedType: String, selectedLanguage: String,
    onTestWarningClick: (String) -> Unit, onSettingsClick: () -> Unit,
    onPermissionClick: () -> Unit, onGuardianManageClick: () -> Unit,
    onGuardianPreviewClick: () -> Unit, onTestOverlayClick: (String) -> Unit,
    onAlertDetailClick: (String) -> Unit = {},
    onRecentAlertClick: (AlertRecord) -> Unit = {},
    guardianConnected: Boolean? = null, guardianDisplayName: String? = null
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
    val hasPhone = GuardianCall.validNumber(savedPhone)
    val family = savedName.ifBlank { "가족" } + if (savedRelation.isNotBlank()) " ($savedRelation)" else ""
    val masked = savedPhone.filter(Char::isDigit).let { digits ->
        if (digits.length >= 8) "${digits.take(3)}-****-${digits.takeLast(4)}" else ""
    }

    Column(
        Modifier.fillMaxSize().background(AnsimCream).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().height(41.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                // 개발 미리보기 버튼은 시안의 홈 화면에 노출되지 않습니다.
                // 테스트가 필요할 때만 로고를 연속 5번 눌러 숨겨진 메뉴를 엽니다.
                Modifier.clickable {
                    demoTapCount += 1
                    if (demoTapCount >= 5) { showDemo = !showDemo; demoTapCount = 0 }
                }.padding(horizontal = 3.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                AnsimBrandMark(size = 23.dp)
                Text("안심문자", color = AnsimInk, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
            IconButton(onClick = onSettingsClick) { HomeSunIcon() }
        }
        Spacer(Modifier.height(9.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomePanel(if (ready) AnsimProtected else AnsimDanger) {
                Text(
                    if (ready) "☑  보호 중" else "⚠  보호 중단",
                    fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (ready) AnsimProtected else AnsimDanger
                )
                Text(
                    if (ready) "문자와 카카오톡을 확인하고 있어요"
                    else "꺼진 권한이 있어 일부 경고를 표시할 수 없어요",
                    color = AnsimMuted, fontSize = 12.sp, lineHeight = 18.sp
                )
                if (!ready) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFF0F1F3))
                    StatusRow("알림 읽기", notification)
                    StatusRow("다른 앱 위에 표시", overlay)
                    if (android.os.Build.VERSION.SDK_INT >= 33) StatusRow("경고 알림 표시", postNotifications)
                    Button(
                        onClick = onPermissionClick,
                        modifier = Modifier.fillMaxWidth().height(43.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AnsimGreen),
                        shape = RoundedCornerShape(6.dp)
                    ) { Text("권한 다시 설정", fontWeight = FontWeight.Bold) }
                }
            }

            HomePanel(if (guardianConnected == true) AnsimProtected else if (!hasPhone) AnsimCaution else Color(0xFFE0E4E9)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("보호자", fontSize = 12.sp, color = AnsimMuted)
                        Text(when {
                            guardianConnected == true -> guardianDisplayName?.takeIf { it.isNotBlank() } ?: family
                            hasPhone -> family
                            else -> "등록된 보호자가 없어요"
                        }, color = AnsimInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (hasPhone) {
                            Text(masked, color = AnsimMuted, fontSize = 12.sp)
                            if (guardianConnected != true) Text("전화번호만 저장됨 · 자동 알림 연결 상태는 확인 전",
                                color = AnsimMuted, fontSize = 11.sp, lineHeight = 17.sp)
                        } else {
                            Text("위험한 문자가 오면 가족에게 바로 연락할 수 있도록 등록해요.",
                                color = AnsimMuted, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                    TextButton(onClick = onGuardianManageClick) {
                        Text(if (hasPhone || guardianConnected == true) "관리" else "등록",
                            color = AnsimGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                if (!hasPhone) {
                    OutlinedButton(onClick = onGuardianManageClick, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)) {
                        Text("보호자 등록", color = AnsimGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 권한이 꺼지면 최근 알림 카드와 하단 통화 버튼을 숨김.
            // 다만 개발용 미리보기에서는 Mock 경고를 계속 테스트할 수 있습니다.
            if (ready || showDemo) {
                HomePanel(Color(0xFFE2E5EA)) {
                    Text("최근 알림", fontWeight = FontWeight.Bold, color = AnsimInk, fontSize = 15.sp)
                    HorizontalDivider(color = Color(0xFFEAECF0))
                    if (showDemo) {
                        Text("시연 예시 · 실제 수신 기록이 아닙니다", fontSize = 11.sp, color = AnsimMuted)
                        DemoAlertLine("위험", "가족 사칭 문자", "어제", AnsimDanger) {
                            onAlertDetailClick("danger")
                        }
                        DemoAlertLine("주의", "택배 사칭 의심 문자", "3일 전", AnsimCaution) {
                            onAlertDetailClick("caution")
                        }
                    } else if (recentAlerts.isEmpty()) {
                        Text("최근 받은 경고가 없어요", color = AnsimMuted,
                            fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        recentAlerts.take(5).forEach { alert ->
                            val elapsed = System.currentTimeMillis() - alert.at
                            val whenText = when {
                                elapsed < TimeUnit.MINUTES.toMillis(1) -> "방금"
                                elapsed < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(elapsed)}분 전"
                                elapsed < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(elapsed)}시간 전"
                                else -> "${TimeUnit.MILLISECONDS.toDays(elapsed)}일 전"
                            }
                            DemoAlertLine(if (alert.level == "danger") "위험" else "주의",
                                "${if (alert.source == "kakao") "카카오톡 · " else "문자 · "}${alert.title}",
                                whenText, if (alert.level == "danger") AnsimDanger else AnsimCaution) {
                                onRecentAlertClick(alert)
                            }
                        }
                    }
                }
            }
            if (showDemo) {
                Text("시연 전용 · 안심문자 로고를 5번 누르면 닫혀요", color = AnsimMuted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { onTestWarningClick("danger") },
                        modifier = Modifier.weight(1f)) { Text("위험", fontSize = 12.sp) }
                    OutlinedButton(onClick = { onTestWarningClick("caution") },
                        modifier = Modifier.weight(1f)) { Text("주의", fontSize = 12.sp) }
                    OutlinedButton(onClick = { onTestWarningClick("timeout") },
                        modifier = Modifier.weight(1f)) { Text("지연", fontSize = 12.sp) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { onTestOverlayClick("checking") },
                        modifier = Modifier.weight(1f)) { Text("확인 중", fontSize = 12.sp) }
                    OutlinedButton(onClick = { onTestOverlayClick("danger") },
                        modifier = Modifier.weight(1f)) { Text("오버레이", fontSize = 12.sp) }
                    OutlinedButton(onClick = onGuardianPreviewClick,
                        modifier = Modifier.weight(1f)) { Text("보호자", fontSize = 12.sp) }
                }
            }
        }
        if (ready) {
            Spacer(Modifier.height(10.dp))
            if (hasPhone) {
                AnsimPrimaryButton("☎  보호자에게 전화", { callGuardian() })
            }
        }
    }
}

@Composable
private fun HomePanel(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(11.dp), shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(accent))
            Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
        }
    }
}

@Composable
private fun StatusRow(title: String, granted: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = AnsimInk, fontSize = 12.sp)
        Text(if (granted) "허용됨" else "꺼짐",
            color = if (granted) AnsimProtected else AnsimDanger, fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DemoAlertLine(level: String, title: String, time: String,
                          accent: Color, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(if (level == "위험") "⚠ 위험" else "⚠ 주의", color = accent,
                fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(13.dp))
            Text(title, Modifier.weight(1f), color = AnsimInk, fontSize = 13.sp)
            Text(time, color = AnsimMuted, fontSize = 11.sp)
        }
        HorizontalDivider(color = Color(0xFFF1F2F4))
    }
}

@Composable
private fun HomeShieldIcon() {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val shield = Path().apply {
            moveTo(w * .5f, h * .09f)
            lineTo(w * .86f, h * .22f)
            lineTo(w * .84f, h * .61f)
            quadraticTo(w * .78f, h * .78f, w * .5f, h * .92f)
            quadraticTo(w * .22f, h * .78f, w * .16f, h * .61f)
            lineTo(w * .14f, h * .22f)
            close()
        }
        drawPath(shield, color = AnsimGreen, style = Stroke(width = w * .068f))
        drawLine(AnsimGreen, Offset(w * .34f, h * .48f), Offset(w * .46f, h * .60f),
            strokeWidth = w * .06f, cap = StrokeCap.Round)
        drawLine(AnsimGreen, Offset(w * .46f, h * .60f), Offset(w * .69f, h * .37f),
            strokeWidth = w * .06f, cap = StrokeCap.Round)
    }
}

@Composable
private fun HomeSunIcon() {
    Canvas(Modifier.size(23.dp)) {
        val r = size.minDimension * .19f
        val c = center
        drawCircle(AnsimInk, radius = r, center = c, style = Stroke(width = 1.6.dp.toPx()))
        for (i in 0 until 8) {
            val radians = (2 * PI * i / 8).toFloat()
            val dx = cos(radians)
            val dy = sin(radians)
            drawLine(
                color = AnsimInk,
                start = Offset(c.x + dx * size.minDimension * .32f, c.y + dy * size.minDimension * .32f),
                end = Offset(c.x + dx * size.minDimension * .43f, c.y + dy * size.minDimension * .43f),
                strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round
            )
        }
    }
}