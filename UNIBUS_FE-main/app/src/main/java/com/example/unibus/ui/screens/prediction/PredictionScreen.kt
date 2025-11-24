package com.example.unibus.ui.screens.prediction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unibus.ui.theme.UnibusBlue
import com.example.unibus.ui.theme.White
import com.example.unibus.ui.screens.home.BusInfo
import com.example.unibus.data.UnibusRepository
import com.example.unibus.data.api.model.DropoffDummy
import com.example.unibus.ui.screens.notifications.NotificationStore
import kotlin.math.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// 데이터 모델 (건물과 역을 모두 포함하도록 수정)
data class Destination(val id: String, val name: String, val detail: String, val type: String) // type: "building" or "station"

// 메인 진입점 Composable
@Composable
fun PredictionScreen(
    onBackClick: () -> Unit,
    onMapClick: (BusInfo, String?) -> Unit,
    isGoingToSchool: Boolean // ★ [추가] 현재 모드 상태를 받음
) {
    var selectedDestination by remember { mutableStateOf<Destination?>(null) } // Destination 모델 사용
    var destinations by remember { mutableStateOf<List<Destination>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var recommendedStopId by remember { mutableStateOf<String?>(null) }
    var recommendedStopName by remember { mutableStateOf<String?>(null) }
    var boardingChoices by remember {
        mutableStateOf(
            listOf(
                Destination("stop-dorm", "인천대생활원", "생활원 정류장", "station"),
                Destination("stop-eng", "정보기술대/공과대 정류장", "공대·정보대 라인", "station"),
                Destination("stop-main-gate", "인천대정문", "정문 정류장", "station")
            )
        )
    }
    var selectedBoarding by remember { mutableStateOf<Destination?>(boardingChoices.firstOrNull()) }
    var etaSeconds by remember { mutableStateOf<Int?>(null) }
    var lastEtaSeconds by remember { mutableStateOf<Int?>(null) }
    var firedAlertStopId by remember { mutableStateOf<String?>(null) }

    // 기본 정류장 좌표 (대략값) – ETA 폴백용
    var stopCoords by remember {
        mutableStateOf(
            mapOf(
                "stop-main-gate" to (37.3775 to 126.6354),
                "stop-eng" to (37.3743 to 126.6339),
                "stop-dorm" to (37.3739 to 126.6298),
                "stop-incheon-univ-stn" to (37.3860 to 126.6394)
            )
        )
    }

    // 한 번만 노선 정류장 좌표를 불러와서 보강
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val route = UnibusRepository.getRoute("inu-a")
                val fromStops = route.stops.associate { it.id to (it.lat to it.lon) }
                if (fromStops.isNotEmpty()) {
                    stopCoords = stopCoords + fromStops
                }
            } catch (_: Exception) {
                // 무시
            }
        }
    }

    LaunchedEffect(isGoingToSchool) {
        isLoading = true
        try {
            if (isGoingToSchool) {
                val guides = UnibusRepository.getDropoffGuides()
                val list = if (guides.isNotEmpty()) guides else DropoffDummy.fallbackDropoffGuides
                destinations = list.map {
                    Destination(
                        id = it.id,
                        name = it.name,
                        detail = it.recommended_stop.name.ifBlank { it.recommended_stop.id },
                        type = "building"
                    )
                }
                // 기본 추천 정류장
                list.firstOrNull()?.let {
                    recommendedStopId = it.recommended_stop.id
                    recommendedStopName = it.recommended_stop.name.ifBlank { it.recommended_stop.id }
                }
            } else {
                // 하교: 탑승 정류장 선택용 (생활원/공대/정문)
                destinations = boardingChoices
                selectedBoarding?.let {
                    recommendedStopId = it.id
                    recommendedStopName = it.name
                }
            }
            loadError = null
        } catch (e: Exception) {
            val list = DropoffDummy.fallbackDropoffGuides
            if (list.isNotEmpty()) {
                destinations = list.map {
                    Destination(
                        id = it.id,
                        name = it.name,
                        detail = it.recommended_stop.name.ifBlank { it.recommended_stop.id },
                        type = "building"
                    )
                }
                list.firstOrNull()?.let {
                    recommendedStopId = it.recommended_stop.id
                    recommendedStopName = it.recommended_stop.name.ifBlank { it.recommended_stop.id }
                }
                loadError = null
            } else {
                destinations = emptyList()
                loadError = e.message ?: "목록을 불러오지 못했습니다."
            }
        } finally {
            isLoading = false
        }
    }

    // 예측된 최적 버스 정보 (간단한 placeholder)
    val predictedBusInfo = remember(selectedDestination, isGoingToSchool, recommendedStopId, etaSeconds) {
        if (selectedDestination != null) {
            val cost = if (isGoingToSchool) "무료" else "유료"
            BusInfo(
                id = 99,
                number = if (isGoingToSchool) "셔틀 A" else "셔틀 C",
                type = "셔틀",
                eta = etaSeconds ?: 0,
                cost = cost,
                currentLocation = recommendedStopName ?: "경로 계산 중",
                nextBusEta = 0,
                nextBusLocation = recommendedStopName ?: "",
                stationId = 0
            )
        } else null
    }

    // 뒤로가기 로직: 대시보드 -> 선택화면 -> 메인화면
    val handleBack: () -> Unit = {
        if (selectedDestination != null) {
            selectedDestination = null // 대시보드에서 선택 화면으로 돌아감
        } else {
            onBackClick() // 선택 화면에서 메인으로 돌아감
        }
    }

    // ★ [분기 로직] 모드에 따라 목록 변경
    val currentDestinationList = destinations
    val title = if (isGoingToSchool) "건물 도착 예측" else "탑승 정류장 선택"
    val promptText = if (isGoingToSchool) "어디로 가는 시간을 예측해 드릴까요?" else "어디에서 가는 시간을 예측해 드릴까요?"


    if (selectedDestination == null) {
        // Step 1: 목적지 선택 화면 호출
        DestinationSelectionScreen(
            title = title,
            destinationList = currentDestinationList,
            onDestinationSelect = { dest ->
                selectedDestination = dest
                if (!isGoingToSchool) {
                    selectedBoarding = dest
                    recommendedStopId = dest.id
                    recommendedStopName = dest.name
                }
            },
            onBackClick = handleBack,
            isLoading = isLoading,
            errorMessage = loadError,
            promptText = promptText
        )
    } else {
        LaunchedEffect(recommendedStopId, isGoingToSchool, selectedDestination) {
            lastEtaSeconds = null
            firedAlertStopId = null
            etaSeconds = null
            while (isActive && selectedDestination != null) {
                val stopId = recommendedStopId
                var newEta: Int? = null

                if (stopId != null) {
                    // 1) 백엔드 ETA
                    try {
                        val eta = UnibusRepository.getEtaBaseline("bus-01", stopId)
                        newEta = eta.eta_seconds
                    } catch (_: Exception) {
                        newEta = null
                    }

                    // 2) 폴백 ETA (거리/속도)
                    if (newEta == null) {
                        val stopLatLon = stopCoords[stopId]
                        if (stopLatLon != null) {
                            try {
                                val vehicle = UnibusRepository.getVehicleLatest("bus-01")
                                val distMeters = haversine(
                                    vehicle.lat,
                                    vehicle.lon,
                                    stopLatLon.first,
                                    stopLatLon.second
                                )
                                val speedMps = max(3.0, vehicle.speed_mps ?: 5.0)
                                newEta = (distMeters / speedMps).roundToInt().coerceAtLeast(1)
                            } catch (_: Exception) {
                                // ignore
                            }
                        }
                    }
                }

                etaSeconds = newEta

                // ETA 60초 이하 진입 시 알림
                val prev = lastEtaSeconds
                if (stopId != null && newEta != null && newEta <= 60 && firedAlertStopId != stopId) {
                    if (prev == null || prev > 60) {
                        NotificationStore.addArrivalAlert(
                            stopName = recommendedStopName ?: "정류장",
                            busNumber = if (isGoingToSchool) "셔틀 A" else "셔틀 C"
                        )
                        firedAlertStopId = stopId
                    }
                }
                lastEtaSeconds = newEta

                delay(10_000) // 10초 주기 폴링
            }
        }
        // Step 2: 예측 대시보드 화면 (DashboardScreen.kt 파일 호출)
        PredictionDashboardScreen(
            building = selectedDestination!!, // Destination 모델을 Building 대신 사용 (이름 통일)
            onBackClick = handleBack,
            etaSeconds = etaSeconds,
            stopName = recommendedStopName,
            onMapClick = {
                // '최적 경로 지도로 보기' 버튼 클릭 시 동작
                if (predictedBusInfo != null) {
                    onMapClick(predictedBusInfo, selectedDestination!!.id)
                }
            }
        )
    }
}

