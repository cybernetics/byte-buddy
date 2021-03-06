package net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class PrimitiveWideningDelegateOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalSourceTypeThrowsException() throws Exception {
        PrimitiveWideningDelegate.forPrimitive(new TypeDescription.ForLoadedType(Object.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalTargetTypeThrowsException() throws Exception {
        PrimitiveWideningDelegate.forPrimitive(new TypeDescription.ForLoadedType(int.class)).widenTo(new TypeDescription.ForLoadedType(Object.class));
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(PrimitiveWideningDelegate.forPrimitive(new TypeDescription.ForLoadedType(int.class)).widenTo(new TypeDescription.ForLoadedType(long.class)).hashCode(),
                is(PrimitiveWideningDelegate.forPrimitive(new TypeDescription.ForLoadedType(int.class)).widenTo(new TypeDescription.ForLoadedType(long.class)).hashCode()));
        assertThat(PrimitiveWideningDelegate.forPrimitive(new TypeDescription.ForLoadedType(int.class)).widenTo(new TypeDescription.ForLoadedType(long.class)),
                is(PrimitiveWideningDelegate.forPrimitive(new TypeDescription.ForLoadedType(int.class)).widenTo(new TypeDescription.ForLoadedType(long.class))));
        assertThat(PrimitiveWideningDelegate.forPrimitive(new TypeDescription.ForLoadedType(int.class)).widenTo(new TypeDescription.ForLoadedType(long.class)).hashCode(),
                not(is(PrimitiveWideningDelegate.forPrimitive(new TypeDescription.ForLoadedType(float.class)).widenTo(new TypeDescription.ForLoadedType(long.class)).hashCode())));
        assertThat(PrimitiveWideningDelegate.forPrimitive(new TypeDescription.ForLoadedType(int.class)).widenTo(new TypeDescription.ForLoadedType(long.class)),
                not(is(PrimitiveWideningDelegate.forPrimitive(new TypeDescription.ForLoadedType(float.class)).widenTo(new TypeDescription.ForLoadedType(long.class)))));
    }
}
