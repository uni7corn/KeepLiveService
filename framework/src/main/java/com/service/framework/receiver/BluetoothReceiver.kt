package com.service.framework.receiver

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.service.framework.util.FwLog
import com.service.framework.util.ServiceStarter

/**
 * 蓝牙广播接收器（核心：酷狗音乐唤醒的关键）
 *
 * 安全研究要点 - 这是酷狗音乐被蓝牙耳机唤醒的核心机制：
 *
 * 1. 蓝牙耳机连接时，系统会发送多个广播：
 *    - BluetoothDevice.ACTION_ACL_CONNECTED: 底层ACL链路连接
 *    - BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED: A2DP音频连接状态
 *    - BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED: 耳机profile连接
 *    - BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED: 蓝牙连接状态
 *
 * 2. 音乐类应用通过注册这些广播，即使应用被杀也能被唤醒
 *
 * 3. 关键：Android 系统认为音乐应用需要响应蓝牙耳机事件
 *    所以允许静态注册的广播在应用被杀后唤醒应用
 *
 * 4. 但是：强制停止(Force Stop)后，应用进入 stopped 状态
 *    此时即使静态注册的广播也不会被接收
 *    这是 Android 的安全设计，用于保护用户意愿
 *
 * 5. 酷狗音乐能在强制停止后被唤醒的可能原因：
 *    - 小米系统的白名单机制
 *    - MIUI 的自启动管理放行
 *    - 系统级别的媒体应用豁免
 *    - 或者有其他未知的唤醒路径
 */
class BluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        FwLog.d("BluetoothReceiver 收到广播: $action")

        when (action) {
            // 蓝牙设备 ACL 连接（底层连接，最先触发）
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device = getBluetoothDevice(intent)
                FwLog.d("蓝牙设备 ACL 连接: ${device?.name ?: "未知设备"}")
                ServiceStarter.startForegroundService(context, "蓝牙ACL连接")
            }

            // 蓝牙设备 ACL 断开
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device = getBluetoothDevice(intent)
                FwLog.d("蓝牙设备 ACL 断开: ${device?.name ?: "未知设备"}")
            }

            // A2DP 音频连接状态变化（音乐播放相关）
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                val device = getBluetoothDevice(intent)

                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        FwLog.d("A2DP 音频已连接: ${device?.name}")
                        ServiceStarter.startForegroundService(context, "A2DP音频连接")
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        FwLog.d("A2DP 音频已断开: ${device?.name}")
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        FwLog.d("A2DP 音频连接中: ${device?.name}")
                        // 连接中也尝试唤醒
                        ServiceStarter.startForegroundService(context, "A2DP连接中")
                    }
                }
            }

            // 蓝牙耳机 Profile 连接状态变化
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                val device = getBluetoothDevice(intent)

                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        FwLog.d("蓝牙耳机已连接: ${device?.name}")
                        ServiceStarter.startForegroundService(context, "蓝牙耳机连接")
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        FwLog.d("蓝牙耳机已断开: ${device?.name}")
                    }
                }
            }

            // 蓝牙适配器状态变化
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        FwLog.d("蓝牙已开启")
                        ServiceStarter.startForegroundService(context, "蓝牙开启")
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        FwLog.d("蓝牙已关闭")
                    }
                }
            }

            // 蓝牙适配器连接状态变化
            BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1)
                when (state) {
                    BluetoothAdapter.STATE_CONNECTED -> {
                        FwLog.d("蓝牙适配器已连接设备")
                        ServiceStarter.startForegroundService(context, "蓝牙设备连接")
                    }
                }
            }

            // A2DP 播放状态变化（音乐开始播放）
            BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1)
                when (state) {
                    BluetoothA2dp.STATE_PLAYING -> {
                        FwLog.d("A2DP 开始播放")
                        ServiceStarter.startForegroundService(context, "A2DP播放")
                    }
                    BluetoothA2dp.STATE_NOT_PLAYING -> {
                        FwLog.d("A2DP 停止播放")
                    }
                }
            }

            // 音频输出变化（耳机插拔等）
            "android.media.AUDIO_BECOMING_NOISY" -> {
                FwLog.d("音频输出变化（可能是耳机拔出）")
            }

            // 测试广播
            "com.service.framework.TEST_WAKEUP" -> {
                FwLog.d("收到测试唤醒广播")
                ServiceStarter.startForegroundService(context, "测试唤醒")
            }
        }
    }

    private fun getBluetoothDevice(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }
}
