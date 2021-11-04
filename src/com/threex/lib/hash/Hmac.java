package com.threex.lib.hash;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.Key;

public class Hmac {
    public static String hash_sha256(String key,String msg){
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8.name()); // 把密鑰字串轉為byte[]
            Key hmacKey = new SecretKeySpec(keyBytes, "HmacSHA256"); // 建立HMAC加密用密鑰
            Mac hmacSHA256 = Mac.getInstance("HmacSHA256"); // 取得SHA256 HMAC的Mac實例
            hmacSHA256.init(hmacKey); // 使用密鑰對Mac進行初始化
            byte [] macData = hmacSHA256.doFinal(msg.getBytes(StandardCharsets.UTF_8.name())); // 對原始訊息進行雜湊計算
            String hexStringOfTheOriginMessage = bytesToHex(macData); //  使用Apache Commons Codec的Hex把雜湊計算的結果轉為Hex字串
            return hexStringOfTheOriginMessage;

//            System.out.println(hexStringOfTheOriginMessage); // 388b02bb9be6c19490d4014aaaccb62a3969f44f3ecef3b2218e7ee1d457188d
//
//            System.out.println(hmacSHA256.getAlgorithm()); // HmacSHA256
//            System.out.println(hmacSHA256.getMacLength()); // 32
//            System.out.println(hmacSHA256.getProvider().getName()); // SunJCE (JCE提供者)
        } catch (Exception e) {
            // 例外處理
        }
        return "";
    }
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

}
