#pragma once

#include <sstream>
#include <iostream>

#include <public.sdk/source/vst/hosting/plugprovider.h>
#include <public.sdk/source/vst/hosting/module.h>
#include <public.sdk/source/vst/hosting/hostclasses.h>
#include <public.sdk/source/vst/hosting/eventlist.h>
#include <public.sdk/source/vst/hosting/parameterchanges.h>
#include <public.sdk/source/vst/hosting/processdata.h>
#include <pluginterfaces/vst/ivsteditcontroller.h>
#include <pluginterfaces/vst/ivstprocesscontext.h>
#include <pluginterfaces/gui/iplugview.h>
#include "pluginterfaces/base/funknown.h"
#include <windows.h>
#include <thread>
#include <queue>
#include <mutex>
#include <condition_variable>
#include <functional>
#include <atomic>

// Обязательно включите
#include <pluginterfaces/vst/ivstmessage.h>  // Для IMessage
#include <pluginterfaces/vst/ivstaudioprocessor.h> // Для IAudioProcessor

class EasyVst
{
public:
    EasyVst();
    ~EasyVst();

    bool init(const std::string& path, int sampleRate, int maxBlockSize, int symbolicSampleSize, bool realtime);
    void destroy();
    static LRESULT WndProc(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam);
    Steinberg::Vst::ProcessContext* processContext();
    void setProcessing(bool processing);
    bool process(int numSamples);
    bool process(float** in, float** out, int framesRead);

    const Steinberg::Vst::BusInfo* busInfo(Steinberg::Vst::MediaType type, Steinberg::Vst::BusDirection direction, int which);
    int numBuses(Steinberg::Vst::MediaType type, Steinberg::Vst::BusDirection direction);
    void setBusActive(Steinberg::Vst::MediaType type, Steinberg::Vst::BusDirection direction, int which, bool active);

    Steinberg::tresult PLUGIN_API notify(Steinberg::Vst::IMessage* message);

    std::string getPluginName() const;
    std::string getCategory() const;
    std::string getVersion() const;
    std::string getSdkVersion() const;
    std::string getVendor() const;

    int32_t getParameterCount() const;
    double getParameterValue(Steinberg::int32 index) const;
    bool setParameterValue(Steinberg::int32 index, double value);

    bool createView();
    void destroyView();

    int getNumChannelsForInputBus(int busIndex) const;
    int getNumChannelsForOutputBus(int busIndex) const;

    bool ensureWindowClassRegistered();

    int numInputs() const;
    int numOutputs() const;

    Steinberg::Vst::Sample32* channelBuffer32(Steinberg::Vst::BusDirection direction, int which);
    Steinberg::Vst::Sample64* channelBuffer64(Steinberg::Vst::BusDirection direction, int which);

    Steinberg::Vst::EventList* eventList(Steinberg::Vst::BusDirection direction, int which);
    Steinberg::Vst::ParameterChanges* parameterChanges(Steinberg::Vst::BusDirection direction, int which);

    const std::string& name();

    void editorMainLoop();
    std::thread _editorThread;
    std::atomic<bool> _editorRunning = false;

    Steinberg::IPtr<Steinberg::Vst::IEditController> _editController = nullptr;
    Steinberg::IPtr<Steinberg::IPlugView> _view = nullptr;
    Steinberg::IPtr<Steinberg::Vst::IComponent> _vstPlug = nullptr;

    bool isDestroyed;

    void setLoggingEnabled(bool enabled);

private:
    bool _loggingEnabled;

    std::thread _workerThread;
    std::queue<std::function<void()>> _taskQueue;
    std::mutex _queueMutex;
    std::condition_variable _cv;
    std::atomic<bool> _running{ false };

    VST3::Hosting::ClassInfo _classInfo;

    HWND _window{ nullptr };

    bool _initialized{ false };

    void _destroy(bool decrementRefCount);

    void _printDebug(const std::string& info);
    void _printError(const std::string& error);

    std::vector<Steinberg::Vst::BusInfo> _inAudioBusInfos, _outAudioBusInfos;
    int _numInAudioBuses = 0, _numOutAudioBuses = 0;

    std::vector<Steinberg::Vst::BusInfo> _inEventBusInfos, _outEventBusInfos;
    int _numInEventBuses = 0, _numOutEventBuses = 0;

    std::vector<Steinberg::Vst::SpeakerArrangement> _inSpeakerArrs, _outSpeakerArrs;

    VST3::Hosting::Module::Ptr _module = nullptr;
    Steinberg::IPtr<Steinberg::Vst::PlugProvider> _plugProvider = nullptr;

    Steinberg::IPtr<Steinberg::Vst::IAudioProcessor> _audioEffect = nullptr;
    Steinberg::Vst::HostProcessData _processData = {};
    Steinberg::Vst::ProcessSetup _processSetup = {};
    Steinberg::Vst::ProcessContext _processContext = {};

    int _sampleRate = 0, _maxBlockSize = 0, _symbolicSampleSize = 0;
    bool _realtime = false;

    std::string _path;
    std::string _name;

    static Steinberg::Vst::HostApplication* _standardPluginContext;
    static int _standardPluginContextRefCount;

    // === Указатели на IConnectionPoint (не реализуем, а используем) ===
    Steinberg::Vst::IConnectionPoint* _controllerConnection = nullptr;
    Steinberg::Vst::IConnectionPoint* _processorConnection = nullptr;
};
