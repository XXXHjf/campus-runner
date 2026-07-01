package com.mikasa.campusrunner.common.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * author  Edith
 * created  2025/12/14 11:30
 * SHA-256加密工具类
 */
@Slf4j
public class EncryptSHA256Util {

    private static final String ALGORITHM = "SHA-256";

//    private static final String KEY = "34C5EA1CBFE5893A8FCF749418765443";//密钥 eaasmrscinMpkuruna -> base64(大写) -> MD5

    /**
     * 对明文data进行SHA-256散列算法加密
     * @param data
     * @return 16进制密文字符串
     */
    public static String encrypt(String data) {
        log.info("Encrypting (SHA-256)...");
        MessageDigest messageDigest;
        String encoderStr = "";
        try {
            messageDigest = MessageDigest.getInstance(ALGORITHM);//使用的算法类型
            messageDigest.update(data.getBytes(StandardCharsets.UTF_8));
            encoderStr = byte2Hex(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        log.info("Encryption complete");
        return encoderStr;
    }


    /**
     * sha256加密 将byte转为16进制
     *
     * @param bytes 字节码
     * @return 加密后的字符串
     */
    private static String byte2Hex(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        String temp;
        for (byte aByte : bytes) {
            temp = Integer.toHexString(aByte & 0xFF);
            if (temp.length() == 1) {
                //1得到一位的进行补0操作
                stringBuilder.append("0");
            }
            stringBuilder.append(temp);
        }
        return stringBuilder.toString();
    }
}
