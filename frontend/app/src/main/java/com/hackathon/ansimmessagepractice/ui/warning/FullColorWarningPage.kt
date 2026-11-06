package com.hackathon.ansimmessagepractice.ui.warning

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.hackathon.ansimmessagepractice.ui.theme.*

private val CautionInk = Color(0xFF18191B)
private val CautionBrown = Color(0xFF4C3801)

@Composable
internal fun FullColorWarningPage(
    data: WarningPayload,
    preview: Boolean,
    callLabel: String,
    isKakao: Boolean,
    onCall: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onReplay: () -> Unit,
    replayEnabled: Boolean,
    onClose: () -> Unit,
    guardianName: String = "보호자"
) {
    val danger = data.level == "danger"
    val background = if (danger) AnsimAlertRed else AnsimYellow
    val ink = if (danger) Color.White else CautionInk
    StatusBarIcons(light = danger)
    Box(Modifier.fillMaxSize().background(background).statusBarsPadding().navigationBarsPadding()) {
        Canvas(Modifier.fillMaxSize()) {
            val k = size.width / 390f
            if (danger) drawCircle(Color(0xFFD3313E), radius = 150.6f * k, center = Offset(309.7f * k, 110.1f * k))
            else drawCircle(Color(0xFFFFD569), radius = 204f * k, center = Offset(-4f * k, 490f * k))
        }
        Column(Modifier.fillMaxSize().padding(start = 22.dp, end = 22.dp, top = 40.dp, bottom = if (danger) 25.dp else 24.dp)) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                WarningChip(
                    if (danger) "위험 · ${scamLabel(data.scamType)}${if (isKakao) " · 카카오톡" else ""}"
                    else "주의 · ${if (data.scamType == "delivery") "택배 사칭 의심" else scamLabel(data.scamType)}${if (isKakao) " · 카카오톡" else ""}",
                    container = if (danger) Color.White else Color(0xFF121318),
                    content = if (danger) AnsimDanger else AnsimYellow
                )
                Spacer(Modifier.height(19.dp))
                // 큰 제목은 사기 유형에 맞는 짧은 행동 문장(시안 54px)을 씁니다.
                Text(koreanBigHeadline(danger, isLinkScam(data.scamType, danger)), color = ink,
                    style = ansimStyle(54, 58.3, FontWeight.ExtraBold).copy(letterSpacing = (-0.02).em))
                Spacer(Modifier.height(16.dp))
                val explanation = if (preview) (if (danger) "가족인 척하는 사기 문자예요." else "처음 보는 인터넷 주소가 있어요.")
                else data.explanation
                Text(explanation, color = if (danger) Color.White.copy(alpha = .88f) else CautionBrown,
                    style = ansimStyle(19, 27, FontWeight.SemiBold))
                if (!data.receivedMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(22.dp))
                    Column(Modifier.fillMaxWidth()
                        .background(if (danger) Color(0xFFA81C25) else Color(0xFFFFE6A6), RoundedCornerShape(20.dp))
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp)) {
                        val suffix = when {
                            isKakao -> "받은 카카오톡"
                            preview && danger -> "저장 안 된 번호"
                            else -> "받은 문자"
                        }
                        Text("${data.sender?.takeIf(String::isNotBlank) ?: "발신자 미확인"} · $suffix",
                            style = ansimStyle(15, 22, FontWeight.Bold),
                            color = if (danger) Color(0xFFFFCCD0) else Color(0xFF68500D))
                        Spacer(Modifier.height(2.dp))
                        Text(data.receivedMessage.orEmpty(), color = ink, style = ansimStyle(16, 24),
                            maxLines = if (danger) 2 else 3, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (danger) {
                    Spacer(Modifier.height(13.dp))
                    val status = data.guardianNotificationStatus
                    val sent = status.equals("success", true) || status.equals("sent", true)
                    val text = when {
                        sent -> "$guardianName 님에게 알렸어요"
                        status == "failed" -> "보호자 알림 전송에 실패했어요. 직접 연락해 주세요."
                        preview && com.hackathon.ansimmessagepractice.guardian.DemoPairing.SHOW_DEMO_NOTICES ->
                            "시연 예시 · 실제 알림이 전송되지는 않았어요."
                        preview -> "$guardianName 님에게 알렸어요"
                        else -> "보호자 알림 상태를 확인하지 못했어요."
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (sent || (preview && !com.hackathon.ansimmessagepractice.guardian.DemoPairing.SHOW_DEMO_NOTICES)) {
                            AnsimIcon(AnsimIcons.Check, Color.White, 20.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(text, color = Color.White, style = ansimStyle(16, 24, FontWeight.Bold))
                    }
                } else if (data.action.isNotBlank()) {
                    Spacer(Modifier.height(17.dp))
                    Text(data.action, color = CautionInk, style = ansimStyle(16, 24, FontWeight.SemiBold))
                }
                Spacer(Modifier.height(16.dp))
            }
            if (danger) {
                WarningPill(callLabel, onCall, Color.White, AnsimDanger, 66.dp, icon = AnsimIcons.Phone,
                    style = ansimStyle(19, 26, FontWeight.Bold))
                Spacer(Modifier.height(12.dp))
                if (isKakao) {
                    Text("카카오톡 대화방에서 차단해 주세요. · 대화방 메뉴 › 차단",
                        color = Color.White, style = ansimStyle(13, 19))
                    Spacer(Modifier.height(8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!isKakao) WarningPill("차단", onBlock, Color.White.copy(alpha = .15f), Color.White, 52.dp,
                        Modifier.weight(1f), style = ansimStyle(19, 26, FontWeight.Bold))
                    WarningPill("신고", onReport, Color.White.copy(alpha = .15f), Color.White, 52.dp,
                        Modifier.weight(1f), style = ansimStyle(19, 26, FontWeight.Bold))
                    WarningPill("다시 듣기", onReplay, Color.White.copy(alpha = .15f), Color.White, 52.dp,
                        Modifier.weight(1.27f), enabled = replayEnabled, style = ansimStyle(19, 26, FontWeight.Bold))
                }
                Spacer(Modifier.height(11.dp))
                WarningTextButton("닫기", onClose, Color.White.copy(alpha = .9f), ansimStyle(16, 22, FontWeight.Bold))
            } else {
                WarningPill(callLabel, onCall, Color(0xFF121318), Color.White, 66.dp,
                    style = ansimStyle(19, 26, FontWeight.Bold))
                Spacer(Modifier.height(10.dp))
                WarningPill("닫기", onClose, Color(0xFFFFE5A8), CautionInk, 54.dp,
                    style = ansimStyle(19, 26, FontWeight.Bold))
            }
        }
    }
}

/** 흰/검정 배경의 작은 둥근 꼬리표 (위험 · 가족 사칭) */
@Composable
internal fun WarningChip(text: String, container: Color, content: Color) {
    Box(Modifier.heightIn(min = 32.dp).background(container, CircleShape).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = content, style = ansimStyle(15, 20, FontWeight.Bold))
    }
}

/** 경고 화면 공통 둥근 버튼 */
@Composable
internal fun WarningPill(
    label: String, onClick: () -> Unit, container: Color, content: Color, height: Dp,
    modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true,
    style: TextStyle = ansimStyle(18, 24, FontWeight.Bold), border: Color? = null,
    secondLine: String? = null
) {
    val shape = CircleShape
    Row(
        modifier.fillMaxWidth().heightIn(min = height)
            .background(container, shape)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        val tint = if (enabled) content else content.copy(alpha = .45f)
        if (icon != null) {
            AnsimIcon(icon, tint, 22.dp)
            Spacer(Modifier.width(8.dp))
        }
        if (secondLine == null) {
            Text(label, color = tint, style = style, textAlign = TextAlign.Center, maxLines = 2)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = tint, style = style, textAlign = TextAlign.Center)
                Text(secondLine, color = tint.copy(alpha = .9f), style = ansimStyle(15, 20), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
internal fun WarningTextButton(label: String, onClick: () -> Unit, color: Color, style: TextStyle = AnsimType.Body) {
    Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Text(label, color = color, style = style)
    }
}

@Composable
internal fun WarningOutline(label: String, onClick: () -> Unit, height: Dp = 52.dp, modifier: Modifier = Modifier,
                            icon: ImageVector? = null, enabled: Boolean = true,
                            style: TextStyle = ansimStyle(17, 24, FontWeight.Bold)) =
    WarningPill(label, onClick, Color.White, AnsimInk, height, modifier, icon, enabled, style, border = AnsimBorder)

@Composable
internal fun TimeoutWarningPage(onRetry: () -> Unit, onCall: () -> Unit, onClose: () -> Unit,
                                callLabel: String = "가족에게 전화해서 확인", foreign: ForeignCopy? = null) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.436f).background(AnsimYellow).statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 38.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnsimIcon(AnsimIcons.Clock, Color(0xFF874700), 17.dp)
                Spacer(Modifier.width(6.dp))
                Text(if (foreign != null) "${foreign.timeoutEyebrow} · 확인 지연" else "확인 지연",
                    color = Color(0xFF874700), style = ansimStyle(15, 22, FontWeight.Bold))
            }
            Spacer(Modifier.height(12.dp))
            Text(foreign?.timeoutTitle ?: "확인을 완료하지\n못했어요", color = AnsimInk,
                style = ansimStyle(28, 37.8, FontWeight.Bold))
            if (foreign != null) {
                Spacer(Modifier.height(4.dp))
                Text("확인을 완료하지 못했어요", color = Color(0xFF4E5460), style = ansimStyle(15, 22, FontWeight.SemiBold))
            }
            Spacer(Modifier.height(if (foreign != null) 10.dp else 15.dp))
            Text(foreign?.timeoutBody ?: "네트워크 연결이 불안정해요.\n안전 여부를 아직 알 수 없는 문자예요.",
                color = Color(0xFF4E5460), style = ansimStyle(17, 26.3))
            if (foreign != null) Text("네트워크 연결이 불안정해요. 안전 여부를 아직 알 수 없어요.",
                color = Color(0xFF4E5460), style = ansimStyle(15, 22))
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFE29A2E)))
        Column(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 21.dp, bottom = 25.dp)) {
            Column(Modifier.fillMaxWidth().background(Color(0xFFF9EEEF), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 13.dp)) {
                Text(foreign?.timeoutWarn ?: "확인되기 전까지 링크를 누르거나 송금하지 마세요.",
                    color = AnsimInk, style = ansimStyle(17, 27.2, FontWeight.Bold))
                if (foreign != null) Text("확인되기 전까지 링크를 누르거나 송금하지 마세요.",
                    color = AnsimMuted, style = ansimStyle(15, 22))
            }
            Spacer(Modifier.weight(1f))
            WarningPill(foreign?.retry ?: "다시 확인", onRetry, AnsimBlue, Color.White, 56.dp,
                secondLine = if (foreign != null) "다시 확인" else null)
            Spacer(Modifier.height(16.dp))
            WarningPill(foreign?.callToCheck ?: callLabel, onCall, Color.White, AnsimInk, 52.dp, icon = AnsimIcons.Phone,
                style = ansimStyle(17, 24, FontWeight.Bold), border = AnsimBorder,
                secondLine = if (foreign != null) "가족에게 전화해서 확인" else null)
            Spacer(Modifier.height(15.dp))
            WarningTextButton(if (foreign != null) "${foreign.close} · 닫기" else "닫기", onClose, AnsimMuted)
        }
    }
}

