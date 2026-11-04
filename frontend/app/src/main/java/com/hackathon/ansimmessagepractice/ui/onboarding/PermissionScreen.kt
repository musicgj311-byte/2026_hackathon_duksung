package com.hackathon.ansimmessagepractice.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.hackathon.ansimmessagepractice.ui.theme.*

/** 감지/오버레이 권한과 잠금 화면 알림 설정. */
@Composable
fun PermissionScreen(onNextClick: () -> Unit, onBackClick: () -> Unit,
                     fromSettings: Boolean = false, onSkipClick: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notification by remember { mutableStateOf(PermissionStatus.notificationAccess(context)) }
    var overlay by remember { mutableStateOf(PermissionStatus.overlay(context)) }
    var postNotifications by remember { mutableStateOf(PermissionStatus.postNotifications(context)) }
    val requestPostNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        postNotifications = PermissionStatus.postNotifications(context)
        // OS에서 팝업을 다시 띄울 수 없거나 사용자가 거부한 경우에는 앱 알림 설정으로 안내합니다.
        if (!granted) LockScreenSettings.open(context)
    }
    var contacts by remember { mutableStateOf(PermissionStatus.contacts(context)) }
    val requestContacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        contacts = PermissionStatus.contacts(context)
    }
    var battery by remember { mutableStateOf(PermissionStatus.batteryExempt(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notification = PermissionStatus.notificationAccess(context)
                overlay = PermissionStatus.overlay(context)
                postNotifications = PermissionStatus.postNotifications(context)
                battery = PermissionStatus.batteryExempt(context)
                contacts = PermissionStatus.contacts(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun launch(intent: Intent) {
        val opened = runCatching { context.startActivity(intent) }.isSuccess
        if (!opened) android.widget.Toast.makeText(context, "휴대폰에서 해당 설정을 직접 열어 주세요.", android.widget.Toast.LENGTH_LONG).show()
    }
    val ready = notification && overlay && postNotifications
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 25.dp, vertical = 17.dp)) {
        if (!fromSettings) OnboardingStepHeader(1, onBackClick)
        else TextButton(onClick = onBackClick) { Text("‹  설정으로", color = AnsimInk) }
        Spacer(Modifier.height(19.dp))
        if (ready) {
            Text("✓", fontSize = 39.sp, color = AnsimProtected, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("필수 권한이\n모두 허용되었어요", fontSize = 28.sp, lineHeight = 36.sp,
                color = AnsimInk, fontWeight = FontWeight.ExtraBold)
        } else {
            Text("필수 권한 ${if (Build.VERSION.SDK_INT >= 33) 3 else 2}가지를\n허용해 주세요", fontSize = 28.sp, lineHeight = 36.sp,
                color = AnsimInk, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(5.dp))
            Text("문자를 확인하고 경고를 띄우는 데 필요해요.", color = AnsimMuted, fontSize = 14.sp)
        }
        Spacer(Modifier.height(22.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            PermissionLine("알림 읽기", "문자·카톡이 오면 바로 확인해요", true, notification) {
                launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            PermissionLine("다른 앱 위에 표시", "다른 앱을 쓰는 중에도 경고를 띄워요", true, overlay) {
                launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
            }
            // 알림이 앱 단위로 차단된 경우에는 앱 전체 알림 설정을 우선 엽니다.
            PermissionLine(
                "위험 알림 허용",
                "앱 알림을 켠 뒤 잠금 화면에도 위험을 알려드려요",
                Build.VERSION.SDK_INT >= 33,
                postNotifications,
                onClick = {
                    val runtimeDenied = Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                    if (runtimeDenied) requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else LockScreenSettings.open(context)
                },
                actionLabel = if (postNotifications) "잠금 화면 설정" else "허용하기"
            )
            PermissionLine("연락처 확인", "모르는 번호를 구분해 더 정확하게 검사해요 (선택)",
                false, contacts, actionLabel = if (contacts) null else "허용하기") {
                if (!contacts) requestContacts.launch(Manifest.permission.READ_CONTACTS)
            }
            if (!ready) PermissionLine("배터리 최적화 제외", "앱이 꺼지지 않게 해요", false, battery) {
                launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
            if (ready) {
                Spacer(Modifier.height(10.dp))
                Text("배터리 설정은 선택 사항이에요. 설정 메뉴에서 언제든 바꿀 수 있어요.",
                    color = AnsimMuted, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
        if (!ready && !fromSettings) {
            Text("필수 권한을 모두 허용하면 다음으로 넘어가요", modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center, color = AnsimMuted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
        }
        AnsimPrimaryButton(if (fromSettings) "설정으로 돌아가기" else "다음", onNextClick,
            enabled = ready || fromSettings)
        if (!ready && !fromSettings) TextButton(onClick = onSkipClick, modifier = Modifier.fillMaxWidth()) {
            Text("나중에 설정할게요", color = AnsimMuted, fontSize = 14.sp,
                textDecoration = TextDecoration.Underline)
        }
    }
}

@Composable
private fun PermissionLine(title: String, detail: String, required: Boolean,
                           granted: Boolean, actionLabel: String? = null,
                           onClick: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 69.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = AnsimInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(if (required) "필수" else "선택", fontSize = 11.sp,
                        color = if (required) AnsimDanger else AnsimMuted)
                }
                Text(detail, color = AnsimMuted, fontSize = 12.sp)
            }
            if (!granted && required) {
                // 시안: 미허용 필수 권한은 파란색 채움 버튼으로 강조합니다.
                Button(onClick = onClick, modifier = Modifier.heightIn(min = 38.dp),
                    shape = RoundedCornerShape(19.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AnsimGreen),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                    Text("허용하기", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else if (actionLabel != null || !granted) {
                TextButton(onClick = onClick) {
                    Text(actionLabel ?: "나중에", color = AnsimGreen, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("✓ 허용됨", color = AnsimProtected, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider(color = Color(0xFFEDF0F2))
    }
}