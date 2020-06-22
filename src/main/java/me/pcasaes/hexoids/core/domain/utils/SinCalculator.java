package me.pcasaes.hexoids.core.domain.utils;

public final class SinCalculator {

    private static final int SIN_TABLE_EXP = 16;
    private static final int SIN_TABLE_SIZE = (int) Math.pow(2, SIN_TABLE_EXP);
    private static final int SIN_TABLE_SIZE_MINUS_ONE = SIN_TABLE_SIZE - 1;
    private static final double[] SIN_TABLE = new double[SIN_TABLE_SIZE];
    private static final double PI_2 = (2. * Math.PI);
    private static final double PI_HALF = (0.5 * Math.PI);
    private static final double SIN_TABLE_SIZE_DIVIDED_BY_PI_2 = SIN_TABLE_SIZE / PI_2;

    static {
        for (int i = 0; i < SIN_TABLE_SIZE; i++) {
            SIN_TABLE[i] = Math.sin(PI_2 * i / (double) SIN_TABLE_SIZE);
        }
    }

    public static float sin(double p) {
        // the resolution is high enough that we can use linear
        return sinLinear(p);
    }

    public static float cos(double p) {
        // the resolution is high enough that we can use linear
        return cosLinear(p);
    }

    public static float sinNoInterpolation(float p) {
        int i = (int) (SIN_TABLE_SIZE_DIVIDED_BY_PI_2 * p);
        return (float) SIN_TABLE[i & SIN_TABLE_SIZE_MINUS_ONE];
    }

    public static float sinLinear(double p) {
        double translated = SIN_TABLE_SIZE_DIVIDED_BY_PI_2 * p;
        int translatedInt = (int) translated;
        double partial = translated - translatedInt;

        int i = translatedInt & SIN_TABLE_SIZE_MINUS_ONE;
        int i2 = (i + 1) & SIN_TABLE_SIZE_MINUS_ONE;
        double v1 = SIN_TABLE[i];
        double v2 = SIN_TABLE[i2];
        return (float) (v1 + (v2 - v1) * partial);
    }

    public static float cosLinear(double p) {
        return sinLinear(p + PI_HALF);
    }

    public static float sin4(double p) {
        double translated = SIN_TABLE_SIZE_DIVIDED_BY_PI_2 * p;
        int translatedInt = (int) translated;
        double partial = translated - translatedInt;

        int ip = (translatedInt + SIN_TABLE_SIZE_MINUS_ONE) & SIN_TABLE_SIZE_MINUS_ONE;
        int i0 = translatedInt & SIN_TABLE_SIZE_MINUS_ONE;
        int i1 = (i0 + 1) & SIN_TABLE_SIZE_MINUS_ONE;
        int i2 = (i0 + 2) & SIN_TABLE_SIZE_MINUS_ONE;

        double a = SIN_TABLE[ip];
        double b = SIN_TABLE[i0];
        double c = SIN_TABLE[i1];
        double d = SIN_TABLE[i2];

        double cMinusB = c - b;
        return (float) (b + partial * (
                cMinusB - (1 / PI_2) * (1. - partial) * (
                        (d - a - 3.0 * cMinusB) * partial + (d + 2.0 * a - 3.0 * b)
                )
        ));
    }

    public static float cos4(double p) {
        return sin4(p + PI_HALF);
    }


    private SinCalculator() {
    }
}
