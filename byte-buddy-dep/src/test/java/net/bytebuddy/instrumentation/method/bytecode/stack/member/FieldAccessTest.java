package net.bytebuddy.instrumentation.method.bytecode.stack.member;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class FieldAccessTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";
    private final boolean isStatic;
    private final StackSize fieldSize;
    private final int getterChange, getterMaximum, getterOpcode;
    private final int putterChange, putterMaximum, putterOpcode;
    @Rule
    public TestRule mockitoRule = new MockitoRule(this);
    @Mock
    private FieldDescription fieldDescription;
    @Mock
    private TypeDescription declaringType, fieldType;
    @Mock
    private MethodVisitor methodVisitor;
    @Mock
    private Instrumentation.Context instrumentationContext;

    public FieldAccessTest(boolean isStatic,
                           StackSize fieldSize,
                           int getterChange,
                           int getterMaximum,
                           int getterOpcode,
                           int putterChange,
                           int putterMaximum,
                           int putterOpcode) {
        this.isStatic = isStatic;
        this.fieldSize = fieldSize;
        this.getterChange = getterChange;
        this.getterMaximum = getterMaximum;
        this.getterOpcode = getterOpcode;
        this.putterChange = putterChange;
        this.putterMaximum = putterMaximum;
        this.putterOpcode = putterOpcode;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {true, StackSize.SINGLE, 1, 1, Opcodes.GETSTATIC, -1, 0, Opcodes.PUTSTATIC},
                {true, StackSize.DOUBLE, 2, 2, Opcodes.GETSTATIC, -2, 0, Opcodes.PUTSTATIC},
                {false, StackSize.SINGLE, 0, 0, Opcodes.GETFIELD, -2, 0, Opcodes.PUTFIELD},
                {false, StackSize.DOUBLE, 1, 1, Opcodes.GETFIELD, -3, 0, Opcodes.PUTFIELD}
        });
    }

    @Before
    public void setUp() throws Exception {
        when(fieldDescription.getDeclaringType()).thenReturn(declaringType);
        when(fieldDescription.getFieldType()).thenReturn(fieldType);
        when(declaringType.getInternalName()).thenReturn(FOO);
        when(fieldDescription.getInternalName()).thenReturn(BAR);
        when(fieldDescription.getDescriptor()).thenReturn(QUX);
        when(fieldType.getStackSize()).thenReturn(fieldSize);
        when(fieldDescription.isStatic()).thenReturn(isStatic);
    }

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testGetter() throws Exception {
        StackManipulation getter = FieldAccess.forField(fieldDescription).getter();
        assertThat(getter.isValid(), is(true));
        StackManipulation.Size size = getter.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(getterChange));
        assertThat(size.getMaximalSize(), is(getterMaximum));
        verify(methodVisitor).visitFieldInsn(getterOpcode, FOO, BAR, QUX);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testPutter() throws Exception {
        StackManipulation getter = FieldAccess.forField(fieldDescription).putter();
        assertThat(getter.isValid(), is(true));
        StackManipulation.Size size = getter.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(putterChange));
        assertThat(size.getMaximalSize(), is(putterMaximum));
        verify(methodVisitor).visitFieldInsn(putterOpcode, FOO, BAR, QUX);
        verifyNoMoreInteractions(methodVisitor);
    }
}
