package com.mikasa.campusrunner.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * author  Edith
 * created  2026/3/9 08:01
 */
@Slf4j
public class EncryptMD5Util {

    /** * MD5加密之方法二 * @explain java实现 * @param str * 待加密字符串 * @return 16进制加密字符串 */
    public static String encrypt(String str) {
        log.info("Encrypting (MD5)...");
        // 加密后的16进制字符串
        String hexStr = "";
        try {

            // 此 MessageDigest 类为应用程序提供信息摘要算法的功能
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            // 转换为MD5码
            byte[] digest = md5.digest(str.getBytes(StandardCharsets.UTF_8));
            hexStr = byte2Hex(digest);
        } catch (Exception e) {

            e.printStackTrace();
        }
        log.info("Encryption complete");
        return hexStr;
    }


    /**
     * 将byte转为16进制
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
