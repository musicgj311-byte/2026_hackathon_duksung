package com.hackathon.ansimmessagepractice.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.theme.*

/**
 * 시안 8p 코드 입력: 하나의 입력창 + 6개 칸(48×60, 간격 8, 회색 1dp 테두리, 현재 칸은 2dp 파랑).
 * 붙여넣기, 연속 입력, 삭제에 모두 대응합니다.
 */
@Composable
fun SixDigitCodeField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    BasicTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it in '0'..'9' }.take(6)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
        cursorBrush = SolidColor(Color.Transparent),
        modifier = modifier.focusRequester(focusRequester).semantics { contentDescription = "6자리 연결번호, ${value.length}자리 입력됨" },
        decorationBox = { innerTextField ->
            Box {
                Row(Modifier.widthIn(max = 328.dp).fillMaxWidth().clickable { focusRequester.requestFocus() },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { index ->
                        val active = index == value.length.coerceAtMost(5) && value.length < 6
                        val shape = RoundedCornerShape(12.dp)
                        Box(
                            Modifier.weight(1f).height(60.dp).background(Color.White, shape)
                                .border(if (active) 2.dp else 1.dp, if (active) AnsimBlue else AnsimBorder, shape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(value.getOrNull(index)?.toString() ?: "", color = AnsimInk,
                                style = ansimStyle(26, 32, FontWeight.Bold), textAlign = TextAlign.Center)
                        }
                    }
                }
                // 키보드를 제공하는 실제 입력창은 별도로 존재합니다.
                Box(Modifier.size(1.dp)) { innerTextField() }
            }
        }
    )
}

/**
 * 시안 7p·10p 코드 표시: 흰 칸(테두리 없음, 높이 56, 간격 10) + 32px 굵은 숫자.
 * expired=true면 숫자를 회색 취소선으로 표시합니다.
 */
@Composable
fun SixDigitReadOnly(code: String, modifier: Modifier = Modifier, expired: Boolean = false) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(6) { i ->
            Box(Modifier.weight(1f).height(56.dp).background(Color.White, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center) {
                Text(code.getOrNull(i)?.toString() ?: "–",
                    color = if (expired) Color(0xFF989EA7) else AnsimInk,
                    style = ansimStyle(32, 40, FontWeight.Bold),
                    textDecoration = if (expired) TextDecoration.LineThrough else null)
            }
        }
    }
}
