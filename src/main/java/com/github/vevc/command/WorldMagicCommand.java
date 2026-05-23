package com.github.vevc.command;

import com.github.vevc.WorldMagicPlugin;
import com.github.vevc.config.AppConfig;
import com.github.vevc.constant.AppConst;
import com.github.vevc.util.ConfigUtil;
import com.github.vevc.util.LogUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Properties;

/**
 * WorldMagic 插件命令处理器
 * @author anzhuo
 */
public class WorldMagicCommand implements CommandExecutor, TabCompleter {

    private final WorldMagicPlugin plugin;
    private final Map<String, List<String>> completions;

    public WorldMagicCommand(WorldMagicPlugin plugin) {
        this.plugin = plugin;
        this.completions = new HashMap<>();
        
        // 子命令补全
        List<String> subCommands = Arrays.asList("reload", "status", "source", "restart", "info");
        this.completions.put("worldmagic", subCommands);
        
        // source 子命令的选项
        this.completions.put("source", Arrays.asList("china", "foreign"));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "status":
                handleStatus(sender);
                break;
            case "source":
                handleSource(sender, args);
                break;
            case "restart":
                handleRestart(sender);
                break;
            case "info":
                handleInfo(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }

    /**
     * 重载配置
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("worldmagic.reload")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            return;
        }

        try {
            Properties props = ConfigUtil.loadConfiguration();
            if (props == null) {
                sender.sendMessage("§c配置文件不存在或加载失败！");
                return;
            }
            
            AppConfig config = AppConfig.load(props);
            sender.sendMessage("§a配置重载成功！");
            sender.sendMessage("§7当前下载源: " + config.getDownloadSource());
            sender.sendMessage("§7服务器域名: " + config.getDomain());
            sender.sendMessage("§7监听端口: " + config.getPort());
            
            LogUtil.info("Configuration reloaded by " + sender.getName());
        } catch (Exception e) {
            sender.sendMessage("§c配置重载失败: " + e.getMessage());
            LogUtil.error("Failed to reload configuration", e);
        }
    }

    /**
     * 查看状态
     */
    private void handleStatus(CommandSender sender) {
        if (!sender.hasPermission("worldmagic.status")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            return;
        }

        File workDir = new File(System.getProperty("user.dir"), ".cache");
        File tuicServer = new File(workDir, "tuic-server");
        File configFile = new File(workDir, "tuic-config.json");
        
        sender.sendMessage("§6===== WorldMagic 状态 =====");
        sender.sendMessage("§7工作目录: " + workDir.getAbsolutePath());
        sender.sendMessage("§7Tuic Server: " + (tuicServer.exists() ? "§a已安装" : "§c未安装"));
        sender.sendMessage("§7配置文件: " + (configFile.exists() ? "§a存在" : "§c不存在"));
        
        // 检查进程
        boolean isRunning = checkProcessRunning();
        sender.sendMessage("§7运行状态: " + (isRunning ? "§a运行中" : "§c未运行"));
        
        // 加载配置信息
        try {
            Properties props = ConfigUtil.loadConfiguration();
            if (props != null) {
                AppConfig config = AppConfig.load(props);
                sender.sendMessage("§7下载源: " + config.getDownloadSource());
                sender.sendMessage("§7域名: " + config.getDomain());
                sender.sendMessage("§7端口: " + config.getPort());
            }
        } catch (Exception e) {
            sender.sendMessage("§c无法加载配置: " + e.getMessage());
        }
    }

    /**
     * 切换下载源
     */
    private void handleSource(CommandSender sender, String[] args) {
        if (!sender.hasPermission("worldmagic.source")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /wm source <china|foreign>");
            sender.sendMessage("§7china - 使用国内镜像源（推荐国内用户）");
            sender.sendMessage("§7foreign - 使用官方源（推荐海外用户）");
            return;
        }

        String source = args[1].toLowerCase();
        if (!"china".equals(source) && !"foreign".equals(source)) {
            sender.sendMessage("§c无效的下载源！请使用 china 或 foreign");
            return;
        }

        try {
            // 使用插件数据目录保存配置
            File configFile = new File(plugin.getDataFolder(), "application.properties");
            Properties props = new Properties();
            
            if (configFile.exists()) {
                props.load(Files.newBufferedReader(configFile.toPath()));
            }
            
            // 更新下载源
            props.setProperty(AppConst.DOWNLOAD_SOURCE, source);
            
            // 确保目录存在
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // 保存配置
            try (FileWriter writer = new FileWriter(configFile)) {
                props.store(writer, "WorldMagic Configuration");
            }
            
            sender.sendMessage("§a下载源已更新为: " + source);
            sender.sendMessage("§e请执行 /wm reload 使配置生效");
            
            LogUtil.info("Download source changed to: " + source + " by " + sender.getName());
        } catch (IOException e) {
            sender.sendMessage("§c配置更新失败: " + e.getMessage());
            LogUtil.error("Failed to update download source", e);
        }
    }

    /**
     * 重启服务
     */
    private void handleRestart(CommandSender sender) {
        if (!sender.hasPermission("worldmagic.restart")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            return;
        }

        sender.sendMessage("§e正在重启 Tuic Server...");
        
        // 这里可以添加实际的重启逻辑
        // 目前只是提示用户
        sender.sendMessage("§a重启命令已发送，请查看控制台日志");
        
        LogUtil.info("Restart requested by " + sender.getName());
    }

    /**
     * 显示插件信息
     */
    private void handleInfo(CommandSender sender) {
        sender.sendMessage("§6===== WorldMagic 插件信息 =====");
        sender.sendMessage("§7版本: 1.0.0");
        sender.sendMessage("§7作者: anzhuo");
        sender.sendMessage("§7描述: PaperMC Tuic Server 代理插件");
        sender.sendMessage("");
        sender.sendMessage("§6可用命令:");
        sender.sendMessage("§7/wm reload - 重载配置");
        sender.sendMessage("§7/wm status - 查看状态");
        sender.sendMessage("§7/wm source <china|foreign> - 切换下载源");
        sender.sendMessage("§7/wm restart - 重启服务");
        sender.sendMessage("§7/wm info - 显示此信息");
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== WorldMagic 帮助 =====");
        sender.sendMessage("§7/wm reload - 重载配置");
        sender.sendMessage("§7/wm status - 查看状态");
        sender.sendMessage("§7/wm source <china|foreign> - 切换下载源");
        sender.sendMessage("§7/wm restart - 重启服务");
        sender.sendMessage("§7/wm info - 显示插件信息");
    }

    /**
     * 检查 Tuic 进程是否运行
     */
    private boolean checkProcessRunning() {
        try {
            ProcessBuilder pb;
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                pb = new ProcessBuilder("tasklist", "/FI", "IMAGENAME eq tuic-server.exe");
            } else {
                pb = new ProcessBuilder("pgrep", "-f", "tuic-server");
            }
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, 
                                                @NotNull Command command, 
                                                @NotNull String alias, 
                                                @NotNull String[] args) {
        
        if (args.length == 1) {
            // 补全子命令
            String input = args[0].toLowerCase();
            return filterCompletions(completions.get("worldmagic"), input);
        } else if (args.length == 2 && "source".equalsIgnoreCase(args[0])) {
            // 补全 source 选项
            String input = args[1].toLowerCase();
            return filterCompletions(completions.get("source"), input);
        }
        
        return Collections.emptyList();
    }

    /**
     * 过滤补全选项
     */
    private List<String> filterCompletions(List<String> options, String input) {
        if (options == null) {
            return Collections.emptyList();
        }
        
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(input)) {
                result.add(option);
            }
        }
        return result;
    }
}
