/**
 * ===============================================================================
 * Fw Android Keep-Alive Framework - Main Entry Point
 * ===============================================================================
 *
 * @author qihao (Pangu-Immortal)
 * @github https://github.com/Pangu-Immortal
 * @createDate 2025-12-09
 *
 * @description
 * Fw 保活框架主入口类
 * 提供一行代码初始化的便捷方式，统一管理所有保活策略
 *
 * Main entry point for the Fw Keep-Alive Framework
 * Provides one-line initialization and unified management of all keep-alive strategies
 *
 * @features
 * - 模块化设计，策略可独立开关
 * - 支持 20+ 种保活策略
 * - 适配 Android 7.0 - 16（API 24-36）
 * - 详细日志记录，便于调试和安全研究
 *
 * @usage
 * 基础用法（使用默认配置）:
 * ```kotlin
 * // 在 Application.onCreate() 中初始化
 * Fw.init(this)
 * ```
 *
 * 自定义配置:
 * ```kotlin
 * Fw.init(this) {
 *     // 基础策略
 *     enableForegroundService(true)      // 前台服务（核心）
 *     enableMediaSession(true)           // MediaSession 媒体会话
 *     enableOnePixelActivity(true)       // 1像素 Activity
 *     enableLockScreenActivity(false)    // 锁屏 Activity
 *     enableFloatWindow(false)           // 悬浮窗
 *
 *     // 定时唤醒策略
 *     enableJobScheduler(true)           // JobScheduler 定时任务
 *     enableWorkManager(true)            // WorkManager 任务调度
 *     enableAlarmManager(true)           // AlarmManager 闹钟唤醒
 *
 *     // 账户同步策略
 *     enableAccountSync(true)            // 账户同步机制
 *
 *     // 广播监听策略
 *     enableSystemBroadcast(true)        // 系统广播监听
 *     enableBluetoothBroadcast(true)     // 蓝牙广播（核心）
 *     enableMediaButtonReceiver(true)    // 媒体按键监听
 *
 *     // 内容观察者策略
 *     enableMediaContentObserver(true)   // 相册变化监听
 *     enableFileObserver(true)           // 文件系统监听
 *
 *     // 双进程策略
 *     enableDualProcess(true)            // Java 层双进程守护
 *
 *     // Native 层策略
 *     enableNativeDaemon(true)           // Native C++ 守护进程
 *     enableNativeSocket(true)           // Native Socket 心跳
 *
 *     // 通知配置
 *     notificationTitle("服务运行中")
 *     notificationContent("点击打开应用")
 *     notificationActivityClass(MainActivity::class.java)
 * }
 * ```
 *
 * 控制方法:
 * ```kotlin
 * Fw.check()           // 手动触发保活检查
 * Fw.stop()            // 停止所有保活策略
 * Fw.isInitialized()   // 检查是否已初始化
 * ```
 *
 * @securityResearch
 * 安全研究要点：
 * - 本框架用于研究商业应用（如酷狗音乐、墨迹天气）的保活机制
 * - 每种策略的实现都有详细注释，便于理解原理
 * - 所有操作都有详细日志输出，便于分析执行流程
 * - 仅供安全研究和学习使用，请遵守法律法规
 *
 * @strategies
 * 支持的保活策略列表：
 *
 * 1. 基础策略（进程优先级提升）
 *    - ForegroundService: 前台服务，提升为前台进程
 *    - MediaSession: 媒体会话，系统认为正在播放媒体
 *    - OnePixelActivity: 1像素透明Activity，锁屏时启动
 *    - LockScreenActivity: 锁屏界面Activity
 *    - FloatWindow: 悬浮窗，保持窗口可见
 *
 * 2. 定时唤醒策略
 *    - JobScheduler: 系统级任务调度
 *    - WorkManager: Jetpack 任务调度
 *    - AlarmManager: 精确闹钟唤醒
 *
 * 3. 账户同步策略
 *    - AccountSync: SyncAdapter 账户同步机制
 *
 * 4. 广播监听策略
 *    - BluetoothReceiver: 蓝牙设备连接（核心：酷狗唤醒机制）
 *    - UsbReceiver: USB设备连接
 *    - NfcReceiver: NFC标签发现
 *    - MediaButtonReceiver: 媒体按键
 *    - MediaMountReceiver: 存储设备挂载
 *    - SystemEventReceiver: 系统事件（开机、网络变化等）
 *
 * 5. 内容观察者策略
 *    - ContentObserver: 监听系统数据变化（相册、联系人、短信）
 *    - FileObserver: 监听文件系统变化（下载、截图）
 *
 * 6. 系统级服务策略
 *    - AccessibilityService: 无障碍服务
 *    - NotificationListenerService: 通知监听服务
 *
 * 7. 双进程守护策略
 *    - DaemonService: Java层独立进程守护
 *    - NativeDaemon: Native C++ fork守护进程
 *    - NativeSocket: Unix Domain Socket心跳
 *
 * ===============================================================================
 */

