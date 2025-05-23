package me.pcasaes.hexoids.core.domain.utils

import me.pcasaes.hexoids.core.domain.utils.TrigUtil.xComponentFromAngleAndMagnitude
import me.pcasaes.hexoids.core.domain.utils.TrigUtil.yComponentFromAngleAndMagnitude
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TrigUtilTest {
    
    @Test
    fun testZeroAngleMagnitude() {
        Assertions.assertEquals(0F, xComponentFromAngleAndMagnitude(0F, 0F), Float.Companion.MAX_VALUE)
        Assertions.assertEquals(0F, yComponentFromAngleAndMagnitude(0F, 0F), Float.Companion.MAX_VALUE)
    }
}