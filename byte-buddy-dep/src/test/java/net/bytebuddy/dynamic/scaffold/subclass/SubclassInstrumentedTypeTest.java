package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.instrumentation.type.AbstractInstrumentedTypeTest;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

public class SubclassInstrumentedTypeTest extends AbstractInstrumentedTypeTest {

    private static final String FOO = "foo", BAR = "bar", FOOBAR = FOO + "." + BAR;

    @Override
    protected InstrumentedType makePlainInstrumentedType() {
        return new SubclassInstrumentedType(
                ClassFileVersion.forCurrentJavaVersion(),
                new TypeDescription.ForLoadedType(Object.class),
                new TypeList.ForLoadedType(Collections.<Class<?>>singletonList(Serializable.class)),
                Opcodes.ACC_PUBLIC,
                new NamingStrategy.Fixed(FOOBAR));
    }

    @Override
    public void testIsAssignableFrom() {
        assertThat(makePlainInstrumentedType().isAssignableFrom(Object.class), is(false));
        assertThat(makePlainInstrumentedType().isAssignableFrom(Serializable.class), is(false));
        assertThat(makePlainInstrumentedType().isAssignableFrom(Integer.class), is(false));
        TypeDescription objectTypeDescription = new TypeDescription.ForLoadedType(Object.class);
        assertThat(makePlainInstrumentedType().isAssignableFrom(objectTypeDescription), is(false));
        TypeDescription serializableTypeDescription = new TypeDescription.ForLoadedType(Serializable.class);
        assertThat(makePlainInstrumentedType().isAssignableFrom(serializableTypeDescription), is(false));
        TypeDescription integerTypeDescription = new TypeDescription.ForLoadedType(Integer.class);
        assertThat(makePlainInstrumentedType().isAssignableFrom(integerTypeDescription), is(false));
    }

    @Override
    @Test
    public void testIsAssignableTo() {
        assertThat(makePlainInstrumentedType().isAssignableTo(Object.class), is(true));
        assertThat(makePlainInstrumentedType().isAssignableTo(Serializable.class), is(true));
        assertThat(makePlainInstrumentedType().isAssignableTo(Integer.class), is(false));
        TypeDescription objectTypeDescription = new TypeDescription.ForLoadedType(Object.class);
        assertThat(makePlainInstrumentedType().isAssignableTo(objectTypeDescription), is(true));
        TypeDescription serializableTypeDescription = new TypeDescription.ForLoadedType(Serializable.class);
        assertThat(makePlainInstrumentedType().isAssignableTo(serializableTypeDescription), is(true));
        TypeDescription integerTypeDescription = new TypeDescription.ForLoadedType(Integer.class);
        assertThat(makePlainInstrumentedType().isAssignableTo(integerTypeDescription), is(false));
    }

    @Override
    @Test
    public void testRepresents() {
        assertThat(makePlainInstrumentedType().represents(Object.class), is(false));
        assertThat(makePlainInstrumentedType().represents(Serializable.class), is(false));
        assertThat(makePlainInstrumentedType().represents(Integer.class), is(false));
    }

    @Override
    @Test
    public void testSupertype() {
        assertThat(makePlainInstrumentedType().getSupertype(), is((TypeDescription) new TypeDescription.ForLoadedType(Object.class)));
        assertThat(makePlainInstrumentedType().getSupertype(), not(is((TypeDescription) new TypeDescription.ForLoadedType(Integer.class))));
        assertThat(makePlainInstrumentedType().getSupertype(), not(is((TypeDescription) new TypeDescription.ForLoadedType(Serializable.class))));
    }

    @Override
    @Test
    public void testInterfaces() {
        TypeList interfaces = makePlainInstrumentedType().getInterfaces();
        assertThat(interfaces.size(), is(1));
        assertThat(interfaces.get(0), is(is((TypeDescription) new TypeDescription.ForLoadedType(Serializable.class))));
    }

    @Override
    @Test
    public void testPackageName() {
        assertThat(makePlainInstrumentedType().getPackageName(), is(FOO));
    }

    @Override
    @Test
    public void testSimpleName() {
        assertThat(makePlainInstrumentedType().getSimpleName(), is(BAR));
    }
}
