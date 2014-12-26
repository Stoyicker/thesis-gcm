package com.jorge.thesis.io.enumrefl;

import sun.reflect.ConstructorAccessor;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * "Of Hacking Enums and Modifying "final static" Fields"
 * http://www.javaspecialists.eu/archive/Issue161.html
 *
 * @author Ken Dobson, Dr. Heinz M. Kabutz
 */
public class EnumBuster<E extends Enum<E>> {
    private static final Class[] EMPTY_CLASS_ARRAY =
            new Class[0];
    private static final Object[] EMPTY_OBJECT_ARRAY =
            new Object[0];

    private static final String VALUES_FIELD = "$VALUES";
    private static final String ORDINAL_FIELD = "ordinal";

    private final ReflectionFactory reflection =
            ReflectionFactory.getReflectionFactory();

    private final Class<E> clazz;

    private final Collection<Field> switchFields;

    /**
     * Construct an EnumBuster for the given enum class and keep
     * the switch statements of the classes specified in
     * switchUsers in sync with the enum values.
     */
    public EnumBuster(Class<E> clazz, Class... switchUsers) {
        try {
            this.clazz = clazz;
            switchFields = findRelatedSwitchFields(switchUsers);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not create the class", e);
        }
    }

    /**
     * Make a new enum instance, without adding it to the values
     * array and using the default ordinal of 0.
     */
    public E make(String value) {
        return make(value, 0,
                EMPTY_CLASS_ARRAY, EMPTY_OBJECT_ARRAY);
    }

    /**
     * Make a new enum instance with the given value, ordinal and
     * additional parameters.  The additionalTypes is used to match
     * the constructor accurately.
     */
    public E make(String value, int ordinal,
                  Class[] additionalTypes, Object[] additional) {
        try {
            ConstructorAccessor ca = findConstructorAccessor(
                    additionalTypes, clazz);
            return constructEnum(clazz, ca, value,
                    ordinal, additional);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not create enum", e);
        }
    }

    /**
     * This method adds the given enum into the array
     * inside the enum class.  If the enum already
     * contains that particular value, then the value
     * is overwritten with our enum.  Otherwise it is
     * added at the end of the array.
     * <p>
     * In addition, if there is a constant field in the
     * enum class pointing to an enum with our value,
     * then we replace that with our enum instance.
     * <p>
     * The ordinal is either set to the existing position
     * or to the last value.
     * <p>
     * Warning: This should probably never be called,
     * since it can cause permanent changes to the enum
     * values.  Use only in extreme conditions.
     *
     * @param e the enum to add
     */
    public void addByValue(E e) {
        try {
            Field valuesField = findValuesField();

            // we get the current Enum[]
            E[] values = values();
            for (int i = 0; i < values.length; i++) {
                E value = values[i];
                if (value.name().equals(e.name())) {
                    setOrdinal(e, value.ordinal());
                    values[i] = e;
                    replaceConstant(e);
                    return;
                }
            }

            // we did not find it in the existing array, thus
            // append it to the array
            E[] newValues =
                    Arrays.copyOf(values, values.length + 1);
            newValues[newValues.length - 1] = e;
            ReflectionHelper.setStaticFinalField(
                    valuesField, newValues);

            int ordinal = newValues.length - 1;
            setOrdinal(e, ordinal);
            addSwitchCase();
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Could not set the enum", ex);
        }
    }

    private ConstructorAccessor findConstructorAccessor(
            Class[] additionalParameterTypes,
            Class<E> clazz) throws NoSuchMethodException {
        Class[] parameterTypes =
                new Class[additionalParameterTypes.length + 2];
        parameterTypes[0] = String.class;
        parameterTypes[1] = int.class;
        System.arraycopy(
                additionalParameterTypes, 0,
                parameterTypes, 2,
                additionalParameterTypes.length);
        Constructor<E> cstr = clazz.getDeclaredConstructor(
                parameterTypes
        );
        return reflection.newConstructorAccessor(cstr);
    }

    private E constructEnum(Class<E> clazz,
                            ConstructorAccessor ca,
                            String value, int ordinal,
                            Object[] additional)
            throws Exception {
        Object[] parms = new Object[additional.length + 2];
        parms[0] = value;
        parms[1] = ordinal;
        System.arraycopy(
                additional, 0, parms, 2, additional.length);
        return clazz.cast(ca.newInstance(parms));
    }

    /**
     * The only time we ever add a new enum is at the end.
     * Thus all we need to do is expand the switch map arrays
     * by one empty slot.
     */
    private void addSwitchCase() {
        try {
            for (Field switchField : switchFields) {
                int[] switches = (int[]) switchField.get(null);
                switches = Arrays.copyOf(switches, switches.length + 1);
                ReflectionHelper.setStaticFinalField(
                        switchField, switches
                );
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not fix switch", e);
        }
    }

    private void replaceConstant(E e)
            throws IllegalAccessException, NoSuchFieldException {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields)
            if (field.getName().equals(e.name())) {
                ReflectionHelper.setStaticFinalField(
                        field, e
                );
            }
    }

    private void setOrdinal(E e, int ordinal)
            throws NoSuchFieldException, IllegalAccessException {
        Field ordinalField = Enum.class.getDeclaredField(
                ORDINAL_FIELD);
        ordinalField.setAccessible(true);
        ordinalField.set(e, ordinal);
    }

    private Field findValuesField()
            throws NoSuchFieldException {
        // first we find the static final array that holds
        // the values in the enum class
        Field valuesField = clazz.getDeclaredField(
                VALUES_FIELD);
        // we mark it to be public
        valuesField.setAccessible(true);
        return valuesField;
    }

    private Collection<Field> findRelatedSwitchFields(
            Class[] switchUsers) {
        Collection<Field> result = new ArrayList<>();
        try {
            for (Class switchUser : switchUsers) {
                Class[] clazzes = switchUser.getDeclaredClasses();
                for (Class suspect : clazzes) {
                    Field[] fields = suspect.getDeclaredFields();
                    for (Field field : fields) {
                        if (field.getName().startsWith("$SwitchMap$" +
                                clazz.getSimpleName())) {
                            field.setAccessible(true);
                            result.add(field);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not fix switch", e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private E[] values()
            throws NoSuchFieldException, IllegalAccessException {
        Field valuesField = findValuesField();
        return (E[]) valuesField.get(null);
    }
}