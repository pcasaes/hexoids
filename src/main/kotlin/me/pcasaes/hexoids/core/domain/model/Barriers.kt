package me.pcasaes.hexoids.core.domain.model

import me.pcasaes.hexoids.core.domain.index.BarrierSpatialIndexFactory.Companion.factory
import me.pcasaes.hexoids.core.domain.model.Barrier.Companion.place
import me.pcasaes.hexoids.core.domain.utils.TrigUtil
import me.pcasaes.hexoids.core.domain.vector.Vector2
import kotlin.math.pow
import kotlin.math.sqrt

class Barriers private constructor() : Iterable<Barrier> {

    companion object {
        @JvmStatic
        fun create(): Barriers {
            return Barriers()
        }
    }

    private val barriers = ArrayList<Barrier>()

    private var started = false

    fun fixedUpdate(timestamp: Long) {
        if (started) {
            return
        }
        started = true

        createCenterDiamond()
        createCornerSquares()


        factory()
            .get()
            .update(this)
    }

    private fun createCornerSquares() {
        val iter = (0.25F / Barrier.LENGTH).toInt() - 6

        var s = place(Vector2.fromXY(0.25F, 0F), TrigUtil.PI / 2F)
        for (i in 0..<iter) {
            s = s.extend()
            if (i > 6) {
                barriers.add(s)
            }
        }

        s = place(Vector2.fromXY(0F, 0.25F), 0F)
        for (i in 0..<iter) {
            s = s.extend()
            if (i > 6) {
                barriers.add(s)
            }
        }

        s = place(Vector2.fromXY(0.25F, 0.75F), TrigUtil.PI / 2F)
        for (i in 0..<iter) {
            s = s.extend()
            if (i > 6) {
                barriers.add(s)
            }
        }

        s = place(Vector2.fromXY(0F, 0.75F), 0F)
        for (i in 0..<iter) {
            s = s.extend()
            if (i > 6) {
                barriers.add(s)
            }
        }


        s = place(Vector2.fromXY(0.75F, 0F), TrigUtil.PI / 2F)
        for (i in 0..<iter) {
            s = s.extend()
            if (i > 6) {
                barriers.add(s)
            }
        }

        s = place(Vector2.fromXY(0.75F, 0.25F), 0F)
        for (i in 0..<iter) {
            s = s.extend()
            if (i > 6) {
                barriers.add(s)
            }
        }

        s = place(Vector2.fromXY(0.75F, 0.75F), TrigUtil.PI / 2F)
        for (i in 0..<iter) {
            s = s.extend()
            if (i > 6) {
                barriers.add(s)
            }
        }

        s = place(Vector2.fromXY(0.75F, 0.75F), 0F)
        for (i in 0..<iter) {
            s = s.extend()
            if (i > 6) {
                barriers.add(s)
            }
        }
    }

    private fun createCenterDiamond() {
        val iter =
            (sqrt((0.5F - 0.25F).toDouble().pow(2.0) + (0.75F - 0.5F).toDouble().pow(2.0)) / Barrier.LENGTH).toInt() - 6

        var s = place(Vector2.fromXY(0.25F, 0.5F), TrigUtil.PI / 4F)
        for (i in 0..<iter) {
            s = s.extend()
            if (i > 6) {
                barriers.add(s)
            }
        }

        s = place(Vector2.fromXY(0.25F, 0.5F), TrigUtil.PI / -4F)
        for (i in 0..<iter) {
            s = s.extend()
            if (i > 6) {
                barriers.add(s)
            }
        }

        s = place(Vector2.fromXY(0.75F, 0.5F), 3 * TrigUtil.PI / 4F)
        for (i in 0..<iter) {
            s = s.extend()
            if (i > 6) {
                barriers.add(s)
            }
        }

        s = place(Vector2.fromXY(0.75F, 0.5F), 3 * TrigUtil.PI / -4F)
        for (i in 0..<iter) {
            s = s.extend()
            if (i > 6) {
                barriers.add(s)
            }
        }
    }

    override fun iterator(): MutableIterator<Barrier> {
        return barriers.iterator()
    }

    fun search(x1: Float, y1: Float, x2: Float, y2: Float, distance: Float): Iterable<Barrier> {
        return factory()
            .get()
            .search(x1, y1, x2, y2, distance)
    }

}
