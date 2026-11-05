package com.hackathon.ansimmessagepractice.ui.warning

import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import com.hackathon.ansimmessagepractice.guardian.DemoPairing
import com.hackathon.ansimmessagepractice.guardian.GuardianCall
import com.hackathon.ansimmessagepractice.guardian.rememberGuardianCallAction
import com.hackathon.ansimmessagepractice.ui.theme.*
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimBottomSheet
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimOutlineButton
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimPrimaryButton
import kotlinx.coroutines.delay

/**
 * 위험도와 접근성 모드에 따라 경고를 구성합니다.
 * 경고 문구는 JudgeResponse의 필드 그대로 사용하고, 버튼 동작은 별도로 결정합니다.
 * FCM 전송 성공은 /judge 응답만으로 추정하지 않습니다.
 */
@Composable
fun WarningScreen(
    riskLevel: String, selectedType: String, selectedLanguage: String = "한국어",
    onCloseClick: () -> Unit, receivedMessage: String? = null,
    senderLabel: String? = null, analysisReason: String? = null,
    guardianNotified: Boolean = false, payload: WarningPayload? = null,
    onManualNumberClick: () -> Unit = {}, onRegisterGuardianClick: () -> Unit = {},
    onRetryClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val callGuardian = rememberGuardianCallAction()
    val rootView = LocalView.current
    val preview = remember(riskLevel, selectedLanguage) {
        PreviewJudgeResults.forLevel(riskLevel, localizedCode(selectedLanguage))
    }
    val isMockPreview = payload == null
    val data = payload ?: preview.copy(
        receivedMessage = receivedMessage ?: preview.receivedMessage,
        sender = senderLabel ?: preview.sender,
        explanation = analysisReason?.takeIf { it.isNotBlank() } ?: preview.explanation
    )
    val danger = data.level == "danger"
    val caution = data.level == "caution"
    val pending = data.level == "timeout" || data.level == "checking"
    val languageCode = data.language.lowercase().substringBefore('-')
    val voiceMode = selectedType == "음성 안내 중심"
    val screenReader = spokenAccessibilityEnabled(context)
    val isKakao = data.sourceApp == "kakao"
    val simpleMode = selectedType == "쉬운 안내" && !pending
    val hearingMode = selectedType == "화면 안내 중심" && !pending
    // 확인 지연(timeout)도 사용자가 고른 알림 방식에 맞는 화면·소리·진동으로 안내합니다.
    val timeout = data.level == "timeout"
    val hearingType = selectedType == "화면 안내 중심"
    val hasPhone = GuardianCall.validNumber(GuardianCall.savedNumber(context))
    val savedName = GuardianCall.savedName(context).ifBlank { "가족" }
    val savedNumber = GuardianCall.savedNumber(context)
    var showNoPhoneDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showBlockHelp by remember { mutableStateOf(false) }
    var showSmsNumberDialog by remember { mutableStateOf(false) }
    var smsManualNumber by remember { mutableStateOf("") }

    // 실제 SMS 전송은 하지 않습니다. 기기에 저장된 신뢰할 수 있는 번호로
    // 기본 문자 앱의 작성 화면만 엽니다. 사기 문자 원문이나 발신 번호는 사용하지 않습니다.
    fun openFamilySms(number: String) {
        if (!GuardianCall.validNumber(number)) {
            Toast.makeText(context, "가족 전화번호를 다시 확인해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        val safeNumber = GuardianCall.cleanNumber(number)
        val message = "의심스러운 문자를 받았어요. 안전한지 확인하려고 연락했어요. 전화 부탁드려요."
        val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", safeNumber, null)).apply {
            putExtra("sms_body", message)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "문자 앱을 찾을 수 없어요. 전화로 확인해 주세요.", Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(context, "문자 앱을 열지 못했어요. 전화로 확인해 주세요.", Toast.LENGTH_LONG).show()
        }
    }
    val sms: () -> Unit = {
        if (hasPhone) openFamilySms(savedNumber)
        else { smsManualNumber = ""; showSmsNumberDialog = true }
    }
    val call: () -> Unit = {
        if (hasPhone) callGuardian() else showNoPhoneDialog = true
    }
    // 시안처럼 관계(딸·아들 등)로 부릅니다. 관계가 없으면 이름, 그것도 없으면 '가족'.
    val savedRelation = context.getSharedPreferences("ansim_settings", android.content.Context.MODE_PRIVATE)
        .getString("guardian_relationship", "").orEmpty().trim()
    val demoLinked = DemoPairing.approved(context)
    val who = when {
        hasPhone && savedRelation.isNotBlank() -> savedRelation
        hasPhone -> savedName
        demoLinked -> "딸"
        else -> "가족"
    }
    val guardianName = when {
        hasPhone && GuardianCall.savedName(context).isNotBlank() -> GuardianCall.savedName(context)
        demoLinked || isMockPreview -> DemoPairing.GUARDIAN_NAME.substringBefore(" (")
        else -> "보호자"
    }
    val callLabel = when (languageCode) {
        "vi" -> "Gọi cho gia đình"
        "en" -> "Call your family"
        "zh" -> "给家人打电话"
        else -> if (caution) "가족에게 물어보기" else "${who}에게 전화하기"
    }
    val closeLabel = when (languageCode) {
        "vi" -> "Đóng · 닫기"
        "en" -> "Close · 닫기"
        "zh" -> "关闭 · 닫기"
        else -> "닫기"
    }
    val internationalCallLabel = if (languageCode == "ko") "☎  $callLabel"
    else "☎  $callLabel\n가족에게 전화하기"
    val headerColor = when (data.level) {
        "danger" -> AnsimDanger
        "caution", "timeout" -> Color(0xFFFFF4E5)
        else -> Color(0xFFF4F6FA)
    }
    val accentColor = when (data.level) {
        "danger" -> AnsimDanger
        "caution", "timeout" -> AnsimCaution
        else -> AnsimGreen
    }

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember(context) {
        TextToSpeech(context.applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }
    DisposableEffect(tts) { onDispose { tts.stop(); tts.shutdown() } }
    val linkType = isLinkScam(data.scamType, danger)
    val foreign = foreignCopy(languageCode)
    // 음성 모드 화면(31p)의 '읽어 드리는 내용'과 실제 소리가 같도록 한 곳에서 만듭니다.
    val voiceQuote = if (isMockPreview && danger && data.scamType == "family_impersonation" && languageCode == "ko")
        "가족인 척 돈을 보내라고 해요. 원래 번호로 직접 전화해서 확인하세요."
    else listOf(data.explanation, data.action).filter(String::isNotBlank).joinToString(" ")
    val timeoutQuote = if (foreign != null) "${foreign.timeoutWarn} ${foreign.callToCheck}."
    else "확인되기 전까지 링크를 누르거나 송금하지 마세요. 가족에게 전화해서 확인하세요."
    val spokenText = sanitizedSpeech(when {
        timeout -> "${foreign?.timeoutTitle?.replace("\n", " ") ?: "확인을 완료하지 못했어요"}. $timeoutQuote"
        simpleMode -> data.easySentence
        voiceMode && (danger || caution) && languageCode == "ko" ->
            "${koreanSummary(danger, linkType)} $voiceQuote"
        else -> "${data.title.replace("\n", " ")}. ${data.explanation}. ${data.action}"
    })
    fun speak() {
        if (screenReader) {
            rootView.announceForAccessibility(spokenText)
            return
        }
        if (!ttsReady || spokenText.isBlank()) return
        if (tts.setLanguage(speechLocale(languageCode)) >= TextToSpeech.LANG_AVAILABLE) {
            tts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "ansim_warning")
        }
    }
    LaunchedEffect(ttsReady, spokenText, voiceMode, pending, screenReader) {
        // Only the voice-oriented mode plays automatically; never compete with TalkBack.
        // 확인 지연도 음성 안내 모드에서는 자동으로 읽어 줍니다(확인 중 단계만 제외).
        if (ttsReady && voiceMode && data.level != "checking" && !screenReader) speak()
    }
    fun openBlockSettings() {
        val started = runCatching {
            // canCurrentUserBlockNumbers()는 직접 차단 권한 여부 검사이므로
            // 단순히 시스템 차단 관리 화면으로 보내는 앱에서는 사용하지 않습니다.
            val telecom = context.getSystemService(android.content.Context.TELECOM_SERVICE) as TelecomManager
            context.startActivity(telecom.createManageBlockedNumbersIntent())
        }.isSuccess
        if (!started) showBlockHelp = true
    }
    LaunchedEffect(data.level, selectedType) {
        if (danger || hearingType || selectedType == "음성 안내 중심") {
            vibrateSafely(context, danger)
        }
    }

    // 주의 모드까지 전부 빨간 화면으로 처리하던 문제 수정.
    var flashVisible by remember { mutableStateOf(false) }
    LaunchedEffect(hearingType, data.level) {
        flashVisible = false
        if (hearingType && (danger || caution || timeout)) {
            // 광과민성 고려: 강한 명암 전환 없이 약한 배경 강조를 10회 반복합니다.
            // 화면을 벗어나면 LaunchedEffect가 취소되므로 점멸도 중단됩니다.
            repeat(10) {
                flashVisible = true
                delay(350)
                flashVisible = false
                delay(850)
            }
        }
    }

    if (showBlockHelp) {
        AlertDialog(onDismissRequest = { showBlockHelp = false },
            title = { Text("번호 차단 방법") },
            text = { Text("휴대폰의 기본 전화 앱에서 설정 → 차단 번호 관리로 이동해 직접 등록해 주세요. 이 앱이 번호를 자동으로 차단하지는 않아요.") },
            confirmButton = { TextButton(onClick = { showBlockHelp = false }) { Text("확인") } })
    }
    if (showReportDialog) {
        AlertDialog(onDismissRequest = { showReportDialog = false },
            title = { Text("사기 의심 신고 안내") },
            text = { Text(if (isKakao)
                "카카오톡 대화방의 신고 메뉴에서 신고할 수 있어요. 인터넷 사기 상담은 118로 문의할 수 있어요."
            else "스미싱 신고 및 상담은 국번 없이 118로 문의하세요. 원본 문자는 신고 전에 지우지 마세요.") },
            confirmButton = {
                TextButton(onClick = {
                    showReportDialog = false
                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:118"))) }
                        .onFailure { Toast.makeText(context, "전화 앱을 열 수 없어요.", Toast.LENGTH_LONG).show() }
                }) { Text("118 전화 앱 열기") }
            }, dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("닫기") } })
    }
    if (showSmsNumberDialog) {
        Dialog(onDismissRequest = { showSmsNumberDialog = false }) {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("가족에게 문자 보내기", color = AnsimInk,
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("믿을 수 있는 가족 번호를 입력해 주세요. 의심 문자에 적힌 번호로는 보내지 마세요.",
                        color = AnsimMuted, fontSize = 14.sp, lineHeight = 21.sp)
                    OutlinedTextField(
                        value = smsManualNumber,
                        onValueChange = { smsManualNumber = it },
                        label = { Text("가족 전화번호") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            showSmsNumberDialog = false
                            openFamilySms(smsManualNumber)
                        },
                        enabled = GuardianCall.validNumber(smsManualNumber),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AnsimGreen)
                    ) { Text("문자 앱 열기") }
                    TextButton(
                        onClick = { showSmsNumberDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("취소", color = AnsimMuted) }
                    Text("문자는 자동으로 전송되지 않아요. 내용을 확인한 뒤 직접 보내 주세요.",
                        color = AnsimMuted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        if (simpleMode) {
            SimpleWarningPage(
                level = data.level,
                sentence = data.easySentence,
                callLabel = "${who}에게 전화",
                call = call,
                onClose = onCloseClick,
                link = linkType, foreign = foreign
            )
        } else if (hearingMode) {
            HearingVisualWarningPage(
                title = data.title,
                explanation = data.explanation,
                action = data.action,
                danger = danger,
                onSms = sms,
                onCall = call,
                onClose = onCloseClick,
                smsLabel = "${who}에게 문자보내기",
                link = linkType, foreign = foreign
            )
        } else if (timeout && selectedType == "쉬운 안내") {
            SimpleTimeoutPage(onRetry = onRetryClick, call = call, onClose = onCloseClick,
                callLabel = "${who}에게 전화", foreign = foreign)
        } else if (timeout && hearingType) {
            HearingTimeoutPage(onRetry = onRetryClick, onSms = sms, onClose = onCloseClick,
                smsLabel = "${who}에게 문자보내기", foreign = foreign)
        } else if (timeout && voiceMode) {
            VoiceTimeoutPage(quote = timeoutQuote, onRetry = onRetryClick, onCall = call,
                onReplay = { speak() }, replayEnabled = ttsReady || screenReader, onClose = onCloseClick,
                callLabel = "${who}에게 전화해서 확인", foreign = foreign)
        } else if (timeout) {
            TimeoutWarningPage(
                onRetry = onRetryClick, onCall = call, onClose = onCloseClick,
                callLabel = "${who}에게 전화해서 확인", foreign = foreign
            )
        } else if (foreign != null && (danger || caution)) {
            // 외국어: 시안 32p 이중언어 화면 (위험·주의 모두)
            BilingualWarningPage(data, isMockPreview, foreign, isKakao, onCall = call, onClose = onCloseClick)
        } else if (isKakao && danger) {
            KakaoWarningPage(
                data = data, preview = isMockPreview, callLabel = "${who}에게 직접 전화하기",
                onCall = call, onReport = { showReportDialog = true },
                onReplay = { speak() }, replayEnabled = ttsReady || screenReader,
                onClose = onCloseClick, guardianName = guardianName
            )
        } else if (voiceMode && (danger || caution)) {
            VoiceGuidedWarningPage(
                data = data, callLabel = if (danger) "${who}에게 직접 전화하기" else "가족에게 물어보기", onCall = call,
                onReplay = { speak() }, replayEnabled = ttsReady || screenReader, quote = voiceQuote
            )
        } else if (danger || caution) {
            FullColorWarningPage(
                data = data, preview = isMockPreview, callLabel = callLabel,
                isKakao = isKakao, onCall = call, onBlock = { openBlockSettings() },
                onReport = { showReportDialog = true }, onReplay = { speak() },
                replayEnabled = ttsReady || screenReader, onClose = onCloseClick,
                guardianName = guardianName
            )
        } else {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                if (voiceMode && !pending) {
                    Text(if (screenReader) "🔊  화면 읽기 기능으로 안내해요" else "🔊  소리로 읽어 드리고 있어요",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AnsimInk)
                }
                Column(
                    Modifier.fillMaxWidth().background(headerColor)
                        .padding(horizontal = 20.dp, vertical = if (hearingMode) 22.dp else 19.dp)
                ) {
                    Text(
                        when (data.level) {
                            "danger" -> when (languageCode) {
                                "vi" -> "⚠ Nguy hiểm · ${scamLabel(data.scamType)} / 위험"
                                "en" -> "⚠ Danger · Scam warning"
                                "zh" -> "⚠ 危险 · 诈骗提醒"
                                else -> "⚠ 위험 · ${scamLabel(data.scamType)}${if (isKakao) " · 카카오톡" else ""}"
                            }
                            "caution" -> if (languageCode != "ko") when (languageCode) {
                                "vi" -> "⚠ Chú ý · 주의"
                                "en" -> "⚠ Caution · 주의"
                                else -> "⚠ 注意 · 주의"
                            } else if (data.scamType == "delivery")
                                "⚠ 주의 · 택배 사칭 의심" else "⚠ 주의 · ${scamLabel(data.scamType)}"
                            "timeout" -> "ⓘ 확인 지연"
                            else -> "◌ 문자 확인 중"
                        },
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = if (danger) Color.White else accentColor
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        data.title, fontSize = if (hearingMode) 35.sp else 29.sp,
                        lineHeight = if (hearingMode) 43.sp else 37.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (danger) Color.White else AnsimInk,
                        modifier = Modifier.semantics { contentDescription = sanitizedSpeech("${data.title}. ${data.explanation}. ${data.action}"); liveRegion = LiveRegionMode.Assertive }
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(data.explanation, fontSize = if (hearingMode) 18.sp else 15.sp,
                        lineHeight = if (hearingMode) 26.sp else 22.sp,
                        color = if (danger) Color.White.copy(alpha = .94f) else AnsimMuted)
                    if (languageCode == "vi" && (danger || caution)) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (danger) "사기 문자예요. 돈을 보내지 마세요."
                            else "사기일 수 있어요. 링크를 누르지 마세요.",
                            fontSize = 13.sp,
                            color = if (danger) Color.White else AnsimInk
                        )
                    }
                }
                // 주의/확인 지연 경고의 헤더 하단에 황색 구분선.
                if (caution || data.level == "timeout") {
                    HorizontalDivider(color = AnsimCaution.copy(alpha = .75f), thickness = 1.dp)
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 17.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (selectedType == "음성 안내 중심" && !pending) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                            Box(Modifier.width(3.dp).height(90.dp).background(AnsimGreen))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("읽어 드리는 내용", color = AnsimMuted, fontSize = 12.sp)
                                Text("“${data.explanation} ${data.action}”", color = AnsimInk,
                                    fontSize = 17.sp, lineHeight = 26.sp)
                            }
                        }
                        Text("문자 속 링크와 전화번호는 읽지 않아요.", color = AnsimMuted, fontSize = 12.sp)
                    } else {
                        if (!data.receivedMessage.isNullOrBlank() && !hearingMode) {
                            Surface(color = Color(0xFFF4F5F7), shape = RoundedCornerShape(10.dp)) {
                                Column(Modifier.fillMaxWidth().padding(13.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("${if (isKakao) "받은 카카오톡" else "받은 문자"} · ${data.sender ?: "알 수 없는 발신자"}",
                                        color = AnsimMuted, fontSize = 12.sp)
                                    Text(data.receivedMessage.orEmpty(), color = AnsimInk,
                                        fontSize = 15.sp, lineHeight = 22.sp)
                                }
                            }
                        }
                        // 받은 문자 바로 아래에 보호자 알림 상태를 표시합니다.
                        // 미리보기에서만 표시하는 시연 상태는 실제 알림 발송으로 취급하지 않습니다.
                        if (danger && !hearingMode) {
                            val status = data.guardianNotificationStatus
                            when {
                                status == "success" -> {
                                    Text("✓ 보호자에게 알림을 보냈어요",
                                        color = AnsimProtected, fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold)
                                }
                                status == "failed" -> {
                                    Text("보호자에게 알림을 보내지 못했어요. 직접 연락해 주세요.",
                                        color = AnsimDanger, fontSize = 13.sp,
                                        lineHeight = 19.sp)
                                }
                                isMockPreview -> {
                                    Text("시연 화면 · 실제 보호자 알림은 전송되지 않았어요.",
                                        color = AnsimMuted, fontSize = 12.sp, lineHeight = 18.sp)
                                }
                                else -> {
                                    Text("보호자 알림 전송 상태를 아직 확인하지 못했어요.",
                                        color = AnsimMuted, fontSize = 12.sp,
                                        lineHeight = 18.sp)
                                }
                            }
                        }
                        if (hearingMode) {
                            Text("화면 강조 · 진동 안내", color = accentColor,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(data.action, color = AnsimInk,
                                fontSize = 18.sp, lineHeight = 27.sp,
                                fontWeight = FontWeight.Bold)
                        } else if (data.action.isNotBlank()) {
                            if (data.level == "timeout") {
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.width(3.dp).height(46.dp).background(AnsimDanger))
                                    Text(data.action, Modifier.weight(1f), color = AnsimInk,
                                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                        lineHeight = 21.sp)
                                }
                            } else if (caution) {
                                // 파란색 좌측선으로 주의 행동을 안내합니다.
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(Modifier.width(3.dp).height(40.dp).background(AnsimGreen))
                                    Text(data.action, Modifier.weight(1f), color = AnsimInk,
                                        fontSize = 13.sp, lineHeight = 20.sp)
                                }
                            } else {
                                Text(data.action, color = AnsimInk, fontSize = 13.sp,
                                    lineHeight = 20.sp, modifier = Modifier.padding(top = 1.dp))
                            }
                        }
                        if (danger && !hasPhone) {
                            Text("가족 전화번호가 등록되지 않았어요. 아는 번호로 직접 전화할 수 있어요.",
                                color = AnsimMuted, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                        if (languageCode == "vi" && data.scamType == "family_impersonation") {
                            Text("가족인 척 돈을 보내라고 해요.", color = AnsimMuted, fontSize = 13.sp)
                        }
                    }
                }
                // 위험도·접근성별 버튼 개수와 순서를 맞춥니다.
                // 모든 동작은 시스템 앱/실제 화면으로 이동합니다. 빈 동작 버튼은 두지 않습니다.
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when {
                        data.level == "checking" -> {
                            // 검사 중 카드는 외부 앱 위 오버레이가 담당하며 별도 버튼을 늘리지 않습니다.
                            WarningTextAction("닫기", onCloseClick)
                        }
                        data.level == "timeout" -> {
                            WarningMainAction("↻  다시 확인", AnsimGreen, onRetryClick)
                            WarningSecondaryAction("☎  ${callLabel}", onClick = call)
                            WarningTextAction(closeLabel, onCloseClick)
                        }
                        voiceMode -> {
                            //  소리 중심 - 전화와 다시 듣기만 표시합니다.
                            WarningMainAction("☎  ${callLabel}", if (danger) AnsimDanger else AnsimGreen, call)
                            WarningSecondaryAction("🔊  다시 듣기", enabled = ttsReady || screenReader, onClick = { speak() })
                            WarningTextAction(closeLabel, onCloseClick)
                        }
                        languageCode != "ko" -> {
                            // 다국어 - 전화와 닫기만 표시합니다.
                            WarningMainAction(internationalCallLabel, if (danger) AnsimDanger else AnsimGreen, call)
                            WarningSecondaryAction(closeLabel, onClick = onCloseClick)
                        }
                        caution -> {
                            // 주의는 버튼 2개 (확인, 닫기).
                            WarningMainAction("☎  가족에게 물어보기", AnsimGreen, call)
                            WarningSecondaryAction(closeLabel, onClick = onCloseClick)
                        }
                        danger -> {
                            // 위험 기본 화면은 전화 / 차단·신고 / 다시 듣기·닫기.
                            WarningMainAction("☎  ${callLabel}", AnsimDanger, call)
                            if (isKakao) {
                                Surface(color = Color(0xFFF5F6F8), shape = RoundedCornerShape(26.dp)) {
                                    Text("카카오톡 대화방에서 차단해 주세요.\n대화방 오른쪽 위 메뉴에서 차단을 선택하세요.",
                                        Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
                                        color = AnsimInk, fontSize = 13.sp, lineHeight = 19.sp)
                                }
                                WarningSecondaryAction("⚑  신고하기", onClick = { showReportDialog = true })
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.weight(1f)) {
                                        WarningSecondaryAction("🚫  이 번호 차단하기", onClick = { openBlockSettings() })
                                    }
                                    Box(Modifier.weight(1f)) {
                                        WarningSecondaryAction("⚑  신고하기", onClick = { showReportDialog = true })
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.weight(1f)) {
                                    WarningSecondaryAction("🔊  다시 듣기", enabled = ttsReady || screenReader, onClick = { speak() })
                                }
                                Box(Modifier.weight(1f)) {
                                    WarningSecondaryAction(closeLabel, onClick = onCloseClick)
                                }
                            }
                        }
                    }
                }
            }
        }
        // 빠른 무한 점멸을 피하고 진한 색 대신 아주 약한 배경 강조를 표시합니다.
        if (flashVisible) {
            Box(Modifier.fillMaxSize().background(accentColor.copy(alpha = 0.10f)))
        }
        // 최종 시안 24p: 보호자 번호가 없을 때 경고 화면 위에 하단 시트로 안내합니다.
        if (showNoPhoneDialog) AnsimBottomSheet(onDismiss = { showNoPhoneDialog = false }) {
            Text("등록된 보호자가 없어요", style = ansimStyle(20, 28, FontWeight.Bold), color = AnsimInk)
            Spacer(Modifier.height(10.dp))
            Text("보호자를 등록하면 버튼 하나로 바로 전화할 수 있어요.", style = AnsimType.Body, color = AnsimMuted)
            Spacer(Modifier.height(22.dp))
            AnsimPrimaryButton("보호자 등록", { showNoPhoneDialog = false; onRegisterGuardianClick() })
            Spacer(Modifier.height(10.dp))
            AnsimOutlineButton("번호 직접 입력", { showNoPhoneDialog = false; onManualNumberClick() }, height = 52.dp)
            Spacer(Modifier.height(6.dp))
            WarningTextButton("닫기", { showNoPhoneDialog = false }, AnsimMuted)
        }
    }
}

