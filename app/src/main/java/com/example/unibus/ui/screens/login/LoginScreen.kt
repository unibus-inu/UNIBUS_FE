package com.example.unibus.ui.screens.login

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

// 하드코딩된 색상 정의(val UnibusBlue) 삭제함 -> Theme에서 가져옴

@Composable
fun LoginScreen(
    onSignupClick: () -> Unit = {},
    onFindPasswordClick: () -> Unit = {}
) {
    // 입력값 상태 관리
    var idText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Scaffold(
        // Theme.kt에서 설정한 background 색상(White) 사용
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
            // 상단 여백
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
            // 아이디 입력
            OutlinedTextField(
                value = idText,
                onValueChange = { idText = it },
                label = { Text("이메일") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                // 텍스트 필드 색상도 테마의 Primary(파란색)를 따라감
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 비밀번호 입력
            OutlinedTextField(
                value = passwordText,
                onValueChange = { passwordText = it },
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

            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. 로그인 버튼 ---
            Button(
                onClick = {
                    // TODO: 서버 로그인 API 연동
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                // Theme.kt의 primary 색상(UnibusBlue) 사용
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "로그인",
                    // Type.kt에서 정의한 버튼용 폰트 스타일(Bold, 16sp) 사용
                    style = MaterialTheme.typography.labelLarge,
                    // Theme.kt의 onPrimary 색상(White) 사용
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
    // 프리뷰에서도 테마를 확인하려면 UNIBUSTheme로 감싸주는 것이 좋습니다.
    // (MainActivity에서는 이미 감싸져 있으니 실제 앱에선 상관없습니다)
    com.example.unibus.ui.theme.UNIBUSTheme {
        LoginScreen(onSignupClick = {})
    }
}