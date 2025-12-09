package com.service.framework.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.core.app.NotificationCompat
import com.service.framework.Fw
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter
import java.util.Timer
import java.util.TimerTask

/**
 * 守护进程服务（运行在独立进程）
 *
 * 核心机制：
 * 1. 运行在 :daemon 进程
 * 2. 定时检查主进程是否存活
 * 3. 主进程死亡时唤醒主进程
 * 4. 与主进程互相守护
 *
 * 安全研究要点：
 * - 双进程守护是常见的保活手段
 * - 利用两个进程互相监控，一个死亡另一个立即拉起
 * - 可以通过检测进程是否存在或通过 AIDL/Socket 通信
 */
class DaemonService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 10002
        private const val CHECK_INTERVAL = 3000L // 3秒检查一次
    }

    private var checkTimer: Timer? = null

    override fun onCreate() {
        super.onCreate()
        FwLog.d("DaemonService onCreate, PID: ${Process.myPid()}")

        // 启动前台通知
        startForegroundWithNotification()

        // 启动定时检查
        startCheckTimer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FwLog.d("DaemonService onStartCommand")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        FwLog.d("DaemonService onDestroy")

        // 停止定时器
        checkTimer?.cancel()
        checkTimer = null

        // 尝试拉起主服务
        tryStartMainService()

        // 尝试重启自己
        tryRestartSelf()
    }

    /**
     * 启动前台通知
     */
    private fun startForegroundWithNotification() {
        val channelId = "fw_daemon_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                "守护服务",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "后台守护服务"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("守护服务")
            .setContentText("正在运行...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            FwLog.e("DaemonService 启动前台服务失败: ${e.message}", e)
        }
    }

    /**
     * 启动定时检查
     */
    private fun startCheckTimer() {
        checkTimer?.cancel()
        checkTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    checkAndRestartMainService()
                }
            }, CHECK_INTERVAL, CHECK_INTERVAL)
        }
    }

    /**
     * 检查并重启主服务
     */
    private fun checkAndRestartMainService() {
        try {
            if (!isMainServiceRunning()) {
                FwLog.d("检测到主服务未运行，尝试拉起")
                tryStartMainService()
            }
        } catch (e: Exception) {
            FwLog.e("检查主服务状态失败: ${e.message}", e)
        }
    }

    /**
     * 检查主服务是否运行
     */
    private fun isMainServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val runningServices = activityManager?.getRunningServices(Int.MAX_VALUE) ?: return false

        val mainServiceName = FwForegroundService::class.java.name
        return runningServices.any { it.service.className == mainServiceName }
    }

    /**
     * 尝试启动主服务
     */
    private fun tryStartMainService() {
        try {
            ServiceStarter.startForegroundService(applicationContext, "守护进程拉起")
        } catch (e: Exception) {
            FwLog.e("拉起主服务失败: ${e.message}", e)
        }
    }

    /**
     * 尝试重启自己
     */
    private fun tryRestartSelf() {
        try {
            val intent = Intent(applicationContext, DaemonService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
        } catch (e: Exception) {
            FwLog.e("重启守护服务失败: ${e.message}", e)
        }
    }
}
