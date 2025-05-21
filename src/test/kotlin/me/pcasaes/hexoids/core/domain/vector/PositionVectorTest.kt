package me.pcasaes.hexoids.core.domain.vector

import me.pcasaes.hexoids.core.domain.vector.PositionVector.Companion.of
import me.pcasaes.hexoids.core.domain.vector.Vector2.Companion.fromAngleMagnitude
import me.pcasaes.hexoids.core.domain.vector.Vector2.Companion.fromXY
import org.junit.jupiter.api.Test

class PositionVectorTest {
    @Test
    fun testReflect() {
        val ps = of(0F, 0F, 0F, 0F, 0)
        ps.moveBy(1F, 0F, 1000)

        ps.reflect(fromXY(0.5F, 0F), fromAngleMagnitude(0F, 1F), 0.001F, 1F)

        println(ps)
    }
}