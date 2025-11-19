package com.example.unibus.ui.screens.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
fun SignupScreen(
    onNavigateBack: () -> Unit = {}
) {
    // --- 상태 관리 ---
    var nickname by remember { mutableStateOf("") }

    // 이메일 관련 상태
    var emailId by remember { mutableStateOf("") }
    var emailDomain by remember { mutableStateOf("inu.ac.kr") } // 기본값 학교 도메인
    var isDomainExpanded by remember { mutableStateOf(false) }
    val domainList = listOf("inu.ac.kr", "gmail.com", "naver.com", "직접입력")

    // 비밀번호 관련 상태
    var password by remember { mutableStateOf("") }
    var passwordCheck by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isPasswordCheckVisible by remember { mutableStateOf(false) }

    // 비밀번호 유효성 검사 (영문 + 숫자 포함)
    val isPasswordValid = remember(password) {
        password.any { it.isLetter() } && password.any { it.isDigit() } && password.length >= 8
    }

    // 비밀번호 일치 여부
    val isPasswordMatch = password == passwordCheck

    // 스크롤 상태
    val scrollState = rememberScrollState()

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
                        "회원가입",
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
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // --- 1. 닉네임 입력 ---
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("이름") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = textFieldColors
            )

            // --- 2. 이메일 입력 (ID + 도메인 드롭다운) ---
            Column {
                Text(
                    text = "이메일 (학교 인증)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 이메일 ID 입력창
                    OutlinedTextField(
                        value = emailId,
                        onValueChange = { emailId = it },
                        placeholder = { Text("example") },
                        modifier = Modifier.weight(1f), // 남은 공간 차지
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = textFieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Text(
                        text = "@",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )

                    // 도메인 선택 드롭다운 (Box로 감싸서 처리)
                    Box(
                        modifier = Modifier.weight(1f) // ID 입력창과 1:1 비율 (조절 가능)
                    ) {
                        OutlinedTextField(
                            value = emailDomain,
                            onValueChange = {
                                // '직접입력'일 경우만 수정 가능하도록 처리할 수도 있음
                                if (emailDomain == "직접입력" || !domainList.contains(emailDomain)) {
                                    emailDomain = it
                                }
                            },
                            readOnly = domainList.contains(emailDomain) && emailDomain != "직접입력", // 목록에 있으면 읽기 전용
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "도메인 선택",
                                    modifier = Modifier.clickable { isDomainExpanded = true }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = textFieldColors
                        )

                        // 드롭다운 메뉴
                        DropdownMenu(
                            expanded = isDomainExpanded,
                            onDismissRequest = { isDomainExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            domainList.forEach { domain ->
                                DropdownMenuItem(
                                    text = { Text(domain) },
                                    onClick = {
                                        emailDomain = if (domain == "직접입력") "" else domain
                                        isDomainExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. 비밀번호 입력 ---
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호") },
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
                isError = password.isNotEmpty() && !isPasswordValid,
                supportingText = {
                    if (password.isNotEmpty() && !isPasswordValid) {
                        Text("영문과 숫자를 포함하여 8자 이상 입력해주세요.", color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            // --- 4. 비밀번호 확인 ---
            OutlinedTextField(
                value = passwordCheck,
                onValueChange = { passwordCheck = it },
                label = { Text("비밀번호 확인") },
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
                isError = passwordCheck.isNotEmpty() && !isPasswordMatch,
                supportingText = {
                    if (passwordCheck.isNotEmpty() && !isPasswordMatch) {
                        Text("비밀번호가 일치하지 않습니다.", color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            Spacer(modifier = Modifier.height(30.dp))

            // --- 5. 가입하기 버튼 ---
            // 모든 조건이 충족되어야 버튼 활성화 (선택사항)
            val isFormValid = nickname.isNotBlank() && emailId.isNotBlank() && isPasswordValid && isPasswordMatch

            Button(
                onClick = {
                    // TODO: 최종 회원가입 데이터 전송 (이메일: $emailId@$emailDomain)
                    val fullEmail = "$emailId@$emailDomain"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = isFormValid, // 조건 불만족 시 비활성화
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(
                    text = "가입하기",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isFormValid) MaterialTheme.colorScheme.onPrimary else Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignupScreenPreview() {
    UNIBUSTheme {
        SignupScreen()
    }
}