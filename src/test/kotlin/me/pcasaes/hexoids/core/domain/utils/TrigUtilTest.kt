package me.pcasaes.hexoids.core.domain.utils

import me.pcasaes.hexoids.core.domain.utils.TrigUtil.calculateXComponentFromAngleAndMagnitude
import me.pcasaes.hexoids.core.domain.utils.TrigUtil.calculateYComponentFromAngleAndMagnitude
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TrigUtilTest {
    
    @Test
    fun testZeroAngleMagnitude() {
        Assertions.assertEquals(0F, calculateXComponentFromAngleAndMagnitude(0F, 0F), Float.Companion.MAX_VALUE)
        Assertions.assertEquals(0F, calculateYComponentFromAngleAndMagnitude(0F, 0F), Float.Companion.MAX_VALUE)
    }
}