package com.example.unibus.ui.screens.prediction

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.BusAlert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
// ★ [수정됨]: 중복 Import 제거 및 정리
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.shape.CircleShape // CircleShape 추가


// 이 파일의 독립 실행성을 위해 Building 데이터 클래스를 재정의합니다.
// (실제 프로젝트에서는 PredictionScreen.kt 파일에서 import하여 사용합니다.)
// 예측 결과 데이터 모델
data class PredictionResult(
    val busName: String,
    val totalRemainingSeconds: Int, // 남은 시간 (초 단위)
    val status: String, // 버스의 현재 위치/상태
    val color: Color // 버스 노선 색상
)

// UI에서 사용할 색상 정의 (PredictionScreen.kt의 UnibusBlue와 White 대체)
private val UnibusBlue = Color(0xFF0D47A1)
private val White = Color(0xFFFFFFFF)

// --- 더미 데이터 ---
// ★ 예측 로직에 필요한 더미 시간 분해 데이터 추가
data class TimeDetail(val label: String, val durationMinutes: Int, val icon: Color)

val mockPredictionDetails = listOf(
    TimeDetail("도보 이동 (집 -> 정류장)", 8, Color(0xFF90A4AE)),
    TimeDetail("버스 대기 시간", 2, Color(0xFFFFB74D)),
    TimeDetail("버스 탑승 시간 (셔틀 A)", 25, UnibusBlue),
    TimeDetail("캠퍼스 도보 (정문 -> 건물)", 5, Color(0xFF4CAF50)),
)
// --- /더미 데이터 ---


