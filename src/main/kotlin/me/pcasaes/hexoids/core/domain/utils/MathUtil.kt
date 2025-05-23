package me.pcasaes.hexoids.core.domain.utils

import kotlin.math.abs

private const val EPSILON = 1e-6F

fun Float.isZero(): Boolean {
    return abs(this) <= EPSILON
}

fun Float.isSame(other: Float): Boolean {
    return (this - other).isZero()
}

fun Float.square(): Float {
    return this * this
}

fun Float.cube(): Float {
    return this * this * this
}
