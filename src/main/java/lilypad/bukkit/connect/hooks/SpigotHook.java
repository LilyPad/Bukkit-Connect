package lilypad.bukkit.connect.hooks;

public final class SpigotHook
{

      private boolean isSpigot;
      private String whitelistMessage;
      private String serverFullMessage;

      public SpigotHook() {
            Class<?> spigotConfig;

            try {
                  spigotConfig = Class.forName("org.spigotmc.SpigotConfig");
                  this.isSpigot = true;
            } catch (Exception exception) {
                  this.isSpigot = false;
                  return;
            }

            try
            {
                  this.whitelistMessage = (String)spigotConfig.getDeclaredField("whitelistMessage").get(null);
                  this.serverFullMessage = (String)spigotConfig.getDeclaredField("serverFullMessage").get(null);
            } catch(Exception exception) {
                  exception.printStackTrace();
            }
      }

      public boolean isSpigot() {
            return this.isSpigot;
      }

      public String getWhitelistMessage() {
            return this.whitelistMessage;
      }

      public String getServerFullMessage() {
            return this.serverFullMessage;
      }

}
