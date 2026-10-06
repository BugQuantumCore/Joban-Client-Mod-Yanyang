/*
 * bench/stub/jni.h — minimal JNI surface for LOCAL SYNTAX VALIDATION
 * of jni_bridge.cpp only (this sandbox has a JRE, not a JDK, so the real
 * jni.h is unavailable). The real build uses the JDK headers via CMake
 * find_package(JNI) / CI — see .github/workflows/native-build.yml.
 *
 * Mirrors the structure of the real C++ jni.h: a JNINativeInterface_
 * function table plus a _JNIEnv wrapper whose inline members forward
 * to the table with `this` as the first argument — so `env->Fn(args)`
 * compiles exactly like against the real header.
 */
#ifndef STUB_JNI_H
#define STUB_JNI_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef uint8_t  jboolean;
typedef int32_t  jint;
typedef int64_t  jlong;

struct _jobject;
typedef struct _jobject *jobject;
typedef jobject jstring;
typedef jobject jclass;
typedef jobject jmethodID;

struct _JNIEnv;
struct _JavaVM;

struct JNINativeInterface_ {
    jint        (*GetVersion)(struct _JNIEnv*);
    jclass      (*FindClass)(struct _JNIEnv*, const char*);
    jmethodID   (*GetMethodID)(struct _JNIEnv*, jclass, const char*, const char*);
    jobject     (*NewObject)(struct _JNIEnv*, jclass, jmethodID, ...);
    jstring     (*NewStringUTF)(struct _JNIEnv*, const char*);
    const char* (*GetStringUTFChars)(struct _JNIEnv*, jstring, jboolean*);
    void        (*ReleaseStringUTFChars)(struct _JNIEnv*, jstring, const char*);
    void*       (*GetDirectBufferAddress)(struct _JNIEnv*, jobject);
    jlong       (*GetDirectBufferCapacity)(struct _JNIEnv*, jobject);
    jobject     (*NewDirectByteBuffer)(struct _JNIEnv*, void*, jlong);
    jboolean    (*ExceptionCheck)(struct _JNIEnv*);
};

struct _JNIEnv {
    const struct JNINativeInterface_ *functions;
#ifdef __cplusplus
    jint GetVersion() { return functions->GetVersion(this); }
    jclass FindClass(const char* name) { return functions->FindClass(this, name); }
    jmethodID GetMethodID(jclass c, const char* n, const char* s) {
        return functions->GetMethodID(this, c, n, s);
    }
    jobject NewObject(jclass c, jmethodID m, ...) {
        return functions->NewObject(this, c, m);
    }
    jstring NewStringUTF(const char* s) { return functions->NewStringUTF(this, s); }
    const char* GetStringUTFChars(jstring s, jboolean* b) {
        return functions->GetStringUTFChars(this, s, b);
    }
    void ReleaseStringUTFChars(jstring s, const char* c) {
        functions->ReleaseStringUTFChars(this, s, c);
    }
    void* GetDirectBufferAddress(jobject b) {
        return functions->GetDirectBufferAddress(this, b);
    }
    jlong GetDirectBufferCapacity(jobject b) {
        return functions->GetDirectBufferCapacity(this, b);
    }
    jobject NewDirectByteBuffer(void* addr, jlong cap) {
        return functions->NewDirectByteBuffer(this, addr, cap);
    }
    jboolean ExceptionCheck() { return functions->ExceptionCheck(this); }
#endif
};

typedef struct _JNIEnv JNIEnv;
typedef struct _JavaVM JavaVM;

#define JNIEXPORT
#define JNICALL
#define JNI_OK 0
#define JNI_VERSION_1_8 0x00010008

#ifdef __cplusplus
}
#endif

#endif /* STUB_JNI_H */
