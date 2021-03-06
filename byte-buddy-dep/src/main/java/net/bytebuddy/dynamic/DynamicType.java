package net.bytebuddy.dynamic;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.dynamic.scaffold.BridgeMethodResolver;
import net.bytebuddy.dynamic.scaffold.FieldRegistry;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.ModifierContributor;
import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.attribute.TypeAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.method.matcher.MethodMatchers;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

/**
 * A dynamic type that is created at runtime, usually as the result of applying a
 * {@link net.bytebuddy.dynamic.DynamicType.Builder} or as the result of an
 * {@link net.bytebuddy.instrumentation.type.auxiliary.AuxiliaryType}.
 * <p>&nbsp;</p>
 * Note that the {@link net.bytebuddy.instrumentation.type.TypeDescription}s will represent their
 * unloaded forms and therefore differ from the loaded types, especially with regards to annotations.
 */
public interface DynamicType {

    /**
     * Returns a description of this dynamic type.
     *
     * @return A description of this dynamic type.
     */
    TypeDescription getDescription();

    /**
     * Returns a byte array representing this dynamic type. This byte array might be reused by this dynamic type and
     * must therefore not be altered.
     *
     * @return A byte array of the type's binary representation.
     */
    byte[] getBytes();

    /**
     * Returns a map of all auxiliary types that are required for making use of the main type.
     *
     * @return A map of all auxiliary types by their descriptions to their binary representation.
     */
    Map<TypeDescription, byte[]> getRawAuxiliaryTypes();

    /**
     * Returns a map of all loaded type initializers for the main type and all auxiliary types, if any.
     *
     * @return A mapping of all types' descriptions to their loaded type initializers.
     */
    Map<TypeDescription, LoadedTypeInitializer> getTypeInitializers();

    /**
     * Checks if a dynamic type requires some form of explicit type initialization, either for itself or for one
     * of its auxiliary types, if any. This is the case when this dynamic type was defined to delegate method calls
     * to a specific instance which is stored in a field of the created type. If this class serialized, it could not
     * be used without its loaded type initializers since the field value represents a specific runtime context.
     *
     * @return {@code true} if this type requires explicit type initialization.
     */
    boolean hasAliveTypeInitializers();

    /**
     * Saves a dynamic type in a given folder using the Java class file format while respecting the naming conventions
     * for saving compiled Java classes. All auxiliary types, if any, are saved in the same directory. The resulting
     * folder structure will resemble the structure that is required for Java runtimes, i.e. each folder representing
     * a segment of the package name.
     *
     * @param folder The base target folder for storing this dynamic type and its auxiliary types, if any.
     * @return A map of type descriptions pointing to files with their stored binary representations within {@code folder}.
     * @throws IOException Thrown if the underlying file operations cause an {@code IOException}.
     */
    Map<TypeDescription, File> saveIn(File folder) throws IOException;

    /**
     * A builder for defining a dynamic type. Implementations of such builders are usually immutable.
     *
     * @param <T> The most specific known loaded type that is implemented by the created dynamic type, usually the
     *            type itself, an interface or the direct super class.
     */
    static interface Builder<T> {

        /**
         * Defines a class file format version for this builder for which the dynamic types should be created.
         *
         * @param classFileVersion The class format version for the dynamic type to implement.
         * @return A builder that writes its classes in a given class format version.
         */
        Builder<T> classFileVersion(ClassFileVersion classFileVersion);

        /**
         * Adds an interface to be implemented the created type.
         *
         * @param interfaceType The interface to implement.
         * @return A builder which will create a dynamic type that implements the given interface.
         */
        OptionalMatchedMethodInterception<T> implement(Class<?>... interfaceType);

        /**
         * Adds an interface to be implemented the created type.
         *
         * @param interfaceType A description of the interface to implement.
         * @return A builder which will create a dynamic type that implements the given interface.
         */
        OptionalMatchedMethodInterception<T> implement(TypeDescription... interfaceType);

        /**
         * Names the currently created dynamic type by a fixed name.
         *
         * @param name A fully qualified name to give to the created dynamic type.
         * @return A builder that will name its dynamic type by the given name.
         */
        Builder<T> name(String name);

        /**
         * Defines modifiers for the created dynamic type.
         *
         * @param modifier A collection of modifiers to be reflected by the created dynamic type.
         * @return A builder that will create a dynamic type that reflects the given modifiers.
         */
        Builder<T> modifiers(ModifierContributor.ForType... modifier);

        /**
         * Defines a matcher for methods that will be ignored for any interception attempt. Any methods
         * that were directly declared on an instrumented type will never be ignored, i.e. ignored methods
         * only represent a filter for methods that are declared in super types that should never be overriden.
         *
         * @param ignoredMethods A method matcher characterizing the methods to be ignored.
         * @return A builder that will always ignore the methods matched by the given method matcher.
         */
        Builder<T> ignoreMethods(MethodMatcher ignoredMethods);

        /**
         * Adds an attribute appender to the currently constructed type which will be applied on the creation of
         * the type.
         *
         * @param attributeAppender An attribute appender to be applied onto the currently created type.
         * @return A builder that will apply the given attribute appender onto the currently created type.
         */
        Builder<T> attribute(TypeAttributeAppender attributeAppender);

        /**
         * Adds annotations to the currently constructed type.
         * <p>&nbsp;</p>
         * Note: The annotations will not be visible to
         * {@link net.bytebuddy.instrumentation.Instrumentation}s.
         *
         * @param annotation The annotations to be added to the currently constructed type.
         * @return A builder that will add the given annotation to the created type.
         */
        Builder<T> annotateType(Annotation... annotation);

        /**
         * Adds an additional ASM {@link org.objectweb.asm.ClassVisitor} to this builder which will be applied in
         * the construction process of this dynamic type.
         *
         * @param classVisitorWrapper The wrapper delegate for the ASM class visitor.
         * @return A builder that will apply the given ASM class visitor.
         */
        Builder<T> classVisitor(ClassVisitorWrapper classVisitorWrapper);

