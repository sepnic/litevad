cmake_minimum_required(VERSION 3.4.1)
project(litevad_jni)

# cflags: compile paramters
set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS}   -std=gnu99 -Wall -Werror")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -Werror -UNDEBUG")

set(TOP_DIR "${CMAKE_SOURCE_DIR}/../../../../../..")

include_directories(${TOP_DIR}/include)

set(LITEVAD_SRC
    ${TOP_DIR}/thirdparty/webrtc/src/signal_processing/energy.c
    ${TOP_DIR}/thirdparty/webrtc/src/signal_processing/division_operations.c
    ${TOP_DIR}/thirdparty/webrtc/src/signal_processing/get_scaling_square.c
    ${TOP_DIR}/thirdparty/webrtc/src/signal_processing/resample.c
    ${TOP_DIR}/thirdparty/webrtc/src/signal_processing/resample_48khz.c
    ${TOP_DIR}/thirdparty/webrtc/src/signal_processing/resample_by_2.c
    ${TOP_DIR}/thirdparty/webrtc/src/signal_processing/resample_by_2_internal.c
    ${TOP_DIR}/thirdparty/webrtc/src/signal_processing/resample_fractional.c
    ${TOP_DIR}/thirdparty/webrtc/src/vad/vad_core.c
    ${TOP_DIR}/thirdparty/webrtc/src/vad/vad_filterbank.c
    ${TOP_DIR}/thirdparty/webrtc/src/vad/vad_gmm.c
    ${TOP_DIR}/thirdparty/webrtc/src/vad/vad_sp.c
    ${TOP_DIR}/thirdparty/webrtc/src/vad/webrtc_vad.c
    ${TOP_DIR}/thirdparty/webrtc/src/resampler/resampler.cc
    ${TOP_DIR}/src/litevad.c)

add_library(litevad-jni SHARED litevad-jni.cpp ${LITEVAD_SRC})
target_include_directories(litevad-jni PRIVATE ${TOP_DIR}/thirdparty/webrtc/inc)
target_link_libraries(litevad-jni android log)
