package com.hackathon.ansimmessagepractice.guardian

import android.content.Context

/**
 * 오프라인 화면 시연을 위한 별도 로컬 상태입니다.
 * 실제 /guardian API, 실기기 연결, 전화번호 등록, 보호자 알림 전송에 사용하지 않습니다.
 * 이 시연 빌드에서는 기본 ON이고, 실제 서버를 이용할 때 설정에서 OFF로 전환할 수 있습니다.
 */
object DemoPairing {
    /**
     * 시연 모드에서 '시연 화면입니다' 같은 추가 안내 문구를 보일지 여부.
     * 최종 시안과 똑같은 화면을 위해 기본값은 false입니다. 심사·외부 공개 시 true로 바꾸면 다시 표시됩니다.
     */
    const val SHOW_DEMO_NOTICES = false
    const val CODE = "482917"
    const val GUARDIAN_NAME = "김지은 (딸)"
    const val GUARDIAN_PHONE_MASKED = "010-****-5678"
    const val GUARDIAN_PHONE = "010-1234-5678"
    const val PROTECTED_NAME = "엄마 (김순자)"
    private const val PREF = "ansim_demo_pairing"
    private const val OPTIONS = "ansim_settings"
    private const val DURATION_MS = 10 * 60 * 1000L

    fun enabled(context: Context): Boolean = context.getSharedPreferences(OPTIONS, Context.MODE_PRIVATE)
        .getBoolean("demo_pairing_enabled", false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(OPTIONS, Context.MODE_PRIVATE).edit()
            .putBoolean("demo_pairing_enabled", enabled).apply()
    }

    /** 시연용 6자리 코드와 10분 만료 시각. API 응답으로 가장하지 않습니다. */
    fun ticket(context: Context, refresh: Boolean = false): PairingTicket {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        var expires = prefs.getLong("expires_at", 0L)
        if (refresh || expires == 0L) {
            expires = now + DURATION_MS
            prefs.edit().putLong("expires_at", expires)
                .putBoolean("requested", false).putBoolean("approved", false).apply()
        }
        return PairingTicket(CODE, expires)
    }

    /** 코드 검증은 로컬 시연일 뿐; 서버와 연동된 두 휴대폰을 연결하지 않습니다. */
    fun request(context: Context, code: String): Boolean {
        val ticket = ticket(context)
        if (code != ticket.code || ticket.expiresAtEpochMillis <= System.currentTimeMillis()) return false
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean("requested", true).apply()
        return true
    }

    fun hasRequest(context: Context): Boolean = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        .getBoolean("requested", false)

    fun approve(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putBoolean("requested", false).putBoolean("approved", true)
            .putLong("approved_at", System.currentTimeMillis()).apply()
    }

    /** 시연 연결 승인 시각 (보호자 관리 화면의 '연결일') */
    fun approvedAt(context: Context): Long = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        .getLong("approved_at", 0L)

    fun approved(context: Context): Boolean = enabled(context) &&
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean("approved", false)

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
