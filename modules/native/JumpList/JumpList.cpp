#define UNICODE
#define _UNICODE

#include <jni.h>
#include <windows.h>
#include <shobjidl.h>
#include <propkey.h>
#include <string>
#include <stdio.h>
#include <vector>

#pragma comment(lib, "ole32.lib")
#pragma comment(lib, "shell32.lib")
#pragma comment(lib, "user32.lib")

static bool g_loggingEnabled = true;
static HANDLE g_hMutex = nullptr;
static WNDPROC g_originalWndProc = nullptr;
static JavaVM* g_jvm = nullptr;

struct JumpListTask {
    jlong id;
    std::wstring title;
    std::wstring arguments;
    std::wstring iconPath;
    jobject handler;

    JumpListTask() : id(0), handler(nullptr) {}

    ~JumpListTask() {
        if (handler != nullptr) {
            JNIEnv* env = nullptr;
            if (g_jvm && g_jvm->GetEnv((void**)&env, JNI_VERSION_1_8) == JNI_OK) {
                env->DeleteGlobalRef(handler);
            }
        }
    }
};

static std::vector<JumpListTask> g_tasks;

void Log(const wchar_t* msg) {
    if (!g_loggingEnabled) return;
    fwprintf_s(stdout, L"%s\n", msg);
    fflush(stdout);
}

void LogHresult(const wchar_t* prefix, HRESULT hr) {
    if (!g_loggingEnabled) return;
    wchar_t buf[512];
    swprintf_s(buf, L"%s: HRESULT = 0x%08X", prefix, hr);
    Log(buf);
}

void LogHwnd(const wchar_t* prefix, HWND hwnd) {
    if (!g_loggingEnabled) return;
    wchar_t buf[512];
    swprintf_s(buf, L"%s: HWND = 0x%p, IsWindow = %d", prefix, hwnd, IsWindow(hwnd));
    Log(buf);
}

HRESULT AddTaskToJumpList(HWND hwnd) {
    wchar_t msg[512];

    Log(L"=== [AddTaskToJumpList] START ===");

    LogHwnd(L"[AddTaskToJumpList] Input HWND", hwnd);
    if (!IsWindow(hwnd)) {
        Log(L"[AddTaskToJumpList] ERROR: Invalid HWND");
        return E_INVALIDARG;
    }

    if (g_tasks.empty()) {
        Log(L"[AddTaskToJumpList] WARNING: список задач пуст");
        return S_OK;
    }

    swprintf_s(msg, L"[AddTaskToJumpList] Задач: %zu", g_tasks.size());
    Log(msg);

    Log(L"[AddTaskToJumpList] --- Создание ICustomDestinationList ---");
    ICustomDestinationList* pList = nullptr;
    HRESULT hr = CoCreateInstance(CLSID_DestinationList, nullptr, CLSCTX_INPROC_SERVER,
        IID_PPV_ARGS(&pList));
    LogHresult(L"[AddTaskToJumpList] CoCreateInstance(DestinationList)", hr);

    if (FAILED(hr) || pList == nullptr) {
        Log(L"[AddTaskToJumpList] ERROR: CoCreateInstance failed");
        return hr;
    }

    Log(L"[AddTaskToJumpList] --- Установка AppID ---");
    hr = pList->SetAppID(L"rf.ebanina.EbaninaVST");
    LogHresult(L"[AddTaskToJumpList] SetAppID", hr);

    Log(L"[AddTaskToJumpList] --- BeginList ---");
    UINT maxSlots = 0;
    IObjectArray* pRemoved = nullptr;
    hr = pList->BeginList(&maxSlots, IID_PPV_ARGS(&pRemoved));
    LogHresult(L"[AddTaskToJumpList] BeginList", hr);

    if (pRemoved != nullptr) {
        pRemoved->Release();
    }

    if (FAILED(hr)) {
        Log(L"[AddTaskToJumpList] ERROR: BeginList failed");
        pList->Release();
        return hr;
    }

    Log(L"[AddTaskToJumpList] --- Создание IObjectCollection ---");
    IObjectCollection* pCollection = nullptr;
    hr = CoCreateInstance(CLSID_EnumerableObjectCollection, nullptr,
        CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&pCollection));

    if (FAILED(hr) || pCollection == nullptr) {
        Log(L"[AddTaskToJumpList] ERROR: FAILED to create ObjectCollection");
        pList->Release();
        return hr;
    }

    wchar_t exePath[MAX_PATH];
    DWORD pathLen = GetModuleFileNameW(nullptr, exePath, MAX_PATH);
    if (pathLen == 0) {
        Log(L"[AddTaskToJumpList] ERROR: GetModuleFileNameW failed");
        pCollection->Release();
        pList->Release();
        return HRESULT_FROM_WIN32(GetLastError());
    }

    wchar_t workDir[MAX_PATH];
    wcscpy_s(workDir, exePath);
    wchar_t* lastBackslash = wcsrchr(workDir, L'\\');
    if (lastBackslash) *lastBackslash = L'\0';

    Log(L"[AddTaskToJumpList] --- Добавление задач в коллекцию ---");
    for (size_t i = 0; i < g_tasks.size(); i++) {
        const JumpListTask& task = g_tasks[i];

        swprintf_s(msg, L"[AddTaskToJumpList] Задача %zu/%zu: ID=%lld, \"%s\"",
            i + 1, g_tasks.size(), task.id, task.title.c_str());
        Log(msg);

        IShellLinkW* pLink = nullptr;
        hr = CoCreateInstance(CLSID_ShellLink, nullptr, CLSCTX_INPROC_SERVER,
            IID_PPV_ARGS(&pLink));
        if (FAILED(hr) || pLink == nullptr) {
            continue;
        }

        pLink->SetPath(exePath);
        pLink->SetWorkingDirectory(workDir);

        if (!task.arguments.empty()) {
            pLink->SetArguments(task.arguments.c_str());
        }

        pLink->SetDescription(task.title.c_str());

        IPropertyStore* pPropStore = nullptr;
        if (SUCCEEDED(pLink->QueryInterface(IID_PPV_ARGS(&pPropStore))) && pPropStore) {
            PROPVARIANT pv;
            PropVariantInit(&pv);
            pv.vt = VT_LPWSTR;
            pv.pwszVal = (LPWSTR)CoTaskMemAlloc((task.title.length() + 1) * sizeof(wchar_t));
            if (pv.pwszVal) {
                wcscpy_s(pv.pwszVal, task.title.length() + 1, task.title.c_str());
                pPropStore->SetValue(PKEY_Title, pv);
                pPropStore->Commit();
                PropVariantClear(&pv);
            }
            pPropStore->Release();
        }

        if (!task.iconPath.empty()) {
            pLink->SetIconLocation(task.iconPath.c_str(), 0);
        }
        else {
            pLink->SetIconLocation(exePath, 0);
        }

        pCollection->AddObject(pLink);
        pLink->Release();
    }

    Log(L"[AddTaskToJumpList] --- Вызов AddUserTasks ---");
    hr = pList->AddUserTasks((IObjectArray*)pCollection);
    LogHresult(L"[AddTaskToJumpList] AddUserTasks", hr);

    pCollection->Release();

    Log(L"[AddTaskToJumpList] --- Вызов CommitList ---");
    hr = pList->CommitList();
    LogHresult(L"[AddTaskToJumpList] CommitList", hr);

    pList->Release();

    Log(L"=== [AddTaskToJumpList] END ===");
    return hr;
}

