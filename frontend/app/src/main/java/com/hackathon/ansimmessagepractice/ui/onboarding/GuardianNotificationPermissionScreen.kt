package com.hackathon.ansimmessagepractice.ui.onboarding

import android.Manifest
import android.os.Build
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.theme.*

/** 보호자는 문자 읽기·오버레이 권한 없이 알림 '받기' 권한만 요청합니다. */
@Composable
fun GuardianNotificationPermissionScreen(onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(guardianCanReceiveNotifications(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = guardianCanReceiveNotifications(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        enabled = guardianCanReceiveNotifications(context)
    }
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 25.dp, vertical = 17.dp)) {
        TextButton(onClick = onBack) { Text("‹  이전", color = AnsimInk) }
        Spacer(Modifier.height(27.dp))
        Text("가족의 위험 알림을\n받아 주세요", color = AnsimInk, fontSize = 28.sp,
            lineHeight = 37.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text("가족에게 위험 문자가 오면 내 휴대폰으로 알려드려요.", color = AnsimMuted,
            fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(26.dp))
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("알림 받기", color = AnsimInk, fontWeight = FontWeight.Bold)
                Text(if (enabled) "✓ 허용됨" else "설정 필요",
                    color = if (enabled) AnsimProtected else AnsimCaution)
            }
        }
        Spacer(Modifier.weight(1f))
        AnsimPrimaryButton(if (enabled) "다음" else "알림 허용하기", onClick = {
            if (enabled) {
                onNext()
            } else if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                runCatching { context.startActivity(intent) }
            }
        })
        if (!enabled) TextButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("나중에 설정할게요", color = AnsimMuted)
        }
    }
}

private fun guardianCanReceiveNotifications(context: Context): Boolean {
    val runtimeGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
}
