package com.example.unibus.ui.screens.home

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.unibus.R
import com.example.unibus.ui.theme.UnibusBlue
import com.example.unibus.ui.theme.White
import com.example.unibus.data.UnibusRepository
import com.example.unibus.ui.screens.profile.ProfileViewModel // ViewModel Import 필요
import kotlinx.coroutines.launch

// --- 데이터 모델 및 더미 데이터 (기존 유지) ---
data class BusInfo(
    val id: Int, val number: String, val type: String, val eta: Int, val cost: String,
    val currentLocation: String, val nextBusEta: Int, val nextBusLocation: String, val stationId: Int
)
data class Station(val id: Int, val name: String)
data class StationMarker(val id: Int, val name: String, val xRatio: Float, val yRatio: Float)

val mockStations = listOf(Station(1, "정문 앞 정류장"), Station(2, "후문 정류장"), Station(3, "캠퍼스 내부 순환"))
val mockStationMarkers = listOf(
    StationMarker(1, "정문 앞 정류장", 0.5f, 0.7f),
    StationMarker(2, "후문 정류장", 0.75f, 0.45f),
    StationMarker(3, "캠퍼스 내부 순환", 0.25f, 0.55f)
)
val schoolBuses = listOf(
    BusInfo(1, "셔틀 A", "셔틀", 3, "무료", "전 정류장 출발", 18, "차고지 대기", 1),
    BusInfo(2, "셔틀 B", "셔틀", 7, "무료", "도서관 진입", 22, "정문 통과", 1),
    BusInfo(3, "3001번", "시내", 12, "유료", "3정거장 전", 24, "OO아파트", 2),
    BusInfo(4, "11-1번", "시내", 15, "유료", "차고지 출발", 30, "회차점 대기", 3)
)
val homeBuses = listOf(
    BusInfo(5, "셔틀 C", "셔틀", 2, "무료", "진입중", 15, "출발 대기", 1),
    BusInfo(6, "셔틀 A", "셔틀", 10, "무료", "학교 앞 사거리", 25, "역전 도착", 2),
    BusInfo(7, "3001번", "시내", 18, "유료", "터미널 사거리", 35, "시청 앞", 3)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHomeScreen(
    onNavigateToEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToWithdraw: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    hasNewNotifications: Boolean,
    onNavigateToPrediction: () -> Unit,
    onSetSelectedBus: (BusInfo?) -> Unit,
    initialSelectedBus: BusInfo?,
    isGoingToSchool: Boolean,
    onModeChange: (Boolean) -> Unit,
    viewModel: ProfileViewModel // [추가] ViewModel 주입
) {
    // ViewModel 상태 구독 (Drawer 정보 업데이트용)
    val profileState by viewModel.uiState.collectAsState()

    val baseBusList = if (isGoingToSchool) schoolBuses else homeBuses
    var liveBusList by remember { mutableStateOf<List<BusInfo>>(emptyList()) }
    var busError by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedStation by remember { mutableStateOf(mockStations.first()) }
    val currentBusList = remember(liveBusList, baseBusList, selectedStation) {
        val source = if (liveBusList.isNotEmpty()) liveBusList else baseBusList
        if (liveBusList.isNotEmpty()) source else source.filter { it.stationId == selectedStation.id }
    }
    var selectedBus by remember { mutableStateOf(initialSelectedBus) }

    LaunchedEffect(initialSelectedBus) { selectedBus = initialSelectedBus }

    var showLogoutDialog by remember { mutableStateOf(false) }
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    suspend fun refreshBusArrivals() {
        isRefreshing = true
        try {
            val response = UnibusRepository.busArrivals()
            liveBusList = response.arrivals.mapIndexed { index, arrival ->
                BusInfo(
                    id = index + 1,
                    number = arrival.route_no,
                    type = "시내",
                    eta = arrival.eta_minutes ?: 0,
                    cost = "유료",
                    currentLocation = arrival.headsign ?: (arrival.route_name ?: "도착 정보"),
                    nextBusEta = arrival.eta_minutes_next ?: (arrival.eta_minutes ?: 0) + 8,
                    nextBusLocation = response.stop_name ?: "정류장",
                    stationId = selectedStation.id
                )
            }
            busError = null
        } catch (e: Exception) {
            busError = e.message
        } finally {
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) { refreshBusArrivals() }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = "로그아웃", fontWeight = FontWeight.Bold) },
            text = { Text(text = "정말 로그아웃 하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) { Text("예", color = UnibusBlue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("아니오", color = Color.Gray) }
            },
            containerColor = White, shape = RoundedCornerShape(16.dp)
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        modifier = Modifier.fillMaxHeight().width(300.dp),
                        drawerContainerColor = White,
                        drawerShape = RoundedCornerShape(0.dp)
                    ) {
                        DrawerContent(
                            userNickname = profileState.nickname, // [수정] ViewModel 데이터 사용
                            profileImageUri = profileState.profileImageUri, // [수정] ViewModel 데이터 사용
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
                    sheetContainerColor = Color.Transparent,
                    sheetContentColor = MaterialTheme.colorScheme.onSurface,
                    sheetTonalElevation = 0.dp,
                    sheetShadowElevation = 0.dp,
                    sheetDragHandle = null,
                    sheetPeekHeight = 160.dp,
                    sheetContent = {
                        BottomSheetContent(
                            busList = currentBusList,
                            selectedStation = selectedStation,
                            selectedBus = selectedBus,
                            onStationChange = { station -> selectedStation = station },
                            onBusClick = { bus ->
                                if (selectedBus?.id == bus.id) {
                                    selectedBus = null
                                    onSetSelectedBus(null)
                                } else {
                                    selectedBus = bus
                                    onSetSelectedBus(bus)
                                }
                            },
                            isRefreshing = isRefreshing,
                            errorMessage = busError,
                            onRefresh = { scope.launch { refreshBusArrivals() } }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        MapArea(
                            selectedBus = selectedBus,
                            selectedStation = selectedStation,
                            onStationClick = { marker ->
                                val station = mockStations.first { it.id == marker.id }
                                selectedStation = station
                                selectedBus = null
                                onSetSelectedBus(null)
                                scope.launch { bottomSheetScaffoldState.bottomSheetState.expand() }
                            }
                        )
                        TopControlBar(
                            isGoingToSchool = isGoingToSchool,
                            onModeChange = { isSchool ->
                                onModeChange(isSchool)
                                selectedBus = null
                                onSetSelectedBus(null)
                                selectedStation = mockStations.first()
                                liveBusList = emptyList()
                                busError = null
                            },
                            onPredictionClick = onNavigateToPrediction,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            hasNewNotifications = hasNewNotifications,
                            onNotificationsClick = onNavigateToNotifications
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// Bottom Sheet Content & Other Components (기존 유지)
// ---------------------------------------------------------
@Composable
fun BottomSheetContent(
    busList: List<BusInfo>,
    selectedStation: Station,
    selectedBus: BusInfo?,
    onStationChange: (Station) -> Unit,
    onBusClick: (BusInfo) -> Unit,
    isRefreshing: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit
) {
    val fastestBus = busList.firstOrNull()
    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f)) {
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight().padding(top = 0.dp),
            color = White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), shadowElevation = 10.dp
        ) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color.LightGray, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "➡️ 목적지: ${selectedStation.name}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp).clickable { })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "현재 ${selectedStation.name} 정보", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                }
                if (fastestBus != null) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = "최적 경로", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Text(text = "${fastestBus.number} (${fastestBus.cost} / ${fastestBus.eta}분 뒤 도착)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = UnibusBlue)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp))
                }
                errorMessage?.let { msg -> Text(text = msg, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall) }
                LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
                    if (busList.isEmpty() && !isRefreshing) {
                        item { Text(text = "표시할 버스 정보가 없습니다. 새로고침을 눌러주세요.", modifier = Modifier.padding(horizontal = 24.dp), color = Color.Gray) }
                    } else {
                        items(busList) { bus ->
                            BusListItem(bus = bus, isSelected = selectedBus?.id == bus.id, onClick = { onBusClick(bus) })
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onRefresh, containerColor = White, contentColor = UnibusBlue, shape = CircleShape,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 24.dp).offset(y = 60.dp).shadow(4.dp, CircleShape)
        ) {
            if (isRefreshing) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = UnibusBlue, strokeWidth = 2.dp)
            else Icon(Icons.Rounded.Refresh, contentDescription = "새로고침")
        }
    }
}

@Composable
fun BusListItem(bus: BusInfo, isSelected: Boolean, onClick: () -> Unit) {
    val SkyBlue = Color(0xFF64B5F6)
    var isNotificationEnabled by remember { mutableStateOf(false) }
    val backgroundColor = if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
    val borderColor = if (isSelected) UnibusBlue else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp
    Row(
        modifier = Modifier.fillMaxWidth().background(backgroundColor, RoundedCornerShape(12.dp)).border(borderWidth, borderColor, RoundedCornerShape(12.dp)).clickable { onClick() }.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(0.25f)) {
            Text(bus.number, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = bus.cost, style = MaterialTheme.typography.bodySmall, color = if (bus.cost == "무료") Color(0xFF4CAF50) else Color.Gray, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(0.35f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = if (bus.eta == 0) "곧 도착" else "${bus.eta}분 뒤", style = MaterialTheme.typography.titleLarge, color = UnibusBlue, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Place, null, Modifier.size(12.dp), Color.Gray)
                Spacer(Modifier.width(2.dp))
                Text(bus.currentLocation, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
            }
        }
        Column(modifier = Modifier.weight(0.25f), horizontalAlignment = Alignment.End) {
            Text("${bus.nextBusEta}분 뒤", style = MaterialTheme.typography.titleMedium, color = SkyBlue, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Place, null, Modifier.size(12.dp), SkyBlue.copy(alpha = 0.7f))
                Spacer(Modifier.width(2.dp))
                Text(bus.nextBusLocation, style = MaterialTheme.typography.labelSmall, color = Color.Gray.copy(alpha = 0.8f), maxLines = 1)
            }
        }
        IconButton(onClick = { isNotificationEnabled = !isNotificationEnabled }, modifier = Modifier.size(36.dp)) {
            Icon(imageVector = if (isNotificationEnabled) Icons.Rounded.Notifications else Icons.Rounded.NotificationsNone, contentDescription = "알림 설정", tint = if (isNotificationEnabled) UnibusBlue else Color.Gray)
        }
    }
}

