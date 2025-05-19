package me.pcasaes.hexoids.core.domain.utils

object MathUtil {
    fun square(value: Float): Float {
        return value * value
    }

    fun cube(value: Float): Float {
        return value * value * value
    }

    fun quad(value: Float): Float {
        return square(square(value))
    }
}
