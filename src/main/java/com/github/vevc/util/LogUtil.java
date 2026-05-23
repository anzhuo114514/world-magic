package com.github.vevc.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author vevc
 */
public final class LogUtil {

    private static final boolean DEBUG = true;
    private static Logger logger;

    public static void init(JavaPlugin javaPlugin) {
        logger = javaPlugin.getLogger();
    }

    public static void info(String msg) {
        if (DEBUG) {
            logger.info(msg);
        }
    }

    public static void error(String msg, Exception e) {
        if (DEBUG) {
            logger.log(Level.SEVERE, msg, e);
        }
    }

    private LogUtil() {
        throw new IllegalStateException("Utility class");
    }
}
