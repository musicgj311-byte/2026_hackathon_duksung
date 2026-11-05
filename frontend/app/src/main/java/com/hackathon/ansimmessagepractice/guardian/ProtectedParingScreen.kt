package com.hackathon.ansimmessagepractice.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimHeading
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimInfoBox
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimOutlineButton
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimPrimaryButton
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimTextLink
import com.hackathon.ansimmessagepractice.ui.onboarding.OnboardingPage
import com.hackathon.ansimmessagepractice.ui.onboarding.OnboardingStepHeader
import com.hackathon.ansimmessagepractice.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 최종 시안 7p(코드 표시)·10p(코드 만료). 실제 API 호출과 로컬 시연 코드를 구분합니다.
 * 시연 모드에서는 한 휴대폰으로 진행해야 하므로 '보호자가 입력하기를 기다리고 있어요' 줄을 누르면
 * 보호자 등록 확인 화면(9p)으로 넘어갑니다.
 */
@Composable
fun ProtectedPairingScreen(onBack: () -> Unit, onLater: () -> Unit,
                           onDemoApproval: () -> Unit = {}) {
    val context = LocalContext.current
    val demo = DemoPairing.enabled(context)
    val scope = rememberCoroutineScope()
    val protectedDeviceId = remember(context) { AnsimDeviceId.value(context, "protected") }
    var ticket by remember { mutableStateOf<PairingTicket?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val remaining = ((ticket?.expiresAtEpochMillis ?: 0L) - now).coerceAtLeast(0L) / 1000L
    val expired = ticket != null && remaining == 0L

    fun issueCode(refresh: Boolean = false) {
        if (busy) return
        if (demo) {
            ticket = DemoPairing.ticket(context, refresh)
            now = System.currentTimeMillis()
            error = ""
            return
        }
        if (!PairingIntegration.isConfigured) {
            error = "연결 서버가 설정되지 않았어요. 시연 설정을 켜면 화면을 확인할 수 있어요."
            return
        }
        busy = true
        error = ""
        ticket = null
        scope.launch {
            runCatching { PairingIntegration.backend.issueCode(protectedDeviceId) }
                .onSuccess { issued ->
                    if (issued.code.length == 6 && issued.code.all(Char::isDigit) &&
                        issued.expiresAtEpochMillis > System.currentTimeMillis()) {
                        ticket = issued
                        now = System.currentTimeMillis()
                    } else error = "서버에서 유효한 연결 코드를 받지 못했어요."
                }
                .onFailure { error = it.message ?: "코드를 발급하지 못했어요." }
            busy = false
        }
    }
    LaunchedEffect(demo) { issueCode() }
    LaunchedEffect(ticket) {
        while (ticket != null && System.currentTimeMillis() < (ticket?.expiresAtEpochMillis ?: 0L)) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
        now = System.currentTimeMillis()
    }
    OnboardingPage(bottomPadding = 24.dp, bottom = {
        if (!expired) {
            AnsimInfoBox(if (demo && DemoPairing.SHOW_DEMO_NOTICES) "시연용 연결 코드예요. 실제 기기 인증이나 가족 알림은 전송되지 않아요."
            else "코드는 10분 동안 한 번만 쓸 수 있어요. 시간이 지나면 새 코드를 받으세요.")
            Spacer(Modifier.height(24.dp))
            AnsimOutlineButton(if (busy) "코드 발급 중…" else "새 코드 받기", { issueCode(true) }, enabled = !busy)
        } else {
            AnsimPrimaryButton(if (busy) "코드 발급 중…" else "새 코드 받기", { issueCode(true) }, enabled = !busy)
        }
        Spacer(Modifier.height(15.dp))
        AnsimTextLink("나중에 연결할게요", onLater)
    }) {
        OnboardingStepHeader(4, onBack)
        Spacer(Modifier.height(22.dp))
        if (expired) AnsimHeading("코드 시간이\n지났어요",
            "10분이 지나 이 코드는 쓸 수 없어요. 새 코드를 받아 보호자에게 다시 알려 주세요.", AnsimType.LeadLong)
        else AnsimHeading("보호자에게\n이 코드를 알려 주세요", "보호자 휴대폰의 안심문자에서 입력하면 연결돼요.")
        Spacer(Modifier.height(if (expired) 25.dp else 27.dp))
        PairingCodeCard(ticket, expired, remaining, busy)
        if (ticket != null && !expired) {
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth()
                .then(if (demo) Modifier.clickable(onClick = onDemoApproval) else Modifier)
                .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), color = AnsimBlue, strokeWidth = 2.dp,
                    trackColor = Color(0xFFE6E8EC))
                Spacer(Modifier.width(9.dp))
                Text("보호자가 입력하기를 기다리고 있어요", style = AnsimType.Body, color = AnsimMuted)
            }
            if (demo && DemoPairing.SHOW_DEMO_NOTICES) AnsimTextLink(
                if (DemoPairing.hasRequest(context)) "보호자 요청 확인하기 (시연)" else "보호자 등록 확인 화면 보기 (시연)",
                onDemoApproval, color = AnsimBlue, underline = false)
        }
        if (error.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(error, style = AnsimType.Body, color = AnsimDangerText)
        }
    }
}

