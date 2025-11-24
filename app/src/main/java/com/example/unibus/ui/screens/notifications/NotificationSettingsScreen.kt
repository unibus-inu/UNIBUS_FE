package com.example.unibus.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unibus.ui.theme.UnibusBlue
import com.example.unibus.ui.theme.White

// [데이터 모델] 이 클래스는 MainHomeScreen과 공유해야 하므로
// 만약 중복 에러가 나면 MainHomeScreen에 있는 정의를 지우거나 하나로 통일하세요.
data class NotificationTarget(
    val busNumber: String,
    val stationId: Int,
    val stationName: String,
    var hasNotified: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    monitoredList: List<NotificationTarget>, // 메인에서 넘겨받은 알림 목록
    onRemoveNotification: (NotificationTarget) -> Unit, // 삭제 이벤트
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("알림 설정 관리", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "현재 도착 알림을 받고 있는\n버스와 정류장 목록입니다.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            if (monitoredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("설정된 알림이 없습니다.", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(monitoredList) { target ->
                        NotificationSettingItem(
                            target = target,
                            onDelete = { onRemoveNotification(target) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationSettingItem(
    target: NotificationTarget,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = target.busNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = UnibusBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (target.hasNotified) {
                    Text("도착완료", fontSize = 12.sp, color = Color.Gray)
                } else {
                    Text("알림 대기중", fontSize = 12.sp, color = Color(0xFF4CAF50))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = target.stationName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "알림 삭제",
                tint = Color.Gray
            )
        }
    }
}