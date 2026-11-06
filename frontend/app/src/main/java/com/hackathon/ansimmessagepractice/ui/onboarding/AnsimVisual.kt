package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hackathon.ansimmessagepractice.ui.theme.*

@Composable
internal fun AnsimPrimaryButton(
    text: String, onClick: () -> Unit, enabled: Boolean = true,
    modifier: Modifier = Modifier, container: Color = AnsimBlue, contentColor: Color = Color.White,
    icon: ImageVector? = null, height: Dp = 56.dp, textStyle: TextStyle = AnsimType.Button
) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = height),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container, contentColor = contentColor,
            disabledContainerColor = AnsimDisabled, disabledContentColor = AnsimDisabledText
        )
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = textStyle, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun AnsimOutlineButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    textColor: Color = AnsimInk, enabled: Boolean = true, icon: ImageVector? = null,
    height: Dp = 56.dp, textStyle: TextStyle = AnsimType.ButtonSmall,
    container: Color = Color.White, borderColor: Color = AnsimBorder
) {
    OutlinedButton(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = height),
        shape = CircleShape, border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = container, contentColor = textColor)
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = textStyle, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun AnsimTextLink(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    color: Color = AnsimMuted, underline: Boolean = true, style: TextStyle = AnsimType.Link
) {
    Box(modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Text(text, style = style, color = color,
            textDecoration = if (underline) TextDecoration.Underline else null)
    }
}

@Composable
internal fun AnsimRadio(selected: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val ring = 1.5.dp.toPx()
        drawCircle(if (selected) AnsimBlue else Color(0xFF7D828B),
            radius = size.minDimension / 2 - ring / 2, style = Stroke(width = ring))
        if (selected) drawCircle(AnsimBlue, radius = 6.25.dp.toPx())
    }
}

@Composable
internal fun AnsimChoiceRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .background(if (selected) AnsimBlueTint else Color.White, RoundedCornerShape(12.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) AnsimBlue else AnsimBorder, RoundedCornerShape(12.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(start = 18.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, Modifier.weight(1f), style = ansimStyle(17, 24, if (selected) FontWeight.Bold else FontWeight.Medium),
            color = if (selected) AnsimBlueText else AnsimInk)
        Spacer(Modifier.width(10.dp))
        AnsimRadio(selected)
    }
}

@Composable
internal fun AnsimTopBar(title: String, onBack: () -> Unit, background: Color = Color.White) {
    Column(Modifier.fillMaxWidth().background(background)) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.CenterStart) {
                AnsimIcon(AnsimIcons.ChevronLeft, AnsimInk, 22.dp,
                    Modifier.padding(start = 20.dp), contentDescription = "뒤로")
            }
            Spacer(Modifier.width(4.dp))
            Text(title, style = AnsimType.TopBar, color = AnsimInk)
        }
        HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
    }
}

@Composable
internal fun AnsimInfoBox(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().background(AnsimCream, RoundedCornerShape(12.dp))
        .padding(start = 18.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
        AnsimIcon(AnsimIcons.Info, AnsimMuted, 19.dp, Modifier.padding(top = 2.5.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, Modifier.weight(1f), style = AnsimType.BodyLong, color = AnsimMuted)
    }
}

@Composable
internal fun AnsimBottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color(0xFF14161C).copy(alpha = .5f))
            .clickable(interactionSource = null, indication = null, onClick = onDismiss))
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color.White, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .clickable(interactionSource = null, indication = null) { }
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 25.dp, bottom = 32.dp),
            content = content
        )
    }
}

/** 화면 큰 제목 + 설명 */
@Composable
internal fun AnsimHeading(title: String, lead: String? = null, leadStyle: TextStyle = AnsimType.Lead) {
    Text(title, style = AnsimType.Title, color = AnsimInk)
    if (lead != null) {
        Spacer(Modifier.height(10.dp))
        Text(lead, style = leadStyle, color = AnsimMuted)
    }
}
