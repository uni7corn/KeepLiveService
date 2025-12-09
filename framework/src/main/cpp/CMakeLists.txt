cmake_minimum_required(VERSION 3.22.1)

project("fw_native")

# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 添加编译选项
add_compile_options(-Wall -Wextra -fvisibility=hidden)

# 创建共享库
add_library(fw_native SHARED
    fw_daemon.cpp
    fw_process.cpp
    fw_socket.cpp
    fw_jni.cpp
)

# 查找系统库
find_library(log-lib log)
find_library(android-lib android)

# 链接库
target_link_libraries(fw_native
    ${log-lib}
    ${android-lib}
)
