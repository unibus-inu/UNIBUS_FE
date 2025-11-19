package com.example.unibus.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unibus.ui.theme.UnibusBlue
import com.example.unibus.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // 상태 관리
    var nickname by remember { mutableStateOf("김유니") } // 초기 닉네임
    var hasProfileImage by remember { mutableStateOf(false) } // 프로필 사진 유무

    Scaffold(
        containerColor = White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("회원정보 수정", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // --- 1. 프로필 사진 영역 ---
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.size(120.dp)
            ) {
                // 프로필 이미지 (또는 기본 아이콘)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F5))
                        .border(1.dp, Color.LightGray, CircleShape)
                        .clickable {
                            // TODO: 갤러리 열기 로직
                            hasProfileImage = !hasProfileImage // (테스트용) 클릭 시 이미지 상태 토글
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (hasProfileImage) {
                        // 실제 이미지가 있을 때 (여기선 임시로 색상으로 표시)
                        Box(modifier = Modifier.fillMaxSize().background(Color.Gray))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "기본 프로필",
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                    }
                }

                // 카메라 아이콘 (편집 배지)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(UnibusBlue)
                        .border(2.dp, White, CircleShape)
                        .clickable { /* 갤러리 열기 */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "사진 변경",
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 사진 삭제 버튼 (사진이 있을 때만 표시)
            if (hasProfileImage) {
                TextButton(
                    onClick = { hasProfileImage = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("프로필 사진 삭제")
                }
            } else {
                // 공간 유지를 위한 투명 박스
                Spacer(modifier = Modifier.height(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. 닉네임 수정 영역 ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "닉네임",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { if (it.length <= 10) nickname = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UnibusBlue,
                        cursorColor = UnibusBlue,
                        focusedLabelColor = UnibusBlue
                    ),
                    trailingIcon = {
                        Text(
                            text = "${nickname.length}/10",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )
                Text(
                    text = "한글, 영문, 숫자 포함 2-10자",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f)) // 남은 공간 밀어내기

            // --- 3. 저장 버튼 ---
            Button(
                onClick = {
                    // TODO: 서버에 변경사항 저장
                    onBackClick() // 저장 후 뒤로가기
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UnibusBlue)
            ) {
                Text(
                    text = "저장하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}