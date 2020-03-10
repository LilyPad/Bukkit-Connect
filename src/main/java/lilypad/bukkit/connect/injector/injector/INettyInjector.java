package lilypad.bukkit.connect.injector.injector;

import org.bukkit.Server;


public interface INettyInjector {

    int injectAndFindPort(Server server, Object handler) throws Exception;
}