// ---------------------------------------------------------
// Step 1: 목적지 선택 화면 UI (BuildingSelectionScreen -> DestinationSelectionScreen)
// ---------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationSelectionScreen(
    title: String,
    destinationList: List<Destination>,
    onDestinationSelect: (Destination) -> Unit,
    onBackClick: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    promptText: String
) {
    if (isLoading) {
        Scaffold(
            containerColor = White,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = White)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = UnibusBlue)
            }
        }
        return
    }

    if (destinationList.isEmpty()) {
        Scaffold(
            containerColor = White,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = White)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(errorMessage ?: "목록을 불러오지 못했습니다.", color = Color.Gray)
            }
        }
        return
    }
    Scaffold(
        containerColor = White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // 챗봇 스타일 질문 카드
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = promptText,
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.SemiBold,
                        color = UnibusBlue
                    )
                }
            Spacer(modifier = Modifier.height(24.dp))

            // 목록 리스트
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(destinationList) { destination ->
                    DestinationSelectionItem(
                        destination = destination,
                        onClick = { onDestinationSelect(destination) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun DestinationSelectionItem(destination: Destination, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                // 건물/역 아이콘
                imageVector = Icons.Rounded.Place,
                contentDescription = null,
                tint = UnibusBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(destination.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(destination.detail, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.graphicsLayer(rotationZ = 180f)
            )
        }
    }
}

// 단순 해버사인 거리(m)
private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0 // meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * asin(min(1.0, sqrt(a)))
    return R * c
}
