#include "EasyVst.h"

Steinberg::Vst::HostApplication* EasyVst::_standardPluginContext = nullptr;
int EasyVst::_standardPluginContextRefCount = 0;

using namespace Steinberg;
using namespace Steinberg::Vst;

EasyVst::EasyVst()
{
}

EasyVst::~EasyVst()
{
	destroy();
}

bool EasyVst::init(const std::string& path, int sampleRate, int maxBlockSize, int symbolicSampleSize, bool realtime)
{
	_printDebug("========== Starting plugin initialization ==========");
	_printDebug("Path: " + path);
	_printDebug("Sample rate: " + std::to_string(sampleRate));
	_printDebug("Max block size: " + std::to_string(maxBlockSize));
	_printDebug("Symbolic sample size: " + std::to_string(symbolicSampleSize));
	_printDebug("Realtime: " + std::string(realtime ? "true" : "false"));

	_destroy(false);
	_printDebug("Previous plugin destroyed (if existed).");

	++_standardPluginContextRefCount;
	_printDebug("Standard plugin context ref count incremented: " + std::to_string(_standardPluginContextRefCount));

	if (!_standardPluginContext)
	{
		_printDebug("Creating standard plugin context...");

		_standardPluginContext = owned(new HostApplication());
		PluginContextFactory::instance().setPluginContext(_standardPluginContext);

		_printDebug("Standard plugin context created and set to PluginContextFactory.");
	}
	else
	{
		_printDebug("Standard plugin context already exists.");
	}

	_path = path;
	_sampleRate = sampleRate;
	_maxBlockSize = maxBlockSize;
	_symbolicSampleSize = symbolicSampleSize;
	_realtime = realtime;

	_processSetup.processMode = _realtime ? ProcessModes::kRealtime : ProcessModes::kOffline;
	_processSetup.symbolicSampleSize = _symbolicSampleSize;
	_processSetup.sampleRate = _sampleRate;
	_processSetup.maxSamplesPerBlock = _maxBlockSize;

	_processData.numSamples = 0;
	_processData.symbolicSampleSize = _symbolicSampleSize;
	_processData.processContext = &_processContext;

	_printDebug("Process setup configured:");
	_printDebug(" - Process mode: " + std::to_string(_processSetup.processMode));
	_printDebug(" - Symbolic sample size: " + std::to_string(_processSetup.symbolicSampleSize));
	_printDebug(" - Sample rate: " + std::to_string(_processSetup.sampleRate));
	_printDebug(" - Max samples per block: " + std::to_string(_processSetup.maxSamplesPerBlock));

	std::string error;
	_module = VST3::Hosting::Module::create(path, error);

	if (!_module)
	{
		_printError("Failed to create module for path: " + path + ". Error: " + error);
		return false;
	}

	_printDebug("Module created successfully for path: " + path);

	VST3::Hosting::PluginFactory factory = _module->getFactory();

	_printDebug("Got plugin factory from module.");

	bool audioEffectClassFound = false;

	for (auto& classInfo : factory.classInfos()) {
		_printDebug("Checking class: " + classInfo.name() + ", category: " + classInfo.category() + ", vendor: " + classInfo.vendor() + ", version: " + classInfo.version());

		EasyVst::_classInfo = classInfo;

		if (classInfo.category() == kVstAudioEffectClass)
		{
			audioEffectClassFound = true;
			_printDebug("Found audio effect class: " + classInfo.name());

			try
			{
				_printDebug("Creating PlugProvider...");
				_plugProvider = owned(new PlugProvider(factory, classInfo, true));

				if (!_plugProvider)
				{
					_printError("No PlugProvider found for class: " + classInfo.name());
					return false;
				}

				_printDebug("PlugProvider created: " + std::to_string(reinterpret_cast<uintptr_t>(_plugProvider.get())));
				_printDebug("Getting plugin component from PlugProvider...");

				_vstPlug = _plugProvider->getComponent();

				if (!_vstPlug)
				{
					_printError("Failed to get plugin component (nullptr).");

					return false;
				}

				_printDebug("Plugin component acquired: " + std::to_string(reinterpret_cast<uintptr_t>((IComponent*)_vstPlug)));
			}
			catch (const std::exception& ex)
			{
				_printError(std::string("Exception caught while creating PlugProvider or getting component: ") + ex.what());

				return false;
			}
			catch (...)
			{
				_printError("Unknown exception caught while creating PlugProvider or getting component");

				return false;
			}

			_audioEffect = nullptr;
			tresult res = _vstPlug->queryInterface(Steinberg::Vst::IAudioProcessor::iid, (void**)&_audioEffect);

			if (res != Steinberg::kResultOk || !_audioEffect)
			{
				_printError("Could not get IAudioProcessor interface");
				return false;
			}

			_audioEffect->addRef();

			_printDebug("IAudioProcessor interface acquired and addRef'd");

			_editController = nullptr;
			res = _plugProvider->getController()->queryInterface(Steinberg::Vst::IEditController::iid, (void**)&_editController);

			if (res != Steinberg::kResultOk || !_editController)
			{
				_printError("Could not get IEditController interface");
				return false;
			}

			_editController->addRef();

			_printDebug("IEditController interface acquired and addRef'd");

			_editController = _plugProvider->getController();
			_name = classInfo.name();

			Steinberg::Vst::IConnectionPoint* processorCP = nullptr;
			Steinberg::Vst::IConnectionPoint* controllerCP = nullptr;

			res = _audioEffect->queryInterface(Steinberg::Vst::IConnectionPoint::iid, (void**)&processorCP);
			if (res == Steinberg::kResultOk && processorCP)
			{
				processorCP->addRef();
			}

			res = _editController->queryInterface(Steinberg::Vst::IConnectionPoint::iid, (void**)&controllerCP);
			if (res == Steinberg::kResultOk && controllerCP)
			{
				controllerCP->addRef();
			}

			if (processorCP && controllerCP)
			{
				processorCP->connect(controllerCP);
				controllerCP->connect(processorCP);

				_processorConnection = processorCP;
				_controllerConnection = controllerCP;

				_printDebug("Processor and controller connected via IConnectionPoint.");
			}
			else
			{
				_printDebug("IConnectionPoint interface not supported by processor or controller.");
			}

			if (processorCP)
				processorCP->release();
			if (controllerCP)
				controllerCP->release();

			_numInAudioBuses = _vstPlug->getBusCount(MediaTypes::kAudio, BusDirections::kInput);
			_numOutAudioBuses = _vstPlug->getBusCount(MediaTypes::kAudio, BusDirections::kOutput);

			_printDebug("Audio buses counts - inputs: " + std::to_string(_numInAudioBuses) + ", outputs: " + std::to_string(_numOutAudioBuses));

			_inAudioBusInfos.clear();
			_outAudioBusInfos.clear();
			_inSpeakerArrs.clear();
			_outSpeakerArrs.clear();

			for (int i = 0; i < _numInAudioBuses; ++i)
			{
				BusInfo info;
				auto result = _vstPlug->getBusInfo(kAudio, kInput, i, info);

				if (result == kResultOk)
				{
					_inAudioBusInfos.push_back(info);
					_vstPlug->activateBus(kAudio, kInput, i, true);
					SpeakerArrangement arr;

					if (_audioEffect->getBusArrangement(kInput, i, arr) == kResultOk)
					{
						_inSpeakerArrs.push_back(arr);
					}
				}
			}

			for (int i = 0; i < _numOutAudioBuses; ++i)
			{
				BusInfo info;
				auto result = _vstPlug->getBusInfo(kAudio, kOutput, i, info);

				if (result == kResultOk)
				{
					_outAudioBusInfos.push_back(info);
					_vstPlug->activateBus(kAudio, kOutput, i, true);
					SpeakerArrangement arr;

					if (_audioEffect->getBusArrangement(kOutput, i, arr) == kResultOk)
					{
						_outSpeakerArrs.push_back(arr);
					}
				}
			}

			_audioEffect->setBusArrangements(
				_inSpeakerArrs.data(), static_cast<uint32>(_numInAudioBuses),
				_outSpeakerArrs.data(), static_cast<uint32>(_numOutAudioBuses)
			);

			_vstPlug->setActive(true);

			tresult res1 = _audioEffect->setupProcessing(_processSetup);
			if (res1 != kResultOk)
			{
				_printError("Failed to setup processing, result: " + std::to_string(res1));
				return false;
			}

			_processData.unprepare();
			_processData = {};
			_processData.prepare(*_vstPlug, _maxBlockSize, _processSetup.symbolicSampleSize);

			if (!_processData.inputs)
			{
				_processData.inputs = new AudioBusBuffers[_numInAudioBuses];
				for (int i = 0; i < _numInAudioBuses; ++i)
				{
					int numChannels = _inAudioBusInfos[i].channelCount;
					_processData.inputs[i].numChannels = numChannels;
					_processData.inputs[i].channelBuffers32 = new Sample32 * [numChannels];

					for (int ch = 0; ch < numChannels; ++ch)
					{
						_processData.inputs[i].channelBuffers32[ch] = new float[_maxBlockSize];
					}
				}
			}
			if (!_processData.outputs)
			{
				_processData.outputs = new AudioBusBuffers[_numOutAudioBuses];

				for (int i = 0; i < _numOutAudioBuses; ++i)
				{
					int numChannels = _outAudioBusInfos[i].channelCount;
					_processData.outputs[i].numChannels = numChannels;
					_processData.outputs[i].channelBuffers32 = new Sample32 * [numChannels];

					for (int ch = 0; ch < numChannels; ++ch)
					{
						_processData.outputs[i].channelBuffers32[ch] = new float[_maxBlockSize];
					}
				}
			}

			_audioEffect->setProcessing(true);
			_printDebug("setProcessing(true) called.");

			isDestroyed = false;
			_printDebug("Plugin '" + _name + "' initialized successfully.");

			return true;
		}
	}

	if (!audioEffectClassFound)
	{
		_printError("No audio effect plugin found in factory: " + error);
	}

	return false;
}

