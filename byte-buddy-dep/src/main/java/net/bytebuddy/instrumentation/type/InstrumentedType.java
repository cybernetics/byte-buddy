package net.bytebuddy.instrumentation.type;

import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementations of this interface represent an instrumented type that is subject to change. Implementations
 * should however be immutable and return new instance when their mutator methods are called.
 */
public interface InstrumentedType extends TypeDescription {

    /**
     * Creates a new instrumented type that includes a new field.
     *
     * @param name      The internalName of the new field.
     * @param fieldType A description of the type of the new field.
     * @param modifiers The modifier of the new field.
     * @return A new instrumented type that is equal to this instrumented type but with the additional field.
     */
    InstrumentedType withField(String name,
                               TypeDescription fieldType,
                               int modifiers);

    /**
     * Creates a new instrumented type that includes a new method or constructor.
     *
     * @param name           The internalName of the new field.
     * @param returnType     A description of the return type of the new field.
     * @param parameterTypes A list of descriptions of the parameter types.
     * @param exceptionTypes A list of descriptions of the exception types that are declared by this method.
     * @param modifiers      The modifier of the new field.
     * @return A new instrumented type that is equal to this instrumented type but with the additional field.
     */
    InstrumentedType withMethod(String name,
                                TypeDescription returnType,
                                List<? extends TypeDescription> parameterTypes,
                                List<? extends TypeDescription> exceptionTypes,
                                int modifiers);

    /**
     * Creates a new instrumented type that includes the given
     * {@link net.bytebuddy.instrumentation.LoadedTypeInitializer}.
     *
     * @param loadedTypeInitializer The type initializer to include.
     * @return A new instrumented type that is equal to this instrumented type but with the additional type initializer.
     */
    InstrumentedType withInitializer(LoadedTypeInitializer loadedTypeInitializer);

    /**
     * Returns the {@link net.bytebuddy.instrumentation.LoadedTypeInitializer}s that were registered
     * for this instrumented type.
     *
     * @return The registered loaded type initializers for this instrumented type.
     */
    LoadedTypeInitializer getLoadedTypeInitializer();

    /**
     * Creates a <i>compressed</i> version of this instrumented type which only needs to fulfil the
     * {@link net.bytebuddy.instrumentation.type.TypeDescription} interface. This allows
     * for a potential compression of the representation of {@code this} instrumented type.
     *
     * @return A (potentially) compressed version of {@code this} instrumented type.
     */
    TypeDescription detach();

    /**
     * An abstract base implementation of an instrumented type.
     */
    abstract static class AbstractBase extends AbstractTypeDescription implements InstrumentedType {

        /**
         * The loaded type initializer for this instrumented type.
         */
        protected final LoadedTypeInitializer loadedTypeInitializer;

        /**
         * A list of field descriptions registered for this instrumented type.
         */
        protected final List<FieldDescription> fieldDescriptions;

        /**
         * A list of method descriptions registered for this instrumented type.
         */
        protected final List<MethodDescription> methodDescriptions;

        /**
         * Creates a new instrumented type with a no-op loaded type initializer and without registered fields or
         * methods.
         */
        protected AbstractBase() {
            loadedTypeInitializer = LoadedTypeInitializer.NoOp.INSTANCE;
            fieldDescriptions = Collections.emptyList();
            methodDescriptions = Collections.emptyList();
        }

