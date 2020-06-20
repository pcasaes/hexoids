package me.pcasaes.hexoids.core.domain.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrigUtilTest {

    @Test
    void testZeroAngleMagnitude() {
        assertEquals(0f, TrigUtil.calculateXComponentFromAngleAndMagnitude(0, 0), Float.MAX_VALUE);
        assertEquals(0f, TrigUtil.calculateYComponentFromAngleAndMagnitude(0, 0), Float.MAX_VALUE);
    }
}