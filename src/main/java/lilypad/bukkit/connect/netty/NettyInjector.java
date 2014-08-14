package lilypad.bukkit.connect.netty;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import lilypad.bukkit.connect.util.ReflectionUtils;
import net.minecraft.util.io.netty.channel.ChannelFuture;
import net.minecraft.util.io.netty.channel.ChannelHandler;
import net.minecraft.util.io.netty.channel.ChannelInitializer;
import net.minecraft.util.io.netty.channel.ChannelPipeline;
import net.minecraft.util.io.netty.channel.socket.SocketChannel;

import org.bukkit.Server;

public class NettyInjector {

	@SuppressWarnings("unchecked")
	public static void inject(Server server, NettyInjectHandler handler) throws Exception {
		Method serverGetHandle = server.getClass().getDeclaredMethod("getServer");
		Object minecraftServer = serverGetHandle.invoke(server);
		// Get Server Connection
		String serverConnectionField = null;
		for(Field field : minecraftServer.getClass().getSuperclass().getDeclaredFields()) {
			if(!field.getType().getSimpleName().equals("ServerConnection")) {
				continue;
			}
			serverConnectionField = field.getName();
			break;
		}
		Object serverConnection = ReflectionUtils.getPrivateField(minecraftServer.getClass().getSuperclass(), minecraftServer, Object.class, serverConnectionField);
		// Get ChannelFuture List // TODO find the field dynamically
		List<ChannelFuture> channelFutureList = ReflectionUtils.getPrivateField(serverConnection.getClass(), serverConnection, List.class, "e");
		// Iterate ChannelFutures
		for(ChannelFuture channelFuture : channelFutureList) {
			// Get ChannelPipeline
			ChannelPipeline channelPipeline = channelFuture.channel().pipeline();
			// Get ServerBootstrapAcceptor
			ChannelHandler serverBootstrapAcceptor = channelPipeline.first();
			// Get Old ChildHandler
			ChannelInitializer<SocketChannel> oldChildHandler = ReflectionUtils.getPrivateField(serverBootstrapAcceptor.getClass(), serverBootstrapAcceptor, ChannelInitializer.class, "childHandler");
			// Set New ChildHandler
			ReflectionUtils.setFinalField(serverBootstrapAcceptor.getClass(), serverBootstrapAcceptor, "childHandler", new NettyChannelInitializer(handler, oldChildHandler));
		}
	}
	
}