@Composable
private fun RedHeader(top: Dp, bottom: Dp, danger: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    StatusBarIcons(light = danger)
    Column(Modifier.fillMaxWidth().background(if (danger) AnsimDanger else AnsimYellow).statusBarsPadding()
        .padding(start = 24.dp, end = 24.dp, top = top, bottom = bottom), content = content)
    // 주의 단계는 시안 20p처럼 노란 영역 아래에 주황 선을 둡니다.
    if (!danger) Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFE29A2E)))
}

@Composable
private fun HeaderEyebrow(text: String, icon: ImageVector?, color: Color = Color.White) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            AnsimIcon(icon, color, 18.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = color, style = ansimStyle(15, 22, FontWeight.Bold))
    }
}

/** 회색 안내 상자: 작은 라벨 + 본문 */
@Composable
private fun GrayQuote(label: String, body: String, labelColor: Color = AnsimMuted,
                      bodyStyle: TextStyle = ansimStyle(17, 27.2), labelWeight: FontWeight = FontWeight.Normal) {
    Column(Modifier.fillMaxWidth().background(AnsimCream, RoundedCornerShape(14.dp))
        .padding(horizontal = 16.dp, vertical = 13.dp)) {
        Text(label, color = labelColor, style = ansimStyle(15, 22, labelWeight))
        Spacer(Modifier.height(4.dp))
        Text(body, color = AnsimInk, style = bodyStyle)
    }
}

