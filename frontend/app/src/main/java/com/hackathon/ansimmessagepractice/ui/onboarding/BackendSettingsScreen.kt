package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.data.api.BackendApi
import com.hackathon.ansimmessagepractice.data.api.BackendConfig
import com.hackathon.ansimmessagepractice.data.api.UserSync
import com.hackathon.ansimmessagepractice.guardian.DemoPairing
import com.hackathon.ansimmessagepractice.ui.theme.AnsimInk
import com.hackathon.ansimmessagepractice.ui.theme.AnsimMuted
import kotlinx.coroutines.launch

/** 일반 설정에서 숨긴 개발자 진단 화면. 서버 URL은 BackendConfig에서만 변경합니다. */
@Composable
fun BackendSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val baseUrl = BackendConfig.get(context)
    var status by remember { mutableStateOf("서버 연결 상태를 확인할 수 있어요.") }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var demoMode by remember { mutableStateOf(DemoPairing.enabled(context)) }
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
        .verticalScroll(rememberScrollState()).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text("‹  설정으로", color = AnsimInk) }
        Text("개발자 도구 · 서버 연결", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = AnsimInk)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("연결 화면 시연 모드", fontWeight = FontWeight.Bold, color = AnsimInk)
                Text("켜면 실제 서버 대신 로컬 연결 시연만 합니다. 실제 연결·알림은 발생하지 않아요.",
                    fontSize = 12.sp, color = AnsimMuted)
            }
            Switch(checked = demoMode, onCheckedChange = {
                demoMode = it; DemoPairing.setEnabled(context, it)
            })
        }
        Text("서버 주소는 data/api/BackendApi.kt의 BackendConfig.SERVER_URL에서 설정해요.",
            fontSize = 14.sp, color = AnsimMuted)
        Text(if (baseUrl.isBlank()) "현재 서버 주소: 미설정 (로컬 경고 시연 유지)"
        else "현재 서버 주소: $baseUrl", fontSize = 13.sp, color = AnsimInk)
        Text("에뮬레이터에서 PC 로컬 서버에 접속할 때는 http://10.0.2.2:8000을 사용할 수 있어요. " +
                "이 경우 디버그 빌드에 HTTP 허용 설정이 필요할 수 있어요.",
            color = AnsimMuted, fontSize = 12.sp)
        OutlinedButton(onClick = {
            running = true
            scope.launch {
                status = runCatching {
                    val health = BackendApi.health(context)
                    val prefs = context.getSharedPreferences("ansim_settings", android.content.Context.MODE_PRIVATE)
                    if (prefs.getBoolean("onboarded", false)) {
                        UserSync.ensureCurrent(context)
                    }
                    "연결 성공: ${health.optString("status")} / fake_judge=${health.optBoolean("fake_judge")}"
                }.getOrElse { "연결 실패: ${it.message ?: "네트워크 오류"}" }
                running = false
            }
        }, enabled = !demoMode && !running && BackendConfig.isConfigured(context), modifier = Modifier.fillMaxWidth()) {
            Text(if (running) "확인 중..." else "연결 테스트 (/health + 사용자 등록)")
        }
        Text(if (demoMode) "시연 모드에서는 연결 테스트를 사용하지 않아요." else status,
            fontSize = 14.sp, color = AnsimInk)
        Spacer(Modifier.height(12.dp))
        Text("개발용 진단 화면입니다. 운영 환경에서는 HTTPS와 사용자 인증이 필요합니다.",
            fontSize = 12.sp, color = AnsimMuted)
    }
}