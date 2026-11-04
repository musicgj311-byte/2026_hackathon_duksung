package com.hackathon.ansimmessagepractice.navigation

import android.content.Context
import android.content.Intent
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_MESSAGE
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_SOURCE
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_RISK
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_REASON
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_SENDER
import com.hackathon.ansimmessagepractice.EXTRA_SCAM_TYPE
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_TITLE
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_EXPLANATION
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_ACTION
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_EASY
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_LANGUAGE
import com.hackathon.ansimmessagepractice.MainActivity

/** 사용자가 오버레이를 누르면 경고 상세 화면을 엽니다. 이 과정에서 메시지 원문을 저장하지 않습니다. */
internal fun openWarningDetails(
    context: Context,
    risk: String,
    receivedMessage: String?,
    senderLabel: String?,
    sourceApp: String = "sms",
    analysisReason: String? = null, scamType: String = "none",
    serverTitle: String? = null, serverExplanation: String? = null,
    serverAction: String? = null, serverEasySentence: String? = null,
    serverLanguage: String? = null
): Boolean = runCatching {
    val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        putExtra(EXTRA_WARNING_RISK, risk)
        putExtra(EXTRA_WARNING_SOURCE, sourceApp)
        putExtra(EXTRA_SCAM_TYPE, scamType.take(50))
        receivedMessage?.take(4000)?.let { putExtra(EXTRA_WARNING_MESSAGE, it) }
        senderLabel?.take(120)?.let { putExtra(EXTRA_WARNING_SENDER, it) }
        analysisReason?.take(1000)?.let { putExtra(EXTRA_WARNING_REASON, it) }
        serverTitle?.take(300)?.let { putExtra(EXTRA_WARNING_TITLE, it) }
        serverExplanation?.take(1000)?.let { putExtra(EXTRA_WARNING_EXPLANATION, it) }
        serverAction?.take(500)?.let { putExtra(EXTRA_WARNING_ACTION, it) }
        serverEasySentence?.take(250)?.let { putExtra(EXTRA_WARNING_EASY, it) }
        serverLanguage?.take(8)?.let { putExtra(EXTRA_WARNING_LANGUAGE, it) }
    }
    context.startActivity(intent)
}.isSuccess