@Composable
internal fun VoiceGuidedWarningPage(
    data: WarningPayload, callLabel: String, onCall: () -> Unit,
    onReplay: () -> Unit, replayEnabled: Boolean, quote: String = ""
) {
    val danger = data.level == "danger"
    val link = isLinkScam(data.scamType, danger)
    val headInk = if (danger) Color.White else AnsimInk
    Column(Modifier.fillMaxSize().background(Color.White)) {
        RedHeader(top = 38.dp, bottom = 29.dp, danger = danger) {
            HeaderEyebrow("소리로 읽어 드리고 있어요", AnsimIcons.Volume2, headInk)
            Spacer(Modifier.height(12.dp))
            Text(koreanTwoLine(danger, link), color = headInk, style = ansimStyle(30, 40.5, FontWeight.Bold))
        }
        Column(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 21.dp, bottom = 24.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                GrayQuote("읽어 드리는 내용", "“$quote”")
            }
            Spacer(Modifier.height(12.dp))
            WarningPill(callLabel, onCall, if (danger) AnsimDanger else Color(0xFF121318), Color.White, 64.dp,
                icon = AnsimIcons.Phone, style = ansimStyle(20, 26, FontWeight.Bold))
            Spacer(Modifier.height(16.dp))
            WarningOutline("다시 듣기", onReplay, 60.dp, icon = AnsimIcons.RotateCcw, enabled = replayEnabled,
                style = ansimStyle(18, 24, FontWeight.Bold))
        }
    }
}

