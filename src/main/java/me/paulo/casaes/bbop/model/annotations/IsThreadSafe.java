package me.paulo.casaes.bbop.model.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Denotes that a method in the model is thread safe.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface IsThreadSafe {
}
