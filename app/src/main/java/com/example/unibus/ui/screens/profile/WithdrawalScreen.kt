package com.example.unibus.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unibus.ui.theme.UnibusBlue
import com.example.unibus.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalScreen(
    onBackClick: () -> Unit,
    onWithdrawConfirm: () -> Unit // 탈퇴 완료 시 실행할 함수
) {
    // 사용자 입력 상태 관리
    var inputText by remember { mutableStateOf("") }
    // 입력값이 "회원탈퇴"와 정확히 일치하는지 확인
    val isEnabled = inputText == "회원탈퇴"

    Scaffold(
        containerColor = White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("회원탈퇴", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // 경고 문구
            Text(
                text = "정말 탈퇴하시겠습니까?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "탈퇴 시 계정 정보와 모든 이용 기록이\n삭제되며 복구할 수 없습니다.",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 안내 및 입력창
            Text(
                text = "탈퇴를 원하시면 아래에\n'회원탈퇴'를 입력해주세요.",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = UnibusBlue
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("회원탈퇴") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = UnibusBlue,
                    cursorColor = UnibusBlue,
                    focusedContainerColor = Color(0xFFFAFAFA),
                    unfocusedContainerColor = Color(0xFFFAFAFA)
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // 탈퇴 버튼 (조건 불충족 시 비활성화)
            Button(
                onClick = onWithdrawConfirm,
                enabled = isEnabled, // "회원탈퇴"를 입력해야만 클릭 가능
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error, // 위험한 작업이므로 빨간색
                    disabledContainerColor = Color(0xFFFFCDD2) // 비활성화 색상 (연한 빨강)
                )
            ) {
                Text(
                    text = "탈퇴하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}