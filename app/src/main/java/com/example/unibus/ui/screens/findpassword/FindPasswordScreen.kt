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
import androidx.compose.ui.unit.sp
import com.example.unibus.ui.theme.UNIBUSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindPasswordScreen(
    onNavigateBack: () -> Unit = {}
) {
    // --- 상태 관리 ---
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    // 인증 프로세스 상태
    var isCodeSent by remember { mutableStateOf(false) }    // 1. 인증번호 발송 여부
    var verificationCode by remember { mutableStateOf("") } // 2. 입력한 인증번호
    var isVerified by remember { mutableStateOf(false) }    // 3. 인증 완료 여부 (true면 비번 변경 화면 표시)

    // 비밀번호 변경 상태 (회원가입과 동일 로직)
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
        cursorColor = MaterialTheme.colorScheme.primary
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

            // 안내 문구 (상태에 따라 변경)
            Text(
                text = if (isVerified) "새로운 비밀번호를 설정해 주세요."
                else "가입 시 등록한 이름과 이메일을 입력해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(10.dp))

            // --- 1단계: 기본 정보 입력 (인증 완료되면 숨기거나 비활성화) ---
            if (!isVerified) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("이름") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isCodeSent, // 코드 보내면 수정 불가
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("이메일") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isCodeSent,
                    colors = textFieldColors
                )

                // 인증번호 발송 버튼
                if (!isCodeSent) {
                    Button(
                        onClick = {
                            // TODO: 실제 이메일 발송 API 호출
                            isCodeSent = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("인증번호 발송", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }

            // --- 2단계: 인증번호 입력 (발송됨 && 아직 인증 안됨) ---
            if (isCodeSent && !isVerified) {
                Text(
                    text = "이메일로 발송된 인증번호 6자리를 입력해주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            verificationCode = it
                        }
                    },
                    label = { Text("인증번호 6자리") },
                    placeholder = { Text("123456") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = textFieldColors
                )

                // 인증 확인 버튼
                Button(
                    onClick = {
                        // TODO: 실제 서버 인증 API 호출
                        // 임시 테스트: 123456 입력 시 성공 처리
                        if (verificationCode == "123456") {
                            isVerified = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = verificationCode.length == 6
                ) {
                    Text("인증 확인", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }

            // --- 3단계: 비밀번호 재설정 (인증 완료 시 표시) ---
            if (isVerified) {
                // 새 비밀번호
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

                // 비밀번호 변경 완료 버튼
                val isFormValid = isPasswordValid && isPasswordMatch
                Button(
                    onClick = {
                        // TODO: 비밀번호 변경 API 호출 -> 성공 시 로그인 화면으로 이동
                        onNavigateBack() // 예시: 변경 후 로그인 화면으로 복귀
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = Color.Gray
                    )
                ) {
                    Text("비밀번호 변경하기", style = MaterialTheme.typography.labelLarge, color = if (isFormValid) Color.White else Color.LightGray)
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