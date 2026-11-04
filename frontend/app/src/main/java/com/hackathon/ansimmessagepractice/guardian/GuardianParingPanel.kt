package com.hackathon.ansimmessagepractice.guardian

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 보호 대상자: 코드 발급만 가능. 보호자 입력은 GuardianCodeInputScreen에서 수행. */
@Composable
fun GuardianPairingPanel(deviceId: String) {
    val scope = rememberCoroutineScope()
    var ticket by remember { mutableStateOf<PairingTicket?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(ticket) {
        while (ticket != null && System.currentTimeMillis() < (ticket?.expiresAtEpochMillis ?: 0L)) {
            currentTime = System.currentTimeMillis()
            delay(1_000)
        }
        currentTime = System.currentTimeMillis()
    }
    val remaining = (((ticket?.expiresAtEpochMillis ?: 0L) - currentTime).coerceAtLeast(0) / 1000L)
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text("발급한 6자리 코드를 가족에게 알려 주세요. 가족은 자신의 휴대폰에 입력해요.",
            color = AnsimMuted, fontSize = 14.sp, lineHeight = 21.sp)
        if (!PairingIntegration.isConfigured) {
            Surface(color = AnsimMint, shape = RoundedCornerShape(9.dp)) {
                Text("아직 연결 서버가 설정되지 않았어요. 현재 실제 코드를 발급할 수 없어요.",
                    Modifier.padding(13.dp), color = AnsimGreenDark, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
        if (ticket != null && remaining > 0) {
            SixDigitReadOnly(ticket!!.code, Modifier.fillMaxWidth())
            Text("남은 시간 ${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}",
                color = if (remaining <= 60) AnsimDanger else AnsimGreenDark,
                fontWeight = FontWeight.Bold)
            Text("일회용 코드예요. 한 번 사용하면 다시 쓸 수 없어요.",
                color = AnsimMuted, fontSize = 13.sp)
        } else if (ticket != null) {
            Text("코드가 만료됐어요. 새 코드를 발급해 주세요.", color = AnsimDanger, fontSize = 14.sp)
        }
        if (error.isNotBlank()) Text(error, color = AnsimDanger, fontSize = 14.sp)
        Button(onClick = {
            if (!PairingIntegration.isConfigured) {
                error = "백엔드 연결 후 이용할 수 있어요."
                return@Button
            }
            busy = true; error = ""; ticket = null
            scope.launch {
                runCatching { PairingIntegration.backend.issueCode(deviceId) }
                    .onSuccess { issued ->
                        if (issued.code.length == 6 && issued.code.all(Char::isDigit)
                            && issued.expiresAtEpochMillis > System.currentTimeMillis()) {
                            ticket = issued
                            currentTime = System.currentTimeMillis()
                        } else error = "서버에서 올바른 6자리 코드를 받지 못했어요."
                    }.onFailure { error = it.message ?: "코드 발급에 실패했어요." }
                busy = false
            }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(7.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AnsimGreen)) {
            Text(if (ticket == null) "6자리 코드 받기" else "새 코드 발급하기", color = Color.White)
        }
    }
}