        /**
         * Defines a bridge method resolver factory to be applied to this type creation. A bridge method resolver is
         * responsible for determining the target method that is invoked by a bridge method. This way, a super method
         * invocation is resolved by invoking the actual super method instead of the bridge method which would in turn
         * resolve the actual method virtually.
         *
         * @param bridgeMethodResolverFactory The bridge method resolver factory that is to be used.
         * @return A builder that will apply the given bridge method resolver factory.
         */
        Builder<T> bridgeMethodResolverFactory(BridgeMethodResolver.Factory bridgeMethodResolverFactory);

        /**
         * Defines the use of a specific factory for a {@link net.bytebuddy.instrumentation.method.MethodLookupEngine}.
         *
         * @param methodLookupEngineFactory The factory to be used.
         * @return A builder that applies the given method lookup engine factory.
         */
        Builder<T> methodLookupEngine(MethodLookupEngine.Factory methodLookupEngineFactory);

        /**
         * Defines a new field for this type.
         *
         * @param name      The name of the method.
         * @param fieldType The type of this field where the current type can be represented by
         *                  {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifier  The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        FieldValueTarget<T> defineField(String name,
                                        Class<?> fieldType,
                                        ModifierContributor.ForField... modifier);

        /**
         * Defines a new field for this type.
         *
         * @param name                 The name of the method.
         * @param fieldTypeDescription The type of this field where the current type can be represented by
         *                             {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifier             The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        FieldValueTarget<T> defineField(String name,
                                        TypeDescription fieldTypeDescription,
                                        ModifierContributor.ForField... modifier);

        /**
         * Defines a new method for this type.
         * <p>&nbsp;</p>
         * Note that a method definition will shadow any method of identical signature that was defined in a super
         * class. This implies that the defined method will be treated as if it does not have a super implementation.
         *
         * @param name           The name of the method.
         * @param returnType     The return type of the method  where the current type can be represented by
         *                       {@link net.bytebuddy.dynamic.TargetType}.
         * @param parameterTypes The parameter types of this method  where the current type can be represented by
         *                       {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifier       The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        ExceptionDeclarableMethodInterception<T> defineMethod(String name,
                                                              Class<?> returnType,
                                                              List<Class<?>> parameterTypes,
                                                              ModifierContributor.ForMethod... modifier);

        /**
         * Defines a new method for this type.
         * <p>&nbsp;</p>
         * Note that a method definition will shadow any method of identical signature that was defined in a super
         * class. This implies that the defined method will be treated as if it does not have a super implementation.
         *
         * @param name           The name of the method.
         * @param returnType     A description of the return type of the method  where the current type can be
         *                       represented by {@link net.bytebuddy.dynamic.TargetType}.
         * @param parameterTypes Descriptions of the parameter types of this method  where the current type can be
         *                       represented by {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifier       The modifiers for this method.
         * @return An interception delegate that exclusively matches the new method.
         */
        ExceptionDeclarableMethodInterception<T> defineMethod(String name,
                                                              TypeDescription returnType,
                                                              List<? extends TypeDescription> parameterTypes,
                                                              ModifierContributor.ForMethod... modifier);

        /**
         * Defines a new constructor for this type. A constructor must not be {@code static}. Instead, a static type
         * initializer is added automatically if such an initializer is required. See
         * {@link net.bytebuddy.instrumentation.method.matcher.MethodMatchers#isTypeInitializer()} for a method
         * matcher for the static constructor.
         * <p>&nbsp;</p>
         * Note that a constructor's implementation must call another constructor of the same class or a constructor of
         * its super class. This constructor call must be hardcoded inside of the constructor's method body. Before
         * this constructor call is made, it is not legal to call any methods or to read any fields of the instance
         * under construction.
         *
         * @param parameterTypes The parameter types of this constructor where the current type can be represented by
         *                       {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifier       The modifiers for this constructor.
         * @return An interception delegate that exclusively matches the new constructor.
         */
        ExceptionDeclarableMethodInterception<T> defineConstructor(Iterable<Class<?>> parameterTypes,
                                                                   ModifierContributor.ForMethod... modifier);

        /**
         * Defines a new constructor for this type. A constructor must not be {@code static}. Instead, a static type
         * initializer is added automatically if such an initializer is required. See
         * {@link net.bytebuddy.instrumentation.method.matcher.MethodMatchers#isTypeInitializer()} for a method
         * matcher for the static constructor.
         * <p>&nbsp;</p>
         * Note that a constructor's implementation must call another constructor of the same class or a constructor of
         * its super class. This constructor call must be hardcoded inside of the constructor's method body. Before
         * this constructor call is made, it is not legal to call any methods or to read any fields of the instance
         * under construction.
         *
         * @param parameterTypes The descriptions of the parameter types of this constructor where the current type can be
         *                       represented by a description of {@link net.bytebuddy.dynamic.TargetType}.
         * @param modifier       The modifiers for this constructor.
         * @return An interception delegate that exclusively matches the new constructor.
         * @see net.bytebuddy.instrumentation.method.matcher.MethodMatchers#isTypeInitializer()
         */
        ExceptionDeclarableMethodInterception<T> defineConstructor(List<? extends TypeDescription> parameterTypes,
                                                                   ModifierContributor.ForMethod... modifier);

        /**
         * Selects a set of methods of this type for instrumentation.
         *
         * @param methodMatcher A matcher describing the methods to be intercepted by this instrumentation.
         * @return An interception delegate for methods matching the given method matcher.
         */
        MatchedMethodInterception<T> method(MethodMatcher methodMatcher);

        /**
         * Selects a set of constructors of this type for instrumentation.
         *
         * @param methodMatcher A matcher describing the constructors to be intercepted by this instrumentation.
         * @return An interception delegate for constructors matching the given method matcher.
         */
        MatchedMethodInterception<T> constructor(MethodMatcher methodMatcher);

        /**
         * Selects a set of byte code methods of this type for instrumentation.
         *
         * @param methodMatcher A matcher describing the byte code methods to be intercepted by this instrumentation.
         * @return An interception delegate for byte code methods matching the given method matcher.
         */
        MatchedMethodInterception<T> invokable(MethodMatcher methodMatcher);