/**
 * 최종 시안 30p: 쉬운 안내는 큰 금지 그림 · 큰 한 문장 · 전화 버튼 하나.
 * 시안에는 없지만 추가 요구 사항에 따라 아래에 '닫기' 버튼을 둡니다.
 * 외국어를 고른 경우 선택 언어 문장을 크게, 한국어를 그 아래에 작게 보여줍니다.
 */
@Composable
private fun SimpleWarningPage(
    level: String, sentence: String, callLabel: String, call: () -> Unit,
    onClose: () -> Unit, link: Boolean = false, foreign: ForeignCopy? = null
) {
    val caution = level == "caution"
    val danger = !caution
    val accent = if (caution) AnsimCaution else AnsimDanger
    Column(
        Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(60.dp))
            Canvas(Modifier.size(192.dp).semantics {
                contentDescription = when {
                    caution -> "주의 표시"
                    link -> "링크 누르기 금지 표시"
                    else -> "돈 보내기 금지 표시"
                }
            }) {
                val k = size.width / 192f
                val c = Offset(96f * k, 96f * k)
                if (caution) {
                    drawLine(accent, Offset(96f * k, 52f * k), Offset(96f * k, 108f * k),
                        strokeWidth = 12f * k, cap = StrokeCap.Round)
                    drawCircle(accent, radius = 7f * k, center = Offset(96f * k, 136f * k))
                } else {
                    if (link) {
                        // 사슬 고리 두 개(링크) 위에 빨간 사선
                        val stroke = Stroke(width = 7f * k, cap = StrokeCap.Round)
                        drawRoundRect(AnsimInk, topLeft = Offset(40f * k, 76f * k), size = Size(66f * k, 40f * k),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f * k), style = stroke)
                        drawRoundRect(AnsimInk, topLeft = Offset(86f * k, 76f * k), size = Size(66f * k, 40f * k),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f * k), style = stroke)
                    } else {
                        // 지폐(둥근 사각형 + 가운데 원) 위에 빨간 사선
                        drawRoundRect(AnsimInk, topLeft = Offset(36f * k, 58f * k), size = Size(120f * k, 76f * k),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(9f * k), style = Stroke(width = 6f * k))
                        drawCircle(AnsimInk, radius = 18f * k, center = c, style = Stroke(width = 6f * k))
                    }
                    drawLine(Color.White, Offset(34f * k, 34f * k), Offset(158f * k, 158f * k),
                        strokeWidth = 17f * k, cap = StrokeCap.Round)
                    drawLine(accent, Offset(34f * k, 34f * k), Offset(158f * k, 158f * k),
                        strokeWidth = 9f * k, cap = StrokeCap.Round)
                }
                drawCircle(accent, radius = 88f * k, center = c, style = Stroke(width = 8.6f * k))
            }
            Spacer(Modifier.height(37.dp))
            if (foreign != null) {
                Text((if (danger) foreign.shortScam + "\n" else "") + if (link) foreign.shortLink else foreign.shortMoney,
                    color = AnsimInk, style = ansimStyle(44, 56, FontWeight.Bold), textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text(koreanSummary(danger, link), color = AnsimMuted,
                    style = ansimStyle(19, 27, FontWeight.SemiBold), textAlign = TextAlign.Center)
            } else {
                Text(koreanEasyHeadline(danger, link),
                    color = AnsimInk, style = ansimStyle(50, 65, FontWeight.Bold), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(24.dp))
        }
        if (foreign != null) WarningPill(foreign.call, call, AnsimBlue, Color.White, 88.dp, icon = AnsimIcons.Phone,
            style = ansimStyle(28, 34, FontWeight.Bold), secondLine = "가족에게 전화")
        else WarningPill(callLabel, call, AnsimBlue, Color.White, 88.dp, icon = AnsimIcons.Phone,
            style = ansimStyle(34, 42, FontWeight.Bold))
        Spacer(Modifier.height(8.dp))
        WarningTextButton(if (foreign != null) "${foreign.close} · 닫기" else "닫기", onClose, AnsimMuted,
            ansimStyle(19, 26, FontWeight.Bold))
    }
}

