#include <jni.h>
#include <windows.h>
#include <mmdeviceapi.h>
#include <endpointvolume.h>

IAudioEndpointVolume* g_pEndpointVolume = NULL;

bool InitVolumeInterface() {
    if (g_pEndpointVolume) return true;

    HRESULT hr = CoInitializeEx(NULL, COINIT_MULTITHREADED);
    if (FAILED(hr) && hr != RPC_E_CHANGED_MODE) return false;

    IMMDeviceEnumerator* pEnumerator = NULL;
    IMMDevice* pDevice = NULL;

    hr = CoCreateInstance(__uuidof(MMDeviceEnumerator), NULL, CLSCTX_INPROC_SERVER, __uuidof(IMMDeviceEnumerator), (LPVOID*)&pEnumerator);
    if (FAILED(hr)) return false;

    hr = pEnumerator->GetDefaultAudioEndpoint(eRender, eMultimedia, &pDevice);
    pEnumerator->Release();
    if (FAILED(hr)) return false;

    hr = pDevice->Activate(__uuidof(IAudioEndpointVolume), CLSCTX_INPROC_SERVER, NULL, (LPVOID*)&g_pEndpointVolume);
    pDevice->Release();

    return SUCCEEDED(hr);
}

extern "C" {
    JNIEXPORT jfloat JNICALL Java_rf_ebanina_ebanina_Player_AudioVolumer_getSystemVolume(JNIEnv* env, jobject obj) {
        if (!InitVolumeInterface()) return 0.0f;

        float currentVolume = 0.0f;
        g_pEndpointVolume->GetMasterVolumeLevelScalar(&currentVolume);
        return (jfloat)currentVolume;
    }

    JNIEXPORT void JNICALL Java_rf_ebanina_ebanina_Player_AudioVolumer_setSystemVolume(JNIEnv* env, jobject obj, jfloat volume) {
        if (!InitVolumeInterface()) return;

        if (volume < 0.0f) volume = 0.0f;
        if (volume > 1.0f) volume = 1.0f;

        g_pEndpointVolume->SetMasterVolumeLevelScalar(volume, NULL);
    }

    JNIEXPORT void JNICALL Java_rf_ebanina_ebanina_Player_AudioVolumer_destroyNative(JNIEnv* env, jobject obj) {
        if (g_pEndpointVolume) {
            g_pEndpointVolume->Release();
            g_pEndpointVolume = NULL;
        }

        CoUninitialize();
    }
}
