package lilypad.bukkit.connect.injector.injector;

import lilypad.bukkit.connect.ConnectPlugin;
import lilypad.bukkit.connect.injector.decoder.NettyDecoderHandler;
import lilypad.bukkit.connect.injector.decoder.NettyDecoderHandlerLegacy;
import lilypad.bukkit.connect.injector.handler.NettyInjectHandlerLegacy;
import lilypad.bukkit.connect.injector.initializer.NettyChannelInitializer;
import lilypad.bukkit.connect.injector.handler.NettyInjectHandler;
import lilypad.bukkit.connect.injector.initializer.NettyChannelInitializerLegacy;
import lilypad.bukkit.connect.login.inject.LoginNettyInjectHandlerLegacy;
import lilypad.bukkit.connect.util.ReflectionUtils;
import net.minecraft.util.io.netty.channel.ChannelFuture;
import net.minecraft.util.io.netty.channel.ChannelHandler;
import net.minecraft.util.io.netty.channel.ChannelInitializer;
import net.minecraft.util.io.netty.channel.ChannelPipeline;
import net.minecraft.util.io.netty.channel.socket.SocketChannel;
import org.bukkit.Server;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.List;

public class NettyInjectorLegacy implements INettyInjector {

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
			ReflectionUtils.setFinalField(serverBootstrapAcceptor.getClass(), serverBootstrapAcceptor, "childHandler", new NettyChannelInitializerLegacy((LoginNettyInjectHandlerLegacy) handler, oldChildHandler));
			// Update Common Port
			commonPort = ((InetSocketAddress) channelFuture.channel().localAddress()).getPort();
		}
		return commonPort;
	}
	
}
