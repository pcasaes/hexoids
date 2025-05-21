package me.pcasaes.hexoids.core.domain.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

class OptimisticConcurrentHashMap<K, V> : ConcurrentHashMap<K, V>() {

    override fun computeIfAbsent(key: K, mappingFunction: Function<in K, out V>): V {
        return this[key] ?: super.computeIfAbsent(key, mappingFunction)
    }
}