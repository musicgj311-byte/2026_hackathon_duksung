package com.hackathon.ansimmessagepractice.guardian

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimHeading
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimInfoBox
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimPrimaryButton
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimTextLink
import com.hackathon.ansimmessagepractice.ui.onboarding.OnboardingEyebrow
import com.hackathon.ansimmessagepractice.ui.onboarding.OnboardingPage
import com.hackathon.ansimmessagepractice.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 최종 시안 8p: 6자리 코드 입력. 시연일 때만 서버 요청 대신 로컬 시뮬레이션을 합니다.
 * showLater=false(홈에서 '가족 추가하기'로 들어온 경우)면 시안처럼 '연결 요청' 버튼만 표시합니다.
 */
@Composable
fun GuardianCodeInputScreen(onBack: () -> Unit, onConnected: () -> Unit, onLater: () -> Unit,
                            onDemoRequest: () -> Unit = {}, showLater: Boolean = true) {
    val context = LocalContext.current
    val demo = DemoPairing.enabled(context)
    val prefs = remember(context) { context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE) }
    val deviceId = remember(context) { AnsimDeviceId.value(context, "guardian") }
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    fun submit() {
        if (busy) return
        if (code.length != 6) {
            error = "6자리 숫자를 모두 입력해 주세요."
            return
        }
        if (demo) {
            if (DemoPairing.request(context, code)) onDemoRequest()
            else error = "코드가 틀렸거나 10분이 지났어요. 가족 휴대폰의 코드를 다시 확인해 주세요."
            return
        }
        if (!PairingIntegration.isConfigured) {
            error = "연결 서버가 준비되지 않았어요. 시연 모드를 사용하거나 서버 설정을 확인해 주세요."
            return
        }
        busy = true
        error = ""
        scope.launch {
            runCatching { PairingIntegration.backend.redeemCode(deviceId, code) }
                .onSuccess { link ->
                    if (link.guardianDeviceId != deviceId || link.protectedDeviceId.isBlank()) {
                        error = "연결 정보를 확인할 수 없어요. 다시 시도해 주세요."
                    } else {
                        prefs.edit().putString("guardian_link_protected_id", link.protectedDeviceId)
                            .putString("guardian_link_guardian_id", link.guardianDeviceId)
                            .putBoolean("guardian_link_confirmed", true).apply()
                        onConnected()
                    }
                }.onFailure { error = it.message ?: "코드가 잘못되었거나 만료되었어요." }
            busy = false
        }
    }

    OnboardingPage(bottomPadding = if (showLater) 16.dp else 32.dp, bottom = {
        AnsimPrimaryButton(if (busy) "연결 중…" else "연결 요청", { submit() }, enabled = !busy)
        if (showLater) {
            Spacer(Modifier.height(15.dp))
            AnsimTextLink("나중에 연결할게요", onLater)
        }
    }) {
        OnboardingEyebrow("보호자 · 가족 연결")
        Spacer(Modifier.height(21.dp))
        AnsimHeading("가족 휴대폰에 보이는\n코드를 입력해 주세요", "가족의 안심문자 앱에 나온 6자리 숫자예요.")
        Spacer(Modifier.height(25.dp))
        Text("연결 코드", style = ansimStyle(15, 22, FontWeight.SemiBold), color = AnsimMuted)
        Spacer(Modifier.height(7.dp))
        SixDigitCodeField(code, { code = it; error = "" }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        AnsimInfoBox("입력하면 가족 휴대폰에 확인 화면이 떠요. 가족이 등록을 눌러야 연결돼요.")
        if (demo && DemoPairing.SHOW_DEMO_NOTICES) Text("시연 코드: ${DemoPairing.CODE} · 서버 인증은 진행하지 않아요.",
            Modifier.padding(top = 12.dp), style = AnsimType.Body, color = AnsimCaution)
        if (error.isNotBlank()) Text(error, Modifier.padding(top = 14.dp),
            style = AnsimType.Body, color = AnsimDangerText)
    }
}

/** 로컬 데모는 기기 간 자동 전달이 불가하므로 승인 화면으로 넘어가는 시연 버튼을 제공합니다. */
@Composable
fun DemoGuardianWaitingScreen(onShowApproval: () -> Unit, onLater: () -> Unit) {
    OnboardingPage(bottomPadding = 16.dp, bottom = {
        AnsimPrimaryButton("가족 승인 화면 보기", onShowApproval)
        Spacer(Modifier.height(15.dp))
        AnsimTextLink("나중에 연결할게요", onLater)
    }) {
        OnboardingEyebrow("보호자 · 가족 연결")
        Spacer(Modifier.height(21.dp))
        AnsimHeading("가족의 확인을\n기다리고 있어요", "가족 휴대폰에서 보호자 등록을 눌러야 연결돼요.")
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(16.dp), color = AnsimBlue, strokeWidth = 2.dp,
                trackColor = Color(0xFFE6E8EC))
            Spacer(Modifier.width(9.dp))
            Text("가족이 등록하기를 기다리고 있어요", style = AnsimType.Body, color = AnsimMuted)
        }
        if (DemoPairing.SHOW_DEMO_NOTICES) Text("서버 없이 두 역할을 한 휴대폰에서 보여주는 시연입니다. 실제 가족에게 요청이 전달되지는 않아요.",
            Modifier.padding(top = 20.dp), style = AnsimType.Body, color = AnsimCaution)
    }
}
