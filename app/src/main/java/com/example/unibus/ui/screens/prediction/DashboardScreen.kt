package com.example.unibus.ui.screens.prediction

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ---------------------------------------------------------
// 1. 데이터 모델 및 상수 정의
// ---------------------------------------------------------

private val UnibusBlue = Color(0xFF0D47A1)
private val White = Color(0xFFFFFFFF)
private val DarkGray = Color(0xFF424242)
private val LightGrayBackground = Color(0xFFEEEEEE)

data class TripFeedback(
    val busName: String = "",
    val boardingStop: String = "",
    val dropOffStop: String = "",
    val departTime: String = "",
    val arriveStationTime: String = "",
    val arriveBuildingTime: String = "",
    val isLate: Boolean? = null
)

data class TimeDetail(val label: String, val durationMinutes: Int, val icon: Color, val iconVector: ImageVector)

val mockPredictionDetails = listOf(
    TimeDetail("도보 이동", 8, Color(0xFF90A4AE), Icons.Rounded.DirectionsWalk),
    TimeDetail("버스 대기", 2, Color(0xFFFFB300), Icons.Rounded.DirectionsBus),
    TimeDetail("버스 탑승", 25, UnibusBlue, Icons.Rounded.DirectionsBus),
    TimeDetail("최종 도보", 5, Color(0xFF4CAF50), Icons.Rounded.DirectionsWalk),
)

// ---------------------------------------------------------
// 2. 헬퍼 함수 (데이터 매핑 로직)
// ---------------------------------------------------------

fun getAvailableBuses(destination: Destination): List<String> {
    return when (destination.name) {
        "인천대입구역" -> listOf("셔틀 A", "8번", "41번", "16-1번")
        "지식정보단지역" -> listOf("909번", "6-1번", "6번")
        "1호관", "2호관" -> listOf("셔틀 A", "셔틀 B", "8번")
        "8호관" -> listOf("셔틀 A", "셔틀 B", "M6724")
        else -> listOf("셔틀 A", "셔틀 B", "8번", "M6724")
    }
}

fun getBoardingStopsOptions(isGoingToSchool: Boolean): List<String> {
    return if (isGoingToSchool) {
        // 등교 승차: 역/시내
        listOf("인천대입구역", "지식정보단지역","공과대학", "기숙사", "그 외")
    } else {
        // 하교 승차: 교내
        listOf("정문", "공과대학", "자연과학대학", "기숙사", "정보기술대학", "본관", "도서관")
    }
}

fun getDropOffStopsOptions(isGoingToSchool: Boolean): List<String> {
    return if (isGoingToSchool) {
        // 등교 하차: 교내
        listOf("정문", "공과대학", "자연과학대학", "기숙사", "정보기술대학", "도서관","사범대")
    } else {
        // 하교 하차: 역/시내
        listOf("인천대입구역", "지식정보단지역","공과대학","기숙사","그 외")
    }
}

