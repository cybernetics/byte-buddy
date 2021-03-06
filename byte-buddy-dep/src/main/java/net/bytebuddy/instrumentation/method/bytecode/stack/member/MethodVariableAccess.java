package net.bytebuddy.instrumentation.method.bytecode.stack.member;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.reference.DownCasting;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Iterator;

/**
 * A stack assignment that loads a method variable from a given index of the local variable array.
 */
public enum MethodVariableAccess {

    /**
     * The accessor handler for a JVM-integer.
     */
    INTEGER(Opcodes.ILOAD, 5, StackSize.SINGLE),

    /**
     * The accessor handler for a {@code long}.
     */
    LONG(Opcodes.LLOAD, 8, StackSize.DOUBLE),

    /**
     * The accessor handler for a {@code float}.
     */
    FLOAT(Opcodes.FLOAD, 11, StackSize.SINGLE),

    /**
     * The accessor handler for a {@code double}.
     */
    DOUBLE(Opcodes.DLOAD, 14, StackSize.DOUBLE),

    /**
     * The accessor handler for a reference type.
     */
    REFERENCE(Opcodes.ALOAD, 17, StackSize.SINGLE);

    /**
     * The opcode for loading this variable.
     */
    private final int loadOpcode;

    /**
     * The offset for any shortcut opcode that allows to load a variable from a low range index, such as
     * {@code ALOAD_0}, {@code ILOAD_0} etc.
     */
    private final int loadOpcodeShortcutOffset;

    /**
     * The size impact of this stack manipulation.
     */
    private final StackManipulation.Size size;

    /**
     * Creates a new method variable access for a given JVM type.
     *
     * @param loadOpcode               The opcode for loading this variable.
     * @param loadOpcodeShortcutOffset The offset for any shortcut opcode that allows to load a variable from a low
     *                                 range index, such as {@code ALOAD_0}, {@code ILOAD_0} etc.
     * @param stackSize                The size of the JVM type.
     */
    private MethodVariableAccess(int loadOpcode, int loadOpcodeShortcutOffset, StackSize stackSize) {
        this.loadOpcode = loadOpcode;
        this.loadOpcodeShortcutOffset = loadOpcodeShortcutOffset;
        this.size = stackSize.toIncreasingSize();
    }

    /**
     * Locates the correct accessor for a variable of a given type.
     *
     * @param typeDescription The type of the variable to be loaded.
     * @return An accessor for the given type.
     */
    public static MethodVariableAccess forType(TypeDescription typeDescription) {
        if (typeDescription.isPrimitive()) {
            if (typeDescription.represents(long.class)) {
                return LONG;
            } else if (typeDescription.represents(double.class)) {
                return DOUBLE;
            } else if (typeDescription.represents(float.class)) {
                return FLOAT;
            } else if (typeDescription.represents(void.class)) {
                throw new IllegalArgumentException("Variable type cannot be void");
            } else {
                return INTEGER;
            }
        } else {
            return REFERENCE;
        }
    }

    /**
     * Loads all method arguments for a given method onto the operand stack, including a reference to {@code this},
     * if the method is non-static.
     *
     * @param methodDescription The method for which all method arguments should be loaded onto the stack, including
     *                          a reference to {@code this} if the method is non-static.
     * @return A stack manipulation representing the loading of all method arguments including a reference to the
     * instance if any.
     */
    public static StackManipulation loadThisReferenceAndArguments(MethodDescription methodDescription) {
        return loadArguments(methodDescription, TypeCastingHandler.NoOp.INSTANCE, true);
    }

    /**
     * Loads all method arguments for a given method onto the operand stack.
     *
     * @param methodDescription The method for which all method arguments should be loaded onto the stack.
     * @return A stack manipulation representing the loading of all method arguments.
     */
    public static StackManipulation loadArguments(MethodDescription methodDescription) {
        return loadArguments(methodDescription, TypeCastingHandler.NoOp.INSTANCE, false);
    }

    /**
     * Creates a stack manipulation for loading all parameters of a Java bridge method onto the operand stack where
     * all variables of the bridge method are casted to the parameter types of the target method. For legally
     * applying this manipulation, the bridge method's parameters must all be super types of the target method.
     * The resulting stack manipulation does not load the {@code this} reference onto the operand stack.
     *
     * @param bridgeMethod The bridge method that is invoking its target method.
     * @param targetMethod The target of the bridge method.
     * @return A stack manipulation that loads all parameters of the bridge method onto the stack while type casting
     * the parameters to the value of its target method.
     */
    public static StackManipulation forBridgeMethodInvocation(MethodDescription bridgeMethod,
                                                              MethodDescription targetMethod) {
        return loadArguments(bridgeMethod, new TypeCastingHandler.ForBridgeTarget(targetMethod), false);
    }

