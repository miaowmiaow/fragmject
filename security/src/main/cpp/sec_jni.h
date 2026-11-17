#pragma once

#include <jni.h>

// 跨 sec_*.cpp 共享的 JNI 实现声明（定义分布在各自模块中）。

extern "C" {
jint jniVerifyLibcTextCrc(JNIEnv *, jobject);
jint jniVerifySelfSoTextCrc(JNIEnv *env, jobject, jstring selfSoPath);
jint jniVerifySelfSoDataCrc(JNIEnv *env, jobject, jstring selfSoPath);
jint jniReadTracerPid(JNIEnv *, jobject);
jint jniCountAnonymousExecSegments(JNIEnv *, jobject);
jint jniScanMapsForSignals(JNIEnv *, jobject);
jint jniFindRootInMaps(JNIEnv *, jobject);
jint jniCountThreadSignals(JNIEnv *, jobject);
jint jniCountFileSignals(JNIEnv *, jobject);
jint jniFindRootFiles(JNIEnv *, jobject);
jint jniCountPltHooks(JNIEnv *, jobject);
jint jniInstallAntiPtrace(JNIEnv *, jobject);
jint jniHardenProcess(JNIEnv *, jobject);
jint jniVerifyApkSignature(JNIEnv *, jobject);
void jniKillSelf(JNIEnv *, jobject);
}  // extern "C"
