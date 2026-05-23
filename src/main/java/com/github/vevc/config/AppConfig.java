package com.github.vevc.config;

import com.github.vevc.constant.AppConst;

import java.util.Properties;

/**
 * @author anzhuo
 */
public class AppConfig {

    private String domain;
    private String port;
    private String uuid;
    private String password;
    private String tuicVersion;
    private String remarksPrefix;
    private String downloadSource;

    public static AppConfig load(Properties props) {
        if (props == null) {
            return null;
        }
        AppConfig cfg = new AppConfig();
        cfg.setDomain(props.getProperty(AppConst.DOMAIN));
        cfg.setPort(props.getProperty(AppConst.PORT));
        cfg.setUuid(props.getProperty(AppConst.UUID));
        cfg.setPassword(props.getProperty(AppConst.PASSWORD));
        cfg.setTuicVersion(props.getProperty(AppConst.TUIC_VERSION));
        cfg.setRemarksPrefix(props.getProperty(AppConst.REMARKS_PREFIX));
        cfg.setDownloadSource(props.getProperty(AppConst.DOWNLOAD_SOURCE, "foreign"));
        return cfg;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTuicVersion() {
        return tuicVersion;
    }

    public void setTuicVersion(String tuicVersion) {
        this.tuicVersion = tuicVersion;
    }

    public String getRemarksPrefix() {
        return remarksPrefix;
    }

    public void setRemarksPrefix(String remarksPrefix) {
        this.remarksPrefix = remarksPrefix;
    }

    public String getDownloadSource() {
        return downloadSource;
    }

    public void setDownloadSource(String downloadSource) {
        this.downloadSource = downloadSource;
    }
}
