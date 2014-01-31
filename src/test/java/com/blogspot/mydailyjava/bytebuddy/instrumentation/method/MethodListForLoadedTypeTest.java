package com.blogspot.mydailyjava.bytebuddy.instrumentation.method;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MethodListForLoadedTypeTest {

    private MethodList methodList;

    @Before
    public void setUp() throws Exception {
        methodList = new MethodList.ForLoadedType(Object.class);
    }

    @Test
    public void testMethodList() throws Exception {
        assertThat(methodList.size(), is(Object.class.getDeclaredMethods().length + Object.class.getDeclaredConstructors().length));
        for (Method method : Object.class.getDeclaredMethods()) {
            assertThat(methodList.filter(MethodMatchers.is(method)).size(), is(1));
        }
        for (Constructor<?> constructor : Object.class.getDeclaredConstructors()) {
            assertThat(methodList.filter(MethodMatchers.is(constructor)).size(), is(1));
        }
    }

    @Test
    public void testMethodListFilter() throws Exception {
        methodList = methodList.filter(MethodMatchers.isMethod());
        assertThat(methodList.size(), is(Object.class.getDeclaredMethods().length));
        for (Method method : Object.class.getDeclaredMethods()) {
            assertThat(methodList.filter(MethodMatchers.is(method)).size(), is(1));
        }
    }
}