@Composable
internal fun BilingualWarningPage(
    data: WarningPayload, preview: Boolean, copy: ForeignCopy, isKakao: Boolean,
    onCall: () -> Unit, onClose: () -> Unit
) {
    val danger = data.level == "danger"
    val link = isLinkScam(data.scamType, danger)
    val headInk = if (danger) Color.White else AnsimInk
    val headSub = if (danger) Color.White.copy(alpha = .9f) else CautionBrown
    Column(Modifier.fillMaxSize().background(Color.White)) {
        RedHeader(top = 38.dp, bottom = 27.dp, danger = danger) {
            HeaderEyebrow("${copy.level(danger)} · ${copy.languageName} / 한국어",
                if (danger) null else AnsimIcons.TriangleAlert, headInk)
            Spacer(Modifier.height(11.5.dp))
            Text(copy.headline(danger, link), color = headInk, style = ansimStyle(28, 36.3, FontWeight.Bold))
            Spacer(Modifier.height(11.dp))
            Text(koreanSummary(danger, link), color = headSub, style = ansimStyle(15, 22))
        }
        Column(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 24.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (data.explanation.isNotBlank()) {
                    Text(data.explanation, color = AnsimInk, style = ansimStyle(17, 26.4))
                    Spacer(Modifier.height(3.dp))
                }
                Text(koreanReason(data.scamType, danger), color = AnsimMuted, style = ansimStyle(15, 22))
                if (!danger && data.action.isNotBlank()) {
                    // 주의 단계는 무엇을 해야 하는지가 핵심이므로 행동 안내를 한 번 더 보여줍니다.
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        Box(Modifier.width(3.dp).fillMaxHeight().background(AnsimBlue))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(data.action, color = AnsimInk, style = ansimStyle(16, 24, FontWeight.SemiBold))
                            Text(if (link) "링크 대신 공식 앱이나 알고 있는 번호로 직접 확인하세요."
                            else "돈을 보내기 전에 알고 있는 번호로 직접 확인하세요.",
                                color = AnsimMuted, style = ansimStyle(15, 22))
                        }
                    }
                }
                if (!data.receivedMessage.isNullOrBlank() || preview) {
                    Spacer(Modifier.height(15.dp))
                    val label = (if (isKakao) copy.receivedKakao else copy.received) +
                            (data.sender?.takeIf(String::isNotBlank)?.let { " · $it" } ?: "")
                    GrayQuote(label,
                        if (preview && copy.languageName == "Tiếng Việt" && danger)
                            "Mẹ ơi, điện thoại con bị hỏng, chuyển giúp con 5 triệu vào tài khoản này nhé"
                        else data.receivedMessage.orEmpty(),
                        labelWeight = FontWeight.SemiBold, bodyStyle = ansimStyle(15, 23.3))
                }
            }
            Spacer(Modifier.height(16.dp))
            if (danger) WarningPill(copy.call, onCall, AnsimDanger, Color.White, 68.dp,
                icon = AnsimIcons.Phone, style = ansimStyle(19, 25, FontWeight.Bold), secondLine = "가족에게 전화하기")
            else WarningPill(copy.ask, onCall, Color(0xFF121318), Color.White, 68.dp,
                icon = AnsimIcons.Phone, style = ansimStyle(19, 25, FontWeight.Bold), secondLine = "가족에게 물어보기")
            Spacer(Modifier.height(16.dp))
            WarningOutline("${copy.close} · 닫기", onClose)
        }
    }
}

