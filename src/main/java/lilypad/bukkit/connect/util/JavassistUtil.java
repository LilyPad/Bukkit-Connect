package lilypad.bukkit.connect.util;

import javassist.ClassPool;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

public class JavassistUtil {

    public static ClassPool getClassPool() {
        ClassPool classPool = ClassPool.getDefault();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader instanceof URLClassLoader) {
            URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
            for (URL url : urlClassLoader.getURLs()) {
                try {
                    // assume files
                    String path = Paths.get(url.toURI()).toString();
                    classPool.appendClassPath(path);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
        return classPool;
    }

}