/** 시안 7p 회색 코드 카드: 위 26 · 라벨 · 15 · 숫자칸 56 · 14 · 남은 시간 · 아래 27 */
@Composable
private fun PairingCodeCard(ticket: PairingTicket?, expired: Boolean, remaining: Long, busy: Boolean) {
    Column(Modifier.fillMaxWidth().background(AnsimCream, RoundedCornerShape(16.dp))
        .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 27.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (expired) "만료된 코드" else "연결 코드", style = ansimStyle(15, 22, FontWeight.SemiBold),
            color = AnsimMuted)
        Spacer(Modifier.height(15.dp))
        if (ticket != null) {
            SixDigitReadOnly(ticket.code, Modifier.fillMaxWidth(), expired = expired)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (expired) {
                    AnsimIcon(AnsimIcons.Clock, AnsimDangerText, 17.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text(if (expired) "유효 시간 만료" else "${remaining / 60}분 ${remaining % 60}초 남음",
                    style = ansimStyle(15, 22, FontWeight.Bold), color = if (expired) AnsimDangerText else AnsimBlue)
            }
        } else {
            Box(Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
                Text(if (busy) "코드를 받고 있어요…" else "아직 발급된 코드가 없어요",
                    style = AnsimType.Body, color = AnsimMuted, textAlign = TextAlign.Center)
            }
        }
    }
}

/** 최종 시안 9p: 연결 요청 승인 화면. 시연 모드에서는 로컬 데이터만 저장합니다. */
@Composable
fun DemoGuardianApprovalScreen(onApprove: () -> Unit, onReject: () -> Unit) {
    OnboardingPage(bottom = {
        AnsimPrimaryButton("보호자로 등록", onApprove)
        Spacer(Modifier.height(24.dp))
        AnsimOutlineButton("모르는 사람이에요", onReject, textColor = AnsimDangerText)
    }) {
        OnboardingStepHeader(4, onReject)
        Spacer(Modifier.height(22.dp))
        AnsimHeading("이 분을\n보호자로 등록할까요?")
        Spacer(Modifier.height(26.dp))
        HorizontalDivider(color = AnsimInk, thickness = 2.dp)
        DemoApprovalRow("이름", DemoPairing.GUARDIAN_NAME.substringBefore(" ("))
        DemoApprovalRow("전화번호", "010-1234-5678")
        Spacer(Modifier.height(22.dp))
        Text("방금 알려 준 코드를 이 분이 입력했어요. 등록하면 위험 문자가 왔을 때 이 분에게 알림이 가고, 경고 화면에서 바로 전화할 수 있어요.",
            style = AnsimType.BodyLong, color = AnsimMuted)
        if (DemoPairing.SHOW_DEMO_NOTICES) Text("시연용 화면입니다. 실제 연결·전화·알림 전송은 이뤄지지 않아요.",
            style = AnsimType.Body, color = AnsimCaution, modifier = Modifier.padding(top = 13.dp))
    }
}

@Composable
private fun DemoApprovalRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(101.dp), style = AnsimType.Row, color = AnsimMuted)
        Text(value, style = ansimStyle(17, 24, FontWeight.Bold), color = AnsimInk)
    }
    HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
}

/** 최종 시안 11p 연결 완료: 체크 아이콘 · 제목 · 아이콘 3줄 · 홈으로 */
@Composable
fun GuardianConnectedScreen(onHome: () -> Unit, demo: Boolean = false) {
    val guardianName = DemoPairing.GUARDIAN_NAME.substringBefore(" (")
    val title = if (demo) "$guardianName 님과\n연결되었어요" else "가족과\n연결되었어요"
    OnboardingPage(bottom = { AnsimPrimaryButton("홈으로", onHome) }) {
        Spacer(Modifier.height(52.dp))
        AnsimIcon(AnsimIcons.CircleCheck, AnsimProtected, 44.dp)
        Spacer(Modifier.height(20.dp))
        Text(title, style = AnsimType.Title, color = AnsimInk)
        Spacer(Modifier.height(26.dp))
        HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
        listOf(
            AnsimIcons.Bell to if (demo) "위험 문자가 오면 $guardianName 님에게 알림이 전송돼요" else "위험 문자가 오면 보호자에게 알림이 전송돼요",
            AnsimIcons.Phone to "경고 화면에서 버튼 하나로 전화할 수 있어요",
            AnsimIcons.Lock to "받은 문자 내용은 보호자에게 전송되지 않아요"
        ).forEach { (icon, label) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                AnsimIcon(icon, AnsimBlue, 22.dp, Modifier.padding(top = 2.dp))
                Spacer(Modifier.width(12.dp))
                Text(label, Modifier.weight(1f), style = ansimStyle(17, 26.4), color = AnsimInk)
            }
            HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
        }
        if (demo && DemoPairing.SHOW_DEMO_NOTICES) Text("화면 시연만 완료했어요. 실제 보호자 연결 및 알림은 아직 아닙니다.",
            Modifier.padding(top = 15.dp), style = AnsimType.Body, color = AnsimCaution)
    }
}
