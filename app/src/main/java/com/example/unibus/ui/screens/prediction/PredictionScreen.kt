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
import com.example.unibus.ui.screens.home.Station

// 데이터 모델 (건물과 역을 모두 포함하도록 수정)
data class Destination(val id: Int, val name: String, val detail: String, val type: String) // type: "building" or "station"

// ★ [수정됨] 등교 모드: 건물 목록
val buildingList = listOf(
    Destination(1, "1호관", "대학본부 / 행정실", "building"),
    Destination(2, "2호관", "교수회관", "building"),
    Destination(3, "3호관", "INU CUBE", "building"),
    Destination(4, "4호관", "정보전산원 (BM컨텐츠관)", "building"),
    Destination(5, "5호관", "자연과학대학", "building"),
    Destination(6, "6호관", "학산도서관 (Library)", "building"),
    Destination(7, "7호관", "정보기술대학 (IT College)", "building"),
    Destination(8, "8호관", "공과대학, 도시과학대학", "building"),
    Destination(9, "9호관", "공동실험실습관", "building"),
    Destination(10, "10호관", "게스트하우스", "building"),
    Destination(11, "11호관", "복지회관 (학생식당/매점)", "building"),
    Destination(12, "12호관", "컨벤션센터", "building"),
    Destination(13, "13호관", "사회과학 / 법학 / 글로벌정경대학", "building"),
    Destination(14, "14호관", "경영대학, 동북아물류대학원", "building"),
    Destination(15, "15호관", "인문대학", "building"),
    Destination(16, "16호관", "예술체육대학", "building"),
    Destination(17, "17호관", "학생회관 (동아리/보건실/우체국)", "building"),
    Destination(18, "18-1호관", "제1기숙사", "building"),
    Destination(18, "18-2호관", "제2기숙사", "building"),
    Destination(18, "18-3호관", "제3기숙사", "building"),
    Destination(19, "19호관", "융합자유전공대학", "building"),
    Destination(20, "20호관", "스포츠센터, 골프연습장", "building"),
    Destination(21, "21호관", "체육관", "building"),
    Destination(22, "22호관", "학군단", "building"),
    Destination(23, "23호관", "강당, 공연장", "building"),
    Destination(24, "24호관", "전망타워", "building"),
    Destination(25, "25호관", "해둥실어린이집", "building"),
    Destination(26, "26호관", "온실", "building"),
    Destination(27, "27호관", "제2공동실험실습관", "building"),
    Destination(28, "28호관", "도시과학대학", "building"),
    Destination(29, "29호관", "생명공학부", "building"),
    Destination(41, "41호관", "바이오컴플렉스", "building")
)

// ★ [추가] 하교 모드: 역 목록
val stationList = listOf(
    Destination(201, "인천대입구역", "인천 1호선 (캠퍼스 타운 방면)", "station"),
    Destination(202, "지식정보단지역", "인천 1호선 (송도달빛축제공원 방면)", "station"),
)

// 메인 진입점 Composable
@Composable
fun PredictionScreen(
    onBackClick: () -> Unit,
    onMapClick: (BusInfo) -> Unit,
    isGoingToSchool: Boolean // ★ [추가] 현재 모드 상태를 받음
) {
    var selectedDestination by remember { mutableStateOf<Destination?>(null) } // Destination 모델 사용

    // 예측된 최적 버스 정보 (더미 데이터)
    val predictedBusInfo = remember(selectedDestination, isGoingToSchool) {
        if (selectedDestination != null) {
            val cost = if (isGoingToSchool) "무료" else "유료"
            val number = if (isGoingToSchool) "셔틀 A" else "M6724" // 하교 시 다른 버스 가정

            BusInfo(
                id = 99,
                number = number,
                type = if (isGoingToSchool) "셔틀" else "광역",
                eta = 3,
                cost = cost,
                currentLocation = if (isGoingToSchool) "정문 진입" else "캠퍼스 출발",
                nextBusEta = 18,
                nextBusLocation = "차고지 대기",
                stationId = 1
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
    val currentDestinationList = if (isGoingToSchool) buildingList else stationList
    val title = if (isGoingToSchool) "건물 도착 예측" else "역/터미널 하차 예측"


    if (selectedDestination == null) {
        // Step 1: 목적지 선택 화면 호출
        DestinationSelectionScreen(
            title = title,
            destinationList = currentDestinationList,
            onDestinationSelect = { selectedDestination = it },
            onBackClick = handleBack
        )
    } else {
        // Step 2: 예측 대시보드 화면 (DashboardScreen.kt 파일 호출)
        PredictionDashboardScreen(
            building = selectedDestination!!, // Destination 모델을 Building 대신 사용 (이름 통일)
            onBackClick = handleBack,
            onMapClick = {
                // '최적 경로 지도로 보기' 버튼 클릭 시 동작
                if (predictedBusInfo != null) {
                    onMapClick(predictedBusInfo)
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
    onBackClick: () -> Unit
) {
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
                    text = "어디로 가는 시간을 예측해 드릴까요?",
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