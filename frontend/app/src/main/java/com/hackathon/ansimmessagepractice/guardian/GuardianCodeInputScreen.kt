package com.hackathon.ansimmessagepractice.guardian

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.hackathon.ansimmessagepractice.ui.theme.*
import kotlinx.coroutines.launch

/** 보호자용 정식 코드 입력: 백엔드 POST /guardian/connect만 연결 성공으로 처리. */
@Composable
fun GuardianCodeInputScreen(onBack: () -> Unit, onConnected: () -> Unit, onLater: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE) }
    val deviceId = remember(context) { AnsimDeviceId.value(context) }
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 25.dp, vertical = 16.dp)) {
        TextButton(onClick = onBack) { Text("‹  이전", color = AnsimInk) }
        Spacer(Modifier.height(26.dp))
        Text("가족 연결 코드를\n입력해 주세요", fontSize = 28.sp, lineHeight = 37.sp,
            fontWeight = FontWeight.ExtraBold, color = AnsimInk)
        Spacer(Modifier.height(9.dp))
        Text("보호받는 가족의 휴대폰에서 받은 6자리 숫자예요.", color = AnsimMuted, fontSize = 14.sp)
        Spacer(Modifier.height(27.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("연결 코드", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AnsimInk)
            Spacer(Modifier.height(10.dp))
            SixDigitCodeField(code, { code = it; error = "" }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            Text("코드는 발급 후 10분 동안 유효하며 한 번만 사용할 수 있어요.",
                fontSize = 13.sp, lineHeight = 19.sp, color = AnsimMuted)
            if (!PairingIntegration.isConfigured) {
                Spacer(Modifier.height(18.dp))
                Text("현재 연결 서버가 설정되지 않았어요. 화면 확인은 가능하지만 실제 가족 연결은 할 수 없어요.",
                    color = AnsimCaution, fontSize = 13.sp, lineHeight = 20.sp)
            }
            if (error.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(error, color = AnsimDanger, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
        AnsimPrimaryButton(if (busy) "연결 중..." else "연결하기", onClick = {
            if (code.length != 6 || busy) return@AnsimPrimaryButton
            if (!PairingIntegration.isConfigured) {
                error = "연결 서버가 준비되지 않았어요. 서버 연동 후 다시 시도해 주세요."
                return@AnsimPrimaryButton
            }
            busy = true; error = ""
            scope.launch {
                runCatching { PairingIntegration.backend.redeemCode(deviceId, code) }
                    .onSuccess { link ->
                        // 백엔드가 반환한 보호자 기기 ID와 실제 앱 기기 ID 일치 여부 확인
                        if (link.guardianDeviceId != deviceId || link.protectedDeviceId.isBlank()) {
                            error = "연결 정보를 확인할 수 없어요. 다시 시도해 주세요."
                        } else {
                            prefs.edit().putString("guardian_link_protected_id", link.protectedDeviceId)
                                .putString("guardian_link_guardian_id", link.guardianDeviceId)
                                .putBoolean("guardian_link_confirmed", true).apply()
                            onConnected()
                        }
                    }
                    .onFailure { error = it.message ?: "코드가 잘못되었거나 만료되었어요. 새 코드를 받아 주세요." }
                busy = false
            }
        }, enabled = code.length == 6 && !busy)
        TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) {
            Text("나중에 연결할게요", color = AnsimMuted)
        }
    }
}
