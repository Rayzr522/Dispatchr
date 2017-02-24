package com.rayzr522.dispatchr;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author Rayzr
 */
public class Dispatchr extends JavaPlugin {

    @Override
    public void onEnable() {
        // Load after everything has finished loading
        new BukkitRunnable() {
            public void run() {
                load();
            }
        }.runTaskLater(this, 10L);
    }

    public void load() {
        saveDefaultConfig();
        reloadConfig();

        FileConfiguration config = getConfig();
        config.getKeys(false).stream().filter(config::isList).forEach(key -> {

            Plugin plugin = Bukkit.getPluginManager().getPlugin(key);
            if (plugin == null) {
                getLogger().severe("The plugin '" + key + "' is not installed!");
                return;
            }

            List<String> commands = config.getStringList(key);

            for (String command : commands) {
                PluginCommand pluginCommand = Bukkit.getPluginCommand(String.format("%s:%s", plugin.getName(), command));
                if (pluginCommand == null) {
                    getLogger().severe("The command '" + command + "' could not be found in the plugin '" + key + "'!");
                    continue;
                }

                if (pluginCommand.getExecutor() instanceof CommandWrapper) {
                    continue;
                }

                if (pluginCommand.getExecutor() instanceof Plugin) {
                    getLogger().warning("The command '" + command + "' in '" + key
                            + "' was wrapped, but the command handler was the plugin itself, meaning it is possible that more than just this one command was wrapped");
                }

                pluginCommand.setExecutor(new CommandWrapper(pluginCommand.getExecutor()));

                System.out.println(Bukkit.getPluginCommand(String.format("%s:%s", plugin.getName(), command)).getExecutor().getClass().getCanonicalName());
            }

        });
    }

    private class CommandWrapper implements CommandExecutor {
        private CommandExecutor originalExecutor;
    
        public CommandWrapper(CommandExecutor originalExecutor) {
            Objects.requireNonNull(originalExecutor);
            this.originalExecutor = originalExecutor;
            System.out.println("Wrapped " + originalExecutor.getClass().getCanonicalName());
    
        }
    
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            System.out.println("Calling wrapper");
            if (!(sender instanceof Player)) {
                System.out.println("Not player!");
                return originalExecutor.onCommand(sender, command, label, args);
            }
    
            String raw = String.format("/%s %s", label, Arrays.stream(args).collect(Collectors.joining(" ")));
            System.out.println("Checking '" + raw + "'");
    
            PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent((Player) sender, raw);
            Bukkit.getPluginManager().callEvent(event);
    
            if (event.isCancelled()) {
                System.out.println("Cancelled");
                return true;
            }
    
            return originalExecutor.onCommand(sender, command, label, args);
        }
    }

}