std::string EasyVst::getVendor() const
{
	return _classInfo.vendor();
}

std::string EasyVst::getPluginName() const
{
	return _classInfo.name();
}

std::string EasyVst::getCategory() const
{
	return _classInfo.category();
}

std::string EasyVst::getVersion() const
{
	return _classInfo.version();
}

std::string EasyVst::getSdkVersion() const
{
	return _classInfo.sdkVersion();
}


void EasyVst::destroy()
{
	_destroy(true);
}

bool EasyVst::process(float** in, float** out, int framesRead)
{
	if (framesRead > _maxBlockSize)
	{
		_printError("framesRead > _maxBlockSize");

		framesRead = _maxBlockSize;
	}

	_processData.numSamples = framesRead;

	if (_processorConnection)
	{
		_printDebug("processorConnection is alive");
	}

	if (_controllerConnection)
	{
		_printDebug("controllerConnection is alive");
	}

	_processContext.sampleRate = _sampleRate;
	_processContext.projectTimeSamples += framesRead;

	static int64_t prev = 0;
	_printDebug("projectTimeSamples: " + std::to_string(_processContext.projectTimeSamples) + " (delta: " + std::to_string(_processContext.projectTimeSamples - prev) + ")");
	prev = _processContext.projectTimeSamples;

	_processContext.state = 0;
	_processContext.state |= Steinberg::Vst::ProcessContext::kPlaying;
	_processContext.state |= Steinberg::Vst::ProcessContext::kTempoValid;
	_processContext.state |= Steinberg::Vst::ProcessContext::kSystemTimeValid;
	_processContext.state |= (1 << 17);  // kContTimeValid
	_processContext.continousTimeSamples = _processContext.projectTimeSamples;

	_printDebug("ProcessContext.state set to: " + std::to_string(_processContext.state));
	_printDebug("kPlaying: " + std::to_string((_processContext.state & Steinberg::Vst::ProcessContext::kPlaying) != 0));
	_printDebug("kTempoValid: " + std::to_string((_processContext.state & Steinberg::Vst::ProcessContext::kTempoValid) != 0));
	_printDebug("kSystemTimeValid: " + std::to_string((_processContext.state & Steinberg::Vst::ProcessContext::kSystemTimeValid) != 0));

	_processData.numSamples = framesRead;
	_processData.processContext = &_processContext;

	if (!_processData.inputs || !_processData.outputs)
	{
		_printError("ProcessData inputs or outputs buffers not set");

		return false;
	}

	size_t inChannelOffset = 0;
	for (int busIdx = 0; busIdx < _numInAudioBuses; ++busIdx)
	{
		int numChannels = _processData.inputs[busIdx].numChannels;

		for (int ch = 0; ch < numChannels; ++ch)
		{
			size_t globalChannelIndex = inChannelOffset + ch;

			if (!in || !in[globalChannelIndex])
			{
				_printError("Input buffer pointer null or out-of-range for channel " + std::to_string(globalChannelIndex));

				return false;
			}

			_processData.inputs[busIdx].channelBuffers32[ch] = in[globalChannelIndex];
		}

		inChannelOffset += numChannels;
	}

	size_t outChannelOffset = 0;
	for (int busIdx = 0; busIdx < _numOutAudioBuses; ++busIdx)
	{
		int numChannels = _processData.outputs[busIdx].numChannels;

		for (int ch = 0; ch < numChannels; ++ch)
		{
			size_t globalChannelIndex = outChannelOffset + ch;

			if (!out || !out[globalChannelIndex])
			{
				_printError("Output buffer pointer null or out-of-range for channel " + std::to_string(globalChannelIndex));
				return false;
			}

			_processData.outputs[busIdx].channelBuffers32[ch] = out[globalChannelIndex];
		}

		outChannelOffset += numChannels;
	}

	_printDebug("Before process call");

	tresult result = _audioEffect->process(_processData);

	_printDebug("After process call, result: " + std::to_string(result));

	if (result != kResultOk)
	{
		_printError("VST process call failed with code: " + std::to_string(result));
		return false;
	}

	for (int busIdx = 0; busIdx < _numInAudioBuses; ++busIdx)
	{
		int numChannels = _processData.inputs[busIdx].numChannels;

		for (int ch = 0; ch < numChannels; ++ch)
		{
			float* buf = _processData.inputs[busIdx].channelBuffers32[ch];
			bool silent = true;

			for (int i = 0; i < framesRead; ++i)
			{
				if (fabsf(buf[i]) > 1e-6f) {
					silent = false;

					break;
				}
			}

			_printDebug("Input bus " + std::to_string(busIdx) + ", ch " + std::to_string(ch) + " is " + (silent ? "silent" : "active"));
		}
	}

	return true;
}

