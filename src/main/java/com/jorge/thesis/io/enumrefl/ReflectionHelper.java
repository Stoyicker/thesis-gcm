package com.jorge.thesis.io.enumrefl;

import sun.reflect.FieldAccessor;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * "Of Hacking Enums and Modifying "final static" Fields"
 * http://www.javaspecialists.eu/archive/Issue161.html
 *
 * @author Ken Dobson, Dr. Heinz M. Kabutz
 */
public final class ReflectionHelper {
    private static final String MODIFIERS_FIELD = "modifiers";

    private static final ReflectionFactory reflection =
            ReflectionFactory.getReflectionFactory();

    public static void setStaticFinalField(
            Field field, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        // we mark the field to be public
        field.setAccessible(Boolean.TRUE);
        // next we change the modifier in the Field instance to
        // not be final anymore, thus tricking reflection into
        // letting us modify the static final field
        Field modifiersField =
                Field.class.getDeclaredField(MODIFIERS_FIELD);
        modifiersField.setAccessible(Boolean.TRUE);
        int modifiers = modifiersField.getInt(field);
        // Blank out the final bit in the modifiers int
        modifiers &= ~Modifier.FINAL;
        modifiersField.setInt(field, modifiers);
        FieldAccessor fa = reflection.newFieldAccessor(
                field, Boolean.FALSE
        );
        fa.set(null, value);
    }
}