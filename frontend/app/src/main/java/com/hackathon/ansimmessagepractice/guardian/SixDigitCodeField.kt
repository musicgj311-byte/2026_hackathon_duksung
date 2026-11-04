package com.hackathon.ansimmessagepractice.guardian

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.theme.AnsimGreen
import com.hackathon.ansimmessagepractice.ui.theme.AnsimInk

/** 하나의 입력창 + 6개의 시각적 칸: 붙여넣기, 연속 입력, 삭제에 모두 대응합니다. */
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
                Row(Modifier.fillMaxWidth().clickable { focusRequester.requestFocus() },
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(6) { index ->
                        val active = index == value.length.coerceAtMost(5)
                        Surface(
                            modifier = Modifier.weight(1f).height(58.dp),
                            shape = RoundedCornerShape(11.dp),
                            border = BorderStroke(if (active) 2.dp else 1.dp,
                                if (active) AnsimGreen else Color(0xFFD5DFD8)),
                            color = Color.White
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(value.getOrNull(index)?.toString() ?: "", color = AnsimInk,
                                    fontSize = 25.sp, fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                // 키보드를 제공하는 실제 입력창은 별도로 존재합니다.
                Box(Modifier.size(1.dp)) { innerTextField() }
            }
        }
    )
}

@Composable
fun SixDigitReadOnly(code: String, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(6) { i ->
            Surface(Modifier.weight(1f).height(58.dp),
                shape = RoundedCornerShape(11.dp),
                border = BorderStroke(1.dp, AnsimGreen),
                color = Color.White) {
                Box(contentAlignment = Alignment.Center) {
                    Text(code.getOrNull(i)?.toString() ?: "–", color = AnsimInk,
                        fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}