/**
 * 최종 시안 29p: 화면·진동 우선 모드. 큰 글씨 두 줄 + 문자/영상통화 버튼 두 개 + 닫기(추가 요구 사항).
 * 외국어를 고른 경우 선택 언어 문장 + 한국어 한 줄을 함께 표시합니다.
 */
@Composable
private fun HearingVisualWarningPage(
    title: String, explanation: String, action: String,
    danger: Boolean, onSms: () -> Unit, onCall: () -> Unit,
    onClose: () -> Unit,
    smsLabel: String = "가족에게 문자보내기",
    link: Boolean = false, foreign: ForeignCopy? = null
) {
    val background = if (danger) AnsimAlertRed else AnsimYellow
    val ink = if (danger) Color.White else Color(0xFF18191B)
    StatusBarIcons(light = danger)
    Box(Modifier.fillMaxSize().background(background).statusBarsPadding().navigationBarsPadding()) {
        Canvas(Modifier.fillMaxSize()) {
            val k = size.width / 390f
            drawCircle(if (danger) Color(0xFFD3313E) else Color(0xFFFFD569), radius = 129.7f * k,
                center = Offset(279.5f * k, 116.5f * k))
        }
        Column(Modifier.fillMaxSize().padding(start = 22.dp, end = 22.dp, top = 40.dp, bottom = 12.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                WarningChip(if (foreign != null) "${foreign.level(danger)} · ${if (danger) "위험" else "주의"}"
                else if (danger) "위험" else "주의",
                    container = if (danger) Color.White else Color(0xFF121318),
                    content = if (danger) AnsimDanger else AnsimYellow)
                Spacer(Modifier.height(19.dp))
                if (foreign != null) {
                    Text(if (danger) foreign.shortScam else foreign.shortCareful, color = ink,
                        style = ansimStyle(50, 58, FontWeight.ExtraBold).copy(letterSpacing = (-0.02).em))
                    Spacer(Modifier.height(16.dp))
                    Text(if (link) foreign.shortLink else foreign.shortMoney, color = ink,
                        style = ansimStyle(44, 52, FontWeight.ExtraBold).copy(letterSpacing = (-0.02).em))
                    Spacer(Modifier.height(18.dp))
                    Text(koreanSummary(danger, link), color = ink.copy(alpha = .9f),
                        style = ansimStyle(19, 27, FontWeight.SemiBold))
                } else {
                    Text(if (danger) "사기문자예요." else "조심하세요.", color = ink,
                        style = ansimStyle(65, 72, FontWeight.ExtraBold).copy(letterSpacing = (-0.03).em))
                    Spacer(Modifier.height(22.dp))
                    Text(koreanHearingAction(link), color = ink,
                        style = ansimStyle(55, 59.4, FontWeight.ExtraBold).copy(letterSpacing = (-0.02).em))
                }
            }
            WarningPill(foreign?.sms ?: smsLabel, onSms, Color.White, if (danger) AnsimDanger else Color(0xFF18191B), 68.dp,
                style = ansimStyle(19, 26, FontWeight.Bold), secondLine = if (foreign != null) "가족에게 문자보내기" else null)
            Spacer(Modifier.height(10.dp))
            WarningPill(foreign?.videoCall ?: "영상통화로 확인", onCall,
                if (danger) Color.White.copy(alpha = .15f) else Color(0xFFFFE5A8), ink, 54.dp,
                style = ansimStyle(19, 26, FontWeight.Bold), secondLine = if (foreign != null) "영상통화로 확인" else null)
            // 추가 요구 사항: 화면 안내 모드에도 닫기 버튼을 둡니다.
            Spacer(Modifier.height(6.dp))
            WarningTextButton(if (foreign != null) "${foreign.close} · 닫기" else "닫기", onClose, ink.copy(alpha = .9f),
                ansimStyle(17, 24, FontWeight.Bold))
        }
    }
}

