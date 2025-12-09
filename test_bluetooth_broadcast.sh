#!/bin/bash

# ============================================================
# KeepLiveService 唤醒测试脚本
# 
# 使用 adb 发送广播或启动服务，测试应用唤醒功能
# 
# 注意：蓝牙广播是受保护的系统广播，无法通过 adb 发送
# 因此使用自定义广播或直接启动服务来测试
# ============================================================

PACKAGE="com.google.services"
RECEIVER="com.google.services.receiver.BluetoothReceiver"
SERVICE="com.google.services.service.MediaKeepLiveService"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

DEVICE_SERIAL=""

print_usage() {
    echo "用法: $0 [-d <device>] <command>"
    echo ""
    echo "选项:"
    echo "  -d <device>  指定设备序列号（多设备时必需）"
    echo ""
    echo "命令:"
    echo "  wakeup    - 发送自定义唤醒广播（推荐）"
    echo "  start     - 直接启动前台服务"
    echo "  stop      - 停止前台服务"
    echo "  kill      - 强制停止应用进程"
    echo "  status    - 检查应用和服务状态"
    echo "  log       - 查看相关日志"
    echo "  install   - 安装 debug APK"
    echo "  devices   - 列出所有连接的设备"
    echo "  test      - 完整测试流程（kill -> wakeup -> status）"
    echo ""
    echo "示例:"
    echo "  $0 devices                  # 列出设备"
    echo "  $0 -d emulator-5554 wakeup  # 发送唤醒广播"
    echo "  $0 -d emulator-5554 test    # 完整测试"
}

adb_cmd() {
    if [ -n "$DEVICE_SERIAL" ]; then
        adb -s "$DEVICE_SERIAL" "$@"
    else
        adb "$@"
    fi
}

list_devices() {
    echo -e "${YELLOW}已连接的设备:${NC}"
    echo ""
    adb devices -l | tail -n +2 | grep -v "^$" | grep -v "offline"
    echo ""
}

check_adb() {
    if ! command -v adb &> /dev/null; then
        echo -e "${RED}错误: adb 未找到${NC}"
        exit 1
    fi
    
    DEVICES=$(adb devices | grep -v "List" | grep -v "^$" | grep "device$")
    DEVICE_COUNT=$(echo "$DEVICES" | grep -c "device$" 2>/dev/null || echo 0)
    
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo -e "${RED}错误: 没有连接的设备${NC}"
        exit 1
    fi
    
    if [ "$DEVICE_COUNT" -gt 1 ] && [ -z "$DEVICE_SERIAL" ]; then
        echo -e "${YELLOW}检测到多个设备，请使用 -d 参数指定:${NC}"
        echo ""
        list_devices
        exit 1
    fi
    
    if [ "$DEVICE_COUNT" -eq 1 ] && [ -z "$DEVICE_SERIAL" ]; then
        DEVICE_SERIAL=$(echo "$DEVICES" | awk '{print $1}')
    fi
    
    echo -e "${GREEN}✓ 使用设备: $DEVICE_SERIAL${NC}"
}

# 发送自定义唤醒广播
send_wakeup() {
    echo -e "${YELLOW}发送唤醒广播...${NC}"
    
    RESULT=$(adb_cmd shell am broadcast \
        -a "com.google.services.TEST_WAKEUP" \
        -n "$PACKAGE/$RECEIVER" 2>&1)
    
    echo "$RESULT"
    
    if echo "$RESULT" | grep -q "Broadcast completed"; then
        echo -e "${GREEN}✓ 广播发送成功${NC}"
    else
        echo -e "${RED}✗ 广播发送失败${NC}"
    fi
}

# 直接启动服务
start_service() {
    echo -e "${YELLOW}启动前台服务...${NC}"
    
    # 使用 am start-foreground-service (Android 8.0+)
    RESULT=$(adb_cmd shell am start-foreground-service \
        -n "$PACKAGE/$SERVICE" \
        --es start_reason "adb测试启动" 2>&1)
    
    if [ $? -ne 0 ]; then
        # 回退到 startservice
        RESULT=$(adb_cmd shell am startservice \
            -n "$PACKAGE/$SERVICE" \
            --es start_reason "adb测试启动" 2>&1)
    fi
    
    echo "$RESULT"
    
    if echo "$RESULT" | grep -q "Error\|Exception"; then
        echo -e "${RED}✗ 服务启动失败${NC}"
    else
        echo -e "${GREEN}✓ 服务启动命令已发送${NC}"
    fi
}

