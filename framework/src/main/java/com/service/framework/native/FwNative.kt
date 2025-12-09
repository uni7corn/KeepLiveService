package com.service.framework.native

import android.content.Context
import com.service.framework.util.FwLog

/**
 * Native 层保活接口
 *
 * 核心功能：
 * 1. Native 守护进程（fork 子进程监控父进程）
 * 2. 进程优先级管理（OOM adj、nice 值）
 * 3. Socket 保活通道（进程间心跳通信）
 * 4. 系统信息获取（内存、进程数等）
 *
 * 安全研究要点：
 * - Native 层可以绕过部分 Java 层限制
 * - fork 出的子进程在父进程死亡后仍可存活
 * - 但强制停止会杀死整个进程组
 * - 某些厂商 ROM 对 Native 守护有额外检测
 *
 * 使用方式：
 * ```kotlin
 * // 启动 Native 守护进程
 * FwNative.startDaemon(
 *     packageName = "com.example.app",
 *     serviceName = "com.example.app.service.MyService",
 *     checkIntervalMs = 3000
 * )
 *
 * // 获取进程信息
 * val oomAdj = FwNative.getOomAdj()
 * val memInfo = FwNative.getMemoryInfo()
 *
 * // 启动 Socket 服务（用于进程间通信）
 * FwNative.startSocketServer("fw_socket")
 * ```
 *
 * 注意：
 * - 需要在 build.gradle 中配置 CMake
 * - 需要加载 native 库：System.loadLibrary("fw_native")
 */
object FwNative {

    private var isLoaded = false
    private var isEnabled = false

    /**
     * 初始化 Native 模块
     *
     * @param context 应用上下文
     * @return 是否初始化成功
     */
    fun init(context: Context): Boolean {
        if (isLoaded) {
            FwLog.d("FwNative: 已初始化，跳过")
            return true
        }

        return try {
            System.loadLibrary("fw_native")
            isLoaded = true
            isEnabled = true
            FwLog.d("FwNative: Native 库加载成功")
            true
        } catch (e: UnsatisfiedLinkError) {
            FwLog.e("FwNative: Native 库加载失败 - ${e.message}", e)
            isLoaded = false
            isEnabled = false
            false
        }
    }

    /**
     * 检查 Native 模块是否可用
     */
    fun isAvailable(): Boolean = isLoaded && isEnabled

    /**
     * 设置是否启用 Native 保活
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        FwLog.d("FwNative: 设置启用状态 = $enabled")
    }

    // ==================== 守护进程 ====================

    /**
     * 启动 Native 守护进程
     *
     * 使用 fork() 创建子进程，监控父进程存活状态
     * 父进程死亡时尝试通过 am 命令重新启动服务
     *
     * @param packageName 应用包名
     * @param serviceName 服务完整类名
     * @param checkIntervalMs 检查间隔（毫秒），默认 3000
     * @return 是否启动成功
     */
    @JvmStatic
    external fun startDaemon(
        packageName: String,
        serviceName: String,
        checkIntervalMs: Int = 3000
    ): Boolean

    /**
     * 停止 Native 守护进程
     */
    @JvmStatic
    external fun stopDaemon()

    /**
     * 检查守护进程是否在运行
     */
    @JvmStatic
    external fun isDaemonRunning(): Boolean

    // ==================== 进程管理 ====================

    /**
     * 获取当前进程的 OOM adj 值
     *
     * OOM adj 值决定进程被杀的优先级：
     * - 值越低，越不容易被杀
     * - 前台进程 = 0
     * - 后台服务 = 500+
     * - 缓存进程 = 900+
     *
     * @return OOM adj 值
     */
    @JvmStatic
    external fun getOomAdj(): Int

    /**
     * 尝试设置 OOM adj 值
     *
     * 注意：需要 root 权限，普通应用无法成功设置
     *
     * @param adj 目标 adj 值
     * @return 是否设置成功
     */
    @JvmStatic
    external fun setOomAdj(adj: Int): Boolean