// ---------------------------------------------------------
// 3. 메인 스크린 Composable
// ---------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionDashboardScreen(
    building: Destination,
    onBackClick: () -> Unit,
    onMapClick: () -> Unit
) {
    val displayTimeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    val isGoingToSchool = remember(building) { building.type == "building" }

    val currentBusList = remember(building) { getAvailableBuses(building) }
    val currentBoardingStops = remember(isGoingToSchool) { getBoardingStopsOptions(isGoingToSchool) }
    val currentDropOffStops = remember(isGoingToSchool) { getDropOffStopsOptions(isGoingToSchool) }

    val totalDuration = mockPredictionDetails.sumOf { it.durationMinutes }
    val bestBus = "8번 버스"
    val busEta = 2
    val arrivalProbability = 92
    val nextAlternative = "셔틀 A, 15분 뒤"
    val finalArrivalTime = LocalTime.now().plusMinutes(totalDuration.toLong()).format(displayTimeFormatter)

    Scaffold(
        containerColor = Color(0xFFF0F0F0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("${building.name} 예측 분석", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기") } },
                actions = { IconButton(onClick = {}) { Icon(Icons.Default.Refresh, "새로고침") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showFeedbackDialog = true },
                containerColor = UnibusBlue,
                contentColor = White,
                icon = { Icon(Icons.Default.Edit, null) },
                text = { Text("도착 확인", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp)
        ) {
            item { ResultActionCard(building, finalArrivalTime, bestBus, busEta); Spacer(modifier = Modifier.height(24.dp)) }

            item {
                ProofConfidenceCard(
                    totalDuration = totalDuration,
                    arrivalProbability = arrivalProbability,
                    details = mockPredictionDetails
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item { ActionOnlyCard(nextAlternative, onMapClick) }
        }

        if (showFeedbackDialog) {
            PredictionFeedbackDialog(
                isGoingToSchool = isGoingToSchool,
                availableBuses = currentBusList,
                availableBoardingStops = currentBoardingStops,
                availableDropOffStops = currentDropOffStops,
                onDismiss = { showFeedbackDialog = false },
                onSubmit = { feedback ->
                    val mode = if (isGoingToSchool) "등교" else "하교"
                    println("피드백 전송 ($mode): $feedback")
                    showFeedbackDialog = false
                }
            )
        }
    }
}

// ---------------------------------------------------------
// 4. 피드백(설문) 다이얼로그 (순서 재배치됨)
// ---------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionFeedbackDialog(
    isGoingToSchool: Boolean,
    availableBuses: List<String>,
    availableBoardingStops: List<String>,
    availableDropOffStops: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (TripFeedback) -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    var feedbackData by remember { mutableStateOf(TripFeedback()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var activeField by remember { mutableStateOf<String?>(null) }
    val timePickerState = rememberTimePickerState(initialHour = LocalTime.now().hour, initialMinute = LocalTime.now().minute, is24Hour = true)

    val onTimeSelected: () -> Unit = {
        val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute).format(timeFormatter)
        feedbackData = when (activeField) {
            "depart" -> feedbackData.copy(departTime = selectedTime)
            "station" -> feedbackData.copy(arriveStationTime = selectedTime)
            "building" -> feedbackData.copy(arriveBuildingTime = selectedTime)
            else -> feedbackData
        }
        showTimePicker = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("도착 정보 공유", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = UnibusBlue)
                Text("다음번의 예측이 더 정확해집니다!", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 20.dp))

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    item {
                        // 1. [순서 변경] 승차 정류장 (어디서 탔는지)
                        SectionTitle("어디서 타셨나요?")
                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableBoardingStops) { stop ->
                                SelectionChip(text = stop, isSelected = feedbackData.boardingStop == stop) { feedbackData = feedbackData.copy(boardingStop = stop) }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. [순서 변경] 버스 종류 (어떤 버스)
                        SectionTitle("어떤 버스를 타셨나요?")
                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableBuses) { bus ->
                                SelectionChip(text = bus, isSelected = feedbackData.busName == bus) { feedbackData = feedbackData.copy(busName = bus) }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. [순서 변경] 하차 정류장 (어디서 내렸는지)
                        SectionTitle("어디서 내리셨나요?")
                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableDropOffStops) { stop ->
                                SelectionChip(text = stop, isSelected = feedbackData.dropOffStop == stop) { feedbackData = feedbackData.copy(dropOffStop = stop) }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 20.dp), color = Color(0xFFEEEEEE))

                        // 4. 버스 승차 시간
                        TimeInputRow("버스 승차", feedbackData.departTime, { activeField = "depart"; showTimePicker = true }) { feedbackData = feedbackData.copy(departTime = LocalTime.now().format(timeFormatter)) }
                        Spacer(modifier = Modifier.height(12.dp))

                        // 5. 버스 하차 시간
                        TimeInputRow("버스 하차", feedbackData.arriveStationTime, { activeField = "station"; showTimePicker = true }) { feedbackData = feedbackData.copy(arriveStationTime = LocalTime.now().format(timeFormatter)) }

                        // 6. [등교 시] 건물 도착 시간
                        if (isGoingToSchool) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TimeInputRow("건물 도착", feedbackData.arriveBuildingTime, { activeField = "building"; showTimePicker = true }) { feedbackData = feedbackData.copy(arriveBuildingTime = LocalTime.now().format(timeFormatter)) }
                        }

                        Divider(modifier = Modifier.padding(vertical = 20.dp), color = Color(0xFFEEEEEE))

                        // 7. 지각 여부
                        Text("수업에 늦으셨나요?", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            SelectionButton("출석 👍", feedbackData.isLate == false, { feedbackData = feedbackData.copy(isLate = false) }, Color(0xFF4CAF50))
                            SelectionButton("지각 👎", feedbackData.isLate == true, { feedbackData = feedbackData.copy(isLate = true) }, Color(0xFFE53935))
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                val isValid = feedbackData.busName.isNotEmpty() &&
                        feedbackData.boardingStop.isNotEmpty() &&
                        feedbackData.dropOffStop.isNotEmpty() &&
                        feedbackData.isLate != null

                Button(onClick = { onSubmit(feedbackData) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = UnibusBlue), enabled = isValid) {
                    Text("데이터 제출하기", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) }
            }
        }
    }
    if (showTimePicker) {
        TimePickerDialog(onDismissRequest = { showTimePicker = false }, confirmButton = { TextButton(onClick = onTimeSelected) { Text("확인") } }, dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("취소") } }) { TimePicker(state = timePickerState) }
    }
}