        /**
         * Creates the dynamic type without loading it.
         *
         * @return An unloaded representation of the dynamic type.
         */
        Unloaded<T> make();

        /**
         * Defines an instrumentation for a method that was added to this instrumentation or a to method selection
         * of existing methods.
         *
         * @param <T> The most specific known loaded type that is implemented by the created dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        static interface MatchedMethodInterception<T> {

            /**
             * Intercepts the currently selected method by a given instrumentation.
             *
             * @param instrumentation An instrumentation to apply to the currently selected method.
             * @return A builder which will intercept the currently selected methods by the given instrumentation.
             */
            MethodAnnotationTarget<T> intercept(Instrumentation instrumentation);

            /**
             * Implements the currently selected methods as {@code abstract} methods.
             *
             * @return A builder which will implement the currently selected methods as {@code abstract} methods.
             */
            MethodAnnotationTarget<T> withoutCode();
        }

        /**
         * Defines an instrumentation for a method that was added to this instrumentation and allows to include
         * exception declarations for the newly defined method.
         *
         * @param <S> The most specific known loaded type that is implemented by the created dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        static interface ExceptionDeclarableMethodInterception<S> extends MatchedMethodInterception<S> {

            /**
             * Defines a number of {@link java.lang.Throwable} types to be include in the exception declaration.
             *
             * @param type The types that should be declared to be thrown by the selected method.
             * @return A target for instrumenting the defined method where the method will declare the given exception
             * types.
             */
            MatchedMethodInterception<S> throwing(Class<?>... type);

