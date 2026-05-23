package com.github.vevc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author anzhuo
 */
public final class Md5Util {

    private static final char[] HEX_DIGITS =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String toHexString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(HEX_DIGITS[(b >> 4) & 0x0F]);
            hex.append(HEX_DIGITS[b & 0x0F]);
        }
        return hex.toString();
    }

    public static String md5(File file) throws IOException {
        byte[] encodeBytes;
        try (FileInputStream in = new FileInputStream(file);
             FileChannel ch = in.getChannel()) {
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            messagedigest.update(byteBuffer);
            encodeBytes = messagedigest.digest();
        } catch (NoSuchAlgorithmException neverHappened) {
            throw new RuntimeException(neverHappened);
        }
        return toHexString(encodeBytes);
    }

    public static String md5(String string) {
        byte[] encodeBytes;
        try {
            encodeBytes = MessageDigest.getInstance("MD5")
                    .digest(string.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException neverHappened) {
            throw new RuntimeException(neverHappened);
        }
        return toHexString(encodeBytes);
    }

    private Md5Util() {
        throw new IllegalStateException("Utility class");
    }
}