package com.service.framework

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.service.framework.account.FwAuthenticator
import com.service.framework.account.FwSyncAdapter
import com.service.framework.core.FwConfig
import com.service.framework.native.FwNative
import com.service.framework.observer.ContentObserverManager
import com.service.framework.observer.FileObserverManager
import com.service.framework.receiver.MediaButtonReceiver
import com.service.framework.receiver.SystemEventReceiver
import com.service.framework.receiver.WifiReceiver
import com.service.framework.service.DaemonService
import com.service.framework.strategy.AlarmStrategy
import com.service.framework.strategy.FloatWindowManager
import com.service.framework.strategy.FwJobService
import com.service.framework.strategy.FwWorker
import com.service.framework.strategy.LockScreenActivity
import com.service.framework.strategy.OnePixelActivity
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * Fw 保活模块入口
 *
 * 使用方式：
 * ```kotlin
 * // 在 Application.onCreate() 中初始化
 * Fw.init(this) {
 *     enableForegroundService(true)
 *     enableMediaSession(true)
 *     enableJobScheduler(true)
 *     enableWorkManager(true)
 *     enableAlarmManager(true)
 *     enableAccountSync(true)
 *     enableBluetoothBroadcast(true)
 *     enableNativeDaemon(true)
 *     enableFileObserver(true)
 *     enableLockScreenActivity(true)
 *     enableFloatWindow(true)
 *     notificationTitle("音乐播放中")
 *     notificationContent("点击打开应用")
 *     notificationActivityClass(MainActivity::class.java)
 * }
 * ```
 *
 * 或使用默认配置：
 * ```kotlin
 * Fw.init(this)
 * ```
 *
 * 安全研究要点：
 * - 模块化设计，方便研究各种保活策略
 * - 每种策略都可以单独开关
 * - 详细日志记录，便于分析
 */
object Fw {

    var config: FwConfig? = null
        private set

    private var isInitialized = false
    private var application: Application? = null

    // 动态注册的广播接收器
    private var systemEventReceiver: BroadcastReceiver? = null
    private var mediaButtonReceiver: BroadcastReceiver? = null
    private var wifiReceiver: BroadcastReceiver? = null
    private var screenReceiver: BroadcastReceiver? = null
    private var lockScreenReceiver: BroadcastReceiver? = null

    /**
     * 初始化（使用默认配置）
     */
    fun init(application: Application) {
        init(application, FwConfig())
    }

    /**
     * 初始化（使用 Builder）
     */
    fun init(application: Application, builder: FwConfig.Builder.() -> Unit) {
        val configBuilder = FwConfig.Builder()
        configBuilder.builder()
        init(application, configBuilder.build())
    }

    /**
     * 初始化
     */
    fun init(application: Application, config: FwConfig) {
        if (isInitialized) {
            FwLog.w("Fw 已初始化，跳过")
            return
        }

        this.application = application
        this.config = config
        this.isInitialized = true

        FwLog.d("========================================")
        FwLog.d("Fw 保活模块初始化")
        FwLog.d("========================================")
        FwLog.d("配置信息:")
        FwLog.d("  - 前台服务: ${config.enableForegroundService}")
        FwLog.d("  - MediaSession: ${config.enableMediaSession}")
        FwLog.d("  - 1像素Activity: ${config.enableOnePixelActivity}")
        FwLog.d("  - 锁屏Activity: ${config.enableLockScreenActivity}")
        FwLog.d("  - 悬浮窗: ${config.enableFloatWindow}")
        FwLog.d("  - JobScheduler: ${config.enableJobScheduler}")
        FwLog.d("  - WorkManager: ${config.enableWorkManager}")
        FwLog.d("  - AlarmManager: ${config.enableAlarmManager}")
        FwLog.d("  - 账户同步: ${config.enableAccountSync}")
        FwLog.d("  - 系统广播: ${config.enableSystemBroadcast}")
        FwLog.d("  - 蓝牙广播: ${config.enableBluetoothBroadcast}")
        FwLog.d("  - 媒体按键: ${config.enableMediaButtonReceiver}")
        FwLog.d("  - 相册监听: ${config.enableMediaContentObserver}")
        FwLog.d("  - 文件监听: ${config.enableFileObserver}")
        FwLog.d("  - 双进程: ${config.enableDualProcess}")
        FwLog.d("  - Native守护: ${config.enableNativeDaemon}")
        FwLog.d("  - Native Socket: ${config.enableNativeSocket}")
        FwLog.d("========================================")

        // 启动所有保活策略
        startAllStrategies()
    }

