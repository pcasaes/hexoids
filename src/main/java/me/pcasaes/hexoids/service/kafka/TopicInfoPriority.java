package me.pcasaes.hexoids.service.kafka;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/**
 * Some topics must be caught up before others during startup.
 * This qualifier allows us to do so. Lower value's means higher priority.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TopicInfoPriority {
    TopicInfoPriority.Priority value();

    enum Priority {
        ONE,
        TWO
    }

    class Literal extends AnnotationLiteral<TopicInfoPriority> implements TopicInfoPriority {
        private final Priority value;

        private Literal(Priority value) {
            this.value = value;
        }

        public static Literal of(Priority value) {
            return new Literal(value);
        }

        @Override
        public Priority value() {
            return this.value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Literal literal = (Literal) o;
            return value == literal.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), value);
        }
    }
}
