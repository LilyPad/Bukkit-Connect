package lilypad.bukkit.connect.injector;

import lilypad.bukkit.connect.util.JavassistUtil;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import lilypad.bukkit.connect.ConnectPlugin;
import lilypad.bukkit.connect.util.ReflectionUtils;

import org.bukkit.Server;

@SuppressWarnings("restriction")
public class OfflineInjector {

	private static Object offlineMinecraftServer;

	public static void inject(Server server) throws Exception {
		Method serverGetHandle = server.getClass().getDeclaredMethod("getServer");
		Object minecraftServer = serverGetHandle.invoke(server);
		// create offline minecraftServer
		ClassPool classPool = JavassistUtil.getClassPool();
		CtClass minecraftServerClass = classPool.getCtClass(minecraftServer.getClass().getName());
		CtClass offlineMinecraftServerClass = classPool.makeClass(minecraftServer.getClass().getName() + "$offline");
		offlineMinecraftServerClass.setSuperclass(minecraftServerClass);
		// ... create delegate field
		CtField delegateField = new CtField(minecraftServerClass, "delegate", offlineMinecraftServerClass);
		offlineMinecraftServerClass.addField(delegateField);
		// ... add our special getOfflineMode
		CtMethod getOnlineModeMethod = new CtMethod(minecraftServerClass.getSuperclass().getDeclaredMethod("getOnlineMode").getReturnType(), "getOnlineMode", new CtClass[] { }, offlineMinecraftServerClass);
		getOnlineModeMethod.setBody("{ return false; }");
		offlineMinecraftServerClass.addMethod(getOnlineModeMethod);
		// ... proxy all declared methods recursively
		CtClass cursorClass = minecraftServerClass;
		while (true) {
			for (CtMethod method : cursorClass.getDeclaredMethods()) {
				if(Modifier.isFinal(method.getModifiers())) {
					continue;
				}
				if(Modifier.isPrivate(method.getModifiers())) {
					continue;
				}
				try {
					offlineMinecraftServerClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
					continue;
				} catch(NotFoundException exception) {
					// proceed
				}
				CtMethod proxyMethod = new CtMethod(method.getReturnType(), method.getName(), method.getParameterTypes(), offlineMinecraftServerClass);
				proxyMethod.setBody("{ return ($r)this.delegate." + method.getName() + "($$); }");
				offlineMinecraftServerClass.addMethod(proxyMethod);
			}
			cursorClass = cursorClass.getSuperclass();
			if (cursorClass == null) {
				break;
			}
			if (cursorClass.getName().equals("java.lang.Object")) {
				break;
			}
		}
		// ... make a blank constructor
		// don't need to make a blank constructor for 1.9
		if (!ConnectPlugin.getProtocol().getGeneralVersion().equalsIgnoreCase("1.9") && !ConnectPlugin.getProtocol().getGeneralVersion().equalsIgnoreCase("1.10")) {
			CtConstructor constructor = new CtConstructor(new CtClass[] { }, offlineMinecraftServerClass);
			constructor.setBody("{ super(null); }");
			offlineMinecraftServerClass.addConstructor(constructor);
		}

		// ... create our class
		Class<?> offlineMinecraftServerJClass = offlineMinecraftServerClass.toClass();
		// ... create an instance of our class without calling the constructor
		ReflectionFactory reflectionFactory = ReflectionFactory.getReflectionFactory();
		Constructor<?> objectConstructor = Object.class.getDeclaredConstructor();
		Constructor<?> serializeConstructor = reflectionFactory.newConstructorForSerialization(offlineMinecraftServerJClass, objectConstructor);
		offlineMinecraftServer = serializeConstructor.newInstance();
		// ... set our delegate, among other stuff
		ReflectionUtils.setFinalField(offlineMinecraftServer.getClass(), offlineMinecraftServer, "delegate", minecraftServer);
		ReflectionUtils.setFinalField(offlineMinecraftServer.getClass().getSuperclass().getSuperclass(), offlineMinecraftServer, "server", server);
		ReflectionUtils.setFinalField(offlineMinecraftServer.getClass().getSuperclass().getSuperclass(), offlineMinecraftServer, "processQueue", ReflectionUtils.getPrivateField(minecraftServer.getClass().getSuperclass(), minecraftServer, Object.class, "processQueue"));
		// get server connection
		Method serverConnectionMethod = null;
		for (Method method : minecraftServer.getClass().getSuperclass().getDeclaredMethods()) {
			if (!method.getReturnType().getSimpleName().equals("ServerConnection")) {
				continue;
			}
			serverConnectionMethod = method;
			break;
		}
		Object serverConnection = serverConnectionMethod.invoke(minecraftServer);
		// set server connection minecraftServer
		ReflectionUtils.setFinalField(serverConnection.getClass(), serverConnection, ConnectPlugin.getProtocol().getOfflineInjectorServerConnection(), offlineMinecraftServer);
		// set protocolsupport minecraftserver
		if (server.getPluginManager().getPlugin("ProtocolSupport") != null) {
			Class<?> supportLoginListener = Class.forName("protocolsupport.protocol.packet.handler.AbstractLoginListener");
			ReflectionUtils.setFinalField(supportLoginListener, null, "server", offlineMinecraftServer);
		}
	}

	public static Object getOfflineMinecraftServer() {
		return offlineMinecraftServer;
	}
	
}
