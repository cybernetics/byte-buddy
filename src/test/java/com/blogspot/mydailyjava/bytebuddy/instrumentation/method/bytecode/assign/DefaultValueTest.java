package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class DefaultValueTest {

    private MethodVisitor methodVisitor;

    @Before
    public void setUp() throws Exception {
        methodVisitor = mock(MethodVisitor.class);
    }

    @Test
    public void testVoid() throws Exception {
        Assignment assignment = DefaultValue.load(new TypeDescription.ForLoadedType(void.class));
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(0));
        assertThat(size.getMaximalSize(), is(0));
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testInt() throws Exception {
        Assignment assignment = DefaultValue.load(new TypeDescription.ForLoadedType(int.class));
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.ICONST_0);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testLong() throws Exception {
        Assignment assignment = DefaultValue.load(new TypeDescription.ForLoadedType(long.class));
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(2));
        assertThat(size.getMaximalSize(), is(2));
        verify(methodVisitor).visitInsn(Opcodes.LCONST_0);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testFloat() throws Exception {
        Assignment assignment = DefaultValue.load(new TypeDescription.ForLoadedType(float.class));
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.FCONST_0);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testDouble() throws Exception {
        Assignment assignment = DefaultValue.load(new TypeDescription.ForLoadedType(double.class));
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(2));
        assertThat(size.getMaximalSize(), is(2));
        verify(methodVisitor).visitInsn(Opcodes.DCONST_0);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testReference() throws Exception {
        Assignment assignment = DefaultValue.load(new TypeDescription.ForLoadedType(Object.class));
        assertThat(assignment.isValid(), is(true));
        Assignment.Size size = assignment.apply(methodVisitor);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.ACONST_NULL);
        verifyNoMoreInteractions(methodVisitor);
    }
}