bool EasyVst::process(int numSamples)
{
	if (numSamples > _maxBlockSize)
	{
		#ifdef _DEBUG
		_printError("numSamples > _maxBlockSize");
		#endif
		numSamples = _maxBlockSize;
	}

	_processData.numSamples = numSamples;
	tresult result = _audioEffect->process(_processData);

	if (result != kResultOk)
	{
		#ifdef _DEBUG
		std::cerr << "VST process failed" << std::endl;
		#endif

		return false;
	}

	return true;
}

const Steinberg::Vst::BusInfo* EasyVst::busInfo(Steinberg::Vst::MediaType type, Steinberg::Vst::BusDirection direction, int which)
{
	if (type == kAudio)
	{
		if (direction == kInput)
		{
			return &_inAudioBusInfos[which];
		}
		else if (direction == kOutput)
		{
			return &_outAudioBusInfos[which];
		}
		else
		{
			return nullptr;
		}
	}
	else if (type == kEvent)
	{
		if (direction == kInput)
		{
			return &_inEventBusInfos[which];
		}
		else if (direction == kOutput)
		{
			return &_outEventBusInfos[which];
		}
		else
		{
			return nullptr;
		}
	}
	else
	{
		return nullptr;
	}
}

int EasyVst::numBuses(Steinberg::Vst::MediaType type, Steinberg::Vst::BusDirection direction)
{
	if (type == kAudio)
	{
		if (direction == kInput)
		{
			return _numInAudioBuses;
		}
		else if (direction == kOutput)
		{
			return _numOutAudioBuses;
		}
		else
		{
			return 0;
		}
	}
	else if (type == kEvent)
	{
		if (direction == kInput)
		{
			return _numInEventBuses;
		}
		else if (direction == kOutput)
		{
			return _numOutEventBuses;
		}
		else
		{
			return 0;
		}
	}
	else
	{
		return 0;
	}
}