    /**
     * 设置进程优先级（nice 值）
     *
     * nice 值范围：-20（最高）到 19（最低）
     * 普通应用只能降低优先级，不能提高
     *
     * @param priority nice 值
     * @return 是否设置成功
     */
    @JvmStatic
    external fun setProcessPriority(priority: Int): Boolean

    /**
     * 获取进程优先级（nice 值）
     */
    @JvmStatic
    external fun getProcessPriority(): Int

    /**
     * 获取进程状态信息
     *
     * 返回 /proc/self/status 的关键信息
     *
     * @return 进程状态字符串
     */
    @JvmStatic
    external fun getProcessStatus(): String

    /**
     * 获取系统内存信息
     *
     * @return LongArray [总内存KB, 空闲内存KB, 可用内存KB]
     */
    @JvmStatic
    external fun getMemoryInfo(): LongArray

    /**
     * 检查是否有 root 权限
     *
     * @return 是否有 root 权限
     */
    @JvmStatic
    external fun checkRoot(): Boolean

    /**
     * 获取系统进程数量
     */
    @JvmStatic
    external fun getProcessCount(): Int

    // ==================== Socket 通信 ====================

    /**
     * 启动 Socket 服务
     *
     * 创建 Unix Domain Socket 服务器
     * 用于进程间心跳通信
     *
     * @param socketName Socket 名称（使用 abstract namespace）
     * @return 是否启动成功
     */
    @JvmStatic
    external fun startSocketServer(socketName: String): Boolean

    /**
     * 停止 Socket 服务
     */
    @JvmStatic
    external fun stopSocketServer()

    /**
     * 连接到 Socket 服务
     *
     * @param socketName Socket 名称
     * @return Socket 文件描述符，失败返回 -1
     */
    @JvmStatic
    external fun connectSocket(socketName: String): Int

    /**
     * 发送心跳
     *
     * @param socketFd Socket 文件描述符
     * @return 是否发送成功
     */
    @JvmStatic
    external fun sendHeartbeat(socketFd: Int): Boolean

    // ==================== 辅助方法 ====================

    /**
     * 获取可读的内存信息
     */
    fun getMemoryInfoReadable(): String {
        if (!isAvailable()) return "Native 模块不可用"

        return try {
            val info = getMemoryInfo()
            val totalMB = info[0] / 1024
            val freeMB = info[1] / 1024
            val availableMB = info[2] / 1024
            "总内存: ${totalMB}MB, 空闲: ${freeMB}MB, 可用: ${availableMB}MB"
        } catch (e: Exception) {
            "获取内存信息失败: ${e.message}"
        }
    }

    /**
     * 获取当前进程信息摘要
     */
    fun getProcessSummary(): String {
        if (!isAvailable()) return "Native 模块不可用"

        return try {
            val oomAdj = getOomAdj()
            val priority = getProcessPriority()
            val isRoot = checkRoot()
            val processCount = getProcessCount()

            buildString {
                appendLine("OOM adj: $oomAdj")
                appendLine("进程优先级: $priority")
                appendLine("Root 权限: $isRoot")
                appendLine("系统进程数: $processCount")
            }
        } catch (e: Exception) {
            "获取进程信息失败: ${e.message}"
        }
    }

    /**
     * 使用默认参数启动守护
     */
    fun startDefaultDaemon(context: Context): Boolean {
        if (!isAvailable()) {
            FwLog.w("FwNative: Native 模块不可用，无法启动守护进程")
            return false
        }

        val packageName = context.packageName
        val serviceName = "com.service.framework.service.FwForegroundService"

        return try {
            val result = startDaemon(packageName, serviceName, 3000)
            FwLog.d("FwNative: 启动守护进程 ${if (result) "成功" else "失败"}")
            result
        } catch (e: Exception) {
            FwLog.e("FwNative: 启动守护进程异常 - ${e.message}", e)
            false
        }
    }
}
