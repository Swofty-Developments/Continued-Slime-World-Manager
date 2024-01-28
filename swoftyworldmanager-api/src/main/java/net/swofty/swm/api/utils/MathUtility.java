package net.swofty.swm.api.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MathUtility {
    public static int[] toIntArray(byte[] buf) {
        ByteBuffer buffer = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN);
        int[] ret = new int[buf.length / 4];

        buffer.asIntBuffer().get(ret);

        return ret;
    }

    public static boolean isEmpty(byte[] array) {
        for (byte b : array) {
            if (b != 0) {
                return false;
            }
        }

        return true;
    }
}
