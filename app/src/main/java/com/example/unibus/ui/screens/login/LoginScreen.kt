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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unibus.R

// UNIBUS 브랜드 컬러
val UnibusBlue = Color(0xFF2979FF)

@Composable
fun LoginScreen( onSignupClick: () -> Unit ={} ) {
    // 입력값 상태 관리
    var idText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Color.White
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
            // 상단 여백 조금 줄임 (로고가 하나 빠졌으므로)
            Spacer(modifier = Modifier.height(80.dp))

            // --- 1. 로고 영역 (텍스트 로고만 유지) ---
            // 버스 아이콘(unibus_logo) 제거됨
            Image(
                painter = painterResource(id = R.drawable.unibus_text), // 텍스트 로고
                contentDescription = "Unibus Text",
                modifier = Modifier.width(160.dp), // 크기 살짝 키움
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(60.dp))

            // --- 2. 입력 필드 영역 ---
            // 아이디 입력
            OutlinedTextField(
                value = idText,
                onValueChange = { idText = it },
                label = { Text("아이디") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
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
                singleLine = true
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
                colors = ButtonDefaults.buttonColors(containerColor = UnibusBlue)
            ) {
                Text(
                    text = "로그인",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
                TextButton(onClick = { /* TODO: 비번 찾기 이동 */ }) {
                    Text("비밀번호 찾기", color = Color.Gray)
                }
            }

            // 소셜 로그인 버튼 영역 제거됨

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(onSignupClick = {})
}