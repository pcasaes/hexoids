package me.pcasaes.hexoids.core.domain.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SinCalculatorTest {

    @Test
    void testSinLinear() {
        float increment = 0.0001f;
        for (float f = -6.5f; f <= 6.5f; f += increment) {
            float expected = (float) Math.sin(f);
            float found = SinCalculator.sinLinear(f);
            assertEquals(expected, found, 0.0000001f, "For input " + f);
        }
    }

    @Test
    void testCosLinear() {
        float increment = 0.0001f;
        for (float f = -6.5f; f <= 6.5f; f += increment) {
            float expected = (float) Math.cos(f);
            float found = SinCalculator.cosLinear(f);
            assertEquals(expected, found, 0.0000001f, "For input " + f);
        }
    }

    @Test
    void testSin4() {
        float increment = 0.0001f;
        for (float f = -6.5f; f <= 6.5f; f += increment) {
            float expected = (float) Math.sin(f);
            float found = SinCalculator.sin4(f);
            assertEquals(expected, found, 0.0000001f, "For input " + f);
        }
    }

    @Test
    void testCos4() {
        float increment = 0.0001f;
        for (float f = -6.5f; f <= 6.5f; f += increment) {
            float expected = (float) Math.cos(f);
            float found = SinCalculator.cos4(f);
            assertEquals(expected, found, 0.0000001f, "For input " + f);
        }
    }
}