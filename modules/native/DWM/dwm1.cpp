#include <jni.h>
#include <windows.h>
#include <dwmapi.h>

#pragma comment(lib, "dwmapi.lib")

extern "C" {
    JNIEXPORT void JNICALL Java_rf_ebanina_UI_Root_setCaptionColor(JNIEnv *env, jclass clazz, jlong wid, jint color) {
        HWND hwnd = (HWND)wid;
        
        COLORREF captionColor = (COLORREF)color;

        DwmSetWindowAttribute(hwnd, 35, &captionColor, sizeof(captionColor));
    }
}