/** 쉬운 안내 모드의 확인 지연: 큰 시계 그림 + 큰 한 문장 + 전화 버튼, 다시 확인, 닫기 */
@Composable
private fun SimpleTimeoutPage(
    onRetry: () -> Unit, call: () -> Unit, onClose: () -> Unit, callLabel: String,
    foreign: ForeignCopy? = null
) {
    Column(
        Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(60.dp))
            Canvas(Modifier.size(192.dp).semantics { contentDescription = "확인 지연 표시" }) {
                val k = size.width / 192f
                val c = Offset(96f * k, 96f * k)
                drawCircle(Color(0xFFE29A2E), radius = 88f * k, center = c, style = Stroke(width = 8.6f * k))
                drawLine(AnsimInk, c, Offset(96f * k, 50f * k), strokeWidth = 9f * k, cap = StrokeCap.Round)
                drawLine(AnsimInk, c, Offset(128f * k, 114f * k), strokeWidth = 9f * k, cap = StrokeCap.Round)
            }
            Spacer(Modifier.height(37.dp))
            if (foreign != null) {
                Text(foreign.timeoutTitle, color = AnsimInk, style = ansimStyle(40, 50, FontWeight.Bold),
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text("확인이 안 됐어요. 링크·송금은 멈추세요.", color = AnsimMuted,
                    style = ansimStyle(19, 27, FontWeight.SemiBold), textAlign = TextAlign.Center)
            } else {
                Text("확인이 안 됐어요.\n링크·송금은\n멈추세요.", color = AnsimInk,
                    style = ansimStyle(46, 60, FontWeight.Bold), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(24.dp))
        }
        if (foreign != null) WarningPill(foreign.call, call, AnsimBlue, Color.White, 88.dp, icon = AnsimIcons.Phone,
            style = ansimStyle(28, 34, FontWeight.Bold), secondLine = "가족에게 전화")
        else WarningPill(callLabel, call, AnsimBlue, Color.White, 88.dp, icon = AnsimIcons.Phone,
            style = ansimStyle(34, 42, FontWeight.Bold))
        Spacer(Modifier.height(10.dp))
        WarningOutline(foreign?.let { "${it.retry} · 다시 확인" } ?: "다시 확인", onRetry, 60.dp,
            icon = AnsimIcons.RotateCcw, style = ansimStyle(20, 26, FontWeight.Bold))
        Spacer(Modifier.height(4.dp))
        WarningTextButton(if (foreign != null) "${foreign.close} · 닫기" else "닫기", onClose, AnsimMuted,
            ansimStyle(19, 26, FontWeight.Bold))
    }
}

