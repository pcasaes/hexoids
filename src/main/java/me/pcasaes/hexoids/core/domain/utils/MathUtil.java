package me.pcasaes.hexoids.core.domain.utils;

public final class MathUtil {

    private MathUtil() {
    }

    public static float square(float val) {
        return val * val;
    }

    public static float cube(float val) {
        return val * val * val;
    }

    public static float quad(float val) {
        return square(square(val));
    }
}
