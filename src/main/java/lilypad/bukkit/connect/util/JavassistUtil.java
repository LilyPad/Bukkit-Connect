package lilypad.bukkit.connect.util;

import javassist.ClassPool;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

public class JavassistUtil {
    public static ClassPool getClassPool() {
        ClassPool classPool = ClassPool.getDefault();

        URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
        for (URL url : classLoader.getURLs()) {
            try {
                // assume files
                String path = Paths.get(url.toURI()).toString();
                classPool.appendClassPath(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println();
        return classPool;
    }
}
