package com.service.framework.core

/**
 * 保活配置类
 *
 * 安全研究用途：配置各种保活策略的开关
 *
 * 策略分类：
 * 1. 基础策略 - 前台服务、MediaSession、1像素Activity
 * 2. 定时唤醒 - JobScheduler、WorkManager、AlarmManager
 * 3. 账户同步 - AccountManager + SyncAdapter
 * 4. 广播策略 - 蓝牙、USB、NFC、系统事件、媒体挂载
 * 5. 观察者策略 - ContentObserver、FileObserver
 * 6. 进程策略 - 双进程守护、Native守护、Socket保活
 */
data class FwConfig(
    // ==================== 基础策略 ====================
    /** 是否启用前台服务 */
    val enableForegroundService: Boolean = true,

    /** 是否启用 MediaSession（模拟媒体播放器） */
    val enableMediaSession: Boolean = true,

    /** 是否启用 1 像素 Activity */
    val enableOnePixelActivity: Boolean = true,

    // ==================== 定时唤醒策略 ====================
    /** 是否启用 JobScheduler */
    val enableJobScheduler: Boolean = true,

    /** JobScheduler 执行间隔（毫秒），最小 15 分钟 */
    val jobSchedulerInterval: Long = 15 * 60 * 1000L,

    /** 是否启用 WorkManager */
    val enableWorkManager: Boolean = true,

    /** WorkManager 执行间隔（分钟），最小 15 分钟 */
    val workManagerIntervalMinutes: Long = 15L,

    /** 是否启用 AlarmManager */
    val enableAlarmManager: Boolean = true,

    /** AlarmManager 执行间隔（毫秒） */
    val alarmManagerInterval: Long = 5 * 60 * 1000L,

    // ==================== 账户同步策略 ====================
    /** 是否启用账户同步 */
    val enableAccountSync: Boolean = true,

    /** 账户类型 */
    val accountType: String = "com.service.framework.account",

    /** 同步间隔（秒） */
    val syncIntervalSeconds: Long = 60L,

    // ==================== 广播策略 ====================
    /** 是否启用系统广播监听 */
    val enableSystemBroadcast: Boolean = true,

    /** 是否启用蓝牙广播监听（核心：酷狗音乐的关键） */
    val enableBluetoothBroadcast: Boolean = true,

    /** 是否启用媒体按键监听 */
    val enableMediaButtonReceiver: Boolean = true,

    /** 是否启用 USB 设备广播（打印机、U盘、配件等） */
    val enableUsbBroadcast: Boolean = true,

    /** 是否启用 NFC 广播（标签发现、适配器状态） */
    val enableNfcBroadcast: Boolean = true,

    /** 是否启用媒体挂载广播（SD卡、U盘挂载） */
    val enableMediaMountBroadcast: Boolean = true,

    // ==================== 内容观察者策略 ====================
    /** 是否启用相册变化监听 */
    val enableMediaContentObserver: Boolean = true,

    /** 是否启用联系人变化监听 */
    val enableContactsContentObserver: Boolean = true,

    /** 是否启用短信变化监听 */
    val enableSmsContentObserver: Boolean = true,

    /** 是否启用设置变化监听 */
    val enableSettingsContentObserver: Boolean = true,

    /** 是否启用文件系统监听（下载、截图、相册目录） */
    val enableFileObserver: Boolean = true,

    // ==================== 双进程策略 ====================
    /** 是否启用双进程守护（Java 层） */
    val enableDualProcess: Boolean = true,

    /** 双进程检查间隔（毫秒） */
    val dualProcessCheckInterval: Long = 3000L,

    // ==================== Native 层策略 ====================
    /** 是否启用 Native 守护进程（C++ fork） */
    val enableNativeDaemon: Boolean = true,

    /** Native 守护进程检查间隔（毫秒） */
    val nativeDaemonCheckInterval: Int = 3000,

    /** 是否启用 Native Socket 保活通道 */
    val enableNativeSocket: Boolean = true,

    /** Native Socket 名称 */
    val nativeSocketName: String = "fw_native_socket",

    /** Native Socket 心跳间隔（毫秒） */
    val nativeSocketHeartbeatInterval: Int = 5000,

    // ==================== 通知配置 ====================
    /** 通知渠道 ID */
    val notificationChannelId: String = "fw_channel",

    /** 通知渠道名称 */
    val notificationChannelName: String = "保活服务",

    /** 通知标题 */
    val notificationTitle: String = "服务运行中",

    /** 通知内容 */
    val notificationContent: String = "点击打开应用",

    /** 通知图标资源 ID */
    val notificationIconResId: Int = android.R.drawable.ic_media_play,

    /** 点击通知打开的 Activity 类名 */
    val notificationActivityClass: Class<*>? = null,

    // ==================== 日志配置 ====================
    /** 是否启用详细日志 */
    val enableDebugLog: Boolean = true,

    /** 日志 TAG */
    val logTag: String = "Fw",

    // ==================== 锁屏和悬浮窗策略 ====================
    /** 是否启用锁屏 Activity（类似墨迹天气的锁屏天气） */
    val enableLockScreenActivity: Boolean = false,

    /** 是否启用悬浮窗保活（需要 SYSTEM_ALERT_WINDOW 权限） */
    val enableFloatWindow: Boolean = false,

    /** 悬浮窗类型：true = 1像素隐藏，false = 可见悬浮球 */
    val floatWindowHidden: Boolean = true
) {

    class Builder {
        private var config = FwConfig()

        // ==================== 基础策略 ====================

        fun enableForegroundService(enable: Boolean) = apply {
            config = config.copy(enableForegroundService = enable)
        }

        fun enableMediaSession(enable: Boolean) = apply {
            config = config.copy(enableMediaSession = enable)
        }

        fun enableOnePixelActivity(enable: Boolean) = apply {
            config = config.copy(enableOnePixelActivity = enable)
        }

        // ==================== 定时唤醒策略 ====================

        fun enableJobScheduler(enable: Boolean) = apply {
            config = config.copy(enableJobScheduler = enable)
        }

        fun jobSchedulerInterval(intervalMs: Long) = apply {
            config = config.copy(jobSchedulerInterval = intervalMs)
        }

        fun enableWorkManager(enable: Boolean) = apply {
            config = config.copy(enableWorkManager = enable)
        }

        fun workManagerIntervalMinutes(minutes: Long) = apply {
            config = config.copy(workManagerIntervalMinutes = minutes)
        }

        fun enableAlarmManager(enable: Boolean) = apply {
            config = config.copy(enableAlarmManager = enable)
        }

        fun alarmManagerInterval(intervalMs: Long) = apply {
            config = config.copy(alarmManagerInterval = intervalMs)
        }

        // ==================== 账户同步策略 ====================

        fun enableAccountSync(enable: Boolean) = apply {
            config = config.copy(enableAccountSync = enable)
        }

        fun accountType(type: String) = apply {
            config = config.copy(accountType = type)
        }

        fun syncIntervalSeconds(seconds: Long) = apply {
            config = config.copy(syncIntervalSeconds = seconds)
        }

        // ==================== 广播策略 ====================

        fun enableSystemBroadcast(enable: Boolean) = apply {
            config = config.copy(enableSystemBroadcast = enable)
        }

        fun enableBluetoothBroadcast(enable: Boolean) = apply {
            config = config.copy(enableBluetoothBroadcast = enable)
        }

        fun enableMediaButtonReceiver(enable: Boolean) = apply {
            config = config.copy(enableMediaButtonReceiver = enable)
        }

        fun enableUsbBroadcast(enable: Boolean) = apply {
            config = config.copy(enableUsbBroadcast = enable)
        }

        fun enableNfcBroadcast(enable: Boolean) = apply {
            config = config.copy(enableNfcBroadcast = enable)
        }

        fun enableMediaMountBroadcast(enable: Boolean) = apply {
            config = config.copy(enableMediaMountBroadcast = enable)
        }

        // ==================== 内容观察者策略 ====================

        fun enableMediaContentObserver(enable: Boolean) = apply {
            config = config.copy(enableMediaContentObserver = enable)
        }

        fun enableContactsContentObserver(enable: Boolean) = apply {
            config = config.copy(enableContactsContentObserver = enable)
        }

        fun enableSmsContentObserver(enable: Boolean) = apply {
            config = config.copy(enableSmsContentObserver = enable)
        }

        fun enableSettingsContentObserver(enable: Boolean) = apply {
            config = config.copy(enableSettingsContentObserver = enable)
        }

        fun enableFileObserver(enable: Boolean) = apply {
            config = config.copy(enableFileObserver = enable)
        }

        // ==================== 双进程策略 ====================

        fun enableDualProcess(enable: Boolean) = apply {
            config = config.copy(enableDualProcess = enable)
        }

        fun dualProcessCheckInterval(intervalMs: Long) = apply {
            config = config.copy(dualProcessCheckInterval = intervalMs)
        }

        // ==================== Native 层策略 ====================

        fun enableNativeDaemon(enable: Boolean) = apply {
            config = config.copy(enableNativeDaemon = enable)
        }

        fun nativeDaemonCheckInterval(intervalMs: Int) = apply {
            config = config.copy(nativeDaemonCheckInterval = intervalMs)
        }

        fun enableNativeSocket(enable: Boolean) = apply {
            config = config.copy(enableNativeSocket = enable)
        }

        fun nativeSocketName(name: String) = apply {
            config = config.copy(nativeSocketName = name)
        }

        fun nativeSocketHeartbeatInterval(intervalMs: Int) = apply {
            config = config.copy(nativeSocketHeartbeatInterval = intervalMs)
        }

        // ==================== 通知配置 ====================

        fun notificationChannelId(id: String) = apply {
            config = config.copy(notificationChannelId = id)
        }

        fun notificationChannelName(name: String) = apply {
            config = config.copy(notificationChannelName = name)
        }

        fun notificationTitle(title: String) = apply {
            config = config.copy(notificationTitle = title)
        }

        fun notificationContent(content: String) = apply {
            config = config.copy(notificationContent = content)
        }

        fun notificationIcon(resId: Int) = apply {
            config = config.copy(notificationIconResId = resId)
        }

        fun notificationActivityClass(clazz: Class<*>) = apply {
            config = config.copy(notificationActivityClass = clazz)
        }

        // ==================== 日志配置 ====================

        fun enableDebugLog(enable: Boolean) = apply {
            config = config.copy(enableDebugLog = enable)
        }

        fun logTag(tag: String) = apply {
            config = config.copy(logTag = tag)
        }

        // ==================== 锁屏和悬浮窗策略 ====================

        fun enableLockScreenActivity(enable: Boolean) = apply {
            config = config.copy(enableLockScreenActivity = enable)
        }

        fun enableFloatWindow(enable: Boolean) = apply {
            config = config.copy(enableFloatWindow = enable)
        }

        fun floatWindowHidden(hidden: Boolean) = apply {
            config = config.copy(floatWindowHidden = hidden)
        }

        fun build(): FwConfig = config
    }

    companion object {
        fun builder() = Builder()
    }
}
