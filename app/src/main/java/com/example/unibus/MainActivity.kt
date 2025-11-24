package com.example.unibus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf // [추가] 리스트 관리를 위해 필요
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.unibus.data.AuthTokenStore
import com.example.unibus.ui.screens.findpassword.FindPasswordScreen
import com.example.unibus.ui.screens.home.BusInfo
import com.example.unibus.ui.screens.home.MainHomeScreen
import com.example.unibus.ui.screens.login.LoginScreen
import com.example.unibus.ui.screens.notifications.NotificationScreen
import com.example.unibus.ui.screens.notifications.NotificationSettingsScreen // [추가] 알림 설정 화면
import com.example.unibus.ui.screens.notifications.NotificationTarget // [추가] 데이터 모델
import com.example.unibus.ui.screens.prediction.PredictionScreen
import com.example.unibus.ui.screens.profile.EditProfileScreen
import com.example.unibus.ui.screens.profile.ProfileViewModel
import com.example.unibus.ui.screens.profile.WithdrawalScreen
import com.example.unibus.ui.screens.signup.SignupScreen
import com.example.unibus.ui.theme.UNIBUSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 앱 전체에서 공유할 ViewModel
        val sharedViewModel: ProfileViewModel by viewModels()

        setContent {
            UNIBUSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // ---------------------------------------------------------
                    // [상태 관리] 앱 전역에서 유지되어야 할 데이터들
                    // ---------------------------------------------------------

                    // 1. 알림 뱃지 상태
                    var hasNewNotifications by remember { mutableStateOf(true) }

                    // 2. 등/하교 모드 상태
                    var isGoingToSchool by remember { mutableStateOf(false) }

                    // 3. 지도 경로(선택된 버스) 상태
                    var mainSelectedBus by remember { mutableStateOf<BusInfo?>(null) }

                    // 4. [신규] 알림 설정된 버스 목록 (화면 이동해도 유지됨)
                    val sharedMonitoredList = remember { mutableStateListOf<NotificationTarget>() }
                    // ---------------------------------------------------------

                    NavHost(navController = navController, startDestination = "login") {

                        // 1. 로그인 화면
                        composable("login") {
                            LoginScreen(
                                onSignupClick = { navController.navigate("signup") },
                                onFindPasswordClick = { navController.navigate("find_password") },
                                onLoginSuccess = {
                                    hasNewNotifications = true
                                    mainSelectedBus = null
                                    isGoingToSchool = false
                                    navController.navigate("main_home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. 회원가입 화면
                        composable("signup") {
                            SignupScreen(onNavigateBack = { navController.popBackStack() })
                        }

                        // 3. 비밀번호 찾기 화면
                        composable("find_password") {
                            FindPasswordScreen(onNavigateBack = { navController.popBackStack() })
                        }

                        // 4. 메인 홈 화면
                        composable("main_home") {
                            MainHomeScreen(
                                onNavigateToEditProfile = { navController.navigate("edit_profile") },
                                onLogout = {
                                    AuthTokenStore.clear()
                                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                                },
                                onNavigateToWithdraw = { navController.navigate("withdrawal") },

                                // 알림 화면 이동
                                hasNewNotifications = hasNewNotifications,
                                onNavigateToNotifications = { navController.navigate("notifications") },

                                // [신규] 알림 설정 관리 화면으로 이동
                                onNavigateToNotificationSettings = { navController.navigate("notification_settings") },

                                // 예측 화면 이동
                                onNavigateToPrediction = { navController.navigate("prediction") },

                                // 모드 상태 전달
                                isGoingToSchool = isGoingToSchool,
                                onModeChange = { newMode -> isGoingToSchool = newMode },

                                // 지도 경로 상태 전달
                                initialSelectedBus = mainSelectedBus,
                                onSetSelectedBus = { bus -> mainSelectedBus = bus },

                                // 공유 ViewModel 전달
                                viewModel = sharedViewModel,

                                // [신규] 공유된 알림 리스트 전달
                                sharedMonitoredList = sharedMonitoredList
                            )
                        }

                        // 5. 회원정보 수정 화면
                        composable("edit_profile") {
                            EditProfileScreen(
                                onBackClick = { navController.popBackStack() },
                                viewModel = sharedViewModel
                            )
                        }

                        // 6. 회원탈퇴 화면
                        composable("withdrawal") {
                            WithdrawalScreen(
                                onBackClick = { navController.popBackStack() },
                                onWithdrawConfirm = {
                                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                                }
                            )
                        }

                        // 7. 알림 메시지함 화면 (도착 알림 내역)
                        composable("notifications") {
                            NotificationScreen(
                                onBackClick = {
                                    hasNewNotifications = false
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 8. [신규] 알림 설정 관리 화면 (목록 확인 및 삭제)
                        composable("notification_settings") {
                            NotificationSettingsScreen(
                                monitoredList = sharedMonitoredList, // 공유된 리스트 전달
                                onRemoveNotification = { target ->
                                    sharedMonitoredList.remove(target) // 삭제 로직 수행
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        // 9. 예측 화면
                        composable("prediction") {
                            PredictionScreen(
                                onBackClick = { navController.popBackStack() },
                                isGoingToSchool = isGoingToSchool,
                                onMapClick = { busInfo ->
                                    mainSelectedBus = busInfo
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}