void EasyVst::setBusActive(MediaType type, BusDirection direction, int which, bool active)
{
	_vstPlug->activateBus(type, direction, which, active);
}

void EasyVst::setProcessing(bool processing)
{
	_audioEffect->setProcessing(processing);
}

Steinberg::Vst::ProcessContext* EasyVst::processContext()
{
	return &_processContext;
}

Steinberg::Vst::Sample32* EasyVst::channelBuffer32(BusDirection direction, int which)
{
	if (direction == kInput)
	{
		return _processData.inputs->channelBuffers32[which];
	}
	else if (direction == kOutput)
	{
		return _processData.outputs->channelBuffers32[which];
	}
	else
	{
		return nullptr;
	}
}

Steinberg::Vst::Sample64* EasyVst::channelBuffer64(BusDirection direction, int which)
{
	if (direction == kInput)
	{
		return _processData.inputs->channelBuffers64[which];
	}
	else if (direction == kOutput)
	{
		return _processData.outputs->channelBuffers64[which];
	}
	else
	{
		return nullptr;
	}
}

Steinberg::Vst::EventList* EasyVst::eventList(Steinberg::Vst::BusDirection direction, int which)
{
	if (direction == kInput)
	{
		return static_cast<Steinberg::Vst::EventList*>(&_processData.inputEvents[which]);
	}
	else if (direction == kOutput)
	{
		return static_cast<Steinberg::Vst::EventList*>(&_processData.outputEvents[which]);
	}
	else
	{
		return nullptr;
	}
}

