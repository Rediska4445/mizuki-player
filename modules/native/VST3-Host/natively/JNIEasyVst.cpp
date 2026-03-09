#include <jni.h>
#include <memory>
#include <vector>
#include "EasyVst.h"

#include <iostream>

extern "C" {
    // Создаем новый объект EasyVst и возвращаем указатель (nativeHandle)
    JNIEXPORT jlong JNICALL Java_rf_vst3_createNativeInstance(JNIEnv* env, jobject thiz) {
        EasyVst* vst = new EasyVst();
        std::cout << "[JNI] Created EasyVst instance: " << vst << std::endl;
        return reinterpret_cast<jlong>(vst);
    }

    // Уничтожаем объект EasyVst
    JNIEXPORT void JNICALL Java_rf_vst3_disposeNativeInstance(JNIEnv* env, jobject thiz, jlong nativeHandle) {
        EasyVst* vst = reinterpret_cast<EasyVst*>(nativeHandle);
        if (vst) {
            std::cout << "[JNI] Destroying EasyVst instance: " << vst << std::endl;
            delete vst;
        }
    }

    JNIEXPORT void JNICALL Java_rf_vst3_setLoggingEnabled(JNIEnv* env, jobject thiz, jlong nativeHandle, jboolean enabled) {
        EasyVst* vst = reinterpret_cast<EasyVst*>(nativeHandle);

        if (vst) {
            vst->setLoggingEnabled(enabled != JNI_FALSE);
        }
    }

    extern "C"
        JNIEXPORT jint JNICALL
        Java_rf_vst3_getParameterCount(JNIEnv* env, jobject obj, jlong nativeHandle)
    {
        EasyVst* vst = reinterpret_cast<EasyVst*>(nativeHandle);
        if (!vst) {
            std::cerr << "[JNI] Invalid nativeHandle in getParameterCount" << std::endl;
            return 0;
        }

        int32_t count = vst->getParameterCount();
        std::cout << "[JNI] Plugin has " << count << " parameters." << std::endl;
        return static_cast<jint>(count);
    }

    extern "C"
        JNIEXPORT jdouble JNICALL
        Java_rf_vst3_getParameterValue(JNIEnv* env, jobject obj, jlong nativeHandle, jint index) {
        EasyVst* vst = reinterpret_cast<EasyVst*>(nativeHandle);
        if (!vst) {
            std::cerr << "[JNI] Invalid nativeHandle in getParameterValue" << std::endl;
            return -1.0;
        }

        double value = vst->getParameterValue(static_cast<Steinberg::int32>(index));
        std::cout << "[JNI] Got parameter " << index << " = " << value << std::endl;
        return static_cast<jdouble>(value);
    }

    extern "C"
        JNIEXPORT jboolean JNICALL
        Java_rf_vst3_setParameterValue(JNIEnv* env, jobject obj, jlong nativeHandle, jint index, jdouble value) {
        EasyVst* vst = reinterpret_cast<EasyVst*>(nativeHandle);
        if (!vst) {
            std::cerr << "[JNI] Invalid nativeHandle in setParameterValue" << std::endl;
            return JNI_FALSE;
        }

        bool success = vst->setParameterValue(static_cast<Steinberg::int32>(index), static_cast<double>(value));
        if (success) {
            std::cout << "[JNI] Set parameter " << index << " = " << value << std::endl;
        }
        else {
            std::cerr << "[JNI] Failed to set parameter " << index << " to " << value << std::endl;
        }
        return success ? JNI_TRUE : JNI_FALSE;
    }

    extern "C" JNIEXPORT void JNICALL Java_rf_vst3_destroy(JNIEnv* env, jobject obj, jlong nativeHandle) {
        EasyVst* g_vstHost = reinterpret_cast<EasyVst*>(nativeHandle);

        if (!g_vstHost) {
            std::cerr << "gHost in zero: " << g_vstHost << std::endl;
        }

        g_vstHost->destroy();
    }

    extern "C" JNIEXPORT jstring JNICALL Java_rf_vst3_getVendor(JNIEnv* env, jobject obj, jlong nativeHandle) {
        EasyVst* g_vstHost = reinterpret_cast<EasyVst*>(nativeHandle);

        if (!g_vstHost) {
            std::cerr << "gHost in zero: " << g_vstHost << std::endl;

            return env->NewStringUTF("NULL");
        }

        std::string vendor = g_vstHost->getVendor();

        return env->NewStringUTF(vendor.c_str());
    }

    extern "C" JNIEXPORT jstring JNICALL Java_rf_vst3_getPluginName(JNIEnv* env, jobject obj, jlong nativeHandle) {
        EasyVst* g_vstHost = reinterpret_cast<EasyVst*>(nativeHandle);

        if (!g_vstHost) {
            std::cerr << "gHost in zero: " << g_vstHost << std::endl;

            return env->NewStringUTF("NULL");
        }

        std::string vendor = g_vstHost->getPluginName();

        return env->NewStringUTF(vendor.c_str());
    }

    extern "C" JNIEXPORT jstring JNICALL Java_rf_vst3_getCategory(JNIEnv* env, jobject obj, jlong nativeHandle) {
        EasyVst* g_vstHost = reinterpret_cast<EasyVst*>(nativeHandle);

        if (!g_vstHost) {
            std::cerr << "gHost in zero: " << g_vstHost << std::endl;

            return env->NewStringUTF("NULL");
        }

        std::string vendor = g_vstHost->getCategory();

        return env->NewStringUTF(vendor.c_str());
    }

    extern "C" JNIEXPORT jstring JNICALL Java_rf_vst3_getVersion(JNIEnv* env, jobject obj, jlong nativeHandle) {
        EasyVst* g_vstHost = reinterpret_cast<EasyVst*>(nativeHandle);

        if (!g_vstHost) {
            std::cerr << "gHost in zero: " << g_vstHost << std::endl;

            return env->NewStringUTF("NULL");
        }

        std::string vendor = g_vstHost->getVersion();

        return env->NewStringUTF(vendor.c_str());
    }

    extern "C" JNIEXPORT jstring JNICALL Java_rf_vst3_getSdkVersion(JNIEnv* env, jobject obj, jlong nativeHandle) {
        EasyVst* g_vstHost = reinterpret_cast<EasyVst*>(nativeHandle);

        if (!g_vstHost) {
            std::cerr << "gHost in zero: " << g_vstHost << std::endl;

            return env->NewStringUTF("NULL");
        }

        std::string vendor = g_vstHost->getSdkVersion();

        return env->NewStringUTF(vendor.c_str());
    }

    // Инициализация плагина
    JNIEXPORT jboolean JNICALL Java_rf_vst3_initializePlugin(JNIEnv* env, jobject thiz,
        jlong nativeHandle,
        jstring pluginPath,
        jint sampleRate,
        jint maxBlockSize,
        jint symbolicSampleSize,
        jboolean realtime) {

        EasyVst* vst = reinterpret_cast<EasyVst*>(nativeHandle);

        if (!vst) 
            return JNI_FALSE;

        const char* pathCStr = env->GetStringUTFChars(pluginPath, nullptr);
        if (!pathCStr)
            return JNI_FALSE;

        bool ok = vst->init(std::string(pathCStr), sampleRate, maxBlockSize, symbolicSampleSize, realtime);

        env->ReleaseStringUTFChars(pluginPath, pathCStr);

        return ok ? JNI_TRUE : JNI_FALSE;
    }

    // Создание View
    JNIEXPORT jboolean JNICALL Java_rf_vst3_createView(JNIEnv* env, jobject thiz, jlong nativeHandle) {
        EasyVst* vst = reinterpret_cast<EasyVst*>(nativeHandle);
        if (!vst)
        {
            std::cerr << "vst is " << vst << std::endl;
            return JNI_FALSE;
        }

        bool result = vst->createView();

        return result ? JNI_TRUE : JNI_FALSE;
    }

    // Получение количества входов
    JNIEXPORT jint JNICALL Java_rf_vst3_getNumInputs(JNIEnv* env, jobject thiz, jlong nativeHandle) {
        EasyVst* vst = reinterpret_cast<EasyVst*>(nativeHandle);
        if (!vst) return 0;

        return vst->numInputs();
    }

    // Получение количества выходов
    JNIEXPORT jint JNICALL Java_rf_vst3_getNumOutputs(JNIEnv* env, jobject thiz, jlong nativeHandle) {
        EasyVst* vst = reinterpret_cast<EasyVst*>(nativeHandle);
        if (!vst) return 0;

        return vst->numOutputs();
    }

    // Обработка звука
    JNIEXPORT jboolean JNICALL Java_rf_vst3_processReplacing(JNIEnv* env, jobject thiz,
        jlong nativeHandle,
        jobjectArray inArray, jobjectArray outArray,
        jint framesRead) {
        EasyVst* vst = reinterpret_cast<EasyVst*>(nativeHandle);
        if (!vst) {
            std::cerr << "[processReplacing] vst is null" << std::endl;
            return JNI_FALSE;
        }

        jsize numInChannels = env->GetArrayLength(inArray);
        jsize numOutChannels = env->GetArrayLength(outArray);

        std::vector<float*> inBuffers(numInChannels);
        std::vector<jfloatArray> inFloats(numInChannels);

        for (jsize i = 0; i < numInChannels; ++i) {
            inFloats[i] = (jfloatArray)env->GetObjectArrayElement(inArray, i);
            if (!inFloats[i]) {
                std::cerr << "[processReplacing] Null input float array at index " << i << std::endl;
                // освобождение ранее полученных массивов
                for (jsize j = 0; j < i; ++j) {
                    env->ReleaseFloatArrayElements(inFloats[j], inBuffers[j], JNI_ABORT);
                }
                return JNI_FALSE;
            }
            inBuffers[i] = env->GetFloatArrayElements(inFloats[i], nullptr);
            if (!inBuffers[i]) {
                std::cerr << "[processReplacing] Failed to get input float array elements at index " << i << std::endl;
                for (jsize j = 0; j < i; ++j) {
                    env->ReleaseFloatArrayElements(inFloats[j], inBuffers[j], JNI_ABORT);
                }
                return JNI_FALSE;
            }
        }

        std::vector<float*> outBuffers(numOutChannels);
        std::vector<jfloatArray> outFloats(numOutChannels);

        for (jsize i = 0; i < numOutChannels; ++i) {
            outFloats[i] = (jfloatArray)env->GetObjectArrayElement(outArray, i);
            if (!outFloats[i]) {
                std::cerr << "[processReplacing] Null output float array at index " << i << std::endl;
                for (jsize j = 0; j < numInChannels; ++j) {
                    env->ReleaseFloatArrayElements(inFloats[j], inBuffers[j], JNI_ABORT);
                }
                // освобождение ранее захваченных выходных буферов
                for (jsize j = 0; j < i; ++j) {
                    env->ReleaseFloatArrayElements(outFloats[j], outBuffers[j], 0);
                }
                return JNI_FALSE;
            }
            outBuffers[i] = env->GetFloatArrayElements(outFloats[i], nullptr);
            if (!outBuffers[i]) {
                std::cerr << "[processReplacing] Failed to get output float array elements at index " << i << std::endl;
                for (jsize j = 0; j < numInChannels; ++j) {
                    env->ReleaseFloatArrayElements(inFloats[j], inBuffers[j], JNI_ABORT);
                }
                for (jsize j = 0; j < i; ++j) {
                    env->ReleaseFloatArrayElements(outFloats[j], outBuffers[j], 0);
                }
                return JNI_FALSE;
            }
        }

        bool result = false;
        try {
            result = vst->process(inBuffers.data(), outBuffers.data(), framesRead);
        }
        catch (const std::exception& ex) {
            std::cerr << "[processReplacing] Exception during processing: " << ex.what() << std::endl;
        }
        catch (...) {
            std::cerr << "[processReplacing] Unknown exception during processing" << std::endl;
        }

        for (jsize i = 0; i < numInChannels; ++i) {
            env->ReleaseFloatArrayElements(inFloats[i], inBuffers[i], JNI_ABORT);
        }
        for (jsize i = 0; i < numOutChannels; ++i) {
            env->ReleaseFloatArrayElements(outFloats[i], outBuffers[i], 0);
        }

        return result ? JNI_TRUE : JNI_FALSE;
    }

    // Реализация getNumChannelsForInputBus (в примере)
    JNIEXPORT jint JNICALL Java_rf_vst3_getNumChannelsForInputBus(JNIEnv* env, jobject thiz, jlong nativeHandle, jint busIndex) {
        EasyVst* vst = reinterpret_cast<EasyVst*>(nativeHandle);
        if (!vst) return 0;
        return vst->getNumChannelsForInputBus(busIndex);
    }

    // Реализация getNumChannelsForOutputBus (в примере)
    JNIEXPORT jint JNICALL Java_rf_vst3_getNumChannelsForOutputBus(JNIEnv* env, jobject thiz, jlong nativeHandle, jint busIndex) {
        EasyVst* vst = reinterpret_cast<EasyVst*>(nativeHandle);
        if (!vst) return 0;
        return vst->getNumChannelsForOutputBus(busIndex);
    }
}
