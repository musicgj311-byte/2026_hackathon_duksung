package com.hackathon.ansimmessagepractice

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * 에뮬레이터 또는 안드로이드 실기기에서 실행하는 계측 테스트 예제입니다.
 *
 * 참고: [Android 테스트 문서](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // 테스트 대상 앱의 Context입니다.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.ansimmessagepractice", appContext.packageName)
    }
}