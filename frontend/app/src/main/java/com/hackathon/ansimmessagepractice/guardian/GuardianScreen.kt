package com.hackathon.ansimmessagepractice.guardian

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.onboarding.OnboardingStepHeader
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimPrimaryButton
import com.hackathon.ansimmessagepractice.ui.theme.*

/** 연락처 입력 -> 입력 정보 확인 -> 로컬 저장 완료.
 * 로컬 연락처 저장과 서버 보호자 연결은 반드시 구분합니다.
 */
@Composable
fun GuardianScreen(onSkipClick: () -> Unit, onBackClick: () -> Unit, fromHome: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE) }
    val deviceId = remember(context) { AnsimDeviceId.value(context) }
    var name by remember { mutableStateOf(GuardianCall.savedName(context)) }
    var relationship by remember { mutableStateOf(prefs.getString("guardian_relationship", "") ?: "") }
    var phone by remember { mutableStateOf(GuardianCall.savedNumber(context)) }
    var stage by remember { mutableIntStateOf(0) }
    var showPairing by remember { mutableStateOf(false) }
    val cleanPhone = GuardianCall.cleanNumber(phone)
    val valid = name.isNotBlank() && GuardianCall.validNumber(cleanPhone)
    // 보호자 앱과 서버로 이미 연결된 경우(연결 완료 화면·보호자 관리에서 들어옴)
    val serverLinked = remember(context) { prefs.getBoolean("server_guardian_linked", false) }

    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 25.dp, vertical = 17.dp)) {
        if (!fromHome) OnboardingStepHeader(4, { if (stage > 0) stage-- else onBackClick() })
        else Row(Modifier.clickable { if (stage > 0) stage-- else onBackClick() }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AnsimIcon(AnsimIcons.ChevronLeft, AnsimInk, 22.dp)
            Spacer(Modifier.width(8.dp))
            Text("보호자 관리", style = AnsimType.TopBar, color = AnsimInk)
        }
        Spacer(Modifier.height(23.dp))
        when (stage) {
            0 -> {
                Text("보호자 정보를 입력해 주세요", style = AnsimType.Title,
                    fontWeight = FontWeight.ExtraBold, color = AnsimInk)
                Spacer(Modifier.height(8.dp))
                Text("위험할 때 바로 전화할 수 있도록 믿을 수 있는 가족 번호를 등록해요.",
                    color = AnsimMuted, fontSize = 14.sp, lineHeight = 21.sp)
                Spacer(Modifier.height(22.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it.take(30) },
                        label = { Text("보호자 이름") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = relationship, onValueChange = { relationship = it.take(20) },
                        label = { Text("관계 (선택)") }, placeholder = { Text("예: 딸, 아들") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it.take(22) },
                        label = { Text("전화번호") }, placeholder = { Text("010-1234-5678") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth())
                    Text(if (serverLinked) "이 전화번호는 이 휴대폰에만 저장되고, 경고 화면의 전화 버튼에 쓰여요."
                    else "이 전화번호는 기기에만 저장돼요. 보호자에게 자동으로 위험 알림을 보내려면 별도의 서버 연결이 필요해요.",
                        color = AnsimMuted, fontSize = 12.sp, lineHeight = 20.sp)
                    if (PairingIntegration.isConfigured && !serverLinked) {
                        TextButton(onClick = { showPairing = !showPairing }) {
                            Text(if (showPairing) "연결 코드 발급 닫기" else "보호자 앱 연결 코드 발급", color = AnsimGreen)
                        }
                        if (showPairing) GuardianPairingPanel(deviceId)
                    }
                }
                Spacer(Modifier.height(8.dp))
                AnsimPrimaryButton("보호자 정보 확인", { stage = 1 }, enabled = valid)
                if (!fromHome) TextButton(onClick = onSkipClick, modifier = Modifier.fillMaxWidth()) {
                    Text("나중에 등록할게요", color = AnsimMuted)
                }
            }
            1 -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text("보호자 정보를 확인해 주세요", style = AnsimType.Title,
                        fontWeight = FontWeight.ExtraBold, color = AnsimInk)
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = AnsimInk)
                    GuardianInfoRow("이름", name.trim())
                    GuardianInfoRow("관계", relationship.trim().ifBlank { "미입력" })
                    GuardianInfoRow("전화번호", cleanPhone)
                    Text("등록하면 경고 화면에서 이 번호로 직접 전화할 수 있어요. 위험 문자 원문은 가족에게 전송되지 않아요.",
                        color = AnsimMuted, fontSize = 13.sp, lineHeight = 21.sp,
                        modifier = Modifier.padding(top = 18.dp))
                }
                Spacer(Modifier.height(12.dp))
                AnsimPrimaryButton("보호자로 등록", {
                    prefs.edit().putString("guardian_name", name.trim())
                        .putString("guardian_relationship", relationship.trim())
                        .putString("guardian_phone", cleanPhone)
                        .putLong("guardian_contact_saved_at", System.currentTimeMillis()).apply()
                    stage = 2
                })
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { stage = 0 }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                    Text("다시 입력", color = AnsimInk)
                }
            }
            else -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    AnsimIcon(AnsimIcons.CircleCheck, AnsimProtected, 44.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("${name.trim()} 님의 전화번호를 저장했어요", color = AnsimInk,
                        style = AnsimType.Title, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(23.dp))
                    HorizontalDivider(color = Color(0xFFEDF0F2))
                    GuardianSuccessRow(AnsimIcons.Phone, "경고 화면에서 버튼 하나로 전화할 수 있어요")
                    GuardianSuccessRow(AnsimIcons.Bell, if (serverLinked) "위험 문자가 오면 보호자에게 알림이 전송돼요"
                    else "서버 연결이 완료되기 전에는 보호자 알림이 전송되지 않아요")
                    GuardianSuccessRow(AnsimIcons.Lock, "받은 문자 원문은 보호자에게 전송하지 않아요")
                }
                Spacer(Modifier.height(12.dp))
                AnsimPrimaryButton(if (fromHome) "보호자 관리로" else "홈으로", onSkipClick)
            }
        }
    }
}

@Composable
private fun GuardianInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 59.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(90.dp), color = AnsimMuted, fontSize = 15.sp)
        Text(value, Modifier.weight(1f), color = AnsimInk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
    HorizontalDivider(color = Color(0xFFF0F1F3))
}

@Composable
private fun GuardianSuccessRow(icon: androidx.compose.ui.graphics.vector.ImageVector, content: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        AnsimIcon(icon, AnsimBlue, 22.dp, Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(12.dp))
        Text(content, modifier = Modifier.weight(1f), style = ansimStyle(17, 26.4), color = AnsimInk)
    }
    HorizontalDivider(color = Color(0xFFF0F1F3))
}