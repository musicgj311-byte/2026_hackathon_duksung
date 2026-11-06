package com.hackathon.ansimmessagepractice.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.hackathon.ansimmessagepractice.ui.theme.*

/**
 * 최종 시안 5~6p. 시안에는 필수 2개로 그려져 있지만 실제로 필요한 필수 권한은 4개입니다.
 *  필수: 알림 읽기 · 다른 앱 위에 표시 · 경고 알림 표시(POST_NOTIFICATIONS) · 연락처 확인
 *  선택: 배터리 최적화 제외
 *  연락처 권한이 없으면 발신자가 저장된 번호인지 알 수 없어, 모르는 번호의 문자를 서버 검사로 보내지 못합니다.
 *  그래서 연락처도 필수로 두고, 4개가 모두 허용되어야 완료 화면(6p)으로 바뀝니다.
 */
@Composable
fun PermissionScreen(onNextClick: () -> Unit, onBackClick: () -> Unit,
                     fromSettings: Boolean = false, onSkipClick: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notification by remember { mutableStateOf(PermissionStatus.notificationAccess(context)) }
    var overlay by remember { mutableStateOf(PermissionStatus.overlay(context)) }
    var postNotifications by remember { mutableStateOf(PermissionStatus.postNotifications(context)) }
    var contacts by remember { mutableStateOf(PermissionStatus.contacts(context)) }
    var battery by remember { mutableStateOf(PermissionStatus.batteryExempt(context)) }
    val postPermissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        postNotifications = PermissionStatus.postNotifications(context)
        if (!granted) LockScreenSettings.open(context)
    }
    // 한 번 거부한 뒤 다시 누르면 시스템 창이 뜨지 않을 수 있어, 두 번째부터는 앱 설정 화면을 엽니다.
    var contactsAsked by remember { mutableStateOf(false) }
    val contactsRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        contacts = PermissionStatus.contacts(context)
        contactsAsked = true
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notification = PermissionStatus.notificationAccess(context)
                overlay = PermissionStatus.overlay(context)
                postNotifications = PermissionStatus.postNotifications(context)
                contacts = PermissionStatus.contacts(context)
                battery = PermissionStatus.batteryExempt(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun open(intent: Intent) {
        runCatching { context.startActivity(intent) }.onFailure {
            android.widget.Toast.makeText(context, "휴대폰 설정에서 직접 권한을 허용해 주세요.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    fun requestPost() {
        val denied = Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (denied) postPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        else LockScreenSettings.open(context)
    }
    fun requestContacts() {
        if (contacts) return
        if (!contactsAsked) contactsRequest.launch(Manifest.permission.READ_CONTACTS)
        else runCatching {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")))
        }.onFailure {
            android.widget.Toast.makeText(context, "휴대폰 설정 > 애플리케이션 > 모두안심 > 권한에서 연락처를 허용해 주세요.",
                android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val ready = notification && overlay && postNotifications && contacts

    if (fromSettings) {
        // 설정 > 권한: 시안 단계 표시 대신 상단 바를 쓰고, 모든 권한 상태를 한 목록에서 보여줍니다.
        Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
            AnsimTopBar("권한", onBackClick)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(if (ready) "필수 권한이 모두 허용되었어요" else "꺼진 필수 권한이 있어요",
                    style = ansimStyle(20, 28, FontWeight.Bold), color = if (ready) AnsimInk else AnsimDangerText)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
                PermissionLine("알림 읽기", "문자·카톡이 오면 바로 확인해요", true, notification) {
                    open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                PermissionLine("다른 앱 위에 표시", "다른 앱을 쓰는 중에도 경고를 띄워요", true, overlay) {
                    open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                }
                PermissionLine("경고 알림 표시", "잠금 화면에서도 위험을 알려드려요", true, postNotifications) { requestPost() }
                PermissionLine("연락처 확인", "모르는 번호의 문자를 골라 검사해요", true, contacts) { requestContacts() }
                PermissionLine("배터리 최적화 제외", "앱이 꺼지지 않게 해요", false, battery) {
                    open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
            Box(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 12.dp)) {
                AnsimPrimaryButton("설정으로 돌아가기", onNextClick)
            }
        }
        return
    }

    if (ready) {
        // 완료 화면
        OnboardingPage(bottom = { AnsimPrimaryButton("다음", onNextClick) }) {
            OnboardingStepHeader(3, onBackClick)
            Spacer(Modifier.height(47.dp))
            AnsimIcon(AnsimIcons.CircleCheck, AnsimProtected, 44.dp)
            Spacer(Modifier.height(22.dp))
            Text("필수 권한이\n모두 허용되었어요", style = AnsimType.Title, color = AnsimInk)
            Spacer(Modifier.height(26.dp))
            HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
            listOf("알림 읽기", "다른 앱 위에 표시", "경고 알림 표시", "연락처 확인").forEach { title ->
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, style = AnsimType.Row, color = AnsimInk)
                    Text("허용됨", style = ansimStyle(17, 24, FontWeight.Bold), color = AnsimProtected)
                }
                HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
            }
            // 선택 권한(배터리)은 완료 화면에서도 계속 허용할 수 있게 남겨 둡니다.
            if (!battery) PermissionLine("배터리 최적화 제외", "앱이 꺼지지 않게 해요", false, battery) {
                open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
            Spacer(Modifier.height(23.dp))
            Text("배터리 설정은 선택 사항이에요. 설정 메뉴에서 언제든 바꿀 수 있어요.",
                style = AnsimType.BodyLong, color = AnsimMuted)
        }
        return
    }

    // 권한 요청 화면
    OnboardingPage(bottomPadding = 16.dp, bottom = {
        Text("필수 권한을 모두 허용하면 다음으로 넘어가요", Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center, style = AnsimType.Body, color = AnsimMuted)
        Spacer(Modifier.height(23.dp))
        AnsimPrimaryButton("다음", onNextClick, enabled = false)
        Spacer(Modifier.height(15.dp))
        AnsimTextLink("나중에 설정할게요", onSkipClick)
    }) {
        OnboardingStepHeader(3, onBackClick)
        Spacer(Modifier.height(22.dp))
        AnsimHeading("필수 권한 4가지를\n허용해 주세요", "문자를 확인하고 경고를 띄우는 데 필요해요.")
        Spacer(Modifier.height(27.dp))
        HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
        PermissionLine("알림 읽기", "문자·카톡이 오면 바로 확인해요", true, notification) {
            open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        PermissionLine("다른 앱 위에 표시", "다른 앱을 쓰는 중에도 경고를 띄워요", true, overlay) {
            open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
        }
        PermissionLine("경고 알림 표시", "잠금 화면에서도 위험을 알려드려요", true, postNotifications) { requestPost() }
        PermissionLine("연락처 확인", "모르는 번호의 문자를 골라 검사해요", true, contacts) { requestContacts() }
        PermissionLine("배터리 최적화 제외", "앱이 꺼지지 않게 해요", false, battery) {
            open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionLine(title: String, detail: String, required: Boolean,
                           granted: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 82.dp).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = ansimStyle(17, 24, FontWeight.Bold), color = AnsimInk)
                Spacer(Modifier.width(6.dp))
                Text(if (required) "필수" else "선택", style = ansimStyle(15, 22, FontWeight.SemiBold),
                    color = if (required) AnsimDangerText else AnsimSubtle)
            }
            Text(detail, style = AnsimType.Body, color = AnsimMuted)
        }
        Spacer(Modifier.width(10.dp))
        when {
            granted -> Row(verticalAlignment = Alignment.CenterVertically) {
                AnsimIcon(AnsimIcons.Check, AnsimProtected, 17.dp)
                Spacer(Modifier.width(5.dp))
                Text("허용됨", style = ansimStyle(15, 22, FontWeight.Bold), color = AnsimProtected)
            }
            required -> Box(Modifier.heightIn(min = 42.dp).background(AnsimBlue, CircleShape)
                .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center) {
                Text("허용하기", style = ansimStyle(15, 22, FontWeight.Bold), color = Color.White)
            }
            // 선택 권한은 시안처럼 파란 밑줄 '나중에'로 표시하고, 누르면 해당 설정을 엽니다.
            else -> Text("나중에", Modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp),
                style = ansimStyle(15, 22, FontWeight.SemiBold), color = AnsimBlue,
                textDecoration = TextDecoration.Underline)
        }
    }
    HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
}
