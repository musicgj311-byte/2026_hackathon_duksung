package com.hackathon.ansimmessagepractice.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.theme.*

/** 보호자 위험 알림 시안. 본 화면의 데이터는 화면 미리보기이며 FCM 성공을 의미하지 않음. */
@Composable
fun GuardianAlertScreen(onBackClick: () -> Unit, onManualNumberClick: () -> Unit = {}) {
    val context = LocalContext.current
    val callGuardian = rememberGuardianCallAction()
    val name = GuardianCall.savedName(context).ifBlank { "가족" }
    val canCall = GuardianCall.validNumber(GuardianCall.savedNumber(context))
    var showHistory by remember { mutableStateOf(false) }
    if (showHistory) AlertDialog(onDismissRequest = { showHistory = false },
        title = { Text("알림 기록 (시연)") },
        text = { Text("가족 위험 문자 감지 · 가족 사칭 (시연 예시)\n\n실제 보호자 푸시 알림 기록은 서버 FCM 연동 후 표시할 수 있어요.") },
        confirmButton = { TextButton(onClick = { showHistory = false }) { Text("확인") } })

    Column(Modifier.fillMaxSize().background(AnsimCream).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp)) {
        TextButton(onClick = onBackClick) { Text("‹  보호자 알림", color = AnsimInk,
            fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(13.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Surface(shape = RoundedCornerShape(11.dp), color = Color.White, shadowElevation = 1.dp) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = AnsimGreen, shape = RoundedCornerShape(8.dp)) {
                        Text("☑", color = Color.White, fontSize = 21.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("안심문자 · 경고 미리보기", color = AnsimMuted, fontSize = 11.sp)
                            Text("방금", color = AnsimMuted, fontSize = 11.sp)
                        }
                        Text("가족에게 위험 문자가 도착했어요", color = AnsimInk,
                            fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("가족 사칭 문자예요. 지금 전화해서 확인해 주세요.",
                            color = AnsimMuted, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
            Surface(shape = RoundedCornerShape(11.dp), color = Color.White, shadowElevation = 1.dp) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(Modifier.fillMaxHeight().width(4.dp).background(AnsimDanger))
                    Column(Modifier.weight(1f).padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$name · 가족", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AnsimInk)
                            Text("위험 · 가족 사칭", color = AnsimDanger, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold)
                        }
                        Text("가족을 사칭해 송금을 요구하는 문자예요. 원래 휴대폰 번호로 직접 전화해 보세요.",
                            color = AnsimInk, fontSize = 14.sp, lineHeight = 22.sp)
                        HorizontalDivider(color = Color(0xFFEEF0F3))
                        GuardianAlertRow("위험 문자 수신", "시연")
                        GuardianAlertRow("경고 확인", "확인 불가")
                        GuardianAlertRow("링크 열기·송금", "확인 불가", green = true)
                    }
                }
            }
            Text("개인정보 보호를 위해 받은 문자 원문은 표시하지 않아요. 실제 전송 내역이 아닌 시연 화면이에요.",
                color = AnsimMuted, fontSize = 12.sp, lineHeight = 19.sp)
        }
        Spacer(Modifier.height(12.dp))
        if (canCall) {
            Button(onClick = { callGuardian() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AnsimGreen)) {
                Text("☎  ${name}에게 전화", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            Button(onClick = onManualNumberClick,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AnsimGreen)) {
                Text("☎  전화할 가족 번호 직접 입력", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showHistory = true }, modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(7.dp)) {
            Text("알림 기록 보기", color = AnsimInk, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GuardianAlertRow(label: String, status: String, green: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = AnsimInk, fontSize = 13.sp)
        Text(status, color = if (green) AnsimProtected else AnsimMuted,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
