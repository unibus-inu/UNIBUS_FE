package com.example.unibus.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unibus.R
import com.example.unibus.ui.theme.UnibusBlue
import com.example.unibus.ui.theme.White
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- 1. 데이터 모델 (Mock Data) ---
data class BusInfo(
    val id: Int,
    val number: String,
    val type: String,
    val eta: Int,
    val currentLocation: String,
    val nextBusEta: Int,
    val nextBusLocation: String
)

val schoolBuses = listOf(
    BusInfo(1, "셔틀 A", "셔틀", 3, "전 정류장 출발", 18, "차고지 대기"),
    BusInfo(2, "셔틀 B", "셔틀", 7, "도서관 진입", 22, "정문 통과"),
    BusInfo(3, "3001번", "시내", 12, "3정거장 전", 24, "OO아파트"),
    BusInfo(4, "11-1번", "시내", 15, "차고지 출발", 30, "회차점 대기")
)

val homeBuses = listOf(
    BusInfo(5, "셔틀 C", "셔틀", 2, "진입중", 15, "출발 대기"),
    BusInfo(6, "셔틀 A", "셔틀", 10, "학교 앞 사거리", 25, "역전 도착"),
    BusInfo(7, "3001번", "시내", 18, "터미널 사거리", 35, "시청 앞")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHomeScreen(
    onNavigateToEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToWithdraw: () -> Unit,
    onNavigateToNotifications: () -> Unit, // ★ 추가: 알림 화면 이동 콜백
    hasNewNotifications: Boolean, // ★ 추가: 알림 상태
    onNavigateToPrediction: () -> Unit
) {
    var isGoingToSchool by remember { mutableStateOf(true) }
    val currentBusList = if (isGoingToSchool) schoolBuses else homeBuses
    var selectedBus by remember { mutableStateOf<BusInfo?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = "로그아웃", fontWeight = FontWeight.Bold) },
            text = { Text(text = "정말 로그아웃 하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("예", color = UnibusBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("아니오", color = Color.Gray)
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(300.dp),
                        drawerContainerColor = White,
                        drawerShape = RoundedCornerShape(0.dp)
                    ) {
                        DrawerContent(
                            userNickname = "김유니",
                            onEditProfileClick = {
                                scope.launch {
                                    drawerState.close()
                                    onNavigateToEditProfile()
                                }
                            },
                            onLogoutClick = {
                                scope.launch {
                                    drawerState.close()
                                    showLogoutDialog = true
                                }
                            },
                            onWithdrawClick = {
                                scope.launch {
                                    drawerState.close()
                                    onNavigateToWithdraw()
                                }
                            }
                        )
                    }
                }
            },
            gesturesEnabled = drawerState.isOpen
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                BottomSheetScaffold(
                    scaffoldState = bottomSheetScaffoldState,
                    sheetContainerColor = White,
                    sheetContentColor = MaterialTheme.colorScheme.onSurface,
                    sheetPeekHeight = 100.dp,
                    sheetShadowElevation = 10.dp,
                    sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    // --- 3. 하단 영역 (Bottom Sheet) ---
                    sheetContent = {
                        BottomSheetContent(
                            busList = currentBusList,
                            onBusClick = { bus -> selectedBus = bus }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        // 2. 지도 영역
                        MapArea(selectedBus = selectedBus)

                        // 1. 상단 컨트롤 바
                        TopControlBar(
                            isGoingToSchool = isGoingToSchool,
                            onModeChange = { isSchool ->
                                isGoingToSchool = isSchool
                                selectedBus = null
                            },
                            onPredictionClick = onNavigateToPrediction,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            hasNewNotifications = hasNewNotifications, // ★ 파라미터 전달
                            onNotificationsClick = onNavigateToNotifications // ★ 파라미터 전달
                        )

                        // [삭제됨] 이전에 여기에 있던 FloatingActionButton은 BottomSheetContent로 이동했습니다.
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 0. 사이드 메뉴 (드로어) 콘텐츠
// ---------------------------------------------------------
@Composable
fun DrawerContent(
    userNickname: String,
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onWithdrawClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 40.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Profile Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = userNickname,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "반갑습니다!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
        Divider(color = Color.LightGray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(20.dp))

        DrawerMenuItem(text = "회원정보 수정", onClick = onEditProfileClick)
        DrawerMenuItem(text = "로그아웃", onClick = onLogoutClick)

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = onWithdrawClick,
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text("회원탈퇴", color = Color.Gray, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun DrawerMenuItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

// ---------------------------------------------------------
// 1. 상단 영역 컴포넌트 (TopControlBar)
// ---------------------------------------------------------
@Composable
fun TopControlBar(
    isGoingToSchool: Boolean,
    onModeChange: (Boolean) -> Unit,
    onPredictionClick: () -> Unit,
    onMenuClick: () -> Unit,
    hasNewNotifications: Boolean, // ★ 파라미터로 받음
    onNotificationsClick: () -> Unit // ★ 파라미터로 받음
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 좌측: 예측 페이지 버튼
        IconButton(
            onClick = onPredictionClick,
            modifier = Modifier
                .size(48.dp)
                .background(White, CircleShape)
                .shadow(4.dp, CircleShape)
        ) {
            Icon(Icons.Rounded.AutoGraph, contentDescription = "예측", tint = UnibusBlue)
        }

        // 중앙: 모드 전환 토글 (알약 스위치)
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(48.dp)
                .shadow(4.dp, RoundedCornerShape(50))
                .background(White, RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                ModeButton("🏫 등교", isGoingToSchool, Modifier.weight(1f)) { onModeChange(true) }
                ModeButton("🏠 하교", !isGoingToSchool, Modifier.weight(1f)) { onModeChange(false) }
            }
        }

        // 우측: 알림 버튼 + 햄버거 메뉴 버튼
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // 1. 알림 버튼 (햄버거 메뉴 왼쪽에 위치)
            IconButton(
                onClick = onNotificationsClick, // ★ 파라미터로 받은 콜백 실행
                modifier = Modifier
                    .size(48.dp)
                    .background(White, CircleShape)
                    .shadow(4.dp, CircleShape)
            ) {
                // 알림 유무에 따라 아이콘 및 색상 변경
                Icon(
                    imageVector = if (hasNewNotifications) Icons.Rounded.Notifications else Icons.Rounded.NotificationsNone,
                    contentDescription = "알림",
                    tint = if (hasNewNotifications) MaterialTheme.colorScheme.error else Color.Gray // 새 알림 강조
                )
            }

            Spacer(modifier = Modifier.width(8.dp)) // 버튼 사이 간격

            // 2. 사이드 메뉴 버튼 (햄버거)
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(White, CircleShape)
                    .shadow(4.dp, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "메뉴",
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ModeButton(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isSelected) UnibusBlue else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

// ---------------------------------------------------------
// 2. 지도 영역 컴포넌트
// ---------------------------------------------------------
@Composable
fun MapArea(selectedBus: BusInfo?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = center
            drawCircle(color = UnibusBlue.copy(alpha = 0.2f), radius = 60f, center = center)
            drawCircle(color = UnibusBlue, radius = 20f, center = center)

            selectedBus?.let {
                drawLine(
                    color = UnibusBlue,
                    start = Offset(center.x, center.y),
                    end = Offset(center.x + 300f, center.y - 400f),
                    strokeWidth = 15f,
                    pathEffect = null
                )
                drawCircle(color = UnibusBlue, radius = 15f, center = Offset(center.x + 300f, center.y - 400f))
            }
        }

        if (selectedBus != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 50.dp, y = (-100).dp),
                shape = RoundedCornerShape(8.dp),
                color = White,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "${selectedBus.number} 이동중..",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = UnibusBlue
                )
            }
        }
    }
}

// ---------------------------------------------------------
// 3. 하단 시트 콘텐츠
// ---------------------------------------------------------
@Composable
fun BottomSheetContent(
    busList: List<BusInfo>,
    onBusClick: (BusInfo) -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            val fastestBus = busList.firstOrNull()
            if (fastestBus != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "가장 빠른 버스",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${fastestBus.number} (${fastestBus.eta}분 뒤)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = UnibusBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = fastestBus.currentLocation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    // [삭제됨] GPS 아이콘 대신 새로고침 버튼이 이동했으므로, 이 아이콘은 이제 FAB이 대체합니다.
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // --- 버스 정보 리스트 ---
            LazyColumn {
                items(busList) { bus ->
                    BusListItem(bus = bus, onClick = { onBusClick(bus) })
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        } // End of Column

        // ★ [수정] 새로고침 버튼 위치 (y = 20.dp)
        FloatingActionButton(
            onClick = {
                scope.launch {
                    isRefreshing = true
                    delay(1000) // 데이터 로딩 시간 시뮬레이션
                    // TODO: 데이터 새로고침 로직
                    isRefreshing = false
                }
            },
            containerColor = White,
            contentColor = UnibusBlue,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd) // Box의 상단 우측에 고정
                .offset(y = 20.dp) // 가장 빠른 버스 텍스트 라인과 수평으로 맞춥니다.
                .shadow(4.dp, CircleShape)
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = "새로고침")
        }
    } // End of Box
}

