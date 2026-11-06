package com.hackathon.ansimmessagepractice.guardian

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimOutlineButton
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimPrimaryButton
import com.hackathon.ansimmessagepractice.ui.onboarding.AnsimTopBar
import com.hackathon.ansimmessagepractice.ui.theme.*

/** 모르는 번호가 아닌, 사용자가 직접 아는 가족 번호로 거는 안전한 대체 경로. */
@Composable
fun ManualCallScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                runCatching {
                    context.contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) phone = cursor.getString(0).orEmpty()
                    }
                }
            }
        }
    }
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding().imePadding()) {
        AnsimTopBar("전화할 번호 입력", onBackClick)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 23.dp)) {
            Text("가족이나 믿을 수 있는 사람의 번호를 입력해 주세요.\n문자에 적힌 번호로는 걸지 마세요.",
                style = AnsimType.LeadLong, color = AnsimMuted)
            Spacer(Modifier.height(19.dp))
            Text("전화번호", style = ansimStyle(15, 22, FontWeight.SemiBold), color = AnsimMuted)
            Spacer(Modifier.height(6.dp))
            val shape = RoundedCornerShape(12.dp)
            BasicTextField(
                value = phone, onValueChange = { phone = it.take(22) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = ansimStyle(22, 30, FontWeight.Bold).copy(color = AnsimInk),
                cursorBrush = SolidColor(AnsimBlue), interactionSource = interaction,
                modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White, shape)
                    .border(if (focused || phone.isNotEmpty()) 2.dp else 1.dp,
                        if (focused || phone.isNotEmpty()) AnsimBlue else AnsimBorder, shape),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                        if (phone.isEmpty()) Text("010-0000-0000", style = ansimStyle(22, 30, FontWeight.Bold),
                            color = Color(0xFFC3C6CC))
                        inner()
                    }
                }
            )
            Spacer(Modifier.height(20.dp))
            AnsimOutlineButton("연락처에서 선택", {
                contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
            }, icon = AnsimIcons.User)
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp)) {
            AnsimPrimaryButton("전화 앱에서 걸기", {
                val number = GuardianCall.cleanNumber(phone)
                if (GuardianCall.validNumber(number)) GuardianCall.dial(context, number)
            }, enabled = GuardianCall.validNumber(phone), icon = AnsimIcons.Phone)
        }
    }
}
