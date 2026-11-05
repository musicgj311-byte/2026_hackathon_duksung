package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.theme.*

/** 최초 1회 역할 선택. '알림 방식' 선택과 혼동하지 않도록 별도 화면에 둡니다. */
@Composable
fun RoleSelectionScreen(
    onProtectedSelected: () -> Unit,
    onGuardianSelected: () -> Unit,
    onBackClick: () -> Unit,
    changingRole: Boolean = false
) {
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 25.dp, vertical = 18.dp)) {
        TextButton(onClick = onBackClick) { Text("‹  이전", color = AnsimInk) }
        Spacer(Modifier.height(22.dp))
        Text("안심문자를 어떻게 사용하시겠어요?", color = AnsimInk,
            fontSize = 28.sp, lineHeight = 37.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(9.dp))
        Text("나에게 맞는 사용 방법을 선택해 주세요.", color = AnsimMuted, fontSize = 14.sp)
        if (changingRole) {
            Spacer(Modifier.height(12.dp))
            Text("역할을 변경하면 선택한 역할의 초기 설정 화면으로 이동해요. 기존 연결은 자동으로 해제되지 않아요.",
                color = AnsimCaution, fontSize = 13.sp, lineHeight = 20.sp)
        }
        Spacer(Modifier.height(30.dp))
        RoleOption("✓", "보호받기", "보호 대상자", "받은 문자를 확인하고\n위험하면 가족에게 알려요.", onProtectedSelected)
        Spacer(Modifier.height(14.dp))
        RoleOption("♡", "가족 보호하기", "보호자", "연결된 가족에게 위험 문자가 오면\n알림을 받아요.", onGuardianSelected)
        Spacer(Modifier.height(26.dp))
        Text("선택한 역할은 나중에 설정에서 바꿀 수 있어요.", color = AnsimMuted,
            fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RoleOption(icon: String, title: String, subtitle: String, description: String, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)) {
        Row(Modifier.padding(19.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            Surface(color = AnsimMint, shape = RoundedCornerShape(11.dp)) {
                Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                    Text(icon, fontSize = 28.sp, color = AnsimGreen)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = AnsimInk, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text(subtitle, color = AnsimGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(description, color = AnsimMuted, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
    }
}