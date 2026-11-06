package com.hackathon.ansimmessagepractice.navigation

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.hackathon.ansimmessagepractice.WarningLaunch
import com.hackathon.ansimmessagepractice.data.api.BackendConfig
import com.hackathon.ansimmessagepractice.data.api.GuardianNotificationRecord
import com.hackathon.ansimmessagepractice.data.api.UserSync
import com.hackathon.ansimmessagepractice.guardian.PairingIntegration
import com.hackathon.ansimmessagepractice.guardian.DemoPairing
import com.hackathon.ansimmessagepractice.guardian.DemoGuardianApprovalScreen
import com.hackathon.ansimmessagepractice.guardian.DemoGuardianWaitingScreen
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
 * 프론트 1이 /judge 응답을 전달할 때 WarningLaunch에 동일한 필드를 채우면 됩니다.
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
    // 큰 글씨는 보호 대상자가 '큰 글씨와 소리' 모드를 선택한 경우에만 적용합니다.
    // 과거 버전의 별도 스위치 설정이 남아 있어도 현재 선택한 모드가 우선합니다.
    val largeTextEnabled = usesLargeText(selectedType, currentRole)
    var risk by remember { mutableStateOf(warningLaunch?.riskLevel ?: "danger") }
    var activeWarning by remember { mutableStateOf(warningLaunch) }
    var alertDetailLevel by remember { mutableStateOf("danger") }
    var selectedAlert by remember { mutableStateOf<AlertRecord?>(null) }
    var selectedGuardianAlert by remember { mutableStateOf<GuardianNotificationRecord?>(null) }
    var guardianFromHome by remember { mutableStateOf(false) }
    // 연결 완료 화면에서 '보호자 정보 입력하기'로 들어온 경우 (뒤로 가면 연결 완료 화면으로)
    var guardianInfoAfterPair by remember { mutableStateOf(false) }
    // 홈·보호자 관리·경고 화면에서 연결 코드 화면(7p)을 연 경우 온보딩이 아닌 홈으로 돌아갑니다.
    var protectedPairFromHome by remember { mutableStateOf(false) }
    fun openProtectedPairFromHome() { protectedPairFromHome = true; currentScreen = "protected_pair" }
    var manualReturnTo by remember { mutableStateOf("home") }
    var permissionsReturnTo by remember { mutableStateOf("home") }
    var backendReturnTo by remember { mutableStateOf("settings") }
    var guardianPreviewReturnTo by remember { mutableStateOf("home") }
    val warningOverlay = remember(context) { WarningOverlay(context.applicationContext) }
    val previewScope = rememberCoroutineScope()
    // 실제 서버 연결 여부(피보호자 홈 표시용). 마지막으로 확인한 값을 먼저 보여주고, 홈에 올 때마다 서버에서 다시 확인합니다.
    var serverGuardianLinked by remember {
        mutableStateOf(if (prefs.contains("server_guardian_linked")) prefs.getBoolean("server_guardian_linked", false) else null)
    }
    LaunchedEffect(currentScreen) {
        if (currentScreen != "home" || currentRole != "protected" ||
            DemoPairing.enabled(context) || !PairingIntegration.isConfigured) return@LaunchedEffect
        runCatching {
            PairingIntegration.backend.connectedGuardianIds(
                com.hackathon.ansimmessagepractice.guardian.AnsimDeviceId.value(context, "protected"))
        }.onSuccess { ids ->
            serverGuardianLinked = ids.isNotEmpty()
            prefs.edit().putBoolean("server_guardian_linked", ids.isNotEmpty()).apply()
        }
    }
    var previewSerial by remember { mutableIntStateOf(0) }
    DisposableEffect(warningOverlay) { onDispose { warningOverlay.dismiss() } }

    LaunchedEffect(launcherOpenKey) {
        if (launcherOpenKey > 0) { activeWarning = null; currentScreen = "splash" }
    }
    // 프로세스 복원 시 상세 화면 기록은 메모리에서 사라질 수 있습니다.
    // 실제 보호자 기록 없이 시연 데이터를 잘못 보여주지 않도록 목록으로 돌아갑니다.
    LaunchedEffect(currentScreen, selectedGuardianAlert) {
        if (currentScreen == "guardian_alert_detail" && selectedGuardianAlert == null) {
            currentScreen = "guardian_home"
        }
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
            .putBoolean("large_text_enabled", usesLargeText(selectedType))
            .putString("language", selectedLanguage)
            .putString("user_type_code", userTypeCode(selectedType))
            .putString("language_code", localizedCode(selectedLanguage))
            .putString("role", currentRole)
            .putBoolean("role_selected", true)
            .putBoolean("needs_server_sync", !DemoPairing.enabled(context)).apply()
        if (!DemoPairing.enabled(context) && BackendConfig.isConfigured(context)) previewScope.launch {
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
        // 연결 API 및 설치 ID는 SharedPreferences의 role을 읽습니다.
        // 역할 변경 중에도 선택 즉시 저장해야 잘못된 기기 ID로 요청하지 않습니다.
        prefs.edit()
            .putString("role", nextRole)
            .putBoolean("role_selected", true)
            .putBoolean("onboarded", alreadyOnboardedForRole)
            .apply()
        currentScreen = if (alreadyOnboardedForRole) {
            if (nextRole == "guardian") "guardian_home" else "home"
        } else if (nextRole == "guardian") {
            "guardian_pair"
        } else {
            "select"
        }
    }
    fun requestRoleChange() {
        changingRole = true
        currentScreen = "role_select"
    }
    val previous = when (currentScreen) {
        "role_select" -> if (changingRole) (if (prefs.getString("role", "protected") == "guardian") "guardian_home" else "home") else "start"
        "select" -> "role_select"
        "guardian_notify" -> if (guardianNotifyFromHome) "guardian_home" else "role_select"
        "guardian_pair" -> if (guardianPairFromHome) "guardian_home" else "role_select"
        "language" -> "select"
        "permission" -> "language"
        "protected_pair" -> if (protectedPairFromHome) "home" else "permission"
        "demo_approval" -> if (currentRole == "guardian") "demo_guardian_wait" else "protected_pair"
        "demo_guardian_wait" -> "guardian_pair"
        "guardian_connected_success" -> if (currentRole == "guardian") "guardian_home" else "home"
        "guardian" -> if (guardianFromHome) "guardian_manage" else if (guardianInfoAfterPair) "guardian_connected_success" else "language"
        "guardian_manage" -> "home"
        "settings", "alert_detail" -> "home"
        "guardian_preview" -> guardianPreviewReturnTo
        "guardian_alert_detail" -> "guardian_home"
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

    // 선택한 알림 방식이 '기본 알림'(큰 글씨와 소리)일 때만 전체 Compose 화면의 글꼴을 확대합니다.
    // 보호자 모드와 나머지 알림 방식은 시스템 글꼴 크기를 그대로 따릅니다.
    val systemDensity = LocalDensity.current
    val scaledDensity = remember(systemDensity, largeTextEnabled, selectedType, currentRole) {
        Density(systemDensity.density, systemDensity.fontScale * largeTextScale(selectedType, currentRole))
    }
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        when (currentScreen) {
            "splash" -> SplashScreen {
                currentRole = prefs.getString("role", "protected") ?: "protected"
                currentScreen = when {
                    // 첫 설치: 역할을 선택한 다음 해당 역할의 권한을 안내합니다.
                    !prefs.getBoolean("role_selected", false) -> "start"
                    // 온보딩 완료 후 실행: 권한 안내 반복 없이 홈으로 이동합니다.
                    prefs.getBoolean("onboarded", false) ->
                        if (currentRole == "guardian") "guardian_home" else "home"
                    // 온보딩 중 종료: 보호 대상자는 권한 단계부터 다시 시작합니다.
                    currentRole == "guardian" -> "guardian_pair"
                    else -> "select"
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
                showLater = !guardianPairFromHome,
                onBack = { currentScreen = if (guardianPairFromHome) "guardian_home" else "role_select" },
                onConnected = {
                    if (guardianPairFromHome) currentScreen = "guardian_home"
                    else currentScreen = "guardian_connected_success"
                },
                onLater = { if (guardianPairFromHome) currentScreen = "guardian_home" else finishSetup() },
                onDemoRequest = { currentScreen = "demo_guardian_wait" }
            )
            "demo_guardian_wait" -> DemoGuardianWaitingScreen(
                onShowApproval = { currentScreen = "demo_approval" },
                onLater = { if (guardianPairFromHome) currentScreen = "guardian_home" else finishSetup() }
            )
            "demo_approval" -> DemoGuardianApprovalScreen(
                onApprove = {
                    DemoPairing.approve(context)
                    currentScreen = "guardian_connected_success"
                },
                onReject = { currentScreen = if (currentRole == "guardian") "demo_guardian_wait" else "protected_pair" }
            )
            "guardian_home" -> GuardianHomeScreen(
                onInputCode = { guardianPairFromHome = true; currentScreen = "guardian_pair" },
                onNotificationSettings = { guardianNotifyFromHome = true; currentScreen = "guardian_notify" },
                onBackendSettings = { backendReturnTo = "guardian_home"; currentScreen = "backend_settings" },
                onManualCall = { manualReturnTo = "guardian_home"; currentScreen = "manual_call" },
                onChangeRole = { requestRoleChange() },
                onAlertClick = { record -> selectedGuardianAlert = record; currentScreen = "guardian_alert_detail" },
                onDemoAlertClick = { guardianPreviewReturnTo = "guardian_home"; currentScreen = "guardian_preview" }
            )
            "permission" -> PermissionScreen(
                onNextClick = { protectedPairFromHome = false; currentScreen = "protected_pair" },
                onBackClick = { currentScreen = "language" },
                onSkipClick = { protectedPairFromHome = false; currentScreen = "protected_pair" }
            )
            "protected_pair" -> com.hackathon.ansimmessagepractice.guardian.ProtectedPairingScreen(
                onBack = { currentScreen = if (protectedPairFromHome) "home" else "permission" },
                onLater = { if (protectedPairFromHome) currentScreen = "home" else finishSetup() },
                onDemoApproval = { currentScreen = "demo_approval" },
                onConnected = { currentScreen = "guardian_connected_success" }
            )
            "guardian_connected_success" -> com.hackathon.ansimmessagepractice.guardian.GuardianConnectedScreen(
                onHome = { guardianInfoAfterPair = false; finishSetup() },
                demo = DemoPairing.enabled(context),
                // 실제 연결 + 피보호자 + 아직 보호자 번호가 없을 때만 정보 입력으로 이어집니다.
                onEnterInfo = if (!DemoPairing.enabled(context) && currentRole == "protected" &&
                    !com.hackathon.ansimmessagepractice.guardian.GuardianCall.validNumber(
                        com.hackathon.ansimmessagepractice.guardian.GuardianCall.savedNumber(context))) {
                    { guardianFromHome = false; guardianInfoAfterPair = true; currentScreen = "guardian" }
                } else null
            )
            "select" -> UserTypeScreen(selectedType, { selectedType = it },
                onNextClick = { currentScreen = "language" }, onBackClick = { currentScreen = "role_select" })
            "language" -> LanguageScreen(selectedLanguage, { selectedLanguage = it },
                onNextClick = { currentScreen = "permission" },
                onBackClick = { currentScreen = "select" })
            "backend_settings" -> BackendSettingsScreen(onBack = { currentScreen = backendReturnTo })
            "permissions_settings" -> PermissionScreen(
                onNextClick = { currentScreen = permissionsReturnTo },
                onBackClick = { currentScreen = permissionsReturnTo }, fromSettings = true
            )
            "guardian" -> GuardianScreen(
                onSkipClick = {
                    if (guardianFromHome) currentScreen = "guardian_manage"
                    else { guardianInfoAfterPair = false; finishSetup() }
                },
                onBackClick = {
                    currentScreen = if (guardianFromHome) "guardian_manage"
                    else if (guardianInfoAfterPair) "guardian_connected_success" else "language"
                },
                fromHome = guardianFromHome
            )
            "guardian_manage" -> GuardianManageScreen(
                onBack = { currentScreen = "home" },
                // 시연 모드에서는 '보호자 변경/등록'이 새 연결 코드(7p)를 받는 흐름입니다.
                onEdit = {
                    if (DemoPairing.enabled(context)) openProtectedPairFromHome()
                    else { guardianFromHome = true; currentScreen = "guardian" }
                }
            )
            "home" -> HomeScreen(
                selectedType = selectedType, selectedLanguage = selectedLanguage,
                guardianConnected = if (DemoPairing.enabled(context)) null else serverGuardianLinked,
                onTestWarningClick = { selectedRisk -> risk = selectedRisk; activeWarning = null; currentScreen = "warning" },
                onSettingsClick = { currentScreen = "settings" },
                onPermissionClick = { permissionsReturnTo = "home"; currentScreen = "permissions_settings" },
                // 연결된 보호자가 있으면 '관리'(15p), 시연 모드에서 아직 없으면 연결 코드(7p)로 갑니다.
                onGuardianManageClick = {
                    if (DemoPairing.enabled(context) && !DemoPairing.approved(context) &&
                        !com.hackathon.ansimmessagepractice.guardian.GuardianCall.validNumber(
                            com.hackathon.ansimmessagepractice.guardian.GuardianCall.savedNumber(context))) openProtectedPairFromHome()
                    else { guardianFromHome = true; currentScreen = "guardian_manage" }
                },
                onManualCallClick = { manualReturnTo = "home"; currentScreen = "manual_call" },
                onGuardianPreviewClick = { guardianPreviewReturnTo = "home"; currentScreen = "guardian_preview" },
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
                onBackClick = { currentScreen = guardianPreviewReturnTo },
                onManualNumberClick = { manualReturnTo = "guardian_preview"; currentScreen = "manual_call" }
            )
            "guardian_alert_detail" -> GuardianAlertScreen(
                onBackClick = { currentScreen = "guardian_home" },
                onManualNumberClick = { manualReturnTo = "guardian_alert_detail"; currentScreen = "manual_call" },
                record = selectedGuardianAlert
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
                        .putBoolean("large_text_enabled", usesLargeText(type))
                        .putString("language", language)
                        .putString("user_type_code", userTypeCode(type))
                        .putString("language_code", localizedCode(language))
                        .putBoolean("needs_server_sync", !DemoPairing.enabled(context)).apply()
                    if (!DemoPairing.enabled(context) && BackendConfig.isConfigured(context)) previewScope.launch {
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
                    onRegisterGuardianClick = {
                        if (DemoPairing.enabled(context)) openProtectedPairFromHome()
                        else { guardianFromHome = true; currentScreen = "guardian_manage" }
                    },
                    onRetryClick = {
                        Toast.makeText(context, "판정 서버 연결 후 다시 확인할 수 있어요.", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    } // CompositionLocalProvider
}