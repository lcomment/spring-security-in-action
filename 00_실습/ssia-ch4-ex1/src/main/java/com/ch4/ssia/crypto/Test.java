package com.ch4.ssia.crypto;

import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.keygen.StringKeyGenerator;

import java.util.Arrays;

public class Test {

    public static void main(String[] args) {
        /*
         * StringKeyGenerator
         */
        StringKeyGenerator stringKeyGenerator = KeyGenerators.string();
        String salt = stringKeyGenerator.generateKey();

        // 376c27cdf3a0ceca
        System.out.println(salt);

        /*
         * BytesKeyGenerator
         * default : 8 byte 길이
         */
        BytesKeyGenerator bytesKeyGenerator = KeyGenerators.secureRandom(16);
        byte[] key = bytesKeyGenerator.generateKey();
        int keyLength = bytesKeyGenerator.getKeyLength();

        // [107, 45, -40, -87, 30, -117, -54, -65, 80, -10, -48, 55, -83, 121, -11, 57] : 16
        System.out.println(Arrays.toString(key) + " : " + keyLength);

        // 같은 키 값 반환
        bytesKeyGenerator = KeyGenerators.shared(16);
        byte[] sharedKey1 = bytesKeyGenerator.generateKey();
        byte[] sharedKey2 = bytesKeyGenerator.generateKey();
        // true
        System.out.println(sharedKey1 == sharedKey2);
    }
}