// ---------------------------------------------------------
// Step 2: 예측 대시보드 화면 (PredictionDashboardScreen)
// ---------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionDashboardScreen(
    building: Building,
    onBackClick: () -> Unit
) {
    // 1. 실시간 현재 시간 상태
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = LocalTime.now()
        }
    }
    // ★ [수정] timeFormatter는 LocalTime.now()와 무관하므로 외부에 선언 (경고 해결)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val displayTimeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // 2. 예측 데이터 목록 (카운트다운용)
    val initialPredictions = remember {
        listOf(
            PredictionResult("M6724 (강남)", 185, "이전 정류장 출발", Color(0xFFE53935)),
            PredictionResult("909 (송도)", 540, "3번째 전 정류장", Color(0xFF388E3C)),
        )
    }
    val countdownPredictions = remember {
        initialPredictions.map { it.totalRemainingSeconds }.toMutableStateList()
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            for (i in countdownPredictions.indices) {
                if (countdownPredictions[i] > 0) {
                    countdownPredictions[i]--
                }
            }
        }
    }

    // 3. ★ [핵심 로직] 최종 도착 시간 및 출발 시간 계산
    val fastestBusEtaMinutes = 3
    val preBusTime = mockPredictionDetails.filter { it.label != "버스 탑승 시간 (셔틀 A)" && it.label != "캠퍼스 도보 (정문 -> 건물)" }
        .sumOf { it.durationMinutes }

    val totalBusRideAndWalk = mockPredictionDetails.filter { it.label == "버스 탑승 시간 (셔틀 A)" || it.label == "캠퍼스 도보 (정문 -> 건물)" }.sumOf { it.durationMinutes }


    // 현재 시간 + (최소 출발 시간 + 버스 대기 + 탑승 + 도보) 를 계산하여 예상 도착 시간 산출
    val arrivalTime = LocalTime.now().plusMinutes(totalBusRideAndWalk.toLong() + fastestBusEtaMinutes)
    val mustLeaveTime = arrivalTime.minusMinutes(totalBusRideAndWalk.toLong() + fastestBusEtaMinutes + preBusTime.toLong()) // 가정을 위해 단순하게 계산

    val targetTime = arrivalTime.format(displayTimeFormatter)
    val displayMustLeaveTime = mustLeaveTime.format(displayTimeFormatter)
    val totalDuration = totalBusRideAndWalk + fastestBusEtaMinutes
    val arrivalProbability = 92
    val nextAlternative = "15분 뒤 셔틀 B"

    Scaffold(
        containerColor = Color(0xFFF0F0F0), // 연한 회색 배경
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("도착 예측 분석", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "건물 선택으로 돌아가기")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 새로고침 로직 */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ★ [추가] 최종 결론 영역
            PredictionResultCard(
                building = building,
                targetTime = targetTime,
                mustLeaveTime = displayMustLeaveTime,
                totalDuration = totalDuration,
                arrivalProbability = arrivalProbability,
                nextAlternative = nextAlternative,
                details = mockPredictionDetails
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 예측 정보 리스트 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("실시간 버스 도착 현황", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.DarkGray)
            }

            // 예측 버스 목록
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(initialPredictions.indices.toList()) { index ->
                    PredictionItem(
                        prediction = initialPredictions[index],
                        remainingSeconds = countdownPredictions[index]
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------
// ★ [새로 추가] 예측 최종 결론 카드 컴포넌트 (PredictionResultCard)
// ---------------------------------------------------------
@Composable
fun PredictionResultCard(
    building: Building,
    targetTime: String,
    mustLeaveTime: String,
    totalDuration: Int,
    arrivalProbability: Int,
    nextAlternative: String,
    details: List<TimeDetail>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. 최종 결론 영역 (최소 출발 시간 & 도착 시각) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = UnibusBlue),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "놓치지 않으려면",
                    color = White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Text(
                    text = "${mustLeaveTime}에 출발해야 합니다!",
                    color = White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "예상 도착: ${building.name} ${targetTime}",
                    color = Color(0xFFFFEE58), // 노란색 경고 톤
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. 도착 시각 및 부가 정보 ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {

                // 부가 정보 Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip(
                        label = "총 예상 소요 시간",
                        value = "${totalDuration}분",
                        color = UnibusBlue
                    )
                    InfoChip(
                        label = "정시 도착 확률",
                        value = "$arrivalProbability%",
                        color = Color(0xFF4CAF50)
                    )
                    InfoChip(
                        label = "캠퍼스 도보",
                        value = "${mockPredictionDetails.last().durationMinutes}분",
                        color = Color(0xFFFFB300)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 3. 중앙 타임라인 (시간 분해 내역) ---
        Text(
            "경로 시간 분해",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp, start = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(White, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            details.forEachIndexed { index, item ->
                TimelineItem(item = item, isLast = index == details.lastIndex)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 4. 다음 최적 대안 ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFFB300))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "만약 놓칠 경우, 다음 최적 대안은 ${nextAlternative}에 있습니다.",
                    fontSize = 14.sp
                )
            }
        }
    }
}

// 부가 정보 칩
@Composable
fun InfoChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

// 타임라인 항목
@Composable
fun TimelineItem(item: TimeDetail, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // 타임라인 아이콘 및 선
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
            // 아이콘 원
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(item.icon)
            )
            // 수직선
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(30.dp)
                        .background(Color(0xFFEEEEEE))
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 내용
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(item.label, fontWeight = FontWeight.Medium, color = Color.Black)
            Text("${item.durationMinutes}분 소요", fontSize = 12.sp, color = Color.Gray)
        }
    }
}


// 개별 버스 예측 항목 Composable (PredictionItem)
@Composable
fun PredictionItem(prediction: PredictionResult, remainingSeconds: Int) {
    // 01. 남은 시간 포맷팅 로직
    val formattedTime by remember(remainingSeconds) {
        derivedStateOf {
            if (remainingSeconds <= 0) {
                "도착 완료"
            } else {
                val minutes = TimeUnit.SECONDS.toMinutes(remainingSeconds.toLong())
                val seconds = remainingSeconds % 60
                "${minutes}분 ${seconds}초"
            }
        }
    }

    // 02. 상태 텍스트 및 색상 결정
    val statusText = if (remainingSeconds <= 0) "운행 종료 또는 도착" else prediction.status
    val timeColor = when {
        remainingSeconds <= 60 -> Color(0xFFE53935) // 1분 이내 (빨강)
        remainingSeconds <= 180 -> Color(0xFFFFB300) // 3분 이내 (주황)
        else -> UnibusBlue // 그 외
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 노선 색상 바
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(prediction.color)
            )
            Spacer(modifier = Modifier.width(12.dp))

            // 버스 노선 정보
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.BusAlert,
                        contentDescription = "버스 아이콘",
                        tint = prediction.color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = prediction.busName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            // 남은 시간 정보
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "도착까지",
                    fontSize = 10.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = formattedTime,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = timeColor
                )
            }
        }
    }
}