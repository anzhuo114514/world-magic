package com.github.vevc;

import com.github.vevc.command.WorldMagicCommand;
import com.github.vevc.config.AppConfig;
import com.github.vevc.service.impl.TuicServiceImpl;
import com.github.vevc.util.ConfigUtil;
import com.github.vevc.util.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Properties;

/**
 * @author anzhuo
 */
public final class WorldMagicPlugin extends JavaPlugin {

    private final TuicServiceImpl tuicService = new TuicServiceImpl();
    private WorldMagicCommand worldMagicCommand;

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getLogger().info("WorldMagicPlugin enabled");
        LogUtil.init(this);
        
        // 注册命令
        this.worldMagicCommand = new WorldMagicCommand(this);
        Objects.requireNonNull(this.getCommand("worldmagic")).setExecutor(this.worldMagicCommand);
        Objects.requireNonNull(this.getCommand("worldmagic")).setTabCompleter(this.worldMagicCommand);
        this.getLogger().info("Commands registered successfully");
        
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            // load config
            Properties props = ConfigUtil.loadConfiguration();
            AppConfig appConfig = AppConfig.load(props);
            if (Objects.isNull(appConfig)) {
                Bukkit.getScheduler().runTask(this, () -> {
                    this.getLogger().info("Configuration not found, disabling plugin");
                    Bukkit.getPluginManager().disablePlugin(this);
                });
                return;
            }

            // install & start apps
            if (this.installApps(appConfig)) {
                Bukkit.getScheduler().runTask(this, () -> {
                    Bukkit.getScheduler().runTaskAsynchronously(this, tuicService::startup);
                    // 不再调用 clean()，避免删除运行中的文件
                });
            } else {
                Bukkit.getScheduler().runTask(this, () -> {
                    this.getLogger().info("Plugin install failed, disabling plugin");
                    Bukkit.getPluginManager().disablePlugin(this);
                });
            }
        });
    }

    private boolean installApps(AppConfig appConfig) {
        try {
            tuicService.install(appConfig);
            return true;
        } catch (Exception e) {
            LogUtil.error("Plugin install failed", e);
            return false;
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.getLogger().info("WorldMagicPlugin disabled");
    }
}
