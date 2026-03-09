
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

#include <windows.h>
#include <thread>
#include <queue>
#include <mutex>
#include <condition_variable>
#include <functional>
#include <atomic>

class HostContextManager {
private:
    static Steinberg::Vst::HostApplication* _sharedContext;

public:
    static Steinberg::Vst::HostApplication* getSharedContext() {
        if (!_sharedContext) {
            _sharedContext = new Steinberg::Vst::HostApplication();
            PluginContextFactory::instance().setPluginContext(_sharedContext);
        }
        return _sharedContext;
    }

    static void releaseSharedContext() {
        if (_sharedContext) {
            PluginContextFactory::instance().setPluginContext(nullptr);
            _sharedContext->release();
            delete _sharedContext;
            _sharedContext = nullptr;
        }
    }
};

Steinberg::Vst::HostApplication* HostContextManager::_sharedContext = nullptr;