// ---------------------------------------------------------
// 5. 보조 UI 컴포넌트
// ---------------------------------------------------------

@Composable
fun ResultActionCard(building: Destination, finalArrivalTime: String, bestBus: String, busEta: Int) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text("$finalArrivalTime 도착", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = DarkGray)
            val subText = if(building.type == "building") "건물 앞까지 예상 시각" else "역/터미널 하차 예상 시각"
            Text(subText, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()) {
                Icon(Icons.Rounded.DirectionsBus, null, tint = UnibusBlue, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("$bestBus 탑승 추천 (${busEta}분 뒤 도착)", fontWeight = FontWeight.Bold, color = UnibusBlue)
            }
        }
    }
}

@Composable
fun ProofConfidenceCard(totalDuration: Int, arrivalProbability: Int, details: List<TimeDetail>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Text("예상 경로 상세 분석 (${totalDuration}분 소요)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkGray, modifier = Modifier.padding(bottom = 16.dp))

            // 1. 게이지
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ConfidenceGauge(probability = arrivalProbability, modifier = Modifier.size(100.dp))
                Column(modifier = Modifier.weight(1f).padding(start = 20.dp)) {
                    Text("예측 신뢰도", fontSize = 14.sp, color = Color.Gray)
                    Text("데이터 기반의 정확도", fontWeight = FontWeight.SemiBold, color = DarkGray)
                    Text("이 경로의 정시 도착 확률은 높습니다.", fontSize = 12.sp, color = Color(0xFF4CAF50))
                }
            }
            // 2. 타임라인 바
            TimeDecompositionBar(details = details)
            Spacer(modifier = Modifier.height(20.dp))
            // 3. 상세 리스트
            details.forEachIndexed { index, item ->
                TimelineItem(item = item, isLast = index == details.lastIndex)
            }
        }
    }
}

@Composable
fun ActionOnlyCard(nextAlternative: String, onMapClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)), shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFFFB300)); Spacer(modifier = Modifier.width(10.dp))
                Text("다음 대안: $nextAlternative", fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onMapClick, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = UnibusBlue)) {
            Icon(Icons.Rounded.Map, null, modifier = Modifier.size(28.dp)); Spacer(modifier = Modifier.width(8.dp))
            Text("지도 보기", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
    }
}

