cmake_minimum_required(VERSION 3.16)
project(mtr-native-scripting CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()
set(CMAKE_CXX_FLAGS_RELEASE "-O2")

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
# Benchmarks (development only, not shipped in resource packs)
# ----------------------------------------------------------------------------
add_executable(lcd_bench bench/lcd_bench.cpp)
target_link_libraries(lcd_bench PRIVATE mtr_script_sdk)

# jslcd_smoke dlopen()s the freshly built script libraries — POSIX only.
# (Windows/macOS CI builds the script libraries themselves, which need
# nothing beyond the C++17 runtime.)
if(UNIX)
    add_executable(jslcd_smoke bench/jslcd_smoke.cpp)
    target_link_libraries(jslcd_smoke PRIVATE mtr_script_sdk ${CMAKE_DL_LIBS})
endif()

# install rule: copy script libraries into a resource-pack layout
install(TARGETS vehicle_lcd pids_arrivals eyecandy_signal jslcd_vehicle jslcd_train_num
        DESTINATION assets/mtr/natives)