Steinberg::Vst::ParameterChanges* EasyVst::parameterChanges(Steinberg::Vst::BusDirection direction, int which)
{
	if (direction == kInput)
	{
		return static_cast<Steinberg::Vst::ParameterChanges*>(&_processData.inputParameterChanges[which]);
	}
	else if (direction == kOutput)
	{
		return static_cast<Steinberg::Vst::ParameterChanges*>(&_processData.outputParameterChanges[which]);
	}
	else
	{
		return nullptr;
	}
}

const std::string& EasyVst::name()
{
	return _name;
}

void EasyVst::_destroy(bool decrementRefCount)
{
	isDestroyed = true;

	if (_processorConnection)
	{
		_processorConnection->release();
		_processorConnection = nullptr;
	}
	if (_controllerConnection)
	{
		_controllerConnection->release();
		_controllerConnection = nullptr;
	}

	if (_audioEffect)
	{
		_audioEffect->release();
		_audioEffect = nullptr;
	}
	if (_editController) {
		_editController->release();
		_editController = nullptr;
	}
	if (_vstPlug) {
		_vstPlug->setActive(false);
		_vstPlug->release();
		_vstPlug = nullptr;
	}

	_audioEffect = nullptr;
	_plugProvider = nullptr;
	_module = nullptr;

	_inAudioBusInfos.clear();
	_outAudioBusInfos.clear();
	_numInAudioBuses = 0;
	_numOutAudioBuses = 0;

	_inEventBusInfos.clear();
	_outEventBusInfos.clear();
	_numInEventBuses = 0;
	_numOutEventBuses = 0;

	_inSpeakerArrs.clear();
	_outSpeakerArrs.clear();

	if (_processData.inputEvents)
	{
		delete[] static_cast<Steinberg::Vst::EventList*>(_processData.inputEvents);
	}
	if (_processData.outputEvents)
	{
		delete[] static_cast<Steinberg::Vst::EventList*>(_processData.outputEvents);
	}
	_processData.unprepare();
	_processData = {};

	_processSetup = {};
	_processContext = {};

	_sampleRate = 0;
	_maxBlockSize = 0;
	_symbolicSampleSize = 0;
	_realtime = false;

	_path = "";
	_name = "";

	if (decrementRefCount)
	{
		if (_standardPluginContextRefCount > 0)
		{
			--_standardPluginContextRefCount;
		}
		if (_standardPluginContext && _standardPluginContextRefCount == 0)
		{
			PluginContextFactory::instance().setPluginContext(nullptr);
			_standardPluginContext->release();
			delete _standardPluginContext;
			_standardPluginContext = nullptr;
		}
	}
}

void EasyVst::destroyView()
{
	if (_editorRunning)
	{
		_editorRunning = false;
		if (_editorThread.joinable())
			_editorThread.join();
	}

	if (_view)
	{
		_view->removed();
		_view->release();
		_view = nullptr;
	}

	if (_window)
	{
		DestroyWindow(_window);
		_window = nullptr;
	}
}


