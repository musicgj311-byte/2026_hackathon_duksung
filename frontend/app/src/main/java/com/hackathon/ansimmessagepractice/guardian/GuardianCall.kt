package com.hackathon.ansimmessagepractice.guardian

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** 통화 대상은 사용자가 직접 등록한 신뢰할 수 있는 번호만 사용합니다. */
object GuardianCall {
    fun savedName(context: Context): String = context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE)
        .getString("guardian_name", "")?.trim().orEmpty()

    fun savedNumber(context: Context): String = context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE)
        .getString("guardian_phone", "")?.trim().orEmpty()

    fun cleanNumber(raw: String): String = raw.trim().filterIndexed { index, char ->
        char.isDigit() || (index == 0 && char == '+')
    }

    fun validNumber(raw: String): Boolean = cleanNumber(raw).count(Char::isDigit) in 9..15

    /**
     * CALL_PHONE 권한을 사용하지 않습니다.
     * 등록된 번호가 채워진 전화 앱을 열고, 실제 발신은 사용자가 확인해서 누릅니다.
     */
    fun requestCall(context: Context) = dial(context, savedNumber(context))

    fun dial(context: Context, number: String) {
        if (!validNumber(number)) {
            Toast.makeText(context, "먼저 믿을 수 있는 가족 번호를 확인해 주세요.", Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", cleanNumber(number), null))
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure {
            Toast.makeText(context, "전화 앱을 열지 못했어요.", Toast.LENGTH_LONG).show()
        }
    }
}

/** 통화 권한 팝업 없이 저장된 보호자 번호를 전화 앱에 미리 입력합니다. */
@Composable
fun rememberGuardianCallAction(): () -> Unit {
    val context = LocalContext.current
    return { GuardianCall.requestCall(context) }
}