# 停止服务
stop_service() {
    echo -e "${YELLOW}停止前台服务...${NC}"
    adb_cmd shell am stopservice -n "$PACKAGE/$SERVICE"
    echo -e "${GREEN}✓ 停止命令已发送${NC}"
}

# 强制停止应用
kill_app() {
    echo -e "${YELLOW}强制停止应用...${NC}"
    adb_cmd shell am force-stop "$PACKAGE"
    sleep 1
    
    PID=$(adb_cmd shell pidof "$PACKAGE" 2>/dev/null)
    if [ -z "$PID" ]; then
        echo -e "${GREEN}✓ 应用已停止${NC}"
    else
        echo -e "${RED}✗ 应用仍在运行 (PID: $PID)${NC}"
    fi
}

# 检查状态
check_status() {
    echo -e "${YELLOW}检查应用状态...${NC}"
    echo ""
    
    # 检查安装
    INSTALLED=$(adb_cmd shell pm list packages | grep "$PACKAGE")
    if [ -n "$INSTALLED" ]; then
        echo -e "${GREEN}✓ 应用已安装${NC}"
    else
        echo -e "${RED}✗ 应用未安装${NC}"
        return
    fi
    
    # 检查进程
    PID=$(adb_cmd shell pidof "$PACKAGE" 2>/dev/null)
    if [ -n "$PID" ]; then
        echo -e "${GREEN}✓ 进程运行中 (PID: $PID)${NC}"
    else
        echo -e "${YELLOW}○ 进程未运行${NC}"
    fi
    
    # 检查服务
    echo ""
    echo -e "${BLUE}服务状态:${NC}"
    adb_cmd shell dumpsys activity services "$PACKAGE" 2>/dev/null | head -20
}

# 查看日志
view_log() {
    echo -e "${YELLOW}查看日志 (Ctrl+C 退出)...${NC}"
    adb_cmd logcat -v time -s "KeepLiveApp:*" "BluetoothReceiver:*" "MediaKeepLiveService:*" "MediaSessionHolder:*"
}

# 安装 APK
install_apk() {
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    
    if [ ! -f "$APK_PATH" ]; then
        echo -e "${YELLOW}构建 APK...${NC}"
        ./gradlew assembleDebug
    fi
    
    if [ -f "$APK_PATH" ]; then
        echo -e "${YELLOW}安装 APK...${NC}"
        adb_cmd install -r "$APK_PATH"
        echo -e "${GREEN}✓ 安装完成${NC}"
    else
        echo -e "${RED}✗ 构建失败${NC}"
    fi
}

# 完整测试流程
run_test() {
    echo "=========================================="
    echo "开始完整测试流程"
    echo "=========================================="
    echo ""
    
    echo -e "${BLUE}[1/4] 检查初始状态${NC}"
    check_status
    echo ""
    
    echo -e "${BLUE}[2/4] 强制停止应用${NC}"
    kill_app
    echo ""
    
    echo -e "${BLUE}[3/4] 发送唤醒广播${NC}"
    send_wakeup
    sleep 2
    echo ""
    
    echo -e "${BLUE}[4/4] 检查唤醒后状态${NC}"
    check_status
    
    echo ""
    echo "=========================================="
    echo "测试完成"
    echo "=========================================="
}

# 主逻辑
main() {
    while getopts "d:h" opt; do
        case $opt in
            d) DEVICE_SERIAL="$OPTARG" ;;
            h) print_usage; exit 0 ;;
        esac
    done
    shift $((OPTIND-1))
    
    COMMAND="$1"
    
    if [ -z "$COMMAND" ]; then
        print_usage
        exit 0
    fi
    
    if [ "$COMMAND" = "devices" ]; then
        list_devices
        exit 0
    fi
    
    check_adb
    echo ""
    
    case "$COMMAND" in
        wakeup)  send_wakeup ;;
        start)   start_service ;;
        stop)    stop_service ;;
        kill)    kill_app ;;
        status)  check_status ;;
        log)     view_log ;;
        install) install_apk ;;
        test)    run_test ;;
        *)
            echo -e "${RED}未知命令: $COMMAND${NC}"
            print_usage
            exit 1
            ;;
    esac
}

main "$@"
