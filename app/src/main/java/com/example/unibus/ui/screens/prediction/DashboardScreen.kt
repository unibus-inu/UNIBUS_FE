package com.example.unibus.ui.screens.prediction

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.example.unibus.ui.screens.home.BusInfo

// ★ [수정됨] PredictionScreen과 타입을 맞추기 위해 Destination 사용 (정의는 PredictionScreen.kt에 있는 것을 공유하거나 여기서 재정의해도 됩니다. 패키지가 같으므로 여기선 제거하고 PredictionScreen의 것을 씁니다.)
// 만약 "Unresolved reference: Destination" 오류가 나면 아래 주석을 해제하세요.
// data class Destination(val id: Int, val name: String, val detail: String, val type: String)

// 예측 결과 데이터 모델
data class PredictionResult(
    val busName: String,
    val totalRemainingSeconds: Int,
    val status: String,
    val color: Color
)
// UI에서 사용할 색상 정의
private val UnibusBlue = Color(0xFF0D47A1)
private val White = Color(0xFFFFFFFF)
private val DarkGray = Color(0xFF424242)
private val LightGrayBackground = Color(0xFFEEEEEE)

// --- 더미 데이터 ---
data class TimeDetail(val label: String, val durationMinutes: Int, val icon: Color, val iconVector: androidx.compose.ui.graphics.vector.ImageVector)

val mockPredictionDetails = listOf(
    TimeDetail("정류장 도보", 8, Color(0xFF90A4AE), Icons.Rounded.DirectionsWalk),
    TimeDetail("버스 대기", 2, Color(0xFFFFB300), Icons.Rounded.DirectionsBus),
    TimeDetail("버스 탑승", 25, UnibusBlue, Icons.Rounded.DirectionsBus),
    TimeDetail("캠퍼스 도보", 5, Color(0xFF4CAF50), Icons.Rounded.DirectionsWalk),
)
// --- /더미 데이터 ---


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionDashboardScreen(
    building: Destination, // ★ [수정] 파라미터 타입을 Destination으로 변경
    onBackClick: () -> Unit,
    onMapClick: () -> Unit
) {
    // 1. 실시간 시간 관련 상태
    val displayTimeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // 2. 예측 로직 (핵심 계산)
    val totalDuration = mockPredictionDetails.sumOf { it.durationMinutes }
    val bestBus = "8번 버스"
    val busEta = 2 // 분 뒤 도착
    val arrivalProbability = 92
    val nextAlternative = "셔틀 A, 15분 뒤"

    // [최종 시간 계산]
    val arrivalTime = LocalTime.now().plusMinutes(totalDuration.toLong())
    val finalArrivalTime = arrivalTime.format(displayTimeFormatter)

    // 최소 출발 시간
    val mustLeaveTime = arrivalTime.minusMinutes(totalDuration.toLong())
    val finalMustLeaveTime = mustLeaveTime.format(displayTimeFormatter)


    Scaffold(
        containerColor = Color(0xFFF0F0F0), // 연한 회색 배경
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("${building.name} 예측 분석", fontWeight = FontWeight.Bold) }, // name 속성 사용
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
        ) {

            // --- A. 최상단 영역 (Result & Action) ---
            item {
                ResultActionCard(
                    building = building,
                    finalArrivalTime = finalArrivalTime,
                    bestBus = bestBus,
                    busEta = busEta,
                    onCloseClick = onBackClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- B. 중앙 영역 (Proof & Confidence) ---
            item {
                ProofConfidenceCard(
                    totalDuration = totalDuration,
                    arrivalProbability = arrivalProbability,
                    details = mockPredictionDetails
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- C. 하단 영역 (Alternatives & Action) ---
            item {
                ActionOnlyCard(
                    mustLeaveTime = finalMustLeaveTime,
                    nextAlternative = nextAlternative,
                    onMapClick = onMapClick
                )
            }
        }
    }
}

// ---------------------------------------------------------
// 컴포넌트 1: A. 최상단 영역 (Result & Action)
// ---------------------------------------------------------
@Composable
fun ResultActionCard(
    building: Destination, // ★ [수정] Destination 타입 사용
    finalArrivalTime: String,
    bestBus: String,
    busEta: Int,
    onCloseClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth()
        ) {
            // 1. 최종 도착 시각 (가장 크게 강조)
            Text(
                text = "${finalArrivalTime} 도착",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DarkGray
            )
            Text(
                text = "${building.name} 건물 문 앞까지 예상 시각",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 2. 추천 버스 정보
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.DirectionsBus,
                    contentDescription = null,
                    tint = UnibusBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${bestBus} 탑승 추천 (${busEta}분 뒤 도착)",
                    fontWeight = FontWeight.Bold,
                    color = UnibusBlue
                )
            }
        }
    }
}

