package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hackathon.ansimmessagepractice.ui.theme.*

@Composable
fun RoleSelectionScreen(onProtectedSelected: () -> Unit, onGuardianSelected: () -> Unit,
                        onBackClick: () -> Unit, changingRole: Boolean = false) {
    var chosen by remember { mutableStateOf("protected") }
    OnboardingPage(bottom = {
        Text("설정에서 언제든 바꿀 수 있어요", modifier = Modifier.fillMaxWidth(),
            style = AnsimType.Body, color = AnsimMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(23.dp))
        AnsimPrimaryButton("다음", { if (chosen == "guardian") onGuardianSelected() else onProtectedSelected() })
    }) {
        Spacer(Modifier.height(48.dp))
        AnsimHeading("이 휴대폰을\n누가 사용하시나요?", "보호자와 보호받는 분 모두 같은 앱을 써요.")
        Spacer(Modifier.height(30.dp))
        Column(verticalArrangement = Arrangement.spacedBy(17.dp)) {
            RoleChoice("protected", chosen, "내 휴대폰을 지켜 주세요", "사기 문자가 오면 경고를 받아요") { chosen = it }
            RoleChoice("guardian", chosen, "가족을 지켜 줄래요", "가족에게 위험 문자가 오면 알림을 받아요 (보호자)") { chosen = it }
            if (changingRole) Text("역할을 바꿔도 이전 가족 연결은 자동으로 해제되지 않아요.",
                style = AnsimType.Body, color = AnsimDangerText)
        }
    }
}

@Composable
private fun RoleChoice(value: String, chosen: String, headline: String,
                       supporting: String, choose: (String) -> Unit) {
    val selected = chosen == value
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) AnsimBlueTint else Color.White, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) AnsimBlue else AnsimBorder, shape)
            .clickable(role = Role.RadioButton) { choose(value) }
            .padding(start = 20.dp, end = 22.dp, top = 18.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnsimIcon(if (value == "protected") AnsimIcons.ShieldCheck else AnsimIcons.Users,
            if (selected) AnsimBlue else AnsimMuted, 30.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(headline, style = ansimStyle(18, 25, FontWeight.Bold),
                color = if (selected) AnsimBlueText else AnsimInk)
            Text(supporting, style = ansimStyle(15, 22.5), color = AnsimMuted)
        }
        Spacer(Modifier.width(12.dp))
        AnsimRadio(selected)
    }
}