/** 화면 안내 모드의 확인 지연: 29p 구조를 노란 색으로. 큰 글씨 + 다시 확인 / 문자 보내기 / 닫기 */
@Composable
private fun HearingTimeoutPage(
    onRetry: () -> Unit, onSms: () -> Unit, onClose: () -> Unit, smsLabel: String,
    foreign: ForeignCopy? = null
) {
    val ink = Color(0xFF18191B)
    Box(Modifier.fillMaxSize().background(AnsimYellow).statusBarsPadding().navigationBarsPadding()) {
        Canvas(Modifier.fillMaxSize()) {
            val k = size.width / 390f
            drawCircle(Color(0xFFFFD569), radius = 129.7f * k, center = Offset(279.5f * k, 116.5f * k))
        }
        Column(Modifier.fillMaxSize().padding(start = 22.dp, end = 22.dp, top = 40.dp, bottom = 12.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                WarningChip(if (foreign != null) "${foreign.timeoutEyebrow} · 확인 지연" else "확인 지연",
                    container = Color(0xFF121318), content = AnsimYellow)
                Spacer(Modifier.height(19.dp))
                if (foreign != null) {
                    Text(foreign.timeoutTitle, color = ink,
                        style = ansimStyle(44, 52, FontWeight.ExtraBold).copy(letterSpacing = (-0.02).em))
                    Spacer(Modifier.height(16.dp))
                    Text(foreign.timeoutWarn, color = ink, style = ansimStyle(24, 32, FontWeight.Bold))
                    Spacer(Modifier.height(14.dp))
                    Text("확인을 완료하지 못했어요. 링크·송금은 멈추세요.", color = ink.copy(alpha = .85f),
                        style = ansimStyle(19, 27, FontWeight.SemiBold))
                } else {
                    Text("확인을\n못 했어요.", color = ink,
                        style = ansimStyle(60, 66, FontWeight.ExtraBold).copy(letterSpacing = (-0.03).em))
                    Spacer(Modifier.height(22.dp))
                    Text("링크·송금은\n멈추세요.", color = ink,
                        style = ansimStyle(50, 56, FontWeight.ExtraBold).copy(letterSpacing = (-0.02).em))
                }
            }
            WarningPill(foreign?.retry ?: "다시 확인", onRetry, Color(0xFF121318), Color.White, 68.dp,
                style = ansimStyle(19, 26, FontWeight.Bold), secondLine = if (foreign != null) "다시 확인" else null)
            Spacer(Modifier.height(10.dp))
            WarningPill(foreign?.sms ?: smsLabel, onSms, Color(0xFFFFE5A8), ink, 54.dp,
                style = ansimStyle(19, 26, FontWeight.Bold), secondLine = if (foreign != null) "가족에게 문자보내기" else null)
            Spacer(Modifier.height(6.dp))
            WarningTextButton(if (foreign != null) "${foreign.close} · 닫기" else "닫기", onClose, ink.copy(alpha = .9f),
                ansimStyle(17, 24, FontWeight.Bold))
        }
    }
}

