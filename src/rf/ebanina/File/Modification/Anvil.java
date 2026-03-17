package rf.ebanina.File.Modification;

import api.AudioMod;
import rf.ebanina.utils.loggining.logging;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@logging(tag = "Mod Loader")
public class Anvil {
    public static Anvil anvil = new Anvil();

    public void loadAllModsFromFolder(String modsFolderPath) {
        File modsDir = new File(modsFolderPath);

        if (!modsDir.exists() || !modsDir.isDirectory()) {
            return;
        }

        File[] files = modsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (files == null) {
            return;
        }

        for (File jarFile : files) {
            try {
                URL[] urls = {
                        jarFile.toURI().toURL()
                };

                try (URLClassLoader loader = new URLClassLoader(urls, Anvil.class.getClassLoader());
                     JarFile jar = new JarFile(jarFile)) {

                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();

                        if (name.endsWith(".class")) {
                            String className = name.replace("/", ".").replace(".class", "");
                            Class<?> clazz = Class.forName(className, true, loader);

                            if (AudioMod.class.isAssignableFrom(clazz) && !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
                                AudioMod modInstance = (AudioMod) clazz.getDeclaredConstructor().newInstance();

                                modInstance.applyMod();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
