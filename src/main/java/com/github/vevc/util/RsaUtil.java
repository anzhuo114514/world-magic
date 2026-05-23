package com.github.vevc.util;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @author anzhuo
 */
public final class RsaUtil {

    /**
     * 加密算法
     */
    private static final String KEY_ALGORITHM = "RSA";

    /**
     * RSA单次加密的最大明文长度
     */
    private static final int MAX_ENCRYPT_BLOCK = 117;

    /**
     * RSA单次解密的最大密文长度
     */
    private static final int MAX_DECRYPT_BLOCK = 128;

    /**
     * 初始化密钥
     *
     * @return 随机生成的密钥对
     */
    public static KeyPair initKey() {
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGen.initialize(1024);
            return keyPairGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取公钥
     *
     * @param keyPair 密钥对对象
     * @return Base64编码的公钥
     */
    public static String getPublicKey(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    /**
     * 获取私钥
     *
     * @param keyPair 密钥对对象
     * @return Base64编码的私钥
     */
    public static String getPrivateKey(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }

    /**
     * 使用公钥加密数据
     *
     * @param data 需要加密的原文
     * @param key  公钥
     * @return 加密后进行Base64的密文
     * @throws Exception 加密失败
     */
    public static String encryptByPublicKey(String data, String key) throws Exception {
        return Base64.getEncoder().encodeToString(encryptByPublicKey(data.getBytes(StandardCharsets.UTF_8), key));
    }

    /**
     * 使用公钥加密数据
     *
     * @param data 需要加密的数据
     * @param key  公钥
     * @return 加密后的数据
     * @throws Exception 加密失败
     */
    public static byte[] encryptByPublicKey(byte[] data, String key) throws Exception {
        // 取得公钥
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(key));
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key publicKey = keyFactory.generatePublic(x509KeySpec);
        // 对数据进行加密
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return handleResult(Cipher.ENCRYPT_MODE, data, cipher);
    }

    /**
     * RSA加解密数据有长度限制，此处对数据做分段处理
     */
    private static byte[] handleResult(int opmode, byte[] data, Cipher cipher) throws Exception {
        int inputLen = data.length;
        int offset = 0;
        byte[] cache;
        int i = 0;
        int maxHandleLength = opmode == Cipher.ENCRYPT_MODE ? MAX_ENCRYPT_BLOCK : MAX_DECRYPT_BLOCK;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            while (inputLen > offset) {
                int length = Math.min(inputLen - offset, maxHandleLength);
                cache = cipher.doFinal(data, offset, length);
                out.write(cache, 0, cache.length);
                i++;
                offset = i * maxHandleLength;
            }
            return out.toByteArray();
        }
    }

    /**
     * 使用私钥解密数据
     *
     * @param data 需要解密的密文
     * @param key  私钥
     * @return 解密后的明文
     * @throws Exception 解密失败
     */
    public static String decryptByPrivateKey(String data, String key) throws Exception {
        return new String(decryptByPrivateKey(Base64.getDecoder().decode(data), key), StandardCharsets.UTF_8);
    }

    /**
     * 使用私钥解密数据
     *
     * @param data 需要解密的数据
     * @param key  私钥
     * @return 解密后的数据
     * @throws Exception 解密失败
     */
    public static byte[] decryptByPrivateKey(byte[] data, String key) throws Exception {
        // 取得私钥
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key));
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
        // 对数据进行解密
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return handleResult(Cipher.DECRYPT_MODE, data, cipher);
    }

    private RsaUtil() {
        throw new IllegalStateException("Utility class");
    }
}
