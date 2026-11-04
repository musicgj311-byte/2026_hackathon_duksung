package com.hackathon.ansimmessagepractice.guardian

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.theme.*

/** 모르는 번호가 아닌, 사용자가 직접 아는 가족 번호로 거는 안전한 대체 경로. */
@Composable
fun ManualCallScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }
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
    Column(Modifier.fillMaxSize().background(AnsimCream).statusBarsPadding().navigationBarsPadding()
        .padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        TextButton(onClick = onBackClick) { Text("‹  전화할 번호 입력", color = AnsimInk) }
        Text("가족이나 믿을 수 있는 사람의 번호를 입력해 주세요. 문자에 적힌 번호로는 걸지 마세요.", color = AnsimMuted)
        OutlinedTextField(value = phone, onValueChange = { phone = it.take(22) }, label = { Text("전화번호") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true,
            modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = {
            contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
            Text("연락처에서 선택")
        }
        Text("선택한 번호 한 개만 가져와요. 전체 연락처나 통화 기록은 읽지 않아요.",
            color = AnsimMuted, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Button(onClick = {
            val number = GuardianCall.cleanNumber(phone)
            if (GuardianCall.validNumber(number)) {
                GuardianCall.dial(context, number)
            }
        }, enabled = GuardianCall.validNumber(phone),
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(7.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AnsimGreen, contentColor = androidx.compose.ui.graphics.Color.White)) {
            Text("☎  전화 앱에서 걸기", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}