LRESULT CALLBACK EasyVst::WndProc(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
	switch (uMsg)
	{
	case WM_CLOSE:
		PostQuitMessage(0);
		return 0;
	case WM_DESTROY:
		return DefWindowProc(hwnd, uMsg, wParam, lParam);
	default:
		return DefWindowProc(hwnd, uMsg, wParam, lParam);
	}
}

void EasyVst::editorMainLoop()
{
	_editorRunning = true;

	MSG msg;

	while (_editorRunning)
	{
		while (PeekMessage(&msg, nullptr, 0, 0, PM_REMOVE))
		{
			if (msg.message == WM_QUIT || msg.message == WM_DESTROY)
			{
				destroyView();
				_editorRunning = false;
				break;
			}

			TranslateMessage(&msg);
			DispatchMessage(&msg);
		}

		Sleep(16);
	}
}

Steinberg::tresult PLUGIN_API EasyVst::notify(Steinberg::Vst::IMessage* message)
{
	_printDebug("Message declined");

	if (!message)
		return Steinberg::kInvalidArgument;

	const char* msgID = message->getMessageID();

	_printDebug("Message received: " + std::string(msgID ? msgID : "nullptr"));
	_printDebug("notify called with message ID: " + std::string(msgID ? msgID : "nullptr"));

	if (_controllerConnection)
	{
		_printDebug("Forwarding message to controller: " + std::string(msgID ? msgID : "nullptr"));
		_controllerConnection->notify(message);
	}

	return Steinberg::kResultOk;
}

int EasyVst::getNumChannelsForInputBus(int busIndex) const
{
	if (busIndex < 0 || busIndex >= _numInAudioBuses)
		return 0;
	return _inAudioBusInfos[busIndex].channelCount;
}

int EasyVst::getNumChannelsForOutputBus(int busIndex) const
{
	if (busIndex < 0 || busIndex >= _numOutAudioBuses)
		return 0;
	return _outAudioBusInfos[busIndex].channelCount;
}

bool EasyVst::ensureWindowClassRegistered()
{
	WNDCLASS wc = {};
	wc.lpfnWndProc = EasyVst::WndProc;
	wc.hInstance = GetModuleHandle(nullptr);
	wc.lpszClassName = L"Vst3EditorWindowClass";

	if (GetClassInfo(wc.hInstance, wc.lpszClassName, &wc))
	{
		return true;
	}

	if (!RegisterClass(&wc))
	{
		DWORD err = GetLastError();
		if (err == ERROR_CLASS_ALREADY_EXISTS)
		{
			_printError("Window class already exists (conflict).");
		}
		else
		{
			_printError("Failed to register window class, error code: " + std::to_string(err));
		}

		return false;
	}

	return true;
}

