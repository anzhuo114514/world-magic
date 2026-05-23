package com.github.vevc.service.impl;

import com.github.vevc.config.AppConfig;
import com.github.vevc.service.AbstractAppService;
import com.github.vevc.util.LogUtil;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author anzhuo
 */
public class TuicServiceImpl extends AbstractAppService {

    private static final String APP_NAME = "tuic-server";
    private static final String APP_CONFIG_NAME = "tuic-config.json";
    private static final String APP_STARTUP_NAME = "startup.sh";
    private static final String APP_DOWNLOAD_URL_FOREIGN = "https://github.com/Itsusinn/tuic/releases/download/v%s/tuic-server-%s-linux-musl";
    private static final String APP_DOWNLOAD_URL_CHINA = "https://gh-proxy.com/https://github.com/Itsusinn/tuic/releases/download/v%s/tuic-server-%s-linux-musl";
    private static final String APP_CONFIG_URL_FOREIGN = "https://raw.githubusercontent.com/vevc/world-magic/refs/heads/main/tuic-config.json";
    private static final String APP_CONFIG_URL_CHINA = "https://gh-proxy.com/https://raw.githubusercontent.com/vevc/world-magic/refs/heads/main/tuic-config.json";
    private static final String TUIC_URL = "tuic://%s%%3A%s@%s:%s?sni=%s&alpn=h3&insecure=1&allowInsecure=1&congestion_control=bbr#%s-tuic";

    @Override
    protected String getAppDownloadUrl(String appVersion, AppConfig appConfig) {
        String arch = OS_IS_ARM ? "aarch64" : "x86_64";
        String baseUrl = "china".equalsIgnoreCase(appConfig.getDownloadSource()) 
                ? APP_DOWNLOAD_URL_CHINA : APP_DOWNLOAD_URL_FOREIGN;
        return String.format(baseUrl, appVersion, arch);
    }

    protected String getConfigDownloadUrl(AppConfig appConfig) {
        return "china".equalsIgnoreCase(appConfig.getDownloadSource()) 
                ? APP_CONFIG_URL_CHINA : APP_CONFIG_URL_FOREIGN;
    }

    @Override
    public void install(AppConfig appConfig) throws Exception {
        File workDir = this.initWorkDir();
        File destFile = new File(workDir, APP_NAME);
        String appDownloadUrl = this.getAppDownloadUrl(appConfig.getTuicVersion(), appConfig);
        LogUtil.info("Tuic server download url: " + appDownloadUrl);
        LogUtil.info("Download source: " + appConfig.getDownloadSource());
        this.download(appDownloadUrl, destFile);
        LogUtil.info("Tuic server downloaded successfully");
        this.setExecutePermission(destFile.toPath());
        LogUtil.info("Tuic server installed successfully");

        // download config
        this.downloadConfig(workDir, appConfig);
        LogUtil.info("Tuic server config downloaded successfully");

        // add startup.sh
        String startupScript = String.format(
                "#!/usr/bin/env sh\n\ncd %s\nexec ./tuic-server -c tuic-config.json", 
                workDir.getAbsolutePath().replace("\\", "/"));
        Files.writeString(new File(workDir, APP_STARTUP_NAME).toPath(), startupScript);
        LogUtil.info("Startup script created");

        // update sub file
        this.updateSubFile(appConfig);
        LogUtil.info("Installation completed");
    }

    private void updateSubFile(AppConfig appConfig) throws Exception {
        String tuicUrl = String.format(TUIC_URL, appConfig.getUuid(), appConfig.getPassword(),
                appConfig.getDomain(), appConfig.getPort(), appConfig.getDomain(), appConfig.getRemarksPrefix());
        String base64Url = Base64.getEncoder().encodeToString(tuicUrl.getBytes(StandardCharsets.UTF_8));
        Path nodeFilePath = new File(this.getWorkDir(), appConfig.getUuid()).toPath();
        Files.write(nodeFilePath, Collections.singleton(base64Url));
    }

    private void downloadConfig(File configPath, AppConfig appConfig) throws Exception {
        String configUrl = this.getConfigDownloadUrl(appConfig);
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(configUrl))
                    .GET()
                    .build();
            HttpResponse<InputStream> response =
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            
            if (response.statusCode() != 200) {
                throw new Exception("Failed to download config, HTTP status: " + response.statusCode());
            }
            
            String content;
            try (InputStream in = response.body()) {
                content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            String configText = content.replace("10008", appConfig.getPort())
                    .replace("YOUR_UUID", appConfig.getUuid())
                    .replace("YOUR_PASSWORD", appConfig.getPassword())
                    .replace("YOUR_DOMAIN", appConfig.getDomain());
            File configFile = new File(configPath, APP_CONFIG_NAME);
            Files.writeString(configFile.toPath(), configText,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    @Override
    public void startup() {
        File workDir = this.getWorkDir();
        File appFile = new File(workDir, APP_NAME);
        File startupFile = new File(workDir, APP_STARTUP_NAME);
        
        try {
            int restartCount = 0;
            final int MAX_RESTARTS = 10; // 最大重启次数
            
            while (Files.exists(appFile.toPath()) && restartCount < MAX_RESTARTS) {
                ProcessBuilder pb;
                String os = System.getProperty("os.name").toLowerCase();
                
                // 跨平台兼容：选择不同的执行方式
                if (os.contains("win")) {
                    // Windows: 使用 bash 或直接执行
                    pb = new ProcessBuilder("bash", startupFile.getAbsolutePath());
                } else {
                    // Linux/Unix: 使用 sh
                    pb = new ProcessBuilder("sh", startupFile.getAbsolutePath());
                }
                
                pb.directory(workDir);
                
                // 重定向输出到 null 设备
                if (os.contains("win")) {
                    pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                    pb.redirectError(ProcessBuilder.Redirect.DISCARD);
                } else {
                    pb.redirectOutput(new File("/dev/null"));
                    pb.redirectError(new File("/dev/null"));
                }
                
                LogUtil.info("Starting Tuic server... (Attempt " + (restartCount + 1) + ")");
                int exitCode = this.startProcess(pb);
                
                if (exitCode == 0) {
                    LogUtil.info("Tuic server process exited normally with code: " + exitCode);
                    break;
                } else {
                    restartCount++;
                    LogUtil.info("Tuic server process exited with code: " + exitCode + ", restarting... (" + restartCount + "/" + MAX_RESTARTS + ")");
                    TimeUnit.SECONDS.sleep(3);
                }
            }
            
            if (restartCount >= MAX_RESTARTS) {
                LogUtil.error("Tuic server failed to start after " + MAX_RESTARTS + " attempts", new Exception("Max restart attempts reached"));
            }
        } catch (Exception e) {
            LogUtil.error("Tuic server startup failed", e);
        }
    }

    @Override
    public void clean() {
        // 不在运行时删除文件，避免影响正在运行的进程
        // 如果需要清理，应该先停止进程再清理
        LogUtil.info("Clean operation skipped to avoid affecting running process");
        
        // 如果确实需要清理，可以在插件禁用时执行
        // 这里保留方法但不执行删除操作
    }
}
