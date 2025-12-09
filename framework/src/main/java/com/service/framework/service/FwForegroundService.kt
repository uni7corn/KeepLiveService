package com.service.framework.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.service.framework.Fw
import com.service.framework.util.FwLog

/**
 * 前台保活服务
 *
 * 核心机制：
 * 1. 使用 MediaSession 让系统认为是媒体应用
 * 2. 前台通知保持服务存活
 * 3. WakeLock 防止 CPU 休眠
 * 4. START_STICKY 确保被杀后重启
 *
 * 安全研究要点：
 * - 酷狗等音乐应用使用 foregroundServiceType="mediaPlayback"
 * - MediaSession 让系统认为需要保持活跃以响应蓝牙耳机按键
 * - 这是绕过后台限制的关键
 */
class FwForegroundService : LifecycleService() {

    companion object {
        const val EXTRA_START_REASON = "start_reason"
        private const val NOTIFICATION_ID = 10001
    }

    private var mediaSession: MediaSessionCompat? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        FwLog.d("FwForegroundService onCreate")

        // 1. 初始化 MediaSession（关键：让系统认为是媒体应用）
        if (Fw.config?.enableMediaSession == true) {
            initMediaSession()
        }

        // 2. 获取 WakeLock 防止 CPU 休眠
        acquireWakeLock()

        // 3. 启动前台通知
        startForegroundWithNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val reason = intent?.getStringExtra(EXTRA_START_REASON) ?: "未知原因"
        FwLog.d("服务启动，原因: $reason")

        // 确保前台通知存在
        startForegroundWithNotification()

        // START_STICKY：服务被杀后系统会尝试重启
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        FwLog.d("FwForegroundService onDestroy")

        // 释放 MediaSession
        mediaSession?.apply {
            isActive = false
            release()
        }
        mediaSession = null

        // 释放 WakeLock
        releaseWakeLock()

        // 尝试重启服务（被杀后自救）
        tryRestartSelf()
    }

    /**
     * 初始化 MediaSession
     *
     * 研究要点：
     * 1. MediaSession 是蓝牙耳机连接时唤醒的关键
     * 2. 系统会向所有有活跃 MediaSession 的应用发送媒体事件
     * 3. 即使应用被杀，蓝牙连接也会唤醒有 MediaSession 的应用
     */
    private fun initMediaSession() {
        try {
            mediaSession = MediaSessionCompat(this, "FwMediaSession").apply {
                // 设置为可接收媒体按键事件
                setFlags(
                    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                )

                // 设置播放状态为"暂停"（关键：让系统认为随时可能播放）
                setPlaybackState(
                    PlaybackStateCompat.Builder()
                        .setState(
                            PlaybackStateCompat.STATE_PAUSED,
                            0L,
                            1.0f
                        )
                        .setActions(
                            PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_STOP or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        )
                        .build()
                )

                // 设置回调处理媒体按键
                setCallback(object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        FwLog.d("MediaSession onPlay - 收到播放按键")
                    }

                    override fun onPause() {
                        FwLog.d("MediaSession onPause - 收到暂停按键")
                    }

                    override fun onStop() {
                        FwLog.d("MediaSession onStop - 收到停止按键")
                    }

                    override fun onSkipToNext() {
                        FwLog.d("MediaSession onSkipToNext - 收到下一曲")
                    }

                    override fun onSkipToPrevious() {
                        FwLog.d("MediaSession onSkipToPrevious - 收到上一曲")
                    }

                    override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                        FwLog.d("MediaSession onMediaButtonEvent: ${mediaButtonEvent?.action}")
                        return super.onMediaButtonEvent(mediaButtonEvent)
                    }
                })

                // 激活 MediaSession（关键：必须激活才能接收事件）
                isActive = true
            }

            FwLog.d("MediaSession 初始化成功")
        } catch (e: Exception) {
            FwLog.e("MediaSession 初始化失败: ${e.message}", e)
        }
    }

    /**
     * 获取 WakeLock
     */
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Fw::WakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L) // 10 分钟
            }
            FwLog.d("WakeLock 获取成功")
        } catch (e: Exception) {
            FwLog.e("WakeLock 获取失败: ${e.message}", e)
        }
    }

    /**
     * 释放 WakeLock
     */
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
            FwLog.d("WakeLock 已释放")
        } catch (e: Exception) {
            FwLog.e("WakeLock 释放失败: ${e.message}", e)
        }
    }

    /**
     * 启动前台通知
     */
    private fun startForegroundWithNotification() {
        val config = Fw.config ?: return

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0+ 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                config.notificationChannelId,
                config.notificationChannelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于保持服务运行"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = buildNotification()

        try {
            // Android 14+ 需要指定前台服务类型
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            FwLog.d("前台服务启动成功")
        } catch (e: Exception) {
            FwLog.e("前台服务启动失败: ${e.message}", e)
        }
    }

    /**
     * 构建通知
     */
    private fun buildNotification(): Notification {
        val config = Fw.config ?: return NotificationCompat.Builder(this, "default")
            .setContentTitle("服务运行中")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        // 创建点击通知的 PendingIntent
        val pendingIntent = config.notificationActivityClass?.let { activityClass ->
            val intent = Intent(this, activityClass).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            PendingIntent.getActivity(
                this,
                0,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
        }

        return NotificationCompat.Builder(this, config.notificationChannelId)
            .setContentTitle(config.notificationTitle)
            .setContentText(config.notificationContent)
            .setSmallIcon(config.notificationIconResId)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * 尝试重启自身
     */
    private fun tryRestartSelf() {
        try {
            val intent = Intent(applicationContext, FwForegroundService::class.java).apply {
                putExtra(EXTRA_START_REASON, "服务被杀后自救")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
            FwLog.d("尝试重启服务")
        } catch (e: Exception) {
            FwLog.e("重启服务失败: ${e.message}", e)
        }
    }
}
