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

// 데이터 모델
data class Building(val id: Int, val name: String, val detail: String)

// ★ 인천대학교 건물 목록
val buildingList = listOf(
    Building(1, "1호관", "대학본부 / 행정실"),
    Building(2, "2호관", "교수회관 / 교직원식당"),
    Building(5, "5호관", "자연과학대학"),
    Building(6, "6호관", "학산도서관 (Library)"),
    Building(7, "7호관", "정보기술대학"),
    Building(8, "8호관", "공과대학 / 도시과학"),
    Building(10, "10호관", "게스트하우스 / 샐러디"),
    Building(11, "11호관", "복지회관 / 학생식당"),
    Building(13, "13호관", "사회과학대학 / 법학"),
    Building(14, "14호관", "경영대학 / 동북아통상"),
    Building(15, "15호관", "인문대학"),
    Building(16, "16호관", "예술체육대학"),
    Building(17, "17호관", "학생회관 / 식당가"),
    Building(20, "20호관", "스포츠센터 / 체육관"),
    Building(27, "27호관", "공동실험관"),
    Building(101, "미추홀 별관", "사범대학"),
)

// 메인 진입점 Composable (상태 관리자 역할)
@Composable
fun PredictionScreen(onBackClick: () -> Unit) {
    // 선택된 건물 상태 (null이면 선택 화면, 값이 있으면 대시보드 화면)
    var selectedBuilding by remember { mutableStateOf<Building?>(null) }

    // 뒤로가기 동작 처리: 대시보드 -> 선택 -> 메인 순으로 이동
    val handleBack: () -> Unit = {
        if (selectedBuilding != null) {
            selectedBuilding = null // 대시보드에서 선택 화면으로 돌아감
        } else {
            onBackClick() // 선택 화면에서 메인으로 돌아감 (MainActivity로 콜백)
        }
    }

    if (selectedBuilding == null) {
        // Step 1: 건물 선택 화면 호출
        BuildingSelectionScreen(
            onBuildingSelect = { selectedBuilding = it }, // 건물 선택 시 상태 업데이트
            onBackClick = handleBack
        )
    } else {
        // Step 2: 예측 대시보드 화면 호출 (DashboardScreen.kt 파일에 정의되어 있음)
        PredictionDashboardScreen(
            building = selectedBuilding!!,
            onBackClick = handleBack
        )
    }
}

// ---------------------------------------------------------
// Step 1: 목적지 선택 화면 (BuildingSelectionScreen)
// ---------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingSelectionScreen(
    onBuildingSelect: (Building) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("도착 예측 분석", fontWeight = FontWeight.Bold) },
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), // 연한 파란색 배경
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

            // 건물 목록 리스트
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(buildingList) { building ->
                    BuildingSelectionItem(
                        building = building,
                        onClick = { onBuildingSelect(building) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun BuildingSelectionItem(building: Building, onClick: () -> Unit) {
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
                imageVector = Icons.Rounded.Place, // 건물 아이콘
                contentDescription = null,
                tint = UnibusBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(building.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(building.detail, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.LightGray, modifier = Modifier.graphicsLayer(rotationZ = 180f)) // 오른쪽 화살표
        }
    }
}