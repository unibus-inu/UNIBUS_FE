package com.example.unibus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.unibus.ui.screens.login.LoginScreen
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
                    // 1. 네비게이션 컨트롤러 생성 (화면 이동을 담당하는 친구)
                    val navController = rememberNavController()

                    // 2. NavHost 설정 (여기서 화면들을 정의함)
                    // startDestination = "login" -> 앱 켜지면 로그인 화면부터 보여줘라
                    NavHost(navController = navController, startDestination = "login") {

                        // 화면 1: 로그인 화면
                        composable("login") {
                            LoginScreen(
                                // 로그인 화면에서 '회원가입' 버튼 누르면 -> 'signup'으로 이동해라
                                onSignupClick = {
                                    navController.navigate("signup")
                                }
                            )
                        }

                        // 화면 2: 회원가입 화면
                        composable("signup") {
                            SignupScreen(
                                // 회원가입 화면에서 '뒤로가기' 누르면 -> 뒤로 가라(popBackStack)
                                onNavigateBack = {
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