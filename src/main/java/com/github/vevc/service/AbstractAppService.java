package com.github.vevc.service;

import com.github.vevc.config.AppConfig;
import com.github.vevc.util.LogUtil;
import org.codehaus.plexus.util.FileUtils;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * @author anzhuo
 */
public abstract class AbstractAppService {

    private static final File WORK_DIR = new File(System.getProperty("user.dir"), ".cache");
    protected static final boolean OS_IS_ARM;

    static {
        String arch = System.getProperty("os.arch").toLowerCase();
        OS_IS_ARM = arch.contains("arm") || arch.contains("aarch64");
    }

    /**
     * get app download url
     *
     * @param appVersion app version
     * @param appConfig app configuration
     * @return url
     */
    protected abstract String getAppDownloadUrl(String appVersion, AppConfig appConfig);

    /**
     * install app
     *
     * @param appConfig app properties
     * @throws Exception e
     */
    protected abstract void install(AppConfig appConfig) throws Exception;

    /**
     * start app
     */
    protected abstract void startup();

    /**
     * clean workspace
     */
    protected abstract void clean();

    protected File initWorkDir() throws IOException {
        FileUtils.forceMkdir(WORK_DIR);
        return WORK_DIR;
    }

    protected File getWorkDir() {
        return WORK_DIR;
    }

    protected void setExecutePermission(Path destFile) throws IOException {
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(destFile);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        Files.setPosixFilePermissions(destFile, perms);
    }

    protected void download(String downloadUrl, File file) throws Exception {
        try (HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS).build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .GET()
                    .build();
            HttpResponse<InputStream> response =
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = response.body()) {
                Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * start process
     *
     * @param pb processBuilder
     * @return exitCode
     * @throws Exception e
     */
    protected int startProcess(ProcessBuilder pb) throws Exception {
        Process process = pb.start();
        try (
                InputStream in = process.getInputStream();
                InputStreamReader inReader = new InputStreamReader(in);
                BufferedReader reader = new BufferedReader(inReader)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                LogUtil.info(line);
            }
        }
        return process.waitFor();
    }
}
