package com.service.framework.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 系统事件广播接收器
 *
 * 监听各种系统事件来拉活应用
 *
 * 安全研究要点：
 * - 静态注册的广播可以在应用被杀后唤醒应用
 * - 但强制停止后所有广播都会被禁用
 * - 不同版本的 Android 对广播的限制不同
 *
 * Android 版本限制说明：
 * - Android 8.0+: 大部分隐式广播无法静态注册
 * - Android 9.0+: 更多广播受限
 * - 但仍有一些广播可以静态注册（如 BOOT_COMPLETED, MY_PACKAGE_REPLACED 等）
 */
class SystemEventReceiver : BroadcastReceiver() {

    companion object {
        /**
         * 获取动态注册的 IntentFilter（用于运行时注册）
         * 这些广播无法静态注册，必须动态注册
         */
        fun getDynamicIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                // 屏幕状态（无法静态注册）
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)

                // 用户解锁（无法静态注册）
                addAction(Intent.ACTION_USER_PRESENT)

                // 网络变化（Android 7.0+ 无法静态注册）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                }

                // 时间相关
                addAction(Intent.ACTION_TIME_TICK) // 每分钟一次
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)

                // 电源相关
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_BATTERY_LOW)
                addAction(Intent.ACTION_BATTERY_OKAY)

                // 飞行模式
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)

                // 应用相关
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")

                // 输入法变化
                addAction(Intent.ACTION_INPUT_METHOD_CHANGED)

                // 配置变化
                addAction(Intent.ACTION_CONFIGURATION_CHANGED)

                // 语言变化
                addAction(Intent.ACTION_LOCALE_CHANGED)

                // 存储相关
                addAction(Intent.ACTION_DEVICE_STORAGE_LOW)
                addAction(Intent.ACTION_DEVICE_STORAGE_OK)

                // Home 键相关
                addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)

                // 壁纸变化
                addAction(Intent.ACTION_WALLPAPER_CHANGED)
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        FwLog.d("SystemEventReceiver 收到广播: $action")

        when (action) {
            // ==================== 开机相关（可静态注册） ====================
            Intent.ACTION_BOOT_COMPLETED -> {
                FwLog.d("设备启动完成")
                ServiceStarter.startForegroundService(context, "开机启动")
            }

            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                FwLog.d("设备锁定启动完成")
                ServiceStarter.startForegroundService(context, "锁定启动")
            }

            // ==================== 屏幕状态 ====================
            Intent.ACTION_SCREEN_ON -> {
                FwLog.d("屏幕点亮")
                ServiceStarter.startForegroundService(context, "屏幕点亮")
            }

            Intent.ACTION_SCREEN_OFF -> {
                FwLog.d("屏幕关闭")
                // 屏幕关闭时也尝试保持服务运行
                ServiceStarter.startForegroundService(context, "屏幕关闭")
            }

            // ==================== 用户解锁 ====================
            Intent.ACTION_USER_PRESENT -> {
                FwLog.d("用户解锁屏幕")
                ServiceStarter.startForegroundService(context, "用户解锁")
            }

            // ==================== 网络变化 ====================
            ConnectivityManager.CONNECTIVITY_ACTION,
            "android.net.conn.CONNECTIVITY_CHANGE" -> {
                FwLog.d("网络状态变化")
                ServiceStarter.startForegroundService(context, "网络变化")
            }

            // ==================== 电源相关 ====================
            Intent.ACTION_POWER_CONNECTED -> {
                FwLog.d("电源连接")
                ServiceStarter.startForegroundService(context, "电源连接")
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                FwLog.d("电源断开")
                ServiceStarter.startForegroundService(context, "电源断开")
            }

            Intent.ACTION_BATTERY_CHANGED -> {
                // 电池状态变化（非常频繁，只记录不拉起）
                FwLog.v("电池状态变化")
            }

            Intent.ACTION_BATTERY_LOW -> {
                FwLog.d("电量低")
                ServiceStarter.startForegroundService(context, "电量低")
            }

            Intent.ACTION_BATTERY_OKAY -> {
                FwLog.d("电量恢复正常")
                ServiceStarter.startForegroundService(context, "电量恢复")
            }

            // ==================== 时间相关 ====================
            Intent.ACTION_TIME_TICK -> {
                // 每分钟一次（太频繁，只记录不拉起）
                FwLog.v("时间 Tick")
            }

            Intent.ACTION_TIME_CHANGED -> {
                FwLog.d("时间设置变化")
                ServiceStarter.startForegroundService(context, "时间变化")
            }

            Intent.ACTION_TIMEZONE_CHANGED -> {
                FwLog.d("时区变化")
                ServiceStarter.startForegroundService(context, "时区变化")
            }

            // ==================== 应用相关（可静态注册） ====================
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                FwLog.d("应用已更新")
                ServiceStarter.startForegroundService(context, "应用更新")
            }

            Intent.ACTION_PACKAGE_ADDED -> {
                val packageName = intent.data?.schemeSpecificPart
                FwLog.d("应用安装: $packageName")
            }

            Intent.ACTION_PACKAGE_REMOVED -> {
                val packageName = intent.data?.schemeSpecificPart
                FwLog.d("应用卸载: $packageName")
            }

            // ==================== 飞行模式 ====================
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                val isEnabled = intent.getBooleanExtra("state", false)
                FwLog.d("飞行模式: ${if (isEnabled) "开启" else "关闭"}")
                ServiceStarter.startForegroundService(context, "飞行模式变化")
            }

            // ==================== Home 键相关 ====================
            Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                val reason = intent.getStringExtra("reason")
                FwLog.d("系统对话框关闭，原因: $reason")
                // homekey 表示按下了 Home 键
                if (reason == "homekey" || reason == "recentapps") {
                    ServiceStarter.startForegroundService(context, "Home键")
                }
            }

            // ==================== 其他 ====================
            Intent.ACTION_LOCALE_CHANGED -> {
                FwLog.d("语言/区域设置变化")
                ServiceStarter.startForegroundService(context, "语言变化")
            }

            Intent.ACTION_CONFIGURATION_CHANGED -> {
                FwLog.d("配置变化")
            }

            Intent.ACTION_DEVICE_STORAGE_LOW -> {
                FwLog.d("存储空间不足")
            }

            Intent.ACTION_DEVICE_STORAGE_OK -> {
                FwLog.d("存储空间恢复正常")
            }

            Intent.ACTION_INPUT_METHOD_CHANGED -> {
                FwLog.d("输入法变化")
            }

            Intent.ACTION_WALLPAPER_CHANGED -> {
                FwLog.d("壁纸变化")
                ServiceStarter.startForegroundService(context, "壁纸变化")
            }

            else -> {
                FwLog.d("收到其他广播: $action")
            }
        }
    }
}
