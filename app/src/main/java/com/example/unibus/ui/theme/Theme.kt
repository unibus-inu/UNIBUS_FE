package com.example.unibus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 다크 모드 색상 팔레트
private val DarkColorScheme = darkColorScheme(
    primary = UnibusLightBlue, // 다크모드에선 조금 더 밝은 파랑이 잘 보임
    onPrimary = Black,         // 파란 버튼 위의 글씨색
    secondary = UnibusBlue,
    background = Black,
    surface = Color(0xFF121212),
    onBackground = White,
    onSurface = White
)

// 라이트 모드 색상 팔레트 (이게 메인입니다)
private val LightColorScheme = lightColorScheme(
    primary = UnibusBlue,      // 브랜드 메인 컬러
    onPrimary = White,         // 버튼 위 글씨색 (파란 버튼 + 흰 글씨)
    secondary = UnibusLightBlue,
    background = White,        // 전체 배경 흰색
    surface = White,           // 카드/상단바 배경 흰색
    onBackground = Black,      // 검은 글씨
    onSurface = Black,
    error = ErrorRed
)

@Composable
fun UNIBUSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic Color: 안드로이드 12+에서 배경화면 색을 따라가는 기능
    // false로 설정하여 우리 브랜드 색(파란색)을 강제로 유지합니다.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 상태바(Status Bar) 색상 설정
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 상태바를 투명하거나 브랜드 컬러로 설정 가능. 여기선 깔끔하게 흰색 배경에 검은 아이콘으로 설정
            window.statusBarColor = if (darkTheme) Black.toArgb() else White.toArgb()

            // 상태바 아이콘 색상 (어두운 테마면 흰 아이콘, 밝은 테마면 검은 아이콘)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}