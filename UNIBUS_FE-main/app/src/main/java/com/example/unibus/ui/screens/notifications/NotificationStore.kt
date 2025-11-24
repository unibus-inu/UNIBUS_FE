package com.example.unibus.ui.screens.notifications

import androidx.compose.runtime.mutableStateListOf
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 간단한 인메모리 알림 저장소.
 * ETA 임계값(1분 이하) 도달 시 Prediction 화면에서 푸시하고,
 * NotificationScreen에서는 동일한 리스트를 그대로 표시한다.
 */
object NotificationStore {
    val notifications = mutableStateListOf<ArrivalNotification>()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun addArrivalAlert(stopName: String, busNumber: String) {
        val nowText = LocalTime.now().format(timeFormatter)
        val nextId = (notifications.maxOfOrNull { it.id } ?: 0) + 1
        notifications.add(
            ArrivalNotification(
                id = nextId,
                stationName = stopName,
                busNumber = busNumber,
                isRead = false,
                time = nowText
            )
        )
    }

    fun markAllRead() {
        notifications.replaceAll { it.copy(isRead = true) }
    }
}
