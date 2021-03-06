package net.bytebuddy;

import org.junit.Test;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

public class ClassFileVersionTest {

    @Test
    public void testCurrentJavaVersionWasManuallyEvaluated() throws Exception {
        // This test is supposed to fail if ByteBuddy was not yet manually considered for
        // a new major release targeting Java.
        assertTrue(ClassFileVersion.forCurrentJavaVersion().getVersionNumber() <= Opcodes.V1_8);
    }

    @Test
    public void testExplicitConstructionOfUnknownVersion() throws Exception {
        assertThat(new ClassFileVersion(Opcodes.V1_8 + 1).getVersionNumber(), is(Opcodes.V1_8 + 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalVersion() throws Exception {
        new ClassFileVersion(0);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(ClassFileVersion.JAVA_V1.hashCode(), is(new ClassFileVersion(Opcodes.V1_1).hashCode()));
        assertThat(ClassFileVersion.JAVA_V1, is(new ClassFileVersion(Opcodes.V1_1)));
        assertThat(ClassFileVersion.JAVA_V1.hashCode(), not(is(new ClassFileVersion(Opcodes.V1_2).hashCode())));
        assertThat(ClassFileVersion.JAVA_V1, not(is(new ClassFileVersion(Opcodes.V1_2))));
    }
}
