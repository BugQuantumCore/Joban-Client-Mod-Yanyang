cmake_minimum_required(VERSION 3.16)
project(mtr-native-scripting CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

# ----------------------------------------------------------------------------
# Compiler flags (portable across gcc / clang / MSVC).
#
# MSVC (the Windows resource-pack target) needs:
#   /utf-8 — REQUIRED. The sources are UTF-8 with CJK string literals
#            ("内环", "号线", "请检查侧线名。", ...). Without it cl.exe
#            decodes them with the active ANSI code page (936/GBK on
#            zh-CN systems), mangling every literal and failing the
#            build with C2001/C3872/C3688.
#   /O2    — gcc's "-O2" is not understood by cl.exe.
# ----------------------------------------------------------------------------
if(MSVC)
    add_compile_options(/utf-8 /O2 /EHsc /W3)
else()
    add_compile_options(-O2)
endif()

# ----------------------------------------------------------------------------
# The script SDK — header-only, consumed by resource-pack script authors.
#   include/mtr/*.hpp  (script.hpp pulls in the full API)
# ----------------------------------------------------------------------------
add_library(mtr_script_sdk INTERFACE)
target_include_directories(mtr_script_sdk INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/include)

# ----------------------------------------------------------------------------
# Example native scripts — each builds one shared library, exactly like
# one JS script entry in mtr_custom_resources.json.
# ----------------------------------------------------------------------------
function(mtr_add_script name)
    add_library(${name} MODULE ${ARGN})
    target_link_libraries(${name} PRIVATE mtr_script_sdk)
    set_target_properties(${name} PROPERTIES
        PREFIX ""                          # libvehicle_lcd.so / vehicle_lcd.dll
        CXX_VISIBILITY_PRESET hidden)      # only the mtr* exports are public
    if(WIN32)
        set_target_properties(${name} PROPERTIES SUFFIX ".dll")
    elseif(APPLE)
        set_target_properties(${name} PROPERTIES SUFFIX ".dylib" PREFIX "lib")
    else()
        set_target_properties(${name} PROPERTIES SUFFIX ".so" PREFIX "lib")
    endif()
endfunction()

mtr_add_script(vehicle_lcd      examples/vehicle_lcd.cpp)
mtr_add_script(pids_arrivals    examples/pids_arrivals.cpp)
mtr_add_script(eyecandy_signal  examples/eyecandy_signal.cpp)
mtr_add_script(jslcd_vehicle    examples/jslcd_vehicle.cpp)
mtr_add_script(jslcd_train_num  examples/jslcd_train_num.cpp)

# ----------------------------------------------------------------------------
# 若益宛 WR2-A03 resource pack (this workspace's pack).
#
# The pack's JS scripts (main.js + train_num.js + 11 includes) are ported to
# C++ one-to-one; each port becomes its own native library, declared in
# mtr_custom_resources.json as
#     { "id": "wr2a03:lcd",       "language": "cpp", "nativeLibrary": ... }
#     { "id": "wr2a03:train_num", "language": "cpp", "nativeLibrary": ... }
# (the script id is embedded in the library — mtrScriptId()).
# ----------------------------------------------------------------------------
mtr_add_script(wr2a03_lcd       examples/wr2a03_lcd.cpp)
mtr_add_script(wr2a03_train_num examples/wr2a03_train_num.cpp)

# ----------------------------------------------------------------------------
# JNI bridge — the dispatcher behind NativeScriptManager.java.
# Built only when a JDK is available (JNI headers); the script libraries
# themselves never need one.
# ----------------------------------------------------------------------------
find_package(JNI QUIET)
if(JNI_FOUND)
    add_library(jcm_native_bridge MODULE jni/jni_bridge.cpp)
    target_link_libraries(jcm_native_bridge PRIVATE mtr_script_sdk)
    target_include_directories(jcm_native_bridge PRIVATE ${JNI_INCLUDE_DIRS})
    set_target_properties(jcm_native_bridge PROPERTIES CXX_VISIBILITY_PRESET hidden)
    if(WIN32)
        set_target_properties(jcm_native_bridge PROPERTIES PREFIX "" SUFFIX ".dll")
    elseif(APPLE)
        set_target_properties(jcm_native_bridge PROPERTIES PREFIX "lib" SUFFIX ".dylib")
    else()
        set_target_properties(jcm_native_bridge PROPERTIES PREFIX "lib" SUFFIX ".so")
        target_link_libraries(jcm_native_bridge PRIVATE ${CMAKE_DL_LIBS})
    endif()
else()
    message(STATUS "JNI not found — skipping jcm_native_bridge (set JAVA_HOME to build it)")
endif()

# ----------------------------------------------------------------------------
# Benchmarks (development only, not shipped in resource packs)
# ----------------------------------------------------------------------------
# lcd_bench links the script directly (same as the README's g++ line:
#   g++ ... bench/lcd_bench.cpp examples/vehicle_lcd.cpp -o lcd_bench)
# — MODULE libraries can't be linked portably, so the example source is
# compiled into the executable instead.
add_executable(lcd_bench bench/lcd_bench.cpp examples/vehicle_lcd.cpp)
target_link_libraries(lcd_bench PRIVATE mtr_script_sdk)

# jslcd_smoke dlopen()s the freshly built script libraries — POSIX only.
# (Windows/macOS CI builds the script libraries themselves, which need
# nothing beyond the C++17 runtime.)
if(UNIX)
    add_executable(jslcd_smoke bench/jslcd_smoke.cpp)
    target_link_libraries(jslcd_smoke PRIVATE mtr_script_sdk ${CMAKE_DL_LIBS})
endif()

# wr2a03_smoke drives the WR2-A03 script libraries through the real ABI on
# EVERY platform (LoadLibrary on Windows, dlopen elsewhere), so the ported
# scripts are verified where they are actually shipped from.
add_executable(wr2a03_smoke bench/wr2a03_smoke.cpp)
target_link_libraries(wr2a03_smoke PRIVATE mtr_script_sdk)
if(UNIX)
    target_link_libraries(wr2a03_smoke PRIVATE ${CMAKE_DL_LIBS})
endif()
add_dependencies(wr2a03_smoke wr2a03_lcd wr2a03_train_num)

# install rule: copy script libraries into a resource-pack layout
install(TARGETS vehicle_lcd pids_arrivals eyecandy_signal jslcd_vehicle jslcd_train_num
                wr2a03_lcd wr2a03_train_num
        DESTINATION assets/mtr/natives)