/** 기호로 시작하던 버튼 문구를 시안 아이콘으로 바꿉니다. */
private fun iconFor(label: String): Pair<androidx.compose.ui.graphics.vector.ImageVector?, String> = when {
    label.startsWith("☎") -> AnsimIcons.Phone to label.drop(1).trim()
    label.startsWith("↻") -> AnsimIcons.RotateCcw to label.drop(1).trim()
    label.startsWith("🔊") -> AnsimIcons.Volume1 to label.removePrefix("🔊").trim()
    label.startsWith("🚫") -> AnsimIcons.Ban to label.removePrefix("🚫").trim()
    label.startsWith("⚑") -> AnsimIcons.Flag to label.drop(1).trim()
    else -> null to label
}

/** 다국어(영어·중국어) 등 공통 경고 화면의 버튼. 시안 버튼과 같은 모양을 씁니다. */
@Composable
private fun WarningMainAction(label: String, color: Color, onClick: () -> Unit) {
    val (icon, text) = iconFor(label)
    WarningPill(text, onClick, color, Color.White, if (text.contains('\n')) 66.dp else 56.dp, icon = icon,
        style = ansimStyle(17, 23, FontWeight.Bold))
}

@Composable
private fun WarningSecondaryAction(
    label: String, enabled: Boolean = true, onClick: () -> Unit
) {
    val (icon, text) = iconFor(label)
    WarningOutline(text, onClick, 48.dp, icon = icon, enabled = enabled,
        style = ansimStyle(15, 22, FontWeight.SemiBold))
}

@Composable
private fun WarningTextAction(label: String, onClick: () -> Unit) {
    WarningTextButton(label, onClick, AnsimMuted)
}
