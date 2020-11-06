package lilypad.bukkit.connect.injector.injector;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.net.InetSocketAddress;
import java.lang.reflect.Method;
import java.util.List;

import lilypad.bukkit.connect.ConnectPlugin;
import lilypad.bukkit.connect.injector.initializer.NettyChannelInitializer;
import lilypad.bukkit.connect.injector.handler.NettyInjectHandler;
import lilypad.bukkit.connect.util.ReflectionUtils;

import org.bukkit.Server;

public class NettyInjector implements INettyInjector {

	@SuppressWarnings("unchecked")
	public int injectAndFindPort(Server server, Object handler) throws Exception {
		Method serverGetHandle = server.getClass().getDeclaredMethod("getServer");
		Object minecraftServer = serverGetHandle.invoke(server);
		// Get Server Connection
		Method serverConnectionMethod = null;
		for(Method method : minecraftServer.getClass().getSuperclass().getDeclaredMethods()) {
			if(!method.getReturnType().getSimpleName().equals("ServerConnection")) {
				continue;
			}
			serverConnectionMethod = method;
			break;
		}
		Object serverConnection = serverConnectionMethod.invoke(minecraftServer);
		// Get ChannelFuture List // TODO find the field dynamically
		List<ChannelFuture> channelFutureList = ReflectionUtils.getPrivateField(serverConnection.getClass(), serverConnection, List.class, ConnectPlugin.getProtocol().getNettyInjectorChannelFutureList());
		// Iterate ChannelFutures
		int commonPort = 0;
		for(ChannelFuture channelFuture : channelFutureList) {
			// Get ChannelPipeline
			ChannelPipeline channelPipeline = channelFuture.channel().pipeline();
			// Get ServerBootstrapAcceptor
			ChannelHandler serverBootstrapAcceptor = channelPipeline.last();
			// Get Old ChildHandler
			ChannelInitializer<SocketChannel> oldChildHandler = ReflectionUtils.getPrivateField(serverBootstrapAcceptor.getClass(), serverBootstrapAcceptor, ChannelInitializer.class, "childHandler");
			// Set New ChildHandler
			ReflectionUtils.setFinalField(serverBootstrapAcceptor.getClass(), serverBootstrapAcceptor, "childHandler", new NettyChannelInitializer((NettyInjectHandler) handler, oldChildHandler));
			// Update Common Port
			commonPort = ((InetSocketAddress) channelFuture.channel().localAddress()).getPort();
		}
		return commonPort;
	}
	
}
