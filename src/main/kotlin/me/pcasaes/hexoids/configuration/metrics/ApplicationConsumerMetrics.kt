package me.pcasaes.hexoids.configuration.metrics

import jakarta.interceptor.InterceptorBinding
import java.lang.annotation.Inherited

@Inherited
@InterceptorBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
annotation class ApplicationConsumerMetrics 
