package org.example.rfid.util;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomIds {

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private RandomIds() {
    }

    public static String epc() {
        char[] buffer = new char[12];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = HEX[random.nextInt(HEX.length)];
        }
        return "EPC-" + new String(buffer);
    }

    public static String transactionId() {
        char[] buffer = new char[16];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = HEX[random.nextInt(HEX.length)];
        }
        return "TX-" + new String(buffer);
    }
}
