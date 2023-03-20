/*
 * Copyright (C) 2018-2023 luoyun <sysu.zqlong@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
#include <cstdio>
#include <cassert>
#include <cstring>
#include <android/log.h>
#include "litevad.h"

#define TAG "LiteVadJNI"

#define pr_dbg(fmt, ...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##__VA_ARGS__)
#define pr_err(fmt, ...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##__VA_ARGS__)

#define JAVA_CLASS_NAME "com/example/litevad_demo/MainActivity"
#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

static void jniThrowException(JNIEnv *env, const char *className, const char *msg) {
    jclass clazz = env->FindClass(className);
    if (!clazz) {
        pr_err("Unable to find exception class %s", className);
        /* ClassNotFoundException now pending */
        return;
    }
    if (env->ThrowNew(clazz, msg) != JNI_OK) {
        pr_err("Failed throwing '%s' '%s'", className, msg);
        /* an exception, most likely OOM, will now be pending */
    }
    env->DeleteLocalRef(clazz);
}

static jlong Litevad_native_create(JNIEnv* env, jobject thiz, jint sample_rate, jint channel_count, jint sample_bits)
{
    pr_dbg("Litevad_native_create");

    litevad_handle_t handle = litevad_create(sample_rate, channel_count, sample_bits);
    return (jlong)handle;
}

static jint Litevad_native_process(JNIEnv *env, jobject thiz, jlong handle, jbyteArray buffer, jint size)
{
    //pr_dbg("Litevad_native_process");

    auto vad = reinterpret_cast<litevad_handle_t>(handle);
    if (vad == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException", "Received null vad-handle");
        return -1;
    }

    jbyte *bytebuffer = env->GetByteArrayElements(buffer, nullptr);
    jint ret = litevad_process(vad, (void *)bytebuffer, size);
    env->ReleaseByteArrayElements(buffer, bytebuffer, 0);
    return ret;
}

static void Litevad_native_reset(JNIEnv *env, jobject thiz, jlong handle)
{
    pr_dbg("Litevad_native_reset");

    auto vad = reinterpret_cast<litevad_handle_t>(handle);
    if (vad == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException", "Received null vad-handle");
        return;
    }

    litevad_reset(vad);
}

static void Litevad_native_destroy(JNIEnv *env, jobject thiz, jlong handle)
{
    pr_dbg("Litevad_native_destroy");

    auto vad = reinterpret_cast<litevad_handle_t>(handle);
    if (vad == nullptr) {
        jniThrowException(env, "java/lang/IllegalStateException", "Received null vad-handle");
        return;
    }

    litevad_destroy(vad);
}

static JNINativeMethod gMethods[] = {
        {"native_create", "(III)J", (void *)Litevad_native_create},
        {"native_process", "(J[BI)I", (void *)Litevad_native_process}, 
        {"native_reset", "(J)V", (void *)Litevad_native_reset},
        {"native_destroy", "(J)V", (void *)Litevad_native_destroy},
};

static int registerNativeMethods(JNIEnv *env, const char *className,JNINativeMethod *getMethods, int methodsNum)
{
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == nullptr) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz,getMethods,methodsNum) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv* env = nullptr;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        pr_err("Failed to get env");
        goto bail;
    }
    assert(env != nullptr);

    if (registerNativeMethods(env, JAVA_CLASS_NAME, gMethods, NELEM(gMethods)) != JNI_TRUE) {
        pr_err("Failed to register native methods");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_6;

bail:
    return result;
}