// ... (나머지 ProofConfidenceCard, ActionOnlyCard, 보조 컴포넌트들은 기존과 동일하므로 그대로 두시면 됩니다)
// (위의 코드 블록만 덮어쓰셔도 되지만, 안전하게 파일 전체를 붙여넣는 것을 추천드립니다.
// 아래는 나머지 부분까지 포함한 전체 코드입니다.)

@Composable
fun ProofConfidenceCard(
    totalDuration: Int,
    arrivalProbability: Int,
    details: List<TimeDetail>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth()
        ) {
            Text(
                "예상 경로 상세 분석 (${totalDuration}분 소요)",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = DarkGray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 1. 예측 신뢰도 게이지
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 게이지 차트
                ConfidenceGauge(
                    probability = arrivalProbability,
                    modifier = Modifier.size(100.dp)
                )
                // 신뢰도 설명
                Column(modifier = Modifier.weight(1f).padding(start = 20.dp)) {
                    Text("예측 신뢰도", fontSize = 14.sp, color = Color.Gray)
                    Text("데이터 기반의 정확도", fontWeight = FontWeight.SemiBold, color = DarkGray)
                    Text("이 경로의 정시 도착 확률은 높습니다.", fontSize = 12.sp, color = Color(0xFF4CAF50))
                }
            }

            // 2. 시각적 타임라인 (시간 분해 막대)
            TimeDecompositionBar(details = details)

            Spacer(modifier = Modifier.height(20.dp))

            // 3. 타임라인 디테일
            details.forEachIndexed { index, item ->
                TimelineItem(item = item, isLast = index == details.lastIndex)
            }
        }
    }
}

@Composable
fun ActionOnlyCard(
    mustLeaveTime: String,
    nextAlternative: String,
    onMapClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        // 2. 대안 옵션 (선택적 표시)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)), // 연한 노란색
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB300))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "다음 최적 대안: ${nextAlternative}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 3. 액션 버튼 (강조)
        Button(
            onClick = onMapClick,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = UnibusBlue)
        ) {
            Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("최적 경로 지도로 보기", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
    }
}

// --- 보조 컴포넌트 ---

@Composable
fun ConfidenceGauge(probability: Int, modifier: Modifier) {
    val angle = (probability * 360 / 100).toFloat()
    val density = LocalDensity.current
    val textSizePx = with(density) { 40.sp.toPx() }
    val yTextOffset = with(density) { 15.dp.toPx() }
    val strokeWidthPx = with(density) { 10.dp.toPx() }

    val sweepAngle by animateFloatAsState(
        targetValue = angle,
        animationSpec = tween(durationMillis = 1000), label = "confidence_sweep_angle"
    )

    Canvas(modifier = modifier) {

        // 배경 트랙
        drawArc(
            color = LightGrayBackground,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            size = Size(size.width, size.height),
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )

        // 진행률 바 (초록색)
        drawArc(
            color = Color(0xFF4CAF50),
            startAngle = 135f,
            sweepAngle = sweepAngle * 270 / 360,
            useCenter = false,
            size = Size(size.width, size.height),
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )

        // 텍스트 중앙에 표시
        drawIntoCanvas {
            it.nativeCanvas.apply {
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = textSizePx
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }
                val x = center.x
                val y = center.y + yTextOffset
                drawText("$probability%", x, y, textPaint)
            }
        }
    }
}

@Composable
fun TimeDecompositionBar(details: List<TimeDetail>) {
    val totalTime = details.sumOf { it.durationMinutes }.toFloat()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(LightGrayBackground)
    ) {
        details.forEach { item ->
            val percentage = item.durationMinutes / totalTime
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(percentage)
                    .background(item.icon)
            )
        }
    }
}

@Composable
fun TimelineItem(item: TimeDetail, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(item.icon),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.iconVector,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(16.dp)
                )
            }
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

        Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)) {
            Text(item.label, fontWeight = FontWeight.Medium, color = Color.Black)
            Text("${item.durationMinutes}분 소요", fontSize = 12.sp, color = Color.Gray)
        }
    }
}