        /**
         * Creates a new instrumented type with the given loaded type initializer and field and methods. All field and
         * method descriptions will be replaced by new instances where type descriptions with the internalName of this
         * type as given by {@code typeInternalName} are replaced by references to {@code this}.
         *
         * @param loadedTypeInitializer A loaded type initializer for this instrumented type.
         * @param typeInternalName      The internal internalName of this instrumented type.
         * @param fieldDescriptions     A list of field descriptions for this instrumented type.
         * @param methodDescriptions    A list of method descriptions for this instrumented type.
         */
        protected AbstractBase(LoadedTypeInitializer loadedTypeInitializer,
                               String typeInternalName,
                               List<? extends FieldDescription> fieldDescriptions,
                               List<? extends MethodDescription> methodDescriptions) {
            this.loadedTypeInitializer = loadedTypeInitializer;
            this.fieldDescriptions = new ArrayList<FieldDescription>(fieldDescriptions.size());
            for (FieldDescription fieldDescription : fieldDescriptions) {
                this.fieldDescriptions.add(new FieldToken(typeInternalName, fieldDescription));
            }
            this.methodDescriptions = new ArrayList<MethodDescription>(methodDescriptions.size());
            for (MethodDescription methodDescription : methodDescriptions) {
                this.methodDescriptions.add(new MethodToken(typeInternalName, methodDescription));
            }
        }

        /**
         * Checks if a specific type is assignable to another type where the source type must be a super
         * type of the target type.
         *
         * @param sourceType The source type to which another type is to be assigned to.
         * @param targetType The target type that is to be assigned to the source type.
         * @return {@code true} if the target type is assignable to the source type.
         */
        private static boolean isAssignable(TypeDescription sourceType, TypeDescription targetType) {
            // Means that '[sourceType] var = ([targetType]) val;' is a valid assignment. This is true, if:
            // (1) Both types are equal.
            if (sourceType.equals(targetType)) {
                return true;
            }
            // The sub type has a super type and this super type is assignable to the super type.
            TypeDescription targetTypeSuperType = targetType.getSupertype();
            if (targetTypeSuperType != null && targetTypeSuperType.isAssignableTo(sourceType)) {
                return true;
            }
            // (2) If the target type is an interface, any of this type's interfaces might be assignable to it.
            if (sourceType.isInterface()) {
                for (TypeDescription interfaceType : targetType.getInterfaces()) {
                    if (interfaceType.isAssignableTo(sourceType)) {
                        return true;
                    }
                }
            }
            // (3) None of these criteria are true, i.e. the types are not assignable.
            return false;
        }

        /**
         * Substitutes an <i>outdated</i> reference to the instrumented type with a reference to <i>this</i>.
         *
         * @param instrumentedTypeName The name of this instrumented type.
         * @param typeDescription      The type description to be checked to represent this instrumented type.
         * @return This type, if the type description represents the name of the instrumented type or the given
         * instrumented type if this is not the case.
         */
        private TypeDescription withSubstitutedSelfReference(String instrumentedTypeName, TypeDescription typeDescription) {
            return typeDescription.getInternalName().equals(instrumentedTypeName) ? this : typeDescription;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return false;
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }

        @Override
        public boolean isInstance(Object object) {
            return isAssignableFrom(object.getClass());
        }

        @Override
        public boolean isAssignableFrom(Class<?> type) {
            return isAssignableFrom(new ForLoadedType(type));
        }

        @Override
        public boolean isAssignableFrom(TypeDescription typeDescription) {
            return isAssignable(this, typeDescription);
        }

        @Override
        public boolean isAssignableTo(Class<?> type) {
            return isAssignableTo(new ForLoadedType(type));
        }

        @Override
        public boolean isAssignableTo(TypeDescription typeDescription) {
            return isAssignable(typeDescription, this);
        }