LRESULT CALLBACK SubclassWndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    if (msg == WM_COPYDATA) {
        COPYDATASTRUCT* pCds = (COPYDATASTRUCT*)lParam;
        if (pCds && pCds->dwData == 1) {
            // Читаем ID
            jlong id = *(jlong*)pCds->lpData;

            // Получаем JNIEnv
            JNIEnv* env = nullptr;
            bool attached = false;
            if (g_jvm->GetEnv((void**)&env, JNI_VERSION_1_8) != JNI_OK) {
                g_jvm->AttachCurrentThread((void**)&env, nullptr);
                attached = true;
            }

            // Вызываем executeHandler в Java
            jclass jumpListClass = env->FindClass("rf/ebanina/UI/Editors/Window/JumpList");
            if (jumpListClass) {
                jmethodID methodId = env->GetStaticMethodID(jumpListClass, "executeHandler", "(J)V");
                if (methodId) {
                    env->CallStaticVoidMethod(jumpListClass, methodId, id);
                }
            }

            if (attached) {
                g_jvm->DetachCurrentThread();
            }

            return TRUE;
        }
    }
    return CallWindowProc(g_originalWndProc, hwnd, msg, wParam, lParam);
}

extern "C" {

    JNIEXPORT void JNICALL Java_rf_ebanina_UI_Editors_Window_JumpList_enableLogging(
        JNIEnv* env, jclass clazz, jboolean enable)
    {
        g_loggingEnabled = (enable == JNI_TRUE);
        if (g_loggingEnabled) {
            fwprintf_s(stdout, L"[JumpList] Логирование ВКЛЮЧЕНО\n");
            fflush(stdout);
        }
    }

    JNIEXPORT void JNICALL Java_rf_ebanina_UI_Editors_Window_JumpList_registerAppUserModelID(
        JNIEnv* env, jclass clazz)
    {
        HRESULT hr = SetCurrentProcessExplicitAppUserModelID(L"rf.ebanina.EbaninaVST");
        if (SUCCEEDED(hr)) {
            fwprintf_s(stderr, L"[JumpList] AppUserModelID установлен: rf.ebanina.EbaninaVST\n");
        }
        else {
            fwprintf_s(stderr, L"[JumpList] ОШИБКА: не удалось установить AppUserModelID!\n");
        }
    }

    JNIEXPORT void JNICALL Java_rf_ebanina_UI_Editors_Window_JumpList_nativeAdd(
        JNIEnv* env, jclass clazz,
        jlong windowId, jlong id,
        jstring title, jstring arguments, jstring iconPath, jobject handler)  // <-- Добавлен handler
    {
        Log(L"=== [JNI nativeAdd] START ===");

        if (windowId == 0) {
            Log(L"[JNI nativeAdd] ERROR: windowId is 0");
            return;
        }

        HWND hwnd = (HWND)windowId;
        if (!IsWindow(hwnd)) {
            Log(L"[JNI nativeAdd] ERROR: HWND is not valid");
            return;
        }

        JumpListTask task;
        task.id = id;

        if (title) {
            const jchar* chars = env->GetStringChars(title, nullptr);
            task.title = (const wchar_t*)chars;
            env->ReleaseStringChars(title, chars);
        }

        if (arguments) {
            const jchar* chars = env->GetStringChars(arguments, nullptr);
            task.arguments = (const wchar_t*)chars;
            env->ReleaseStringChars(arguments, chars);
        }

        if (iconPath) {
            const jchar* chars = env->GetStringChars(iconPath, nullptr);
            task.iconPath = (const wchar_t*)chars;
            env->ReleaseStringChars(iconPath, chars);
        }

        if (handler != nullptr) {
            task.handler = env->NewGlobalRef(handler);
        }

        g_tasks.push_back(task);

        wchar_t msg[256];
        swprintf_s(msg, L"[JNI nativeAdd] Задача ID=%lld добавлена. Всего: %zu", id, g_tasks.size());
        Log(msg);

        HRESULT coInit = CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
        if (SUCCEEDED(coInit) || coInit == RPC_E_CHANGED_MODE) {
            AddTaskToJumpList(hwnd);
            if (coInit == S_OK) {
                CoUninitialize();
            }
        }

        Log(L"=== [JNI nativeAdd] END ===");
    }

    JNIEXPORT void JNICALL Java_rf_ebanina_UI_Editors_Window_JumpList_clearAll(
        JNIEnv* env, jclass clazz, jlong windowId)
    {
        Log(L"=== [JNI clearAll] START ===");

        // 1. Сначала ПОЛНОСТЬЮ удаляем Jump List для этого AppID
        Log(L"[JNI clearAll] --- Удаление Jump List ---");
        ICustomDestinationList* pList = nullptr;
        HRESULT hr = CoCreateInstance(CLSID_DestinationList, nullptr, CLSCTX_INPROC_SERVER,
            IID_PPV_ARGS(&pList));

        if (SUCCEEDED(hr) && pList != nullptr) {
            hr = pList->SetAppID(L"rf.ebanina.EbaninaVST");
            if (SUCCEEDED(hr)) {
                hr = pList->DeleteList(L"rf.ebanina.EbaninaVST");
                LogHresult(L"[JNI clearAll] DeleteList", hr);
            }
            pList->Release();
        }
        else {
            LogHresult(L"[JNI clearAll] CoCreateInstance", hr);
        }

        g_tasks.clear();
        Log(L"[JNI clearAll] g_tasks очищен");

        if (windowId != 0) {
            HWND hwnd = (HWND)windowId;
            if (IsWindow(hwnd)) {
                HRESULT coInit = CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
                if (SUCCEEDED(coInit) || coInit == RPC_E_CHANGED_MODE) {
                    AddTaskToJumpList(hwnd);
                    if (coInit == S_OK) {
                        CoUninitialize();
                    }
                }
            }
        }

        Log(L"=== [JNI clearAll] END ===");
    }

    JNIEXPORT void JNICALL Java_rf_ebanina_UI_Editors_Window_JumpList_executeHandlerById(
        JNIEnv* env, jclass clazz, jlong id)
    {
        Log(L"=== [JNI executeHandlerById] START ===");

        for (const auto& task : g_tasks) {
            if (task.id == id && task.handler != nullptr) {
                Log(L"[JNI executeHandlerById] Вызов run() для ID=%lld");

                jclass runnableClass = env->GetObjectClass(task.handler);
                jmethodID runMethod = env->GetMethodID(runnableClass, "run", "()V");

                if (runMethod) {
                    env->CallVoidMethod(task.handler, runMethod);

                    if (env->ExceptionCheck()) {
                        Log(L"[JNI executeHandlerById] Исключение при вызове run()");
                        env->ExceptionDescribe();
                        env->ExceptionClear();
                    }
                }
                else {
                    Log(L"[JNI executeHandlerById] Метод run() не найден");
                }
                break;
            }
        }

        Log(L"=== [JNI executeHandlerById] END ===");
    }

    JNIEXPORT void JNICALL Java_rf_ebanina_UI_Editors_Window_JumpList_nativeRemove(
        JNIEnv* env, jclass clazz,
        jlong windowId, jlong id)
    {
        Log(L"=== [JNI nativeRemove] START ===");

        if (windowId == 0) {
            Log(L"[JNI nativeRemove] ERROR: windowId is 0");
            return;
        }

        HWND hwnd = (HWND)windowId;
        if (!IsWindow(hwnd)) {
            Log(L"[JNI nativeRemove] ERROR: HWND is not valid");
            return;
        }

        for (auto it = g_tasks.begin(); it != g_tasks.end(); ++it) {
            if (it->id == id) {
                g_tasks.erase(it);
                wchar_t msg[256];
                swprintf_s(msg, L"[JNI nativeRemove] Задача ID=%lld удалена. Осталось: %zu", id, g_tasks.size());
                Log(msg);

                HRESULT coInit = CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
                if (SUCCEEDED(coInit) || coInit == RPC_E_CHANGED_MODE) {
                    AddTaskToJumpList(hwnd);
                    if (coInit == S_OK) {
                        CoUninitialize();
                    }
                }
                Log(L"=== [JNI nativeRemove] END ===");
                return;
            }
        }

        Log(L"[JNI nativeRemove] Задача с таким ID не найдена");
        Log(L"=== [JNI nativeRemove] END ===");
    }

    JNIEXPORT jstring JNICALL Java_rf_ebanina_UI_Editors_Window_JumpList_nativeGet(
        JNIEnv* env, jclass clazz, jlong id)
    {
        for (const auto& task : g_tasks) {
            if (task.id == id) {
                std::wstring result = task.title + L"|" + task.arguments + L"|" + task.iconPath;
                return env->NewString((const jchar*)result.c_str(), (jsize)result.length());
            }
        }
        return nullptr;
    }

    JNIEXPORT jint JNICALL Java_rf_ebanina_UI_Editors_Window_JumpList_nativeSize(
        JNIEnv* env, jclass clazz)
    {
        return (jint)g_tasks.size();
    }

    JNIEXPORT jboolean JNICALL Java_jfx_jumpList_Main_isAlreadyRunning(
        JNIEnv* env, jclass clazz)
    {
        g_hMutex = CreateMutexW(nullptr, TRUE, L"EbaninaVST_SingleInstance_Mutex");
        if (g_hMutex == nullptr) {
            return JNI_FALSE;
        }

        if (GetLastError() == ERROR_ALREADY_EXISTS) {
            CloseHandle(g_hMutex);
            g_hMutex = nullptr;
            return JNI_TRUE;
        }

        return JNI_FALSE;
    }

    JNIEXPORT void JNICALL Java_jfx_jumpList_JumpListTest_sendArgsToExistingInstance(
        JNIEnv* env, jclass clazz, jlong id)
    {
        // Находим главное окно по заголовку
        HWND hwnd = FindWindowA(nullptr, "EbaninaVST");
        if (hwnd == nullptr) {
            return;  // Окно не найдено — просто выходим
        }

        // Готовим структуру
        COPYDATASTRUCT cds;
        cds.dwData = 1;
        cds.cbData = sizeof(jlong);
        cds.lpData = (PVOID)&id;

        // Отправляем — SendMessage блокирует до обработки
        SendMessage(hwnd, WM_COPYDATA, 0, (LPARAM)&cds);

        // Возвращаемся — процесс завершится сам после main()
    }

    JNIEXPORT jboolean JNICALL Java_rf_ebanina_UI_Editors_Window_JumpList_isAlreadyRunning(
        JNIEnv* env, jclass clazz)
    {
        static HANDLE hMutex = nullptr;
        hMutex = CreateMutexW(nullptr, TRUE, L"EbaninaVST_SingleInstance_Mutex");
        if (hMutex == nullptr) {
            return JNI_FALSE;
        }

        if (GetLastError() == ERROR_ALREADY_EXISTS) {
            CloseHandle(hMutex);
            hMutex = nullptr;
            return JNI_TRUE;
        }

        return JNI_FALSE;
    }

    JNIEXPORT void JNICALL Java_jfx_jumpList_Main_registerWindowForCopyData(
        JNIEnv* env, jclass clazz, jlong hwnd)
    {
        env->GetJavaVM(&g_jvm);
        g_originalWndProc = (WNDPROC)SetWindowLongPtr((HWND)hwnd, GWLP_WNDPROC, (LONG_PTR)SubclassWndProc);
    }
}