@Composable
internal fun KakaoWarningPage(
    data: WarningPayload, preview: Boolean, callLabel: String, onCall: () -> Unit,
    onReport: () -> Unit, onReplay: () -> Unit, replayEnabled: Boolean,
    onClose: () -> Unit, guardianName: String = "보호자"
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        RedHeader(top = 34.dp, bottom = 25.dp) {
            HeaderEyebrow("위험 · ${scamLabel(data.scamType)} · 카카오톡", AnsimIcons.TriangleAlert)
            Spacer(Modifier.height(10.dp))
            Text(koreanTwoLine(true, isLinkScam(data.scamType, true)), color = Color.White,
                style = ansimStyle(30, 40.5, FontWeight.Bold))
            Spacer(Modifier.height(14.dp))
            Text(data.explanation, color = Color.White.copy(alpha = .88f), style = ansimStyle(17, 24))
        }
        Column(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 19.dp, bottom = 24.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                GrayQuote("받은 카카오톡 · ${data.sender?.takeIf(String::isNotBlank) ?: "친구 아님"}",
                    data.receivedMessage ?: "받은 메시지 원문이 없어요.", labelColor = AnsimSubtle,
                    bodyStyle = ansimStyle(17, 26.3))
                Spacer(Modifier.height(13.dp))
                val status = data.guardianNotificationStatus
                val sent = status.equals("success", true) || status.equals("sent", true) ||
                        (preview && !com.hackathon.ansimmessagepractice.guardian.DemoPairing.SHOW_DEMO_NOTICES)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sent) {
                        AnsimIcon(AnsimIcons.Check, AnsimProtected, 17.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(when {
                        sent -> "보호자($guardianName)에게 알림을 보냈어요"
                        status == "failed" -> "보호자 알림 전송에 실패했어요."
                        preview -> "시연 예시 · 실제 보호자 알림은 전송되지 않았어요."
                        else -> "보호자 알림 전송 상태를 확인하지 못했어요."
                    }, color = if (sent) AnsimProtected else AnsimMuted, style = ansimStyle(15, 22, FontWeight.SemiBold))
                }
            }
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, AnsimBorder, RoundedCornerShape(12.dp))
                .padding(start = 18.dp, end = 16.dp, top = 13.dp, bottom = 13.dp)) {
                AnsimIcon(AnsimIcons.Ban, AnsimInk, 18.dp, Modifier.padding(top = 2.dp))
                Spacer(Modifier.width(9.dp))
                Column {
                    Text("카카오톡 대화방에서 차단해 주세요.", color = AnsimInk, style = ansimStyle(15, 22.5, FontWeight.Bold))
                    Text("대화방 오른쪽 위 메뉴 › 차단", color = AnsimMuted, style = ansimStyle(15, 22.5))
                }
            }
            Spacer(Modifier.height(14.dp))
            WarningPill(callLabel, onCall, AnsimDanger, Color.White, 60.dp, icon = AnsimIcons.Phone,
                style = ansimStyle(19, 26, FontWeight.Bold))
            Spacer(Modifier.height(14.dp))
            WarningOutline("신고하기", onReport, icon = AnsimIcons.Flag)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WarningOutline("다시 듣기", onReplay, modifier = Modifier.weight(1f), icon = AnsimIcons.Volume1,
                    enabled = replayEnabled)
                WarningOutline("닫기", onClose, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun VoiceTimeoutPage(
    quote: String, onRetry: () -> Unit, onCall: () -> Unit, onReplay: () -> Unit,
    replayEnabled: Boolean, onClose: () -> Unit, callLabel: String, foreign: ForeignCopy? = null
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        RedHeader(top = 38.dp, bottom = 29.dp, danger = false) {
            HeaderEyebrow(if (foreign != null) "Voice · 소리로 읽어 드리고 있어요" else "소리로 읽어 드리고 있어요",
                AnsimIcons.Volume2, AnsimInk)
            Spacer(Modifier.height(12.dp))
            Text(foreign?.timeoutTitle ?: "확인을 완료하지\n못했어요", color = AnsimInk,
                style = ansimStyle(30, 40.5, FontWeight.Bold))
            if (foreign != null) {
                Spacer(Modifier.height(6.dp))
                Text("확인을 완료하지 못했어요", color = CautionBrown, style = ansimStyle(15, 22, FontWeight.SemiBold))
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 21.dp, bottom = 16.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                GrayQuote("읽어 드리는 내용", "“$quote”")
            }
            Spacer(Modifier.height(12.dp))
            WarningPill(foreign?.retry ?: "다시 확인", onRetry, AnsimBlue, Color.White, 64.dp,
                style = ansimStyle(20, 26, FontWeight.Bold), secondLine = if (foreign != null) "다시 확인" else null)
            Spacer(Modifier.height(12.dp))
            WarningOutline("다시 듣기", onReplay, 56.dp, icon = AnsimIcons.RotateCcw, enabled = replayEnabled,
                style = ansimStyle(18, 24, FontWeight.Bold))
            Spacer(Modifier.height(12.dp))
            WarningOutline(foreign?.callToCheck ?: callLabel, onCall, 52.dp, icon = AnsimIcons.Phone)
            Spacer(Modifier.height(4.dp))
            WarningTextButton(if (foreign != null) "${foreign.close} · 닫기" else "닫기", onClose, AnsimMuted)
        }
    }
}