// ---------------------------------------------------------
// Drawer Content (수정됨: 뷰모델 데이터 반영)
// ---------------------------------------------------------
@Composable
fun DrawerContent(
    userNickname: String,
    profileImageUri: Uri?, // [추가] 이미지 URI 받기
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onWithdrawClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 60.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 40.dp)) {
            // [수정] 프로필 이미지 표시 로직 (Coil 사용)
            if (profileImageUri != null) {
                AsyncImage(
                    model = profileImageUri,
                    contentDescription = "Profile Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.LightGray)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Profile Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.LightGray)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = userNickname, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(text = "오늘도 좋은 하루!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
        Divider(color = Color.LightGray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(20.dp))
        DrawerMenuItem(text = "회원정보 수정", onClick = onEditProfileClick)
        DrawerMenuItem(text = "로그아웃", onClick = onLogoutClick)
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onWithdrawClick, modifier = Modifier.align(Alignment.Start)) {
            Text("회원탈퇴", color = Color.Gray, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun DrawerMenuItem(text: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TopControlBar(isGoingToSchool: Boolean, onModeChange: (Boolean) -> Unit, onPredictionClick: () -> Unit, onMenuClick: () -> Unit, hasNewNotifications: Boolean, onNotificationsClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp).height(56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onPredictionClick, modifier = Modifier.size(48.dp).background(White, CircleShape).shadow(4.dp, CircleShape)) {
            Icon(Icons.Rounded.AutoGraph, contentDescription = "예측", tint = UnibusBlue)
        }
        Box(modifier = Modifier.width(180.dp).height(48.dp).shadow(4.dp, RoundedCornerShape(50)).background(White, RoundedCornerShape(50)).clip(RoundedCornerShape(50))) {
            Row(modifier = Modifier.fillMaxSize()) {
                ModeButton("🏫 등교", isGoingToSchool, Modifier.weight(1f)) { onModeChange(true) }
                ModeButton("🏠 하교", !isGoingToSchool, Modifier.weight(1f)) { onModeChange(false) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onNotificationsClick, modifier = Modifier.size(48.dp).background(White, CircleShape).shadow(4.dp, CircleShape)) {
                Icon(imageVector = if (hasNewNotifications) Icons.Rounded.Notifications else Icons.Rounded.NotificationsNone, contentDescription = "알림", tint = if (hasNewNotifications) MaterialTheme.colorScheme.error else Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onMenuClick, modifier = Modifier.size(48.dp).background(White, CircleShape).shadow(4.dp, CircleShape)) {
                Icon(Icons.Rounded.Menu, contentDescription = "메뉴", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun ModeButton(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.fillMaxSize().background(if (isSelected) UnibusBlue else Color.Transparent).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(text, color = if (isSelected) White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun MapArea(selectedBus: BusInfo?, selectedStation: Station, onStationClick: (StationMarker) -> Unit) {
    var translation by remember { mutableStateOf(Offset.Zero) }
    val estimatedArrivalTime = remember(selectedBus) {
        if (selectedBus != null) java.time.LocalTime.now().plusMinutes(selectedBus.eta.toLong()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) else null
    }
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFE3F2FD))
            .pointerInput(Unit) { detectTapGestures { offset ->
                val adjustedOffset = offset - translation
                for (marker in mockStationMarkers) {
                    val markerX = marker.xRatio * size.width
                    val markerY = marker.yRatio * size.height
                    if (adjustedOffset.x > markerX - 60f && adjustedOffset.x < markerX + 60f && adjustedOffset.y > markerY - 60f && adjustedOffset.y < markerY + 60f) {
                        onStationClick(marker); break
                    }
                }
            }}
            .pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); translation += dragAmount } }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            withTransform({ translate(left = translation.x, top = translation.y) }) {
                val center = center
                drawCircle(color = UnibusBlue.copy(alpha = 0.2f), radius = 60f, center = center)
                drawCircle(color = UnibusBlue, radius = 20f, center = center)
                selectedBus?.let { bus ->
                    val destinationOffset = Offset(size.width * 0.9f, size.height * 0.1f)
                    val currentBusLocation = Offset(size.width * 0.3f, size.height * 0.4f)
                    val stationOffset = mockStationMarkers.firstOrNull { it.id == selectedStation.id }?.let { Offset(it.xRatio * size.width, it.yRatio * size.height) } ?: center
                    drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = Offset(size.width * 0.1f, size.height * 0.2f), end = currentBusLocation, strokeWidth = 12f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f), cap = StrokeCap.Round)
                    drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = currentBusLocation, end = stationOffset, strokeWidth = 12f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f), cap = StrokeCap.Round)
                    drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = stationOffset, end = destinationOffset, strokeWidth = 12f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f), cap = StrokeCap.Round)
                    drawLine(color = UnibusBlue, start = currentBusLocation, end = stationOffset, strokeWidth = 15f, cap = StrokeCap.Round)
                    drawCircle(color = White, radius = 20f, center = currentBusLocation)
                    drawCircle(color = UnibusBlue.copy(alpha = 0.8f), radius = 15f, center = currentBusLocation)
                    drawCircle(color = Color.Red, radius = 15f, center = destinationOffset)
                }
                mockStationMarkers.forEach { marker ->
                    val markerX = marker.xRatio * size.width
                    val markerY = marker.yRatio * size.height
                    drawCircle(color = if (marker.id == selectedStation.id) Color(0xFFFFC107) else Color.DarkGray, radius = if (marker.id == selectedStation.id) 30f else 20f, center = Offset(markerX, markerY))
                    drawCircle(color = White, radius = 8f, center = Offset(markerX, markerY))
                }
            }
        }
        if (selectedBus != null) {
            Surface(modifier = Modifier.align(Alignment.Center).offset(x = 100.dp, y = (-150).dp), shape = RoundedCornerShape(8.dp), color = White, shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "${selectedBus.number} (${selectedBus.cost})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = UnibusBlue)
                    Text(text = "$estimatedArrivalTime 도착 예상", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

fun Modifier.shadow(elevation: androidx.compose.ui.unit.Dp, shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape, clip: Boolean = false, ambientColor: Color = androidx.compose.ui.graphics.Color.Black, spotColor: Color = androidx.compose.ui.graphics.Color.Black): Modifier = this.then(Modifier.graphicsLayer { shadowElevation = elevation.toPx(); this.shape = shape; this.clip = clip; this.ambientShadowColor = ambientColor; this.spotShadowColor = spotColor })