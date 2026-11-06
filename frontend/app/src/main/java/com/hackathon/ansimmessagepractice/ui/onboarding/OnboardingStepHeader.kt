package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hackathon.ansimmessagepractice.ui.theme.AnsimBlue
import com.hackathon.ansimmessagepractice.ui.theme.AnsimType

@Composable
internal fun OnboardingStepHeader(step: Int, @Suppress("UNUSED_PARAMETER") onBackClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("$step / 4", style = AnsimType.Step, color = AnsimBlue)
        Spacer(Modifier.height(7.5.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFFECEEF0), RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth(step / 4f).fillMaxHeight().background(AnsimBlue, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
internal fun OnboardingEyebrow(text: String) {
    Text(text, style = AnsimType.Step, color = AnsimBlue)
}

@Composable
internal fun OnboardingPage(
    bottomPadding: androidx.compose.ui.unit.Dp = 32.dp,
    bottom: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = bottomPadding)
    ) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), content = content)
        bottom()
    }
}