    /**
     * Loads all arguments of a given method onto the stack.
     *
     * @param methodDescription    The method for which all method arguments should be loaded onto the stack.
     * @param typeCastingHandler   A handler for applying type castings, if required.
     * @param includeThisReference {@code true} if the {@code this} references should also be loaded onto the stack.
     * @return A stack manipulation representing the specified variable loading.
     */
    private static StackManipulation loadArguments(MethodDescription methodDescription,
                                                   TypeCastingHandler typeCastingHandler,
                                                   boolean includeThisReference) {
        int stackValues = (!includeThisReference || methodDescription.isStatic() ? 0 : 1) + methodDescription.getParameterTypes().size();
        StackManipulation[] stackManipulation = new StackManipulation[stackValues];
        int parameterIndex = 0, stackIndex;
        if (!methodDescription.isStatic()) {
            if (includeThisReference) {
                stackManipulation[parameterIndex++] = MethodVariableAccess.REFERENCE.loadFromIndex(0);
            }
            stackIndex = methodDescription.getDeclaringType().getStackSize().getSize();
        } else {
            stackIndex = 0;
        }
        for (TypeDescription parameterType : methodDescription.getParameterTypes()) {
            stackManipulation[parameterIndex++] = typeCastingHandler
                    .wrapNext(forType(parameterType).loadFromIndex(stackIndex), parameterType);
            stackIndex += parameterType.getStackSize().getSize();
        }
        return new StackManipulation.Compound(stackManipulation);
    }

    /**
     * Creates a stack assignment for a given index of the local variable array.
     * <p>&nbsp;</p>
     * The index has to be relative to the method's local variable array size.
     *
     * @param variableOffset The offset of the variable where {@code double} and {@code long} types
     *                       count two slots.
     * @return A stack manipulation representing the method retrieval.
     */
    public StackManipulation loadFromIndex(int variableOffset) {
        return new ArgumentLoadingStackManipulation(variableOffset);
    }

    /**
     * A handler for optionally applying a type casting for each method parameter that is loaded onto the operand
     * stack.
     */
    private static interface TypeCastingHandler {

        /**
         * Returns the given stack manipulation while possibly wrapping the operation by a type casting
         * if this is required.
         *
         * @param variableAccess The stack manipulation that represents the variable access.
         * @param parameterType  The type of the loaded variable that is represented by the stack manipulation.
         * @return A stack manipulation that represents the given variable access and potentially an additional
         * type casting.
         */
        StackManipulation wrapNext(StackManipulation variableAccess, TypeDescription parameterType);

        /**
         * A non-operative implementation of a
         * {@link net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess.TypeCastingHandler}
         * that merely returns the given stack manipulation.
         */
        static enum NoOp implements TypeCastingHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation wrapNext(StackManipulation variableAccess, TypeDescription parameterType) {
                return variableAccess;
            }
        }

        /**
         * A {@link net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess.TypeCastingHandler}
         * that casts all parameters that are loaded for a method to their target method's type.
         */
        static class ForBridgeTarget implements TypeCastingHandler {

            /**
             * An iterator over all parameter types of the target method.
             */
            private final Iterator<TypeDescription> typeIterator;

            /**
             * Creates a new type casting handler for a bridge method.
             *
             * @param targetMethod The target of the bridge method.
             */
            public ForBridgeTarget(MethodDescription targetMethod) {
                typeIterator = targetMethod.getParameterTypes().iterator();
            }

            @Override
            public StackManipulation wrapNext(StackManipulation variableAccess, TypeDescription parameterType) {
                TypeDescription targetParameterType = typeIterator.next();
                return targetParameterType.equals(parameterType)
                        ? variableAccess
                        : new StackManipulation.Compound(variableAccess, new DownCasting(targetParameterType));
            }

            @Override
            public String toString() {
                return "MethodVariableAccess.TypeCastingHandler.ForBridgeTarget{typeIterator=" + typeIterator + '}';
            }
        }

    }

    /**
     * A stack manipulation for loading a variable of a method's local variable array onto the operand stack.
     */
    private class ArgumentLoadingStackManipulation implements StackManipulation {

        /**
         * The index of the local variable array from which the variable should be loaded.
         */
        private final int variableIndex;

        /**
         * Creates a new argument loading stack manipulation.
         *
         * @param variableIndex The index of the local variable array from which the variable should be loaded.
         */
        private ArgumentLoadingStackManipulation(int variableIndex) {
            this.variableIndex = variableIndex;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            switch (variableIndex) {
                case 0:
                    methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutOffset);
                    break;
                case 1:
                    methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutOffset + 1);
                    break;
                case 2:
                    methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutOffset + 2);
                    break;
                case 3:
                    methodVisitor.visitInsn(loadOpcode + loadOpcodeShortcutOffset + 3);
                    break;
                default:
                    methodVisitor.visitVarInsn(loadOpcode, variableIndex);
                    break;
            }
            return size;
        }

        /**
         * Returns the outer instance.
         *
         * @return The outer instance.
         */
        private MethodVariableAccess getMethodVariableAccess() {
            return MethodVariableAccess.this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && MethodVariableAccess.this == ((ArgumentLoadingStackManipulation) other).getMethodVariableAccess()
                    && variableIndex == ((ArgumentLoadingStackManipulation) other).variableIndex;
        }

        @Override
        public int hashCode() {
            return MethodVariableAccess.this.hashCode() + 31 * variableIndex;
        }

        @Override
        public String toString() {
            return "MethodVariableAccess.ArgumentLoadingStackManipulation{variableIndex=" + variableIndex + '}';
        }
    }
}