    /**
     * 启动所有保活策略
     */
    private fun startAllStrategies() {
        val context = application ?: return
        val config = config ?: return

        // 1. 启动前台服务
        if (config.enableForegroundService) {
            FwLog.d("启动前台服务...")
            ServiceStarter.startForegroundService(context, "初始化启动")
        }

        // 2. 启动守护进程（Java 层）
        if (config.enableDualProcess) {
            FwLog.d("启动守护进程...")
            startDaemonService(context)
        }

        // 3. 调度 JobScheduler
        if (config.enableJobScheduler) {
            FwLog.d("调度 JobScheduler...")
            FwJobService.schedule(context)
        }

        // 4. 调度 WorkManager
        if (config.enableWorkManager) {
            FwLog.d("调度 WorkManager...")
            FwWorker.schedule(context)
        }

        // 5. 调度 AlarmManager
        if (config.enableAlarmManager) {
            FwLog.d("调度 AlarmManager...")
            AlarmStrategy.schedule(context)
        }

        // 6. 注册账户同步
        if (config.enableAccountSync) {
            FwLog.d("注册账户同步...")
            FwAuthenticator.addAccount(context)
            FwSyncAdapter.enableSync(context)
        }

        // 7. 动态注册系统广播
        if (config.enableSystemBroadcast) {
            FwLog.d("注册系统广播...")
            registerDynamicReceivers(context)
        }

        // 8. 注册 ContentObserver
        if (config.enableMediaContentObserver ||
            config.enableContactsContentObserver ||
            config.enableSmsContentObserver ||
            config.enableSettingsContentObserver) {
            FwLog.d("注册 ContentObserver...")
            ContentObserverManager.registerAll(context)
        }

        // 9. 注册 FileObserver
        if (config.enableFileObserver) {
            FwLog.d("启动 FileObserver...")
            FileObserverManager.registerAll(context)
        }

        // 10. 注册屏幕开关监听（用于1像素Activity）
        if (config.enableOnePixelActivity) {
            FwLog.d("注册屏幕监听（1像素Activity）...")
            registerScreenReceiver(context)
        }

        // 11. 注册锁屏监听（用于锁屏Activity）
        if (config.enableLockScreenActivity) {
            FwLog.d("注册锁屏监听...")
            registerLockScreenReceiver(context)
        }

        // 12. 启动悬浮窗
        if (config.enableFloatWindow) {
            FwLog.d("启动悬浮窗...")
            startFloatWindow(context)
        }

        // 13. 初始化 Native 模块
        if (config.enableNativeDaemon || config.enableNativeSocket) {
            FwLog.d("初始化 Native 模块...")
            initNativeModule(context)
        }

        FwLog.d("========================================")
        FwLog.d("所有保活策略已启动")
        FwLog.d("========================================")
    }

    /**
     * 启动守护进程服务
     */
    private fun startDaemonService(context: Context) {
        try {
            val intent = Intent(context, DaemonService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            FwLog.e("启动守护进程失败: ${e.message}", e)
        }
    }

    /**
     * 初始化 Native 模块
     */
    private fun initNativeModule(context: Context) {
        val config = config ?: return

        // 初始化 Native 库
        if (!FwNative.init(context)) {
            FwLog.w("Native 模块初始化失败，跳过 Native 层策略")
            return
        }

        // 启动 Native 守护进程
        if (config.enableNativeDaemon) {
            FwLog.d("启动 Native 守护进程...")
            val result = FwNative.startDaemon(
                context.packageName,
                "com.service.framework.service.FwForegroundService",
                config.nativeDaemonCheckInterval
            )
            FwLog.d("Native 守护进程启动: ${if (result) "成功" else "失败"}")
        }

        // 启动 Native Socket 服务
        if (config.enableNativeSocket) {
            FwLog.d("启动 Native Socket 服务...")
            val result = FwNative.startSocketServer(config.nativeSocketName)
            FwLog.d("Native Socket 服务启动: ${if (result) "成功" else "失败"}")
        }
    }

    /**
     * 启动悬浮窗
     */
    private fun startFloatWindow(context: Context) {
        val config = config ?: return

        if (!FloatWindowManager.canDrawOverlays(context)) {
            FwLog.w("没有悬浮窗权限，跳过悬浮窗策略")
            return
        }

        if (config.floatWindowHidden) {
            FloatWindowManager.showOnePixelFloat(context)
        } else {
            FloatWindowManager.showVisibleFloat(context)
        }
    }

    /**
     * 动态注册广播接收器
     */
    private fun registerDynamicReceivers(context: Context) {
        // 系统事件广播
        systemEventReceiver = SystemEventReceiver()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                systemEventReceiver,
                SystemEventReceiver.getDynamicIntentFilter(),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                systemEventReceiver,
                SystemEventReceiver.getDynamicIntentFilter()
            )
        }
        FwLog.d("系统事件广播注册成功")

