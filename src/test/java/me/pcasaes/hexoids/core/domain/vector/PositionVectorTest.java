package me.pcasaes.hexoids.core.domain.vector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PositionVectorTest {

    @Test
    void testReflect() {
        PositionVector ps = PositionVector.of(0, 0, 0, 0F, 0);
        ps.moveBy(1F, 0, 1000);

        ps.reflect(Vector2.fromXY(0.5F, 0), Vector2.fromAngleMagnitude(0, 1F), 0.001F, 1F);

        System.out.println(ps);
    }
}