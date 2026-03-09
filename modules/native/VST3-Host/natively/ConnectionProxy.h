#pragma once
#include <pluginterfaces/vst/ivstmessage.h>
#include <pluginterfaces/base/funknown.h>

class ConnectionProxy : public Steinberg::Vst::IConnectionPoint
{
public:
    ConnectionProxy(Steinberg::FUnknown* owner) : _refCount(1), _owner(owner), _destination(nullptr), _connected(nullptr) {}

    // Правильный сигнатурный тип возвращаемого значения
    Steinberg::uint32 PLUGIN_API addRef() override
    {
        return ++_refCount;
    }

    Steinberg::uint32 PLUGIN_API release() override
    {
        Steinberg::uint32 count = --_refCount;
        if (count == 0)
            delete this;
        return count;
    }

    Steinberg::tresult PLUGIN_API queryInterface(const Steinberg::TUID iid, void** obj) override
    {
        if (!obj)
            return Steinberg::kInvalidArgument;

        if (memcmp(iid, Steinberg::Vst::IConnectionPoint::iid, sizeof(Steinberg::TUID)) == 0)
        {
            *obj = static_cast<Steinberg::Vst::IConnectionPoint*>(this);
            addRef();
            return Steinberg::kResultOk;
        }
        if (memcmp(iid, Steinberg::FUnknown::iid, sizeof(Steinberg::TUID)) == 0)
        {
            *obj = static_cast<Steinberg::FUnknown*>(this);
            addRef();
            return Steinberg::kResultOk;
        }
        *obj = nullptr;
        return Steinberg::kNoInterface;
    }

    Steinberg::tresult PLUGIN_API connect(Steinberg::Vst::IConnectionPoint* other) override
    {
        if (_connected)
            _connected->release();

        _connected = other;

        if (_connected)
            _connected->addRef();

        return Steinberg::kResultOk;
    }

    Steinberg::tresult PLUGIN_API disconnect()
    {
        if (_connected)
        {
            _connected->release();
            _connected = nullptr;
        }
        return Steinberg::kResultOk;
    }

    Steinberg::tresult PLUGIN_API notify(Steinberg::Vst::IMessage* message) override
    {
        if (!_destination)
            return Steinberg::kInvalidArgument;

        return _destination->notify(message);
    }

    void setDestination(ConnectionProxy* dest)
    {
        if (_destination)
            _destination->release();

        _destination = dest;
        if (_destination)
            _destination->addRef();
    }

protected:
    Steinberg::uint32 _refCount;
    Steinberg::FUnknown* _owner;
    Steinberg::Vst::IConnectionPoint* _connected;
    ConnectionProxy* _destination;

    ~ConnectionProxy()
    {
        if (_connected)
            _connected->release();
        if (_destination)
            _destination->release();
    }
};
