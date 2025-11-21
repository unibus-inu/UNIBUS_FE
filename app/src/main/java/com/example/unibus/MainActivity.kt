package com.example.unibus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
// ★ 아래 Import들이 꼭 있어야 'by remember'와 상태 변경이 작동합니다.
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.unibus.ui.screens.findpassword.FindPasswordScreen
import com.example.unibus.ui.screens.home.BusInfo
import com.example.unibus.ui.screens.home.MainHomeScreen
import com.example.unibus.ui.screens.login.LoginScreen
import com.example.unibus.ui.screens.notifications.NotificationScreen
import com.example.unibus.ui.screens.prediction.PredictionScreen
import com.example.unibus.ui.screens.profile.EditProfileScreen
import com.example.unibus.ui.screens.profile.WithdrawalScreen
import com.example.unibus.ui.screens.signup.SignupScreen
import com.example.unibus.ui.theme.UNIBUSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UNIBUSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // 알림 상태 관리
                    var hasNewNotifications by remember { mutableStateOf(true) }

                    // ★ [핵심] 모드 상태 전역 관리 (반드시 'var'로 선언해야 바꿀 수 있습니다)
                    var isGoingToSchool by remember { mutableStateOf(false) }

                    // 지도 경로 상태 관리
                    var mainSelectedBus by remember { mutableStateOf<BusInfo?>(null) }

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
                                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                                },
                                onNavigateToWithdraw = { navController.navigate("withdrawal") },

                                // 알림 관련
                                hasNewNotifications = hasNewNotifications,
                                onNavigateToNotifications = { navController.navigate("notifications") },

                                // 예측 화면 이동
                                onNavigateToPrediction = { navController.navigate("prediction") },

                                // ★ 모드 상태 전달 및 설정 (람다 파라미터 이름을 newMode로 하여 충돌 방지)
                                isGoingToSchool = isGoingToSchool,
                                onModeChange = { newMode -> isGoingToSchool = newMode },

                                // 지도 경로 상태 전달 및 설정
                                initialSelectedBus = mainSelectedBus,
                                onSetSelectedBus = { bus -> mainSelectedBus = bus }
                            )
                        }

                        // 5. 회원정보 수정 화면
                        composable("edit_profile") {
                            EditProfileScreen(onBackClick = { navController.popBackStack() })
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

                        // 7. 알림 메시지함 화면
                        composable("notifications") {
                            NotificationScreen(
                                onBackClick = {
                                    hasNewNotifications = false
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 8. 예측 화면
                        composable("prediction") {
                            PredictionScreen(
                                onBackClick = { navController.popBackStack() },
                                // ★ 여기서도 모드 상태 전달
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