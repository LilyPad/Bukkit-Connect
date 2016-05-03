package lilypad.bukkit.connect.injector;

import java.lang.reflect.Modifier;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import lilypad.bukkit.connect.ConnectPlugin;
import lilypad.bukkit.connect.util.ReflectionUtils;

import org.bukkit.Server;

public class PacketInjector {
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void injectStringMaxSize(Server server, String protocol, int packetId, final int maxSize) throws Exception {
		Object minecraftServer = ReflectionUtils.getPrivateField(server.getClass(), server, Object.class, "console");
		String minecraftServerClassName = minecraftServer.getClass().getName();
		final String minecraftPackage = minecraftServerClassName.substring(0, minecraftServerClassName.lastIndexOf('.'));
		// get the packet
		Class<?> enumProtocolClass = Class.forName(minecraftPackage + ".EnumProtocol");
		Object enumProtocol = ReflectionUtils.getPrivateField(enumProtocolClass, null, enumProtocolClass, protocol.toUpperCase());
		Map protocolDirections = ReflectionUtils.getPrivateField(enumProtocolClass, enumProtocol, Map.class, ConnectPlugin.getProtocol().getPacketInjectorProtocolDirections());
		Object serverBoundDirection = ReflectionUtils.getPrivateField(Class.forName(minecraftPackage + ".EnumProtocolDirection"), null, Object.class, "SERVERBOUND");
		Map serverBound = (Map) protocolDirections.get(serverBoundDirection);
		if(!serverBound.containsKey(packetId)) {
			throw new IllegalArgumentException("Packet Id does not exist: " + packetId);
		}
		Class<?> packetClass = (Class<?>) serverBound.get(packetId);
		// create packet proxy
		ClassPool classPool = ClassPool.getDefault();
		CtClass packetCtClass = classPool.getCtClass(packetClass.getName());
		final CtClass packetCtClassProxy = classPool.getAndRename(packetClass.getName(), packetClass.getName() + "$stringMaxSize" + maxSize);
		packetCtClassProxy.setSuperclass(packetCtClass);
		for(CtField field : packetCtClassProxy.getDeclaredFields()) {
			if(Modifier.isPrivate(field.getModifiers())) {
				continue;
			}
			packetCtClassProxy.removeField(field);
		}
		CtMethod decodeCtMethod = packetCtClassProxy.getDeclaredMethod(ConnectPlugin.getProtocol().getPacketInjectorDecodeCtMethod(), new CtClass[] { classPool.getCtClass(minecraftPackage + ".PacketDataSerializer") });
		decodeCtMethod.instrument(new ExprEditor() {
			public void edit(MethodCall methodCall) throws CannotCompileException {
				if(!methodCall.getClassName().equals(minecraftPackage + ".PacketDataSerializer") 
						|| !methodCall.getMethodName().equals("c") 
						|| !methodCall.getSignature().equals("(I)Ljava/lang/String;")) {
					return;
				}
				methodCall.replace("{ $1 = " + maxSize + "; $_ = $proceed($$); }");
			}
		});
		CtMethod handleCtMethod = packetCtClassProxy.getDeclaredMethod(ConnectPlugin.getProtocol().getPacketInjectorHandleCtMethod(), new CtClass[] { classPool.getCtClass(minecraftPackage + ".PacketListener") });
		handleCtMethod.instrument(new ExprEditor() {
			public void edit(MethodCall methodCall) throws CannotCompileException {
				try {
					methodCall.getMethod().setBody("{ super.a($$); }");
				} catch (NotFoundException exception) {
					exception.printStackTrace();
				}
			}
		});
		Class<?> packetClassProxy = packetCtClassProxy.toClass();
		// replace packet
		serverBound.put(packetId, packetClassProxy);
	}
	
}
