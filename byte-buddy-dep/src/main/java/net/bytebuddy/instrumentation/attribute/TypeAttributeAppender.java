package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationAppender;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.ClassVisitor;

import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * An appender that writes attributes or annotations to a given ASM {@link org.objectweb.asm.ClassVisitor}.
 */
public interface TypeAttributeAppender {

    /**
     * Applies this type attribute appender.
     *
     * @param classVisitor    The class visitor to which the annotations of this visitor should be written to.
     * @param typeDescription A description of the instrumented type that is target of the ongoing instrumentation.
     */
    void apply(ClassVisitor classVisitor, TypeDescription typeDescription);

    /**
     * A type attribute appender that does not append any attributes.
     */
    static enum NoOp implements TypeAttributeAppender {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            /* do nothing */
        }
    }

    /**
     * An attribute appender that writes all annotations that are found on a given target type to the
     * instrumented type this type attribute appender is applied onto. The visibility for the annotation
     * will be inferred from the annotations' {@link java.lang.annotation.RetentionPolicy}.
     */
    static enum ForSuperType implements TypeAttributeAppender {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            AnnotationAppender annotationAppender =
                    new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor));
            for (Annotation annotation : typeDescription.getSupertype().getAnnotations()) {
                annotationAppender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation));
            }
        }
    }

    /**
     * An attribute appender that appends a single annotation to a given type. The visibility for the annotation
     * will be inferred from the annotation's {@link java.lang.annotation.RetentionPolicy}.
     */
    static class ForAnnotation implements TypeAttributeAppender {

        /**
         * The annotations to write to the given type.
         */
        private final Annotation[] annotation;

        /**
         * Creates a new single annotation attribute appender.
         *
         * @param annotation The annotations to append.
         */
        public ForAnnotation(Annotation... annotation) {
            this.annotation = annotation;
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            AnnotationAppender annotationAppender =
                    new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor));
            for (Annotation annotation : this.annotation) {
                annotationAppender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation));
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && Arrays.equals(annotation, ((ForAnnotation) other).annotation);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(annotation);
        }

        @Override
        public String toString() {
            return "TypeAttributeAppender.ForAnnotation{annotation=" + Arrays.toString(annotation) + '}';
        }
    }

    /**
     * Writes all annotations that are found on a given loaded Java type as visible annotations to the target type.
     */
    static class ForLoadedType implements TypeAttributeAppender {

        /**
         * The class of which the annotations are to be copied.
         */
        private final Class<?> type;

        /**
         * Creates a new attribute appender that writes all annotations found on a given loaded type.
         *
         * @param type The loaded type
         */
        public ForLoadedType(Class<?> type) {
            this.type = type;
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            AnnotationAppender annotationAppender =
                    new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor));
            for (Annotation annotation : type.getAnnotations()) {
                annotationAppender.append(annotation, AnnotationAppender.AnnotationVisibility.RUNTIME);
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && type.equals(((ForLoadedType) other).type);
        }

        @Override
        public int hashCode() {
            return type.hashCode();
        }

        @Override
        public String toString() {
            return "TypeAttributeAppender.ForLoadedType{type=" + type + '}';
        }
    }

    /**
     * A compound type attribute appender that concatenates a number of other attribute appenders.
     */
    static class Compound implements TypeAttributeAppender {

        /**
         * The type attribute appenders this compound appender represents in their application order.
         */
        private final TypeAttributeAppender[] typeAttributeAppender;

        /**
         * Creates a new compound attribute appender.
         *
         * @param typeAttributeAppender The type attribute appenders to concatenate in the order of their application.
         */
        public Compound(TypeAttributeAppender... typeAttributeAppender) {
            this.typeAttributeAppender = typeAttributeAppender;
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            for (TypeAttributeAppender typeAttributeAppender : this.typeAttributeAppender) {
                typeAttributeAppender.apply(classVisitor, typeDescription);
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && Arrays.equals(typeAttributeAppender, ((Compound) other).typeAttributeAppender);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(typeAttributeAppender);
        }

        @Override
        public String toString() {
            return "TypeAttributeAppender.Compound{" + Arrays.toString(typeAttributeAppender) + '}';
        }
    }
}
