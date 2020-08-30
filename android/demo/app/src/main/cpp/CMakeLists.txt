cmake_minimum_required(VERSION 3.4.1)

# cflags: compile paramters
set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS}   -std=gnu99 -Wall -Werror")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -Werror -UNDEBUG")

set(JNILIBS_DIR "${CMAKE_SOURCE_DIR}/../../../jniLibs")

include_directories(${JNILIBS_DIR}/include)

add_library(litevad SHARED IMPORTED)
set_target_properties(litevad PROPERTIES IMPORTED_LOCATION "${JNILIBS_DIR}/libs/${ANDROID_ABI}/liblitevad.so")

add_library(litevad-jni SHARED litevad-jni.cpp)

target_link_libraries(litevad-jni litevad android log)
