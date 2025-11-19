package com.example.unibus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 기본적으로 안드로이드 시스템 폰트(Roboto/San-serif)를 사용합니다.
// 나중에 'Pretendard'나 'Noto Sans KR' 같은 폰트 파일을 추가하면 여기서 변경 가능합니다.
val Typography = Typography(
    // 화면 제목 (예: 로그인, 회원가입 타이틀)
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    // 소제목 (예: 입력폼 라벨)
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    // 본문 텍스트 (일반 내용)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // 작은 텍스트 (설명, 플레이스홀더)
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    // 버튼 텍스트
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp, // 버튼 글씨는 조금 큼직하게
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)