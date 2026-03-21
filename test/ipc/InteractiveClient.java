//package ipc;
//
//import rf.ebanina.ebanina.Player.AudioPlugins.VST.VST3LoadException;
//import rf.ebanina.ipc.mod.IPC.IPCHost;
//import rf.ebanina.ipc.mod.IPC.IPCHostFactory;
//import rf.ebanina.ipc.mod.VST.IPCPluginWrapper;
//
//public class InteractiveClient {
//    public static void main(String[] args) throws Exception {
//        IPCHost ipcHost = IPCHostFactory.buildHost(512, 2);
//
//        if (ipcHost == null) {
//            throw new VST3LoadException();
//        }
//
//        ipcHost.start();
//
//        IPCPluginWrapper pluginWrapper = new IPCPluginWrapper(
//                ipcHost,
//                "vst",
//                "A:\\Music\\VST2\\FabFilter Pro-C 2.dll",
//                44100,
//                512,
//                true,
//                false
//        );
//
//        pluginWrapper.openEditor();
//    }
//}