bool EasyVst::createView()
{
	_printDebug("Starting createView()...");

	if (!_editController)
	{
		_printError("EditController is missing. Cannot proceed with createView().");

		return false;
	}

	_printDebug("EditController pointer valid.");

	if (_view)
	{
		_printDebug("Editor view already exists. Skipping createView().");
		destroyView();
		return false;
	}
	if (_window)
	{
		_printDebug("Editor window already exists. Skipping createView().");
		return false;
	}

	_printDebug("No existing view or window detected.");
	_printDebug("Requesting editor view from edit controller...");

	_view = _editController->createView(ViewType::kEditor);

	if (!_view)
	{
		_printError("createView() returned nullptr - edit controller does not provide a view.");
		return false;
	}

	ViewRect viewRect = {};
	tresult res = _view->getSize(&viewRect);

	_printDebug("Called _view->getSize(&viewRect), result code: " + std::to_string(res));

	if (res != kResultOk)
	{
		DestroyWindow(_window);
		_printError("Failed to get editor view size. Result: " + std::to_string(res));
		_window = nullptr;
		_view->release();
		_view = nullptr;
		return false;
	}

	_printDebug("Editor view size: left=" + std::to_string(viewRect.left) +
		", top=" + std::to_string(viewRect.top) +
		", right=" + std::to_string(viewRect.right) +
		", bottom=" + std::to_string(viewRect.bottom) +
		" width=" + std::to_string(viewRect.getWidth()) +
		" height=" + std::to_string(viewRect.getHeight()));

	#ifdef _WIN32
	tresult platformSupported = _view->isPlatformTypeSupported(Steinberg::kPlatformTypeHWND);

	_printDebug("Checking HWND platform support, result: " + std::to_string(platformSupported));

	if (platformSupported != kResultTrue)
	{
		_printError("Editor view does not support HWND platform.");
		_view->release();
		_view = nullptr;

		return false;
	}

	_printDebug("Editor view supports HWND platform.");

	if (!ensureWindowClassRegistered())
	{
		_printError("Failed to register window class needed for editor view.");
		_view->release();
		_view = nullptr;

		return false;
	}

	_printDebug("Window class registered successfully.");

	_printDebug("Creating native window with size: width=" + std::to_string(viewRect.getWidth()) +
		", height=" + std::to_string(viewRect.getHeight()));

	_window = CreateWindowEx(
		0,
		L"Vst3EditorWindowClass",
		std::wstring(_name.begin(), _name.end()).c_str(),
		WS_OVERLAPPEDWINDOW,
		CW_USEDEFAULT, CW_USEDEFAULT,
		static_cast<int>(viewRect.getWidth()),
		static_cast<int>(viewRect.getHeight()),
		nullptr,
		nullptr,
		GetModuleHandle(nullptr),
		this
	);

	if (!_window)
	{
		DWORD err = GetLastError();
		_printError("Failed to create window. GetLastError(): " + std::to_string(err));
		_view->release();
		_view = nullptr;

		return false;
	}

	_printDebug("Created native window successfully: HWND=" + std::to_string(reinterpret_cast<uintptr_t>(_window)));

	SetWindowLongPtr(_window, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(this));
	ShowWindow(_window, SW_SHOW);

	_printDebug("Window shown on screen.");

	res = _view->attached(_window, Steinberg::kPlatformTypeHWND);

	_printDebug("Called _view->attached() to HWND, result: " + std::to_string(res));

	if (res != kResultOk)
	{
		_printError("Failed to attach editor view to HWND. Result: " + std::to_string(res));
		DestroyWindow(_window);
		_window = nullptr;
		_view->release();
		_view = nullptr;
		return false;
	}

	_printDebug("Editor view attached successfully to HWND.");

	Steinberg::ViewRect sizeRect = {};
	sizeRect.left = 0;
	sizeRect.top = 0;
	sizeRect.right = viewRect.getWidth();
	sizeRect.bottom = viewRect.getHeight();

	_view->onSize(&sizeRect);

	_printDebug("Called _view->onSize() with rectangle: left=0, top=0, right=" +
		std::to_string(sizeRect.right) + ", bottom=" + std::to_string(sizeRect.bottom));

	_printDebug("Starting editorMainLoop().");

	editorMainLoop();

	_printDebug("Finished editorMainLoop().");

	return true;
#endif
}

int32_t EasyVst::getParameterCount() const
{
	if (!_editController)
		return 0;
	return static_cast<int32_t>(_editController->getParameterCount());
}

double EasyVst::getParameterValue(Steinberg::int32 index) const
{
	if (!_editController)
		return -1.0;

	Steinberg::Vst::ParameterInfo info;
	if (_editController->getParameterInfo(index, info) != Steinberg::kResultOk)
		return -1.0;

	return _editController->getParamNormalized(info.id);
}

bool EasyVst::setParameterValue(Steinberg::int32 index, double value)
{
	if (!_editController || value < 0.0 || value > 1.0)
		return false;

	Steinberg::Vst::ParameterInfo info;
	if (_editController->getParameterInfo(index, info) != Steinberg::kResultOk)
		return false;

	_editController->setParamNormalized(info.id, value);
	return true;
}

int EasyVst::numInputs() const
{
	return _numInAudioBuses;
}

int EasyVst::numOutputs() const
{
	return _numOutAudioBuses;
}

void EasyVst::setLoggingEnabled(bool enabled) {
	_loggingEnabled = enabled;
}

void EasyVst::_printDebug(const std::string& info) {
	if (_loggingEnabled)
	{
		std::cout << "Debug info for VST3 plugin \"" << _path << "\": " << info << std::endl;
	}
}

void EasyVst::_printError(const std::string& error) {
	if (_loggingEnabled)
	{
		std::cerr << "Error loading VST3 plugin \"" << _path << "\": " << error << std::endl;
	}
}