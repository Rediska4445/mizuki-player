package rf;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class vst3 {
    public vst3() {
        this(new File("editorhost.dll"));
    }

    public vst3(File path) {
        System.load(path.getAbsolutePath());

        nativeHandle = createNativeInstance();
    }

    public ExecutorService executor = Executors.newSingleThreadExecutor();

    private AtomicBoolean isStarted = new AtomicBoolean(false);

    public native long createNativeInstance();
    public native void disposeNativeInstance(long nativeHandle);

    public File plugin;
    public int sampleRate;
    public int maxBlockSize;
    public int symbolicSampleSize;
    public boolean isRealtime;
    public int numInputs;
    public int numOutputs;

    private long nativeHandle;

    private Runnable onInitialize = null;
    private Runnable onDestroy = null;
    private Runnable onCreateView = null;

    public vst3 setOnInitialize(Runnable onInitialize) {
        this.onInitialize = onInitialize;
        return this;
    }

    public native boolean initializePlugin(long nativeHandle, String pluginPath, int sampleRate, int maxBlockSize, int symbolicSampleSize, boolean realtime);

    public Future<Boolean> asyncInit(File plugin, int sampleRate, int maxBlockSize, int symbolicSampleSize, boolean isRealtime) {
        return executor.submit(() -> {
            boolean result = false;

            if (!isStarted.get()) {
                result = initPlugin(
                        (this.plugin = plugin).getAbsolutePath(),
                        this.sampleRate = sampleRate,
                        this.maxBlockSize = maxBlockSize,
                        this.symbolicSampleSize = symbolicSampleSize,
                        this.isRealtime = isRealtime
                );
            }

            numInputs = getNumInputs();
            numOutputs = getNumOutputs();

            isStarted.set(true);

            if (onInitialize != null) {
                onInitialize.run();
            }

            return result;
        });
    }

    public boolean initPlugin(String pluginPath, int sampleRate, int maxBlockSize, int symbolicSampleSize, boolean realtime) {
        return initializePlugin(nativeHandle, pluginPath, sampleRate, maxBlockSize, symbolicSampleSize, realtime);
    }

    public native boolean createView(long nativeHandle);

    public boolean createView() {
        return createView(nativeHandle);
    }

    public void asyncCreateView() {
        executor.submit(() -> {
            if(!isStarted.get()) {
                throw new NullPointerException("Host is not initialized");
            }

            createView();

            if(onCreateView != null) {
                onCreateView.run();
            }
        });
    }

    public native void destroy(long nativeHandle);

    public void destroy() {
        destroy(nativeHandle);
    }

    public void asyncReCreateView() {
        executor.submit(() -> {
            if(!isStarted.get()) {
                throw new NullPointerException("host need to be initialized");
            }

            destroy();

            initPlugin(plugin.getAbsolutePath(), sampleRate, maxBlockSize, symbolicSampleSize, isRealtime);

            createView();

            if(onCreateView != null) {
                onCreateView.run();
            }
        });
    }

    public native int getNumChannelsForInputBus(long nativeHandle, int busIndex);

    public int getNumChannelsForInputBus(int busIndex) {
        return getNumChannelsForInputBus(nativeHandle, busIndex);
    }

    public native int getNumChannelsForOutputBus(long nativeHandle, int busIndex);

    public int getNumChannelsForOutputBus(int busIndex) {
        return getNumChannelsForOutputBus(nativeHandle, busIndex);
    }

    public native int getNumInputs(long nativeHandle);

    public int getNumInputs() {
        return getNumInputs(nativeHandle);
    }

    public native int getNumOutputs(long nativeHandle);

    public int getNumOutputs() {
        return getNumInputs(nativeHandle);
    }

    public native boolean processReplacing(long nativeHandle, float[][] inArray, float[][] outArray, int framesRead);

    public boolean process(float[][] inArray, float[][] outArray, int framesRead) {
        if(isStarted.get()) {
            return processReplacing(nativeHandle, inArray, outArray, framesRead);
        }

        return false;
    }

    private native int getParameterCount(long nativeHandle);

    public int getParameterCount() {
        return getParameterCount(nativeHandle);
    }

    private native double getParameterValue(long nativeHandle, int index);

    public double getParameterValue(int index) {
        return getParameterValue(nativeHandle, index);
    }

    private native boolean setParameterValue(long nativeHandle, int index, double value);

    public boolean setParameterValue(int index, double value) {
        return setParameterValue(nativeHandle, index, value);
    }

    public native boolean isDestroyed();

    public native String getVendor(long nativeHandle);

    public String getVendor() {
        return getVendor(nativeHandle);
    }

    public native String getPluginName(long nativeHandle);

    public String getPluginName() {
        return getPluginName(nativeHandle);
    }

    public native String getCategory(long nativeHandle);

    public String getCategory() {
        return getCategory(nativeHandle);
    }

    public native String getVersion(long nativeHandle);

    public String getVersion() {
        return getVersion(nativeHandle);
    }

    public native String getSdkVersion(long nativeHandle);

    public String getSdkVersion() {
        return getSdkVersion(nativeHandle);
    }

    public native void setLoggingEnabled(long nativeHandle, boolean enabled);

    public void setLoggingEnable(boolean enable) {
        setLoggingEnabled(nativeHandle, enable);
    }

    @Override
    public String toString() {
        return "vst3{" +
                "executor=" + executor +
                ", isStarted=" + isStarted +
                ", plugin=" + plugin +
                ", sampleRate=" + sampleRate +
                ", maxBlockSize=" + maxBlockSize +
                ", symbolicSampleSize=" + symbolicSampleSize +
                ", isRealtime=" + isRealtime +
                ", numInputs=" + numInputs +
                ", numOutputs=" + numOutputs +
                ", nativeHandle=" + nativeHandle +
                ", onInitialize=" + onInitialize +
                ", onDestroy=" + onDestroy +
                ", onCreateView=" + onCreateView +
                '}';
    }
}
