package com.example.unibus.ui.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.unibus.R

@Composable
fun LoginScreen(
    onSignupClick: () -> Unit = {},
    onFindPasswordClick: () -> Unit = {},
    onLoginSuccess: () -> Unit = {} // 로그인 성공 시 실행할 콜백 추가
) {
    // 입력값 상태 관리
    var idText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var showLoginError by remember { mutableStateOf(false) } // 에러 메시지 상태

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // --- 1. 로고 영역 ---
            Image(
                painter = painterResource(id = R.drawable.unibus_text),
                contentDescription = "Unibus Text",
                modifier = Modifier.width(160.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(60.dp))

            // --- 2. 입력 필드 영역 ---
            OutlinedTextField(
                value = idText,
                onValueChange = {
                    idText = it
                    showLoginError = false // 입력 시 에러 메시지 숨김
                },
                label = { Text("이메일") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = passwordText,
                onValueChange = {
                    passwordText = it
                    showLoginError = false // 입력 시 에러 메시지 숨김
                },
                label = { Text("비밀번호") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            // 에러 메시지 (로그인 실패 시 표시)
            AnimatedVisibility(
                visible = showLoginError,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = "아이디 또는 비밀번호를 확인해주세요.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. 로그인 버튼 ---
            Button(
                onClick = {
                    // TODO: 실제 서버 API 연동 위치
                    // 임시 로그인 로직 (테스트용)
                    if (idText == "test" && passwordText == "1234") {
                        onLoginSuccess()
                    } else {
                        showLoginError = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "로그인",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 4. 회원가입 / 비밀번호 찾기 ---
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSignupClick) {
                    Text("회원가입", color = Color.Gray)
                }
                Text(
                    text = "|",
                    color = Color.LightGray,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                TextButton(onClick = onFindPasswordClick) {
                    Text("비밀번호 찾기", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    com.example.unibus.ui.theme.UNIBUSTheme {
        LoginScreen(onSignupClick = {}, onFindPasswordClick = {}, onLoginSuccess = {})
    }
}