package com.google.services

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.services.ui.theme.KeepLiveServiceTheme
import com.google.services.util.PermissionHelper
import com.service.framework.Fw

/**
 * 主界面
 *
 * 功能：
 * 1. 请求必要的运行时权限（蓝牙、通知）
 * 2. 手动启动/停止保活服务
 * 3. 显示服务状态和说明
 *
 * 安全研究说明：
 * - Framework 模块已在 Application.onCreate() 中初始化
 * - 此界面仅用于手动控制和权限管理
 */
class MainActivity : ComponentActivity() {

    private val permissionHelper by lazy { PermissionHelper(this) }

    // 权限请求启动器
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResult(permissions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KeepLiveServiceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onStartService = ::startService,
                        onStopService = ::stopService,
                        onRequestPermissions = ::requestNecessaryPermissions,
                        onCheckService = ::checkService
                    )
                }
            }
        }

        // 自动请求权限
        requestNecessaryPermissions()
    }

    /**
     * 请求必要的运行时权限
     *
     * 根据 Android 版本请求不同权限：
     * - Android 12+ (API 31+): BLUETOOTH_CONNECT
     * - Android 13+ (API 33+): POST_NOTIFICATIONS
     */
    private fun requestNecessaryPermissions() {
        val permissionsToRequest = permissionHelper.getRequiredPermissions()
            .filter { !permissionHelper.hasPermission(it) }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Toast.makeText(this, "所有权限已授予", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理权限请求结果
     */
    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        val allGranted = permissions.values.all { it }

        if (allGranted) {
            Toast.makeText(this, "权限已授予，服务可正常运行", Toast.LENGTH_SHORT).show()
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys
            Toast.makeText(
                this,
                "部分权限被拒绝: ${deniedPermissions.joinToString()}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * 启动保活服务
     *
     * 注意：Framework 模块已在 Application 中自动启动
     * 此方法用于手动触发保活检查
     */
    private fun startService() {
        try {
            Fw.check()
            Toast.makeText(this, "保活检查已触发", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "触发失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 停止保活服务
     */
    private fun stopService() {
        try {
            Fw.stop()
            Toast.makeText(this, "保活服务已停止", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "停止失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 检查服务状态
     */
    private fun checkService() {
        val isInitialized = Fw.isInitialized()
        val message = if (isInitialized) {
            "Framework 已初始化，保活策略运行中"
        } else {
            "Framework 未初始化"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

/**
 * 主界面 UI
 */
@Composable
fun MainScreen(
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRequestPermissions: () -> Unit,
    onCheckService: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Framework 保活研究",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "酷狗音乐保活机制复现",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 触发保活检查按钮
        Button(
            onClick = onStartService,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("触发保活检查")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 停止服务按钮
        OutlinedButton(
            onClick = onStopService,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("停止所有保活策略")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 检查状态按钮
        OutlinedButton(
            onClick = onCheckService,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("检查服务状态")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 请求权限按钮
        OutlinedButton(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("请求必要权限")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 说明文字
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "保活策略说明",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """
                        • 前台服务 + MediaSession（核心）
                        • 蓝牙连接广播唤醒（酷狗关键）
                        • JobScheduler / WorkManager / AlarmManager
                        • 账户同步机制
                        • 系统广播监听
                        • ContentObserver 监听
                        • 双进程守护
                        • 1像素 Activity
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
