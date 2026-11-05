package com.hackathon.ansimmessagepractice

import com.hackathon.ansimmessagepractice.data.api.guardianScamTypeName
import org.junit.Assert.assertEquals
import org.junit.Test

/** 보호자 알림에 백엔드 코드가 그대로 노출되지 않도록 확인하는 회귀 테스트. */
class GuardianLabelTest {
    @Test fun translatesKnownBackendTypes() {
        assertEquals("결제 알림 사칭", guardianScamTypeName("payment_alert"))
        assertEquals("가족 사칭", guardianScamTypeName("family_impersonation"))
        assertEquals("기관 사칭", guardianScamTypeName("agency_impersonation"))
        assertEquals("부고·초대장 사칭", guardianScamTypeName("obituary_invitation"))
        assertEquals("지원금·환급 사칭", guardianScamTypeName("subsidy_refund"))
        assertEquals("구인·대출 사칭", guardianScamTypeName("job_loan"))
        assertEquals("출입국·체류 사칭", guardianScamTypeName("immigration"))
    }

    @Test fun unknownTypeDoesNotLeakBackendCode() {
        assertEquals("기타 사기 의심", guardianScamTypeName("new_backend_type_code"))
        assertEquals("사기 의심", guardianScamTypeName("unknown"))
        assertEquals("결제 알림 사칭", guardianScamTypeName(" PAYMENT-ALERT "))
    }
}
