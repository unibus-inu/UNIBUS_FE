package com.example.unibus.utils


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.unibus.R // R 클래스 import 확인 필요 (본인 패키지명에 맞게)

/**
 * 안드로이드 시스템 알림(상단바)을 관리하는 객체입니다.
 */
object NotificationUtils {
    private const val CHANNEL_ID = "bus_arrival_channel"
    private const val CHANNEL_NAME = "버스 도착 알림"

    /**
     * 알림 채널을 생성합니다. (안드로이드 8.0 오레오 이상 필수)
     * 앱 시작 시(MainActivity 등) 한 번만 호출해주면 됩니다.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "설정한 버스가 도착하기 직전에 알림을 보냅니다."
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 실제 알림을 발송하는 함수입니다.
     * @param busNumber 버스 번호 (예: "셔틀 A")
     * @param stationName 정류장 이름
     */
    fun sendArrivalNotification(context: Context, busNumber: String, stationName: String) {
        val notificationId = System.currentTimeMillis().toInt() // 고유 ID 생성 (알림이 쌓이도록)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // [주의] 알림 아이콘 (drawable에 실제 아이콘이 있어야 함)
            .setContentTitle("🚌 버스 도착 임박!")
            .setContentText("$busNumber 버스가 곧 $stationName 에 도착합니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 헤드업 알림 표시
            .setAutoCancel(true) // 터치 시 삭제

        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // 알림 권한이 없을 경우 예외 처리
            e.printStackTrace()
        }
    }
}