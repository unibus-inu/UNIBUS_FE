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
import com.example.unibus.ui.screens.findpassword.FindPasswordScreen
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
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "login") {

                        // 1. 로그인 화면
                        composable("login") {
                            LoginScreen(
                                onSignupClick = {
                                    navController.navigate("signup")
                                }, // 콤마(,) 주의!
                                onFindPasswordClick = {
                                    navController.navigate("find_password")
                                }
                            )
                        }

                        // 2. 회원가입 화면
                        composable("signup") {
                            SignupScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 3. 비밀번호 찾기 화면
                        composable("find_password") {
                            FindPasswordScreen(
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