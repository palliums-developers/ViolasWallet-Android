package com.quincysx.crypto.utils;

public final class CheckUtils {
    public static void checkArgument(boolean b, String str, Object... errorMessageArgs) {
        checkArgument(b, String.format(str, errorMessageArgs));
    }

    public static void checkArgument(boolean b, String str) {
        if (!b) {
            throw new RuntimeException(str);
        }
    }

    public static void checkArgument(boolean b) {
        checkArgument(b, "error");
    }
}
