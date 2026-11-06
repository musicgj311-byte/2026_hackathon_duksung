package com.hackathon.ansimmessagepractice.ui.warning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.guardian.GuardianCall
import com.hackathon.ansimmessagepractice.guardian.rememberGuardianCallAction
import com.hackathon.ansimmessagepractice.ui.theme.*

@Composable
internal fun WarningIcon(status: Status, color: Color) {
    Box(Modifier.size(98.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
        Text(if (status == Status.CHECKING) "…" else "!", color = color,
            fontSize = 65.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.semantics { contentDescription = "안전 경고" })
    }
}

@Composable
internal fun ReceivedCard(label: String, message: String?, sender: String?, color: Color) {
    Surface(Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(19.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(label, color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(message?.takeIf { it.isNotBlank() } ?: "의심스러운 문자가 도착했어요.",
                color = AnsimInk, fontSize = 17.sp, lineHeight = 25.sp)
            if (!sender.isNullOrBlank()) Text(sender, color = AnsimMuted, fontSize = 14.sp)
        }
    }
}

@Composable
internal fun ReplayButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, border = BorderStroke(1.5.dp, Color.White),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        shape = RoundedCornerShape(40.dp)) {
        Text("🔊 $label", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun CallButton(text: String, color: Color, inverted: Boolean = false) {
    val context = LocalContext.current
    val callGuardian = rememberGuardianCallAction()
    Button(onClick = { callGuardian() },
        modifier = Modifier.fillMaxWidth().heightIn(min = 79.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (inverted) AnsimGreen else Color.White,
            contentColor = if (inverted) Color.White else color
        ), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp)) {
        Text("☎", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(9.dp))
        Text(text, fontSize = if (text.contains('\n')) 19.sp else 22.sp,
            lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun CloseButton(label: String, close: () -> Unit) {
    OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.4.dp, Color.White),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}