        // 媒体按键广播
        if (config?.enableMediaButtonReceiver == true) {
            mediaButtonReceiver = MediaButtonReceiver()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    mediaButtonReceiver,
                    MediaButtonReceiver.getIntentFilter(),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    mediaButtonReceiver,
                    MediaButtonReceiver.getIntentFilter()
                )
            }
            FwLog.d("媒体按键广播注册成功")
        }

        // WiFi 广播
        wifiReceiver = WifiReceiver()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                wifiReceiver,
                WifiReceiver.getIntentFilter(),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                wifiReceiver,
                WifiReceiver.getIntentFilter()
            )
        }
        FwLog.d("WiFi 广播注册成功")
    }

    /**
     * 注册屏幕开关监听（用于 1 像素 Activity）
     */
    private fun registerScreenReceiver(context: Context) {
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        FwLog.d("屏幕关闭，启动1像素Activity")
                        OnePixelActivity.start(context)
                    }
                    Intent.ACTION_SCREEN_ON,
                    Intent.ACTION_USER_PRESENT -> {
                        FwLog.d("屏幕点亮，关闭1像素Activity")
                        OnePixelActivity.finish()
                    }
                }
            }
        }

        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(screenReceiver, filter)
        }
        FwLog.d("屏幕监听注册成功")
    }

    /**
     * 注册锁屏监听（用于锁屏 Activity）
     */
    private fun registerLockScreenReceiver(context: Context) {
        lockScreenReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        // 检查是否锁屏
                        if (LockScreenActivity.isKeyguardLocked(context)) {
                            FwLog.d("屏幕锁定，启动锁屏Activity")
                            LockScreenActivity.start(context)
                        }
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        FwLog.d("用户解锁，关闭锁屏Activity")
                        LockScreenActivity.finish()
                    }
                }
            }
        }

        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(lockScreenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(lockScreenReceiver, filter)
        }
        FwLog.d("锁屏监听注册成功")
    }

    /**
     * 停止所有保活策略
     */
    fun stop() {
        val context = application ?: return

        FwLog.d("停止所有保活策略...")

        // 停止前台服务
        ServiceStarter.stopForegroundService(context)

        // 停止守护进程
        try {
            context.stopService(Intent(context, DaemonService::class.java))
        } catch (e: Exception) {
            FwLog.e("停止守护进程失败: ${e.message}", e)
        }

        // 取消 JobScheduler
        FwJobService.cancel(context)

        // 取消 WorkManager
        FwWorker.cancel(context)

        // 取消 AlarmManager
        AlarmStrategy.cancel(context)

        // 禁用账户同步
        FwSyncAdapter.disableSync(context)

        // 取消 ContentObserver
        ContentObserverManager.unregisterAll(context)

        // 停止 FileObserver
        FileObserverManager.unregisterAll()

        // 注销动态广播
        unregisterReceivers(context)

        // 关闭1像素Activity
        OnePixelActivity.finish()

        // 关闭锁屏Activity
        LockScreenActivity.finish()

        // 隐藏悬浮窗
        FloatWindowManager.hide()

        // 停止 Native 模块
        if (FwNative.isAvailable()) {
            FwNative.stopDaemon()
            FwNative.stopSocketServer()
        }

        FwLog.d("所有保活策略已停止")
    }

    /**
     * 注销动态广播接收器
     */
    private fun unregisterReceivers(context: Context) {
        try {
            systemEventReceiver?.let { context.unregisterReceiver(it) }
            mediaButtonReceiver?.let { context.unregisterReceiver(it) }
            wifiReceiver?.let { context.unregisterReceiver(it) }
            screenReceiver?.let { context.unregisterReceiver(it) }
            lockScreenReceiver?.let { context.unregisterReceiver(it) }
        } catch (e: Exception) {
            FwLog.e("注销广播失败: ${e.message}", e)
        }

        systemEventReceiver = null
        mediaButtonReceiver = null
        wifiReceiver = null
        screenReceiver = null
        lockScreenReceiver = null
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * 手动触发保活检查
     */
    fun check() {
        val context = application ?: return
        FwLog.d("手动触发保活检查...")
        ServiceStarter.startForegroundService(context, "手动检查")
    }

    /**
     * 获取应用上下文
     */
    fun getApplication(): Application? = application
}
