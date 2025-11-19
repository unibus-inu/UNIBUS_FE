package com.example.unibus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.unibus.ui.screens.findpassword.FindPasswordScreen
import com.example.unibus.ui.screens.home.MainHomeScreen
import com.example.unibus.ui.screens.login.LoginScreen
import com.example.unibus.ui.screens.profile.EditProfileScreen
import com.example.unibus.ui.screens.profile.WithdrawalScreen
import com.example.unibus.ui.screens.notifications.NotificationScreen // 추가된 알림 화면
import com.example.unibus.ui.screens.signup.SignupScreen
import com.example.unibus.ui.theme.UNIBUSTheme
import com.example.unibus.ui.screens.prediction.PredictionScreen

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

                    // ★ [수정] hasNewNotifications 변수를 이 위치에 선언해야 참조 가능합니다.
                    var hasNewNotifications by remember { mutableStateOf(true) }

                    NavHost(navController = navController, startDestination = "login") {

                        // 1. 로그인 화면
                        composable("login") {
                            LoginScreen(
                                onSignupClick = { navController.navigate("signup") },
                                onFindPasswordClick = { navController.navigate("find_password") },
                                onLoginSuccess = {
                                    // 로그인 성공 시 알림 상태 초기화 (옵션)
                                    hasNewNotifications = true
                                    navController.navigate("main_home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. 회원가입 화면
                        composable("signup") {
                            SignupScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 3. 비밀번호 찾기 화면
                        composable("find_password") {
                            FindPasswordScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 4. 메인 홈 화면 (알림 상태 전달)
                        composable("main_home") {
                            MainHomeScreen(
                                onNavigateToEditProfile = {
                                    navController.navigate("edit_profile")
                                },
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onNavigateToWithdraw = {
                                    navController.navigate("withdrawal")
                                },
                                // 알림 상태 전달
                                hasNewNotifications = hasNewNotifications,
                                onNavigateToNotifications = {
                                    navController.navigate("notifications")
                                },
                                onNavigateToPrediction = { navController.navigate("prediction") }
                            )
                        }

                        // 5. 회원정보 수정 화면
                        composable("edit_profile") {
                            EditProfileScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        // 6. 회원탈퇴 화면
                        composable("withdrawal") {
                            WithdrawalScreen(
                                onBackClick = { navController.popBackStack() },
                                onWithdrawConfirm = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 7. 알림 메시지함 화면 (알림 상태 업데이트)
                        composable("notifications") {
                            NotificationScreen(
                                onBackClick = {
                                    // 뒤로가기 시 알림 상태를 false로 업데이트 (빨간 점 사라짐)
                                    hasNewNotifications = false
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("prediction") {
                            PredictionScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}