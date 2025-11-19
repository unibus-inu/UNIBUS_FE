package com.example.unibus.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unibus.ui.theme.UnibusBlue
import com.example.unibus.ui.theme.White

// --- 더미 데이터 ---
data class ArrivalNotification(
    val id: Int,
    val stationName: String,
    val busNumber: String,
    val isRead: Boolean = false,
    val time: String
)

val mockNotifications = listOf(
    ArrivalNotification(1, "정문 앞 정류장", "셔틀 A", false, "5분 전"),
    ArrivalNotification(2, "도서관 입구", "3001번", false, "10분 전"),
    ArrivalNotification(3, "역전 공영 주차장", "셔틀 B", true, "어제"),
    ArrivalNotification(4, "후문 정류장", "11-1번", true, "어제"),
)
// --- /더미 데이터 ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackClick: () -> Unit
) {
    // 실제 앱에서는 여기서 서버에서 알림 리스트를 불러옵니다.
    // 여기서는 mockNotifications를 사용합니다.
    var notifications by remember { mutableStateOf(mockNotifications) }

    Scaffold(
        containerColor = White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("알림 메시지함", fontWeight = FontWeight.Bold) },
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
        ) {
            if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("도착 알림이 없습니다.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(notifications) { notification ->
                        NotificationItem(notification = notification)
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: ArrivalNotification) {
    // 읽지 않은 알림은 배경색을 연하게 강조
    val backgroundColor = if (notification.isRead) White else Color(0xFFE3F2FD) // 연한 하늘색

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 버스 아이콘
        Icon(
            imageVector = Icons.Rounded.DirectionsBus,
            contentDescription = null,
            tint = UnibusBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))

        // 메시지 내용
        Column(modifier = Modifier.weight(1f)) {
            // [정류장] [버스]
            Text(
                text = "${notification.stationName} (${notification.busNumber})",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 도착 메시지
            Text(
                text = "잠시 후 도착합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (notification.isRead) Color.Gray else UnibusBlue // 읽지 않았으면 파란색 강조
            )
        }

        // 알림 시간
        Text(
            text = notification.time,
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray
        )
    }
}