        @Override
        public boolean represents(Class<?> type) {
            return type.getName().equals(getName());
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public TypeDescription getComponentType() {
            return null;
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            return null;
        }

        @Override
        public TypeDescription getEnclosingClass() {
            return null;
        }

        @Override
        public String getSimpleName() {
            return getName().substring(getPackageName().length() + 1, getName().length());
        }

        @Override
        public String getCanonicalName() {
            return getName();
        }

        @Override
        public boolean isAnonymousClass() {
            return false;
        }

        @Override
        public boolean isLocalClass() {
            return false;
        }

        @Override
        public boolean isMemberClass() {
            return false;
        }

        @Override
        public FieldList getDeclaredFields() {
            return new FieldList.Explicit(fieldDescriptions);
        }

        @Override
        public MethodList getDeclaredMethods() {
            return new MethodList.Explicit(methodDescriptions);
        }

        @Override
        public String getPackageName() {
            int packageIndex = getName().lastIndexOf('.');
            if (packageIndex == -1) {
                return "";
            } else {
                return getName().substring(0, packageIndex);
            }
        }

        @Override
        public StackSize getStackSize() {
            return StackSize.SINGLE;
        }

        @Override
        public String getDescriptor() {
            return "L" + getInternalName() + ";";
        }

        @Override
        public TypeDescription getDeclaringType() {
            return null;
        }

        @Override
        public LoadedTypeInitializer getLoadedTypeInitializer() {
            return loadedTypeInitializer;
        }

        /**
         * An implementation of a new field for the enclosing instrumented type.
         */
        protected class FieldToken extends FieldDescription.AbstractFieldDescription {

            /**
             * The name of the field token.
             */
            private final String name;

            /**
             * The type of the field.
             */
            private final TypeDescription fieldType;

            /**
             * The modifiers of the field.
             */
            private final int modifiers;

            /**
             * Creates a new field for the enclosing instrumented type.
             *
             * @param name      The internalName of the field.
             * @param fieldType The type description of the field.
             * @param modifiers The modifiers of the field.
             */
            public FieldToken(String name, TypeDescription fieldType, int modifiers) {
                this.name = name;
                this.fieldType = fieldType;
                this.modifiers = modifiers;
            }

            /**
             * Creates a new field for the enclosing instrumented type.
             *
             * @param typeInternalName The internal name of the enclosing instrumented type.
             * @param fieldDescription The field description to copy.
             */
            private FieldToken(String typeInternalName, FieldDescription fieldDescription) {
                name = fieldDescription.getName();
                fieldType = withSubstitutedSelfReference(typeInternalName, fieldDescription.getFieldType());
                modifiers = fieldDescription.getModifiers();
            }

            @Override
            public TypeDescription getFieldType() {
                return fieldType;
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
                return false;
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return null;
            }

            @Override
            public Annotation[] getAnnotations() {
                return new Annotation[0];
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return new Annotation[0];
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getDescriptor() {
                return fieldType.getDescriptor();
            }

            @Override
            public TypeDescription getDeclaringType() {
                return AbstractBase.this;
            }

            @Override
            public int getModifiers() {
                return modifiers;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                if (!super.equals(o)) return false;
                FieldToken that = (FieldToken) o;
                return modifiers == that.modifiers
                        && fieldType.equals(that.fieldType)
                        && name.equals(that.name);
            }

            @Override
            public int hashCode() {
                int result = super.hashCode();
                result = 31 * result + name.hashCode();
                result = 31 * result + fieldType.hashCode();
                result = 31 * result + modifiers;
                return result;
            }

            @Override
            public String toString() {
                return "InstrumentedType.FieldToken{" +
                        "name='" + name + '\'' +
                        ", fieldType=" + fieldType +
                        ", modifiers=" + modifiers +
                        '}';
            }
        }

        /**
         * An implementation of a new method or constructor for the enclosing instrumented type.
         */
        protected class MethodToken extends MethodDescription.AbstractMethodDescription {

            /**
             * The internal name of the represented method.
             */
            private final String internalName;

            /**
             * The return type of the represented method.
             */
            private final TypeDescription returnType;

            /**
             * The parameter types of the represented method.
             */
            private final List<TypeDescription> parameterTypes;

            /**
             * The exception types of the represented method.
             */
            private final List<TypeDescription> exceptionTypes;

            /**
             * The modifiers of the represented method.
             */
            private final int modifiers;

            /**
             * Creates a new method or constructor for the enclosing instrumented type.
             *
             * @param internalName   The internal internalName of the method or constructor.
             * @param returnType     A description of the return type of this method.
             * @param parameterTypes A list of descriptions of the parameter types of this method.
             * @param exceptionTypes A list of descriptions of the exception types that are declared by this method.
             * @param modifiers      The modifiers of this method.
             */
            public MethodToken(String internalName,
                               TypeDescription returnType,
                               List<? extends TypeDescription> parameterTypes,
                               List<? extends TypeDescription> exceptionTypes,
                               int modifiers) {
                this.internalName = internalName;
                this.returnType = returnType;
                this.parameterTypes = new ArrayList<TypeDescription>(parameterTypes);
                this.exceptionTypes = new ArrayList<TypeDescription>(exceptionTypes);
                this.modifiers = modifiers;
            }

            /**
             * Creates a new method or constructor for the enclosing instrumented type.
             *
             * @param typeInternalName  The internal internalName of the enclosing instrumented type.
             * @param methodDescription The method description to copy.
             */
            private MethodToken(String typeInternalName, MethodDescription methodDescription) {
                internalName = methodDescription.getInternalName();
                returnType = withSubstitutedSelfReference(typeInternalName, methodDescription.getReturnType());
                parameterTypes = new ArrayList<TypeDescription>(methodDescription.getParameterTypes().size());
                for (TypeDescription typeDescription : methodDescription.getParameterTypes()) {
                    parameterTypes.add(withSubstitutedSelfReference(typeInternalName, typeDescription));
                }
                exceptionTypes = new ArrayList<TypeDescription>(methodDescription.getExceptionTypes().size());
                for (TypeDescription typeDescription : methodDescription.getExceptionTypes()) {
                    exceptionTypes.add(withSubstitutedSelfReference(typeInternalName, typeDescription));
                }
                modifiers = methodDescription.getModifiers();
            }

            @Override
            public TypeDescription getReturnType() {
                return returnType;
            }

            @Override
            public TypeList getParameterTypes() {
                return new TypeList.Explicit(parameterTypes);
            }

            @Override
            public Annotation[][] getParameterAnnotations() {
                return new Annotation[0][0];
            }

            @Override
            public TypeList getExceptionTypes() {
                return new TypeList.Explicit(exceptionTypes);
            }

            @Override
            public boolean isConstructor() {
                return CONSTRUCTOR_INTERNAL_NAME.equals(internalName);
            }

            @Override
            public boolean isTypeInitializer() {
                return false;
            }

            @Override
            public boolean represents(Method method) {
                return false;
            }

            @Override
            public boolean represents(Constructor<?> constructor) {
                return false;
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
                return false;
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return null;
            }

            @Override
            public Annotation[] getAnnotations() {
                return new Annotation[0];
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return new Annotation[0];
            }

            @Override
            public String getName() {
                return isConstructor() ? getDeclaringType().getName() : getInternalName();
            }

            @Override
            public String getInternalName() {
                return internalName;
            }

            @Override
            public TypeDescription getDeclaringType() {
                return AbstractBase.this;
            }

            @Override
            public int getModifiers() {
                return modifiers;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                if (!super.equals(o)) return false;
                MethodToken that = (MethodToken) o;
                return modifiers == that.modifiers
                        && internalName.equals(that.internalName)
                        && parameterTypes.equals(that.parameterTypes)
                        && returnType.equals(that.returnType);
            }

            @Override
            public int hashCode() {
                int result = super.hashCode();
                result = 31 * result + internalName.hashCode();
                result = 31 * result + returnType.hashCode();
                result = 31 * result + parameterTypes.hashCode();
                result = 31 * result + modifiers;
                return result;
            }

            @Override
            public String toString() {
                return "InstrumentedType.MethodToken{" +
                        "internalName='" + internalName + '\'' +
                        ", returnType=" + returnType +
                        ", parameterTypes=" + parameterTypes +
                        ", modifiers=" + modifiers +
                        '}';
            }
        }
    }
}
