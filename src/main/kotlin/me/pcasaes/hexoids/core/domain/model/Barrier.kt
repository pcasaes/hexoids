package me.pcasaes.hexoids.core.domain.model

import me.pcasaes.hexoids.core.domain.utils.TrigUtil
import me.pcasaes.hexoids.core.domain.vector.Vector2
import pcasaes.hexoids.proto.BarrierDto
import kotlin.math.hypot

class Barrier private constructor(
    val centerPosition: Vector2,
    private val rotationAngle: Float,
) {

    companion object {
        const val WIDTH: Float = 0.0006F
        const val HALF_WIDTH: Float = WIDTH / 2F
        const val LENGTH: Float = 0.0032F
        const val HALF_LENGTH: Float = LENGTH / 2F
        val HALF_HYPOTENUSE: Float = hypot(HALF_LENGTH.toDouble(), HALF_WIDTH.toDouble()).toFloat()

        @JvmStatic
        fun place(centerPosition: Vector2, rotationAngle: Float): Barrier {
            return Barrier(centerPosition, rotationAngle)
        }
    }

    val from: Vector2

    val to: Vector2

    private val vector: Vector2

    val normal: Vector2

    private val dto: BarrierDto

    init {
        val fromCenterToCorner = Vector2.fromAngleMagnitude(rotationAngle, HALF_HYPOTENUSE)

        this.from = centerPosition
            .minus(fromCenterToCorner)

        this.to = centerPosition
            .add(fromCenterToCorner)

        this.vector = Vector2.fromAngleMagnitude(
            rotationAngle,
            LENGTH
        )

        this.normal = Vector2.fromAngleMagnitude(rotationAngle + TrigUtil.QUARTER_CIRCLE_IN_RADIANS, 1F)

        this.dto = BarrierDto.newBuilder()
            .setX(centerPosition.getX())
            .setY(centerPosition.getY())
            .setAngle(rotationAngle)
            .build()
    }

    fun extend(): Barrier {
        return place(centerPosition.add(vector), rotationAngle)
    }

    fun toDto(): BarrierDto {
        return dto
    }


}