            /**
             * Defines a number of {@link java.lang.Throwable} types to be include in the exception declaration.
             *
             * @param type Descriptions of the types that should be declared to be thrown by the selected method.
             * @return A target for instrumenting the defined method where the method will declare the given exception
             * types.
             */
            MatchedMethodInterception<S> throwing(TypeDescription... type);
        }

        /**
         * An optional matched method interception allows to define an interception without requiring the definition
         * of an implementation.
         *
         * @param <S> The most specific known loaded type that is implemented by the created dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        static interface OptionalMatchedMethodInterception<S> extends MatchedMethodInterception<S>, Builder<S> {
            /* This interface is merely a combinator of the matched method interception and the builder interfaces. */
        }

        /**
         * A builder to which a method was just added or an interception for existing methods was specified such that
         * attribute changes can be applied to these methods.
         *
         * @param <S> The most specific known loaded type that is implemented by the created dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        static interface MethodAnnotationTarget<S> extends Builder<S> {

            /**
             * Defines an attribute appender factory to be applied onto the currently selected methods.
             *
             * @param attributeAppenderFactory The attribute appender factory to apply onto the currently selected
             *                                 methods.
             * @return A builder where the given attribute appender factory will be applied to the currently selected methods.
             */
            MethodAnnotationTarget<S> attribute(MethodAttributeAppender.Factory attributeAppenderFactory);

            /**
             * Defines annotations to be added to the currently selected method.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to
             * {@link net.bytebuddy.instrumentation.Instrumentation}s.
             *
             * @param annotation The annotations to add to the currently selected methods.
             * @return A builder where the given annotation will be added to the currently selected methods.
             */
            MethodAnnotationTarget<S> annotateMethod(Annotation... annotation);

            /**
             * Defines annotations to be added to a parameter of the currently selected methods.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to
             * {@link net.bytebuddy.instrumentation.Instrumentation}s.
             *
             * @param parameterIndex The index of the parameter to annotate.
             * @param annotation     The annotations to add to a parameter of the currently selected methods.
             * @return A builder where the given annotation will be added to a parameter of the currently selected
             * methods.
             */
            MethodAnnotationTarget<S> annotateParameter(int parameterIndex, Annotation... annotation);
        }

        /**
         * A builder to which a field was just added such that default values can be defined for the field. Default
         * values must only be defined for {@code static} fields of a primitive type or of the {@link java.lang.String}
         * type.
         *
         * @param <S> The most specific known type of the dynamic type, usually the type itself, an interface or the
         *            direct super class.
         */
        static interface FieldValueTarget<S> extends FieldAnnotationTarget<S> {

            /**
             * Defines a {@code boolean} value to become the optional default value for the recently defined
             * {@code static} field. Defining such a boolean default value is only legal for fields that are
             * represented as an integer within the Java virtual machine. These types are the {@code boolean} type,
             * the {@code byte} type, the {@code short} type, the {@code char} type and the {@code int} type.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            FieldAnnotationTarget<S> value(boolean value);

            /**
             * Defines an {@code int} value to be become the optional default value for the recently defined
             * {@code static} field. Defining such an integer default value is only legal for fields that are
             * represented as an integer within the Java virtual machine. These types are the {@code boolean} type,
             * the {@code byte} type, the {@code short} type, the {@code char} type and the {@code int} type. By
             * extension, integer types can also be defined for {@code long} types and are automatically converted.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            FieldAnnotationTarget<S> value(int value);

            /**
             * Defined a default value for a {@code long}-typed {@code static} field. This is only legal if the
             * defined field is also of type {@code long}.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            FieldAnnotationTarget<S> value(long value);

            /**
             * Defined a default value for a {@code float}-typed {@code static} field. This is only legal if the
             * defined field is also of type {@code float}.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            FieldAnnotationTarget<S> value(float value);

            /**
             * Defined a default value for a {@code double}-typed {@code static} field. This is only legal if the
             * defined field is also of type {@code double}.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            FieldAnnotationTarget<S> value(double value);

            /**
             * Defined a default value for a {@link java.lang.String}-typed {@code static} field. This is only legal if
             * the defined field is also of type {@link java.lang.String}. The string must not be {@code null}.
             *
             * @param value The value to be defined as a default value for the recently defined field.
             * @return A field annotation target for the currently defined field.
             */
            FieldAnnotationTarget<S> value(String value);

            /**
             * A validator for assuring that a given value can be represented by a given primitive type.
             */
            static enum NumericRangeValidator {

                /**
                 * A validator for {@code boolean} values.
                 */
                BOOLEAN(0, 1),

                /**
                 * A validator for {@code byte} values.
                 */
                BYTE(Byte.MIN_VALUE, Byte.MAX_VALUE),

                /**
                 * A validator for {@code short} values.
                 */
                SHORT(Short.MIN_VALUE, Short.MAX_VALUE),

                /**
                 * A validator for {@code char} values.
                 */
                CHARACTER(Character.MIN_VALUE, Character.MAX_VALUE),

                /**
                 * A validator for {@code int} values.
                 */
                INTEGER(Integer.MIN_VALUE, Integer.MAX_VALUE),

                /**
                 * A validator for {@code long} values.
                 */
                LONG(Integer.MIN_VALUE, Integer.MAX_VALUE) {
                    @Override
                    public Object validate(int value) {
                        return (long) value;
                    }
                };
                /**
                 * The minimum and maximum values for an {@code int} value for the represented primitive value.
                 */
                private final int minimum, maximum;

                /**
                 * Creates a new numeric range validator.
                 *
                 * @param minimum The minimum {@code int} value that can be represented by this primitive type.
                 * @param maximum The maximum {@code int} value that can be represented by this primitive type.
                 */
                private NumericRangeValidator(int minimum, int maximum) {
                    this.minimum = minimum;
                    this.maximum = maximum;
                }

                /**
                 * Identifies the correct validator for a given type description.
                 *
                 * @param typeDescription The type of a field for which a default value should be validated.
                 * @return The corresponding numeric range validator.
                 */
                public static NumericRangeValidator of(TypeDescription typeDescription) {
                    if (typeDescription.represents(boolean.class)) {
                        return BOOLEAN;
                    } else if (typeDescription.represents(byte.class)) {
                        return BYTE;
                    } else if (typeDescription.represents(short.class)) {
                        return SHORT;
                    } else if (typeDescription.represents(char.class)) {
                        return CHARACTER;
                    } else if (typeDescription.represents(int.class)) {
                        return INTEGER;
                    } else if (typeDescription.represents(long.class)) {
                        return LONG;
                    } else {
                        throw new IllegalStateException(String.format("A field of type %s does not permit an " +
                                "integer-typed default value", typeDescription));
                    }
                }

                /**
                 * Validates and wraps a given {@code int} value for the represented numeric range.
                 *
                 * @param value The value to be validated for a given numeric range.
                 * @return The wrapped value after validation.
                 */
                public Object validate(int value) {
                    if (value < minimum || value > maximum) {
                        throw new IllegalArgumentException(String.format("The value %d overflows for %s", value, this));
                    }
                    return value;
                }
            }
        }

        /**
         * A builder to which a field was just added such that attribute changes can be applied to this field.
         *
         * @param <S> The most specific known type of the dynamic type, usually the type itself, an interface or the
         *            direct super class.
         */
        static interface FieldAnnotationTarget<S> extends Builder<S> {

            /**
             * Defines an attribute appender factory to be applied onto the currently selected field.
             *
             * @param attributeAppenderFactory The attribute appender factory to apply onto the currently selected
             *                                 field.
             * @return A builder where the given attribute appender factory will be applied to the currently selected field.
             */
            FieldAnnotationTarget<S> attribute(FieldAttributeAppender.Factory attributeAppenderFactory);

            /**
             * Defines annotations to be added to the currently selected field.
             * <p>&nbsp;</p>
             * Note: The annotations will not be visible to
             * {@link net.bytebuddy.instrumentation.Instrumentation}s.
             *
             * @param annotation The annotations to add to the currently selected field.
             * @return A builder where the given annotation will be added to the currently selected field.
             */
            FieldAnnotationTarget<S> annotateField(Annotation... annotation);
        }

        /**
         * An abstract base implementation for a dynamic type builder. For representing the built type, the
         * {@link net.bytebuddy.dynamic.TargetType} class can be used as a placeholder.
         *
         * @param <S> The most specific known loaded type that is implemented by the created dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        abstract static class AbstractBase<S> implements Builder<S> {

            /**
             * This builder's currently registered field tokens.
             */
            protected final List<FieldToken> fieldTokens;

            /**
             * This builder's currently registered method tokens.
             */
            protected final List<MethodToken> methodTokens;

            /**
             * Creates a new builder for a dynamic type.
             *
             * @param fieldTokens  A list of fields registered for this builder.
             * @param methodTokens A list of methods registered for this builder.
             */
            protected AbstractBase(List<FieldToken> fieldTokens, List<MethodToken> methodTokens) {
                this.fieldTokens = fieldTokens;
                this.methodTokens = methodTokens;
            }

            /**
             * If a type is represented by a {@link net.bytebuddy.dynamic.TargetType} placeholder, this method
             * substitutes the type by the instrumented type.
             *
             * @param type             The type to consider for substitution.
             * @param instrumentedType The instrumented type which the type might be substituted against.
             * @return The actually represented type.
             */
            private static TypeDescription considerSubstitution(TypeDescription type, TypeDescription instrumentedType) {
                return type.represents(TargetType.class) ? instrumentedType : type;
            }

            /**
             * Adds all fields and methods to an instrumented type.
             *
             * @param instrumentedType The instrumented type that is basis of the alteration.
             * @return The created instrumented type with all fields and methods applied.
             */
            protected InstrumentedType applyRecordedMembersTo(InstrumentedType instrumentedType) {
                for (FieldToken fieldToken : fieldTokens) {
                    instrumentedType = instrumentedType.withField(fieldToken.name,
                            fieldToken.resolveFieldType(instrumentedType),
                            fieldToken.modifiers);
                }
                for (MethodToken methodToken : methodTokens) {
                    instrumentedType = instrumentedType.withMethod(methodToken.internalName,
                            methodToken.resolveReturnType(instrumentedType),
                            methodToken.resolveParameterTypes(instrumentedType),
                            methodToken.resolveExceptionTypes(),
                            methodToken.modifiers);
                }
                return instrumentedType;
            }

            @Override
            public OptionalMatchedMethodInterception<S> implement(Class<?>... interfaceType) {
                TypeDescription[] typeDescription = new TypeDescription[interfaceType.length];
                int index = 0;
                for (Class<?> type : interfaceType) {
                    typeDescription[index++] = new TypeDescription.ForLoadedType(type);
                }
                return implement(typeDescription);
            }

            @Override
            public FieldValueTarget<S> defineField(String name,
                                                   Class<?> fieldType,
                                                   ModifierContributor.ForField... modifier) {
                return defineField(name, new TypeDescription.ForLoadedType(fieldType), modifier);
            }

            @Override
            public ExceptionDeclarableMethodInterception<S> defineMethod(String name,
                                                                         Class<?> returnType,
                                                                         List<Class<?>> parameterTypes,
                                                                         ModifierContributor.ForMethod... modifier) {
                return defineMethod(name,
                        new TypeDescription.ForLoadedType(returnType),
                        new TypeList.ForLoadedType(parameterTypes),
                        modifier);
            }

            @Override
            public ExceptionDeclarableMethodInterception<S> defineConstructor(Iterable<Class<?>> parameterTypes,
                                                                              ModifierContributor.ForMethod... modifier) {
                List<Class<?>> parameterTypesList;
                if (parameterTypes instanceof List) {
                    parameterTypesList = (List<Class<?>>) parameterTypes;
                } else {
                    parameterTypesList = new ArrayList<Class<?>>();
                    for (Class<?> parameterType : parameterTypes) {
                        parameterTypesList.add(parameterType);
                    }
                }
                return defineConstructor(new TypeList.ForLoadedType(parameterTypesList), modifier);
            }

            /**
             * A method token representing a latent method that is defined for the built dynamic type.
             */
            protected static class MethodToken implements MethodRegistry.LatentMethodMatcher {

                /**
                 * The internal name of the method.
                 */
                protected final String internalName;

                /**
                 * A description of the return type of the method or a type describing the
                 * {@link net.bytebuddy.dynamic.TargetType} placeholder.
                 */
                protected final TypeDescription returnType;

                /**
                 * A list of parameter type descriptions for the method which might be represented by the
                 * {@link net.bytebuddy.dynamic.TargetType} placeholder.
                 */
                protected final List<TypeDescription> parameterTypes;

                /**
                 * A list of exception type descriptions for the method.
                 */
                protected final List<TypeDescription> exceptionTypes;

                /**
                 * The modifiers of the method.
                 */
                protected final int modifiers;

                /**
                 * Creates a new method token representing a constructor to implement for the built dynamic type.
                 *
                 * @param parameterTypes A list of parameters for the constructor.
                 * @param exceptionTypes A list of exception types that are declared for the constructor.
                 * @param modifiers      The modifiers of the constructor.
                 */
                public MethodToken(List<? extends TypeDescription> parameterTypes,
                                   List<? extends TypeDescription> exceptionTypes,
                                   int modifiers) {
                    this(MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                            new TypeDescription.ForLoadedType(void.class),
                            parameterTypes,
                            exceptionTypes,
                            modifiers);
                }

                /**
                 * Creates a new method token representing a method to implement for the built dynamic type.
                 *
                 * @param internalName   The internal name of the method.
                 * @param returnType     The return type of the method.
                 * @param parameterTypes A list of parameters for the method.
                 * @param exceptionTypes A list of exception types that are declared for the method.
                 * @param modifiers      The modifiers of the method.
                 */
                public MethodToken(String internalName,
                                   TypeDescription returnType,
                                   List<? extends TypeDescription> parameterTypes,
                                   List<? extends TypeDescription> exceptionTypes,
                                   int modifiers) {
                    this.internalName = internalName;
                    this.returnType = returnType;
                    this.parameterTypes = Collections.unmodifiableList(new ArrayList<TypeDescription>(parameterTypes));
                    this.exceptionTypes = Collections.unmodifiableList(new ArrayList<TypeDescription>(exceptionTypes));
                    this.modifiers = modifiers;
                }

                @Override
                public MethodMatcher manifest(TypeDescription instrumentedType) {
                    return MethodDescription.CONSTRUCTOR_INTERNAL_NAME.equals(internalName) ? isConstructor() : named(internalName)
                            .and(MethodMatchers.returns(resolveReturnType(instrumentedType)))
                            .and(takesArguments(resolveParameterTypes(instrumentedType)));
                }

                /**
                 * Resolves the return type for the method which could be represented by the
                 * {@link net.bytebuddy.dynamic.TargetType} placeholder type.
                 *
                 * @param instrumentedType The instrumented place which is used for replacement.
                 * @return A type description for the actual return type.
                 */
                protected TypeDescription resolveReturnType(TypeDescription instrumentedType) {
                    return considerSubstitution(returnType, instrumentedType);
                }

                /**
                 * Resolves the parameter types for the method which could be represented by the
                 * {@link net.bytebuddy.dynamic.TargetType} placeholder type.
                 *
                 * @param instrumentedType The instrumented place which is used for replacement.
                 * @return A list of type descriptions for the actual parameter types.
                 */
                protected List<TypeDescription> resolveParameterTypes(TypeDescription instrumentedType) {
                    List<TypeDescription> parameterTypes = new ArrayList<TypeDescription>(this.parameterTypes.size());
                    for (TypeDescription parameterType : this.parameterTypes) {
                        parameterTypes.add(considerSubstitution(parameterType, instrumentedType));
                    }
                    return parameterTypes;
                }

                /**
                 * Resolves the declared exception types for the method.
                 *
                 * @return A list of type descriptions for the actual exception types.
                 */
                protected List<TypeDescription> resolveExceptionTypes() {
                    return this.exceptionTypes;
                }

                /**
                 * Returns the internal name of this method token.
                 *
                 * @return The internal name of this method token.
                 */
                public String getInternalName() {
                    return internalName;
                }

                /**
                 * Returns a description of the return type of this method token.
                 *
                 * @return A description of the return type of this method token.
                 */
                public TypeDescription getReturnType() {
                    return returnType;
                }

                /**
                 * Returns a list of descriptions of the parameter types of this method token.
                 *
                 * @return A list of descriptions of the parameter types of this method token.
                 */
                public List<TypeDescription> getParameterTypes() {
                    return parameterTypes;
                }

                /**
                 * Returns a list of exception types of this method token.
                 *
                 * @return A list of exception types of this method token.
                 */
                public List<TypeDescription> getExceptionTypes() {
                    return exceptionTypes;
                }

                /**
                 * Returns the modifiers for this method token.
                 *
                 * @return The modifiers for this method token.
                 */
                public int getModifiers() {
                    return modifiers;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && internalName.equals(((MethodToken) other).internalName)
                            && parameterTypes.equals(((MethodToken) other).parameterTypes)
                            && returnType.equals(((MethodToken) other).returnType);
                }

                @Override
                public int hashCode() {
                    int result = internalName.hashCode();
                    result = 31 * result + returnType.hashCode();
                    result = 31 * result + parameterTypes.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "MethodToken{" +
                            "internalName='" + internalName + '\'' +
                            ", returnType=" + returnType +
                            ", parameterTypes=" + parameterTypes +
                            ", exceptionTypes=" + exceptionTypes +
                            ", modifiers=" + modifiers + '}';
                }
            }

            /**
             * A field token representing a latent field that is defined for the built dynamic type.
             */
            protected static class FieldToken implements FieldRegistry.LatentFieldMatcher {

                /**
                 * The name of the field.
                 */
                protected final String name;

                /**
                 * A description of the field type or a description of the
                 * {@link net.bytebuddy.dynamic.TargetType} placeholder.
                 */
                protected final TypeDescription fieldType;

                /**
                 * The field modifiers.
                 */
                protected final int modifiers;

                /**
                 * Creates a new field token.
                 *
                 * @param name      The name of the field.
                 * @param fieldType A description of the field type.
                 * @param modifiers The modifers of the field.
                 */
                public FieldToken(String name, TypeDescription fieldType, int modifiers) {
                    this.name = name;
                    this.fieldType = fieldType;
                    this.modifiers = modifiers;
                }

                /**
                 * Resolves the field type which could be represented by the
                 * {@link net.bytebuddy.dynamic.TargetType} placeholder type.
                 *
                 * @param instrumentedType The instrumented place which is used for replacement.
                 * @return A type description for the actual field type.
                 */
                protected TypeDescription resolveFieldType(TypeDescription instrumentedType) {
                    return considerSubstitution(fieldType, instrumentedType);
                }

                /**
                 * Returns the name of this field token.
                 *
                 * @return The name of this field token.
                 */
                public String getName() {
                    return name;
                }

                /**
                 * Returns the type of this field token.
                 *
                 * @return The type of this field token.
                 */
                public TypeDescription getFieldType() {
                    return fieldType;
                }

                /**
                 * Returns the modifiers of this field token.
                 *
                 * @return The modifiers of this field token.
                 */
                public int getModifiers() {
                    return modifiers;
                }

                @Override
                public String getFieldName() {
                    return name;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && name.equals(((FieldToken) other).name);
                }

                @Override
                public int hashCode() {
                    return name.hashCode();
                }

                @Override
                public String toString() {
                    return "FieldToken{" +
                            "internalName='" + name + '\'' +
                            ", fieldType=" + fieldType +
                            ", modifiers=" + modifiers + '}';
                }
            }

            /**
             * A base implementation of a builder that is capable of manifesting a change that was not yet applied to
             * the builder.
             *
             * @param <U> The most specific known loaded type that is implemented by the created dynamic type, usually the
             *            type itself, an interface or the direct super class.
             */
            protected abstract class AbstractDelegatingBuilder<U> implements Builder<U> {

                @Override
                public Builder<U> classFileVersion(ClassFileVersion classFileVersion) {
                    return materialize().classFileVersion(classFileVersion);
                }

                @Override
                public OptionalMatchedMethodInterception<U> implement(Class<?>... interfaceType) {
                    return materialize().implement(interfaceType);
                }

                @Override
                public OptionalMatchedMethodInterception<U> implement(TypeDescription... interfaceType) {
                    return materialize().implement(interfaceType);
                }

                @Override
                public Builder<U> name(String name) {
                    return materialize().name(name);
                }

                @Override
                public Builder<U> modifiers(ModifierContributor.ForType... modifier) {
                    return materialize().modifiers(modifier);
                }

                @Override
                public Builder<U> ignoreMethods(MethodMatcher ignoredMethods) {
                    return materialize().ignoreMethods(ignoredMethods);
                }

                @Override
                public Builder<U> attribute(TypeAttributeAppender attributeAppender) {
                    return materialize().attribute(attributeAppender);
                }

                @Override
                public Builder<U> annotateType(Annotation... annotation) {
                    return materialize().annotateType(annotation);
                }

                @Override
                public Builder<U> classVisitor(ClassVisitorWrapper classVisitorWrapper) {
                    return materialize().classVisitor(classVisitorWrapper);
                }

                @Override
                public Builder<U> methodLookupEngine(MethodLookupEngine.Factory methodLookupEngineFactory) {
                    return materialize().methodLookupEngine(methodLookupEngineFactory);
                }

                @Override
                public Builder<U> bridgeMethodResolverFactory(BridgeMethodResolver.Factory bridgeMethodResolverFactory) {
                    return materialize().bridgeMethodResolverFactory(bridgeMethodResolverFactory);
                }

                @Override
                public FieldValueTarget<U> defineField(String name,
                                                       Class<?> fieldType,
                                                       ModifierContributor.ForField... modifier) {
                    return materialize().defineField(name, fieldType, modifier);
                }

                @Override
                public FieldValueTarget<U> defineField(String name,
                                                       TypeDescription fieldTypeDescription,
                                                       ModifierContributor.ForField... modifier) {
                    return materialize().defineField(name, fieldTypeDescription, modifier);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> defineMethod(String name,
                                                                             Class<?> returnType,
                                                                             List<Class<?>> parameterTypes,
                                                                             ModifierContributor.ForMethod... modifier) {
                    return materialize().defineMethod(name, returnType, parameterTypes, modifier);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> defineMethod(String name,
                                                                             TypeDescription returnType,
                                                                             List<? extends TypeDescription> parameterTypes,
                                                                             ModifierContributor.ForMethod... modifier) {
                    return materialize().defineMethod(name, returnType, parameterTypes, modifier);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> defineConstructor(Iterable<Class<?>> parameterTypes,
                                                                                  ModifierContributor.ForMethod... modifier) {
                    return materialize().defineConstructor(parameterTypes, modifier);
                }

                @Override
                public ExceptionDeclarableMethodInterception<U> defineConstructor(List<? extends TypeDescription> parameterTypes,
                                                                                  ModifierContributor.ForMethod... modifier) {
                    return materialize().defineConstructor(parameterTypes, modifier);
                }

                @Override
                public MatchedMethodInterception<U> method(MethodMatcher methodMatcher) {
                    return materialize().method(methodMatcher);
                }

                @Override
                public MatchedMethodInterception<U> constructor(MethodMatcher methodMatcher) {
                    return materialize().constructor(methodMatcher);
                }

                @Override
                public MatchedMethodInterception<U> invokable(MethodMatcher methodMatcher) {
                    return materialize().invokable(methodMatcher);
                }

                @Override
                public Unloaded<U> make() {
                    return materialize().make();
                }

                /**
                 * Materializes the current state of the build before applying another modification.
                 *
                 * @return A builder with all pending changes materialized.
                 */
                protected abstract Builder<U> materialize();
            }
        }
    }

    /**
     * A dynamic type that has been loaded into the running instance of the Java virtual machine.
     *
     * @param <T> The most specific known loaded type that is implemented by this dynamic type, usually the
     *            type itself, an interface or the direct super class.
     */
    static interface Loaded<T> extends DynamicType {

        /**
         * Returns the loaded main class.
         *
         * @return A loaded class representation of this dynamic type.
         */
        Class<? extends T> getLoaded();

        /**
         * Returns a map of all loaded auxiliary types to this dynamic type.
         *
         * @return A mapping from the fully qualified names of all auxiliary types to their loaded class representations.
         */
        Map<TypeDescription, Class<?>> getLoadedAuxiliaryTypes();
    }

    /**
     * A dynamic type that has not yet been loaded by a given {@link java.lang.ClassLoader}.
     *
     * @param <T> The most specific known loaded type that is implemented by this dynamic type, usually the
     *            type itself, an interface or the direct super class.
     */
    static interface Unloaded<T> extends DynamicType {

        /**
         * Attempts to load this dynamic type including all of its auxiliary types, if any.
         *
         * @param classLoader          The class loader to use for this class loading.
         * @param classLoadingStrategy The class loader strategy which should be used for this class loading.
         * @return This dynamic type in its loaded state.
         * @see net.bytebuddy.dynamic.ClassLoadingStrategy.Default
         */
        Loaded<T> load(ClassLoader classLoader, ClassLoadingStrategy classLoadingStrategy);
    }

    /**
     * A default implementation of a dynamic type.
     */
    static class Default implements DynamicType {

        /**
         * The file name extension for Java class files.
         */
        private static final String CLASS_FILE_EXTENSION = ".class";

        /**
         * A type description of this dynamic type.
         */
        protected final TypeDescription typeDescription;

        /**
         * The byte array representing this dynamic type.
         */
        protected final byte[] binaryRepresentation;

        /**
         * The loaded type initializer for this dynamic type.
         */
        protected final LoadedTypeInitializer loadedTypeInitializer;

        /**
         * A list of auxiliary types for this dynamic type.
         */
        protected final List<? extends DynamicType> auxiliaryTypes;

        /**
         * Creates a new dynamic type.
         *
         * @param typeDescription       A description of this dynamic type.
         * @param binaryRepresentation  A byte array containing the binary representation of this dynamic type.
         * @param loadedTypeInitializer The loaded type initializer of this dynamic type.
         * @param auxiliaryTypes        The auxiliary type required for this dynamic type.
         */
        public Default(TypeDescription typeDescription,
                       byte[] binaryRepresentation,
                       LoadedTypeInitializer loadedTypeInitializer,
                       List<? extends DynamicType> auxiliaryTypes) {
            this.typeDescription = typeDescription;
            this.binaryRepresentation = binaryRepresentation;
            this.loadedTypeInitializer = loadedTypeInitializer;
            this.auxiliaryTypes = auxiliaryTypes;
        }

        @Override
        public TypeDescription getDescription() {
            return typeDescription;
        }

        @Override
        public Map<TypeDescription, LoadedTypeInitializer> getTypeInitializers() {
            Map<TypeDescription, LoadedTypeInitializer> classLoadingCallbacks = new HashMap<TypeDescription, LoadedTypeInitializer>();
            for (DynamicType auxiliaryType : auxiliaryTypes) {
                classLoadingCallbacks.putAll(auxiliaryType.getTypeInitializers());
            }
            classLoadingCallbacks.put(typeDescription, loadedTypeInitializer);
            return classLoadingCallbacks;
        }

        @Override
        public boolean hasAliveTypeInitializers() {
            for (LoadedTypeInitializer loadedTypeInitializer : getTypeInitializers().values()) {
                if (loadedTypeInitializer.isAlive()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public byte[] getBytes() {
            return binaryRepresentation;
        }

        @Override
        public Map<TypeDescription, byte[]> getRawAuxiliaryTypes() {
            Map<TypeDescription, byte[]> auxiliaryTypes = new HashMap<TypeDescription, byte[]>();
            for (DynamicType auxiliaryType : this.auxiliaryTypes) {
                auxiliaryTypes.put(auxiliaryType.getDescription(), auxiliaryType.getBytes());
                auxiliaryTypes.putAll(auxiliaryType.getRawAuxiliaryTypes());
            }
            return auxiliaryTypes;
        }

        @Override
        public Map<TypeDescription, File> saveIn(File folder) throws IOException {
            Map<TypeDescription, File> savedFiles = new HashMap<TypeDescription, File>();
            File target = new File(folder, typeDescription.getName().replace('.', File.separatorChar) + CLASS_FILE_EXTENSION);
            FileOutputStream fileOutputStream = new FileOutputStream(target);
            try {
                fileOutputStream.write(binaryRepresentation);
            } finally {
                fileOutputStream.close();
            }
            savedFiles.put(typeDescription, target);
            for (DynamicType auxiliaryType : auxiliaryTypes) {
                savedFiles.putAll(auxiliaryType.saveIn(folder));
            }
            return savedFiles;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Default aDefault = (Default) other;
            return auxiliaryTypes.equals(aDefault.auxiliaryTypes)
                    && Arrays.equals(binaryRepresentation, aDefault.binaryRepresentation)
                    && typeDescription.equals(aDefault.typeDescription)
                    && loadedTypeInitializer.equals(aDefault.loadedTypeInitializer);

        }

        @Override
        public int hashCode() {
            int result = typeDescription.hashCode();
            result = 31 * result + Arrays.hashCode(binaryRepresentation);
            result = 31 * result + loadedTypeInitializer.hashCode();
            result = 31 * result + auxiliaryTypes.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "DynamicType.Default{" +
                    "typeDescription='" + typeDescription + '\'' +
                    ", binaryRepresentation=" + Arrays.toString(binaryRepresentation) +
                    ", typeInitializer=" + loadedTypeInitializer +
                    ", auxiliaryTypes=" + auxiliaryTypes +
                    '}';
        }

        /**
         * A default implementation of an unloaded dynamic type.
         *
         * @param <T> The most specific known loaded type that is implemented by this dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        public static class Unloaded<T> extends Default implements DynamicType.Unloaded<T> {

            /**
             * Creates a new unloaded representation of a dynamic type.
             *
             * @param typeDescription       A description of this dynamic type.
             * @param typeByte              An array of byte of the binary representation of this dynamic type.
             * @param loadedTypeInitializer The type initializer of this dynamic type.
             * @param auxiliaryTypes        The auxiliary types that are required for this dynamic type.
             */
            public Unloaded(TypeDescription typeDescription,
                            byte[] typeByte,
                            LoadedTypeInitializer loadedTypeInitializer,
                            List<? extends DynamicType> auxiliaryTypes) {
                super(typeDescription, typeByte, loadedTypeInitializer, auxiliaryTypes);
            }

            @Override
            public DynamicType.Loaded<T> load(ClassLoader classLoader, ClassLoadingStrategy classLoadingStrategy) {
                LinkedHashMap<TypeDescription, byte[]> types = new LinkedHashMap<TypeDescription, byte[]>(getRawAuxiliaryTypes());
                types.put(typeDescription, binaryRepresentation);
                return new Default.Loaded<T>(typeDescription,
                        binaryRepresentation,
                        loadedTypeInitializer,
                        auxiliaryTypes,
                        initialize(classLoadingStrategy.load(classLoader, types)));
            }

            /**
             * Runs all loaded type initializers for all loaded classes.
             *
             * @param uninitialized The uninitialized loaded classes mapped by their type description.
             * @return A new hash map that contains the same classes as those given.
             */
            private Map<TypeDescription, Class<?>> initialize(Map<TypeDescription, Class<?>> uninitialized) {
                Map<TypeDescription, LoadedTypeInitializer> typeInitializers = getTypeInitializers();
                for (Map.Entry<TypeDescription, Class<?>> entry : uninitialized.entrySet()) {
                    typeInitializers.get(entry.getKey()).onLoad(entry.getValue());
                }
                return new HashMap<TypeDescription, Class<?>>(uninitialized);
            }

            @Override
            public String toString() {
                return "DynamicType.Default.Unloaded{" +
                        "typeDescription='" + typeDescription + '\'' +
                        ", binaryRepresentation=" + Arrays.toString(binaryRepresentation) +
                        ", typeInitializer=" + loadedTypeInitializer +
                        ", auxiliaryTypes=" + auxiliaryTypes +
                        '}';
            }
        }

        /**
         * A default implementation of a loaded dynamic type.
         *
         * @param <T> The most specific known loaded type that is implemented by this dynamic type, usually the
         *            type itself, an interface or the direct super class.
         */
        protected static class Loaded<T> extends Default implements DynamicType.Loaded<T> {

            /**
             * The loaded types for the given loaded dynamic type.
             */
            private final Map<TypeDescription, Class<?>> loadedTypes;

            /**
             * Creates a new representation of a loaded dynamic type.
             *
             * @param typeDescription       A description of this dynamic type.
             * @param typeByte              An array of byte of the binary representation of this dynamic type.
             * @param loadedTypeInitializer The type initializer of this dynamic type.
             * @param auxiliaryTypes        The auxiliary types that are required for this dynamic type.
             * @param loadedTypes           A map of loaded types for this dynamic type and all its auxiliary types.
             */
            protected Loaded(TypeDescription typeDescription,
                             byte[] typeByte,
                             LoadedTypeInitializer loadedTypeInitializer,
                             List<? extends DynamicType> auxiliaryTypes,
                             Map<TypeDescription, Class<?>> loadedTypes) {
                super(typeDescription, typeByte, loadedTypeInitializer, auxiliaryTypes);
                this.loadedTypes = loadedTypes;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends T> getLoaded() {
                return (Class<? extends T>) loadedTypes.get(typeDescription);
            }

            @Override
            public Map<TypeDescription, Class<?>> getLoadedAuxiliaryTypes() {
                Map<TypeDescription, Class<?>> loadedAuxiliaryTypes = new HashMap<TypeDescription, Class<?>>(loadedTypes);
                loadedAuxiliaryTypes.remove(typeDescription);
                return loadedAuxiliaryTypes;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && super.equals(other) && loadedTypes.equals(((Default.Loaded) other).loadedTypes);
            }

            @Override
            public int hashCode() {
                return 31 * super.hashCode() + loadedTypes.hashCode();
            }

            @Override
            public String toString() {
                return "DynamicType.Default.Loaded{" +
                        "typeDescription='" + typeDescription + '\'' +
                        ", binaryRepresentation=" + Arrays.toString(binaryRepresentation) +
                        ", typeInitializer=" + loadedTypeInitializer +
                        ", auxiliaryTypes=" + auxiliaryTypes +
                        ", loadedTypes=" + loadedTypes +
                        '}';
            }
        }
    }
}
