package com.hackathon.ansimmessagepractice.navigation

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.hackathon.ansimmessagepractice.WarningLaunch
import com.hackathon.ansimmessagepractice.data.api.BackendConfig
import com.hackathon.ansimmessagepractice.data.api.UserSync
import com.hackathon.ansimmessagepractice.guardian.PairingIntegration
import com.hackathon.ansimmessagepractice.detection.AlertRecord
import com.hackathon.ansimmessagepractice.guardian.GuardianAlertScreen
import com.hackathon.ansimmessagepractice.guardian.GuardianScreen
import com.hackathon.ansimmessagepractice.guardian.GuardianManageScreen
import com.hackathon.ansimmessagepractice.guardian.GuardianCodeInputScreen
import com.hackathon.ansimmessagepractice.guardian.GuardianHomeScreen
import com.hackathon.ansimmessagepractice.guardian.ManualCallScreen
import com.hackathon.ansimmessagepractice.overlay.WarningOverlay
import com.hackathon.ansimmessagepractice.ui.history.AlertDetailScreen
import com.hackathon.ansimmessagepractice.ui.onboarding.*
import com.hackathon.ansimmessagepractice.ui.warning.PreviewJudgeResults
import com.hackathon.ansimmessagepractice.ui.warning.WarningPayload
import com.hackathon.ansimmessagepractice.ui.warning.WarningScreen
import com.hackathon.ansimmessagepractice.ui.warning.localizedCode
import com.hackathon.ansimmessagepractice.ui.warning.userTypeCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 실제 백엔드 주소가 설정되면 사용자 등록/보호자 페어링을 호출합니다.
 */