@Composable
fun BusListItem(
    bus: BusInfo,
    onClick: () -> Unit
) {
    val SkyBlue = Color(0xFF64B5F6)
    var isNotificationEnabled by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- 1. 좌측 (25%) ---
        Column(modifier = Modifier.weight(0.25f)) {
            Text(bus.number, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(bus.type, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        // --- 2. 중앙 (35%) ---
        Column(modifier = Modifier.weight(0.35f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (bus.eta == 0) "곧 도착" else "${bus.eta}분 뒤",
                style = MaterialTheme.typography.titleLarge,
                color = UnibusBlue,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Place, null, Modifier.size(12.dp), Color.Gray)
                Spacer(Modifier.width(2.dp))
                Text(bus.currentLocation, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
            }
        }

        // --- 3. 우측 정보 (25%) ---
        Column(modifier = Modifier.weight(0.25f), horizontalAlignment = Alignment.End) {
            Text("${bus.nextBusEta}분 뒤", style = MaterialTheme.typography.titleMedium, color = SkyBlue, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Place, null, Modifier.size(12.dp), SkyBlue.copy(alpha = 0.7f))
                Spacer(Modifier.width(2.dp))
                Text(bus.nextBusLocation, style = MaterialTheme.typography.labelSmall, color = Color.Gray.copy(alpha = 0.8f), maxLines = 1)
            }
        }

        // --- 4. 우측 끝 알림 버튼 ---
        IconButton(
            onClick = { isNotificationEnabled = !isNotificationEnabled },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isNotificationEnabled) Icons.Rounded.Notifications else Icons.Rounded.NotificationsNone,
                contentDescription = "알림 설정",
                tint = if (isNotificationEnabled) UnibusBlue else Color.Gray
            )
        }
    }
}

fun Modifier.shadow(
    elevation: androidx.compose.ui.unit.Dp,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape,
    clip: Boolean = false,
    ambientColor: Color = androidx.compose.ui.graphics.Color.Black,
    spotColor: Color = androidx.compose.ui.graphics.Color.Black,
): Modifier = this.then(
    Modifier.graphicsLayer(
        shadowElevation = elevation.value,
        shape = shape,
        clip = clip,
        ambientShadowColor = ambientColor,
        spotShadowColor = spotColor
    )
)