// 전체 모듈에서 공통으로 사용할 Gradle 설정을 추가하는 최상위 빌드 파일입니다.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}