@Composable
fun ConfidenceGauge(probability: Int, modifier: Modifier) {
    val angle = (probability * 360 / 100).toFloat()
    val density = LocalDensity.current
    val textSizePx = with(density) { 32.sp.toPx() }
    val strokeWidthPx = with(density) { 10.dp.toPx() }
    val sweepAngle by animateFloatAsState(targetValue = angle, animationSpec = tween(durationMillis = 1000), label = "gauge")
    Canvas(modifier = modifier) {
        drawArc(color = LightGrayBackground, startAngle = 135f, sweepAngle = 270f, useCenter = false, size = Size(size.width, size.height), style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round))
        drawArc(color = Color(0xFF4CAF50), startAngle = 135f, sweepAngle = sweepAngle * 270 / 360, useCenter = false, size = Size(size.width, size.height), style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round))
        drawIntoCanvas { it.nativeCanvas.apply { val p = android.graphics.Paint().apply { color = android.graphics.Color.BLACK; textSize = textSizePx; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true }; drawText("$probability%", center.x, center.y + (textSizePx/3), p) } }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
}

@Composable
fun SelectionChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.height(36.dp).background(if (isSelected) UnibusBlue else Color(0xFFF5F5F5), RoundedCornerShape(50)).border(1.dp, if (isSelected) UnibusBlue else Color(0xFFE0E0E0), RoundedCornerShape(50)).clickable { onClick() }.padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) Color.White else Color.Black, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
fun TimeInputRow(label: String, value: String, onBoxClick: () -> Unit, onNowClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DarkGray)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(80.dp).height(36.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp)).clickable { onBoxClick() }, contentAlignment = Alignment.Center) {
                Text(text = if (value.isNotEmpty()) value else "--:--", color = if (value.isNotEmpty()) Color.Black else Color.LightGray, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            SmallFloatingActionButton(onClick = onNowClick, containerColor = Color(0xFFE3F2FD), contentColor = UnibusBlue, modifier = Modifier.size(36.dp), shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.AccessTime, "현재", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun SelectionButton(text: String, isSelected: Boolean, onClick: () -> Unit, activeColor: Color) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (isSelected) activeColor.copy(alpha = 0.1f) else Color.Transparent, contentColor = if (isSelected) activeColor else Color.Gray),
        border = androidx.compose.foundation.BorderStroke(width = if (isSelected) 2.dp else 1.dp, color = if (isSelected) activeColor else Color.LightGray),
        shape = RoundedCornerShape(50)
    ) {
        Text(text, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun TimePickerDialog(onDismissRequest: () -> Unit, confirmButton: @Composable () -> Unit, dismissButton: @Composable (() -> Unit)? = null, content: @Composable () -> Unit) {
    AlertDialog(onDismissRequest = onDismissRequest, confirmButton = confirmButton, dismissButton = dismissButton, text = { Column(horizontalAlignment = Alignment.CenterHorizontally) { content() } }, containerColor = Color.White)
}

@Composable
fun TimeDecompositionBar(details: List<TimeDetail>) {
    val totalTime = details.sumOf { it.durationMinutes }.toFloat()
    Row(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)).background(LightGrayBackground)) {
        details.forEach { item -> Box(modifier = Modifier.fillMaxHeight().weight(item.durationMinutes / totalTime).background(item.icon)) }
    }
}

@Composable
fun TimelineItem(item: TimeDetail, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(item.icon), contentAlignment = Alignment.Center) { Icon(item.iconVector, null, tint = White, modifier = Modifier.size(16.dp)) }
            if (!isLast) Box(modifier = Modifier.width(2.dp).height(30.dp).background(Color(0xFFEEEEEE)))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)) {
            Text(item.label, fontWeight = FontWeight.Medium, color = Color.Black)
            Text("${item.durationMinutes}분 소요", fontSize = 12.sp, color = Color.Gray)
        }
    }
}