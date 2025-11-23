package com.example.unibus.ui.screens.findpassword

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.unibus.ui.theme.UNIBUSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindPasswordScreen(
    onNavigateBack: () -> Unit = {}
) {
    // --- 상태 관리 ---
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    // 인증/에러 상태
    var isVerified by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) } // 👈 [추가] 불일치 에러 상태

    // 비밀번호 변경 상태
    var newPassword by remember { mutableStateOf("") }
    var newPasswordCheck by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isPasswordCheckVisible by remember { mutableStateOf(false) }

    // 비밀번호 유효성 검사
    val isPasswordValid = remember(newPassword) {
        newPassword.any { it.isLetter() } && newPassword.any { it.isDigit() } && newPassword.length >= 8
    }
    val isPasswordMatch = newPassword == newPasswordCheck

    // 텍스트 필드 스타일
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary,
        errorBorderColor = MaterialTheme.colorScheme.error, // 에러 시 빨간 테두리
        errorLabelColor = MaterialTheme.colorScheme.error
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "비밀번호 찾기",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 안내 문구
            Text(
                text = if (isVerified) "새로운 비밀번호를 설정해 주세요."
                else "가입 시 등록한 이름과 이메일을 입력해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(10.dp))

            // --- 1단계: 신원 확인 ---
            if (!isVerified) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false // 다시 입력하면 에러 메시지 끄기
                    },
                    label = { Text("이름") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = textFieldColors,
                    isError = isError // 에러 시 빨간 테두리
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        isError = false
                    },
                    label = { Text("이메일") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = textFieldColors,
                    isError = isError
                )

                // 🚨 [추가] 에러 메시지 표시 영역
                if (isError) {
                    Text(
                        text = "입력하신 정보가 일치하지 않습니다.\n다시 확인해 주세요.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // 확인 버튼
                Button(
                    onClick = {
                        // [테스트 로직] 이름이 "유니", 이메일이 "uni" 일 때만 성공으로 가정
                        // 나중에 실제 API 연동 시 이 부분을 교체하면 됩니다.
                        if (name == "유니" && email == "uni") {
                            isVerified = true
                            isError = false
                        } else {
                            isVerified = false
                            isError = true // 불일치 시 에러 표시
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = name.isNotEmpty() && email.isNotEmpty()
                ) {
                    Text("확인", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }

            // --- 2단계: 비밀번호 재설정 (신원 확인 완료 시 표시) ---
            if (isVerified) {
                // 새 비밀번호 입력
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("새 비밀번호") },
                    placeholder = { Text("영문, 숫자 조합 8자 이상") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = textFieldColors,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = "비밀번호 보이기")
                        }
                    },
                    isError = newPassword.isNotEmpty() && !isPasswordValid,
                    supportingText = {
                        if (newPassword.isNotEmpty() && !isPasswordValid) {
                            Text("영문과 숫자를 포함하여 8자 이상 입력해주세요.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                // 새 비밀번호 확인
                OutlinedTextField(
                    value = newPasswordCheck,
                    onValueChange = { newPasswordCheck = it },
                    label = { Text("새 비밀번호 확인") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = textFieldColors,
                    visualTransformation = if (isPasswordCheckVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    trailingIcon = {
                        val image = if (isPasswordCheckVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { isPasswordCheckVisible = !isPasswordCheckVisible }) {
                            Icon(imageVector = image, contentDescription = "비밀번호 보이기")
                        }
                    },
                    isError = newPasswordCheck.isNotEmpty() && !isPasswordMatch,
                    supportingText = {
                        if (newPasswordCheck.isNotEmpty() && !isPasswordMatch) {
                            Text("비밀번호가 일치하지 않습니다.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                val isFormValid = isPasswordValid && isPasswordMatch
                Button(
                    onClick = {
                        onNavigateBack() // 비밀번호 변경 후 복귀
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = Color.Gray
                    )
                ) {
                    Text(
                        text = "비밀번호 변경하기",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isFormValid) Color.White else Color.LightGray
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FindPasswordScreenPreview() {
    UNIBUSTheme {
        FindPasswordScreen()
    }
}