package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import org.objectweb.asm.MethodVisitor;

/**
 * Represents a {@link java.lang.String} value that is stored in a type's constant pool.
 */
public class TextConstant implements StackManipulation {

    /**
     * The size impact of loading a {@link String} onto the operand stack.
     */
    private static final Size SIZE = StackSize.SINGLE.toIncreasingSize();

    /**
     * The text value to load onto the operand stack.
     */
    private final String text;

    /**
     * Creates a new stack manipulation to load a {@code String} constant onto the operand stack.
     *
     * @param text The value of the {@code String} to be loaded.
     */
    public TextConstant(String text) {
        this.text = text;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        methodVisitor.visitLdcInsn(text);
        return SIZE;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && text.equals(((TextConstant) other).text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return "TextConstant{'" + text + '\'' + '}';
    }
}