@Composable
fun AnsimApp(
    warningLaunch: WarningLaunch? = null,
    onWarningLaunchHandled: () -> Unit = {},
    launcherOpenKey: Int = 0
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE) }
    var currentScreen by rememberSaveable { mutableStateOf(if (warningLaunch != null) "warning" else "splash") }
    // 기존 설치자의 role 키가 없다면 이전 기본 동작(보호 대상자)을 유지합니다.
    var currentRole by remember { mutableStateOf(prefs.getString("role", "protected") ?: "protected") }
    var changingRole by remember { mutableStateOf(false) }
    var guardianPairFromHome by remember { mutableStateOf(false) }
    var guardianNotifyFromHome by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(prefs.getString("notification_type", "기본 알림") ?: "기본 알림") }
    var selectedLanguage by remember { mutableStateOf(prefs.getString("language", "한국어") ?: "한국어") }
    var risk by remember { mutableStateOf(warningLaunch?.riskLevel ?: "danger") }
    var activeWarning by remember { mutableStateOf(warningLaunch) }
    var alertDetailLevel by remember { mutableStateOf("danger") }
    var selectedAlert by remember { mutableStateOf<AlertRecord?>(null) }
    var guardianFromHome by remember { mutableStateOf(false) }
    var manualReturnTo by remember { mutableStateOf("home") }
    var permissionsReturnTo by remember { mutableStateOf("home") }
    var backendReturnTo by remember { mutableStateOf("settings") }
    val warningOverlay = remember(context) { WarningOverlay(context.applicationContext) }
    val previewScope = rememberCoroutineScope()
    var previewSerial by remember { mutableIntStateOf(0) }
    DisposableEffect(warningOverlay) { onDispose { warningOverlay.dismiss() } }

    LaunchedEffect(launcherOpenKey) {
        if (launcherOpenKey > 0) { activeWarning = null; currentScreen = "splash" }
    }
    LaunchedEffect(warningLaunch?.requestId) {
        warningLaunch?.let { incoming ->
            risk = incoming.riskLevel
            activeWarning = incoming
            currentScreen = "warning"
            onWarningLaunchHandled()
        }
    }
    fun finishSetup() {
        prefs.edit().putBoolean("onboarded", true)
            .putString("notification_type", selectedType)
            .putString("language", selectedLanguage)
            .putString("user_type_code", userTypeCode(selectedType))
            .putString("language_code", localizedCode(selectedLanguage))
            .putString("role", currentRole)
            .putBoolean("role_selected", true)
            .putBoolean("needs_server_sync", true).apply()
        if (BackendConfig.isConfigured(context)) previewScope.launch {
            runCatching { UserSync.ensureCurrent(context, currentRole) }
                .onFailure { Toast.makeText(context, "서버 사용자 등록 실패: ${it.message}", Toast.LENGTH_LONG).show() }
        }
        changingRole = false
        currentScreen = if (currentRole == "guardian") "guardian_home" else "home"
    }
    fun chooseRole(nextRole: String) {
        // 사용자 제공 AnsimApp의 선택 상태 저장 / 재실행 로직을 유지합니다.
        val alreadyOnboardedForRole = !changingRole &&
                prefs.getBoolean("onboarded", false) &&
                prefs.getString("role", null) == nextRole
        currentRole = nextRole
        guardianPairFromHome = false
        guardianNotifyFromHome = false
        if (!changingRole) {
            prefs.edit()
                .putString("role", nextRole)
                .putBoolean("role_selected", true)
                .putBoolean("onboarded", alreadyOnboardedForRole)
                .apply()
        }
        currentScreen = if (alreadyOnboardedForRole) {
            if (nextRole == "guardian") "guardian_home" else "home"
        } else if (nextRole == "guardian") {
            "guardian_notify"
        } else {
            // 보호 대상자는 권한 설정을 첫 온보딩 단계로 표시합니다.
            "permission"
        }
    }
    fun requestRoleChange() {
        changingRole = true
        currentScreen = "role_select"
    }
    val previous = when (currentScreen) {
        "role_select" -> if (changingRole) (if (prefs.getString("role", "protected") == "guardian") "guardian_home" else "home") else "start"
        "select" -> "permission"
        "guardian_notify" -> if (guardianNotifyFromHome) "guardian_home" else "role_select"
        "guardian_pair" -> if (guardianPairFromHome) "guardian_home" else "guardian_notify"
        "language" -> "select"
        "permission" -> "role_select"
        "guardian" -> if (guardianFromHome) "guardian_manage" else "language"
        "guardian_manage" -> "home"
        "settings", "guardian_preview", "alert_detail" -> "home"
        "permissions_settings" -> permissionsReturnTo
        "backend_settings" -> backendReturnTo
        "manual_call" -> manualReturnTo
        "warning" -> if (currentRole == "guardian") "guardian_home" else "home"
        else -> null
    }
    BackHandler(enabled = previous != null) {
        if (currentScreen == "role_select" && changingRole) {
            currentRole = prefs.getString("role", "protected") ?: "protected"
            changingRole = false
        }
        if (previous != null) currentScreen = previous
    }

    // 화면 이동은 Navigation 라이브러리 대신 currentScreen 문자열 상태로 관리합니다.
    // 새 화면을 추가하면 아래 분기와 이전 화면(previous) 이동 규칙을 함께 확인하세요.
    when (currentScreen) {
        "splash" -> SplashScreen {
            currentRole = prefs.getString("role", "protected") ?: "protected"
            currentScreen = when {
                // 첫 설치: 역할을 선택한 다음 해당 역할의 권한을 안내합니다.
                !prefs.getBoolean("role_selected", false) -> "role_select"
                // 온보딩 완료 후 실행: 권한 안내 반복 없이 홈으로 이동합니다.
                prefs.getBoolean("onboarded", false) ->
                    if (currentRole == "guardian") "guardian_home" else "home"
                // 온보딩 중 종료: 보호 대상자는 권한 단계부터 다시 시작합니다.
                currentRole == "guardian" -> "guardian_notify"
                else -> "permission"
            }
        }
        "start" -> StartScreen(name = "Android", onStartClick = { currentScreen = "role_select" })
        "role_select" -> RoleSelectionScreen(
            onProtectedSelected = { chooseRole("protected") },
            onGuardianSelected = { chooseRole("guardian") },
            onBackClick = {
                if (changingRole) {
                    currentRole = prefs.getString("role", "protected") ?: "protected"
                    changingRole = false
                    currentScreen = if (currentRole == "guardian") "guardian_home" else "home"
                } else currentScreen = "start"
            },
            changingRole = changingRole
        )
        "guardian_notify" -> GuardianNotificationPermissionScreen(
            onNext = {
                if (guardianNotifyFromHome) currentScreen = "guardian_home"
                else { guardianPairFromHome = false; currentScreen = "guardian_pair" }
            },
            onBack = { currentScreen = if (guardianNotifyFromHome) "guardian_home" else "role_select" }
        )
        "guardian_pair" -> GuardianCodeInputScreen(
            onBack = { currentScreen = if (guardianPairFromHome) "guardian_home" else "guardian_notify" },
            onConnected = { finishSetup() },
            onLater = { if (guardianPairFromHome) currentScreen = "guardian_home" else finishSetup() }
        )
        "guardian_home" -> GuardianHomeScreen(
            onInputCode = { guardianPairFromHome = true; currentScreen = "guardian_pair" },
            onNotificationSettings = { guardianNotifyFromHome = true; currentScreen = "guardian_notify" },
            onBackendSettings = { backendReturnTo = "guardian_home"; currentScreen = "backend_settings" },
            onManualCall = { manualReturnTo = "guardian_home"; currentScreen = "manual_call" },
            onChangeRole = { requestRoleChange() }
        )
        "permission" -> PermissionScreen(
            onNextClick = { currentScreen = "select" },
            onBackClick = { currentScreen = "role_select" },
            onSkipClick = { currentScreen = "select" }
        )
        "select" -> UserTypeScreen(selectedType, { selectedType = it },
            onNextClick = { currentScreen = "language" }, onBackClick = { currentScreen = "permission" })
        "language" -> LanguageScreen(selectedLanguage, { selectedLanguage = it },
            onNextClick = { guardianFromHome = false; currentScreen = "guardian" },
            onBackClick = { currentScreen = "select" })
        "backend_settings" -> BackendSettingsScreen(onBack = { currentScreen = backendReturnTo })
        "permissions_settings" -> PermissionScreen(
            onNextClick = { currentScreen = permissionsReturnTo },
            onBackClick = { currentScreen = permissionsReturnTo }, fromSettings = true
        )
        "guardian" -> GuardianScreen(
            onSkipClick = { if (guardianFromHome) currentScreen = "guardian_manage" else finishSetup() },
            onBackClick = { currentScreen = if (guardianFromHome) "guardian_manage" else "language" },
            fromHome = guardianFromHome
        )
        "guardian_manage" -> GuardianManageScreen(
            onBack = { currentScreen = "home" },
            onEdit = { guardianFromHome = true; currentScreen = "guardian" }
        )
        "home" -> HomeScreen(
            selectedType = selectedType, selectedLanguage = selectedLanguage,
            onTestWarningClick = { selectedRisk -> risk = selectedRisk; activeWarning = null; currentScreen = "warning" },
            onSettingsClick = { currentScreen = "settings" },
            onPermissionClick = { permissionsReturnTo = "home"; currentScreen = "permissions_settings" },
            onGuardianManageClick = { guardianFromHome = true; currentScreen = "guardian_manage" },
            onGuardianPreviewClick = { currentScreen = "guardian_preview" },
            onAlertDetailClick = { level -> selectedAlert = null; alertDetailLevel = level; currentScreen = "alert_detail" },
            onRecentAlertClick = { alert -> selectedAlert = alert; alertDetailLevel = alert.level; currentScreen = "alert_detail" },
            onTestOverlayClick = { selectedRisk ->
                if (warningOverlay.hasPermission()) {
                    previewSerial++
                    if (selectedRisk == "checking") {
                        val thisPreview = previewSerial
                        warningOverlay.showChecking()
                        previewScope.launch {
                            delay(3_000)
                            if (previewSerial == thisPreview) warningOverlay.dismissChecking()
                        }
                    } else warningOverlay.show(selectedRisk)
                } else {
                    Toast.makeText(context, "다른 앱 위에 표시 권한을 먼저 허용해 주세요.", Toast.LENGTH_LONG).show()
                    warningOverlay.openPermissionSettings()
                }
            }
        )
        "guardian_preview" -> GuardianAlertScreen(
            onBackClick = { currentScreen = "home" },
            onManualNumberClick = { manualReturnTo = "guardian_preview"; currentScreen = "manual_call" }
        )
        "alert_detail" -> AlertDetailScreen(level = alertDetailLevel, record = selectedAlert,
            onBack = { currentScreen = "home" },
            onManualNumber = { manualReturnTo = "alert_detail"; currentScreen = "manual_call" })
        "settings" -> NotificationSettingsScreen(
            currentType = selectedType, currentLanguage = selectedLanguage,
            onBackClick = { currentScreen = "home" },
            onPermissionClick = { permissionsReturnTo = "settings"; currentScreen = "permissions_settings" },
            onChangeRoleClick = { requestRoleChange() },
            onBackendClick = { backendReturnTo = "settings"; currentScreen = "backend_settings" },
            onGuardianClick = { guardianFromHome = true; currentScreen = "guardian_manage" },
            onSaveClick = { type, language ->
                selectedType = type; selectedLanguage = language
                prefs.edit().putString("notification_type", type)
                    .putString("language", language)
                    .putString("user_type_code", userTypeCode(type))
                    .putString("language_code", localizedCode(language))
                    .putBoolean("needs_server_sync", true).apply()
                if (BackendConfig.isConfigured(context)) previewScope.launch {
                    runCatching { UserSync.ensureCurrent(context, "protected") }
                        .onFailure { Toast.makeText(context, "설정 서버 동기화 실패: ${it.message}", Toast.LENGTH_LONG).show() }
                }
                currentScreen = "home"
            }
        )
        "manual_call" -> ManualCallScreen(onBackClick = { currentScreen = manualReturnTo })
        "warning" -> {
            val warningData = activeWarning?.let { incoming ->
                // 실제 알림에서는 샘플 발신자·본문·유형을 추정해서 보여주면 안 됩니다.
                // 시연 텍스트는 숨겨진 미리보기에서만 사용합니다.
                val generic = PreviewJudgeResults.forLevel(incoming.riskLevel, localizedCode(selectedLanguage))
                WarningPayload(
                    level = incoming.riskLevel,
                    scamType = incoming.scamType ?: "none",
                    title = incoming.title ?: generic.title,
                    explanation = incoming.explanation ?: incoming.analysisReason ?: "의심스러운 메시지가 도착했어요. 직접 확인해 주세요.",
                    action = incoming.action ?: generic.action,
                    easySentence = incoming.easySentence ?: generic.easySentence,
                    language = incoming.language ?: localizedCode(selectedLanguage),
                    receivedMessage = incoming.receivedMessage,
                    sender = incoming.senderLabel,
                    sourceApp = incoming.sourceApp,
                    guardianNotificationStatus = incoming.notificationStatus
                )
            }
            WarningScreen(
                riskLevel = risk, selectedType = selectedType, selectedLanguage = selectedLanguage,
                onCloseClick = { activeWarning = null; currentScreen = if (currentRole == "guardian") "guardian_home" else "home" },
                payload = warningData,
                onManualNumberClick = { manualReturnTo = "warning"; currentScreen = "manual_call" },
                onRegisterGuardianClick = { guardianFromHome = true; currentScreen = "guardian_manage" },
                onRetryClick = {
                    Toast.makeText(context, "판정 서버 연결 후 다시 확인할 수 있어요.", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}