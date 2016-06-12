package lilypad.bukkit.connect.login;

import com.mojang.authlib.GameProfile;
import javassist.*;
import javassist.bytecode.*;
import lilypad.bukkit.connect.util.JavassistUtil;

import java.lang.reflect.Field;

public class LoginListenerProxy {
    private static Class instance;
    private static Field packetListenerField;

    private static Class loginListenerClass;
    private static Field profileField;

    private static Field findFieldOfType(Class type, Class fieldType) {
        for (Field field : type.getDeclaredFields()) {
            if (field.getType() == fieldType) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    private static Class create(Object networkManager) throws Exception {
        Object originalLoginListener = null;
        packetListenerField = null;
        Field[] networkManagerFields = networkManager.getClass().getDeclaredFields();
        for (Field field : networkManagerFields) {
            if (field.getType().getSimpleName().equals("PacketListener")) {
                field.setAccessible(true);
                packetListenerField = field;
                originalLoginListener = field.get(networkManager);
                break;
            }
        }


        if (originalLoginListener == null || !originalLoginListener.getClass().getSimpleName().equals("LoginListener")) {
            throw new Exception("Could not find LoginListener in NetworkManager!");
        }

        loginListenerClass = originalLoginListener.getClass();
        profileField = findFieldOfType(loginListenerClass, GameProfile.class);

        Class<?> loginListenerClass = originalLoginListener.getClass();

        ClassPool classPool = JavassistUtil.getClassPool();
        CtClass loginListenerCtClass = classPool.getCtClass(loginListenerClass.getName());
        CtClass loginListenerProxyCtClass = classPool.makeClass(loginListenerClass.getName() + "$UuidInjector", loginListenerCtClass);

        loginListenerProxyCtClass.setSuperclass(loginListenerCtClass);

        CtConstructor loginListenerConstructor = loginListenerCtClass.getDeclaredConstructors()[0];

        CtConstructor loginListenerProxyCtConstructor = new CtConstructor(loginListenerConstructor.getParameterTypes(), loginListenerProxyCtClass);
        loginListenerProxyCtConstructor.setBody("{ super($$); }");
        loginListenerProxyCtClass.addConstructor(loginListenerProxyCtConstructor);

        loginListenerProxyCtClass.addField(CtField.make("public java.lang.Runnable injectUuidCallback;", loginListenerProxyCtClass));

        CtMethod initUuidMethod = null;
        for (CtMethod method : loginListenerCtClass.getMethods()) {
            MethodInfo methodInfo = method.getMethodInfo();
            ConstPool constPool = methodInfo.getConstPool();
            CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
            if (codeAttribute == null) {
                continue;
            }
            CodeIterator code = codeAttribute.iterator();
            while (code.hasNext()) {
                int index = code.next();
                if (code.byteAt(index) == Opcode.LDC) {
                    int cstIndex = code.byteAt(index + 1);
                    if (constPool.getTag(cstIndex) == 8) {
                        if (constPool.getStringInfo(cstIndex).equals("OfflinePlayer:")) {
                            initUuidMethod = method;
                        }
                    }
                }
            }
        }

        if (initUuidMethod == null) {
            throw new Exception("Could not find initUUID in LoginListener!");
        }

        CtMethod newMethod = new CtMethod(initUuidMethod.getReturnType(), initUuidMethod.getName(), initUuidMethod.getParameterTypes(), loginListenerProxyCtClass);
        newMethod.setBody("{ this.injectUuidCallback.run(); }");
        loginListenerProxyCtClass.addMethod(newMethod);

        return loginListenerProxyCtClass.toClass();
    }

    public static synchronized Class get(Object networkManager) throws Exception {
        if (instance == null) {
            instance = create(networkManager);
        }
        return instance;
    }

    public static Field getPacketListenerField() {
        return packetListenerField;
    }

    public static Field getProfileField() {
        return profileField;
    }
}
