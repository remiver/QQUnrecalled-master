package com.fkzhang.qqunrecalled;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;

/**
 * Created by fkzhang on 1/27/2016.
 */
public class ReflectionUtil {

    public static Object getObjectField(Object o, String fieldName, String type) {
        Field[] fields = o.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals(fieldName) && field.getType().getName().equals(type)) {
                field.setAccessible(true);
                try {
                    return field.get(o);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    public static Object getObjectField(Object o, String fieldName, Class<?> type) {
        return getObjectField(o, fieldName, type.getName());
    }

    public static Object getObjectField(Class<?> cls, Object o, String fieldName) {
        try {
            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(o);
        } catch (Exception e) {
            XposedBridge.log(e);
        }
        return null;
    }

    public static Method getStaticMethod(Class<?> cls, String methodName, Class<?> returnType, Class<?>... parameters) {
        for (Method method : cls.getDeclaredMethods()) {
            if (!method.getName().equals(methodName) || method.getReturnType() != returnType)
                continue;

            Class<?>[] pars = method.getParameterTypes();
            if (parameters.length != pars.length)
                continue;
            boolean found = true;
            for (int i = 0; i < parameters.length; i++) {
                if (pars[i] != parameters[i]) {
                    found = false;
                    break;
                }
            }

            if (found) {
                return method;
            }
        }

        return null;
    }

    public static Method getMethod(Object o, String methodName, Class<?> returnType, Class<?>... parameters) {
        for (Method method : o.getClass().getDeclaredMethods()) {
            if (!method.getName().equals(methodName) || method.getReturnType() != returnType)
                continue;

            Class<?>[] pars = method.getParameterTypes();
            if (parameters.length != pars.length)
                continue;
            boolean found = true;
            for (int i = 0; i < parameters.length; i++) {
                if (pars[i] != parameters[i]) {
                    found = false;
                    break;
                }
            }

            if (found) {
                return method;
            }
        }

        return null;
    }

    public static Method getMethod(Object o, String methodName, String returnType, Class<?>... parameters) {
        for (Method method : o.getClass().getDeclaredMethods()) {
            if (!method.getName().equals(methodName) || !method.getReturnType().getName().equals(returnType))
                continue;

            Class<?>[] pars = method.getParameterTypes();
            if (parameters.length != pars.length)
                continue;
            boolean found = true;
            for (int i = 0; i < parameters.length; i++) {
                if (pars[i] != parameters[i]) {
                    found = false;
                    break;
                }
            }

            if (found) {
                return method;
            }
        }

        return null;
    }

    public static Method getMethod(Object o, String methodName, String returnType, String... parameters) {
        for (Method method : o.getClass().getDeclaredMethods()) {
            if (!method.getName().equals(methodName) || !method.getReturnType().getName().equals(returnType))
                continue;

            Class<?>[] pars = method.getParameterTypes();
            if (parameters.length != pars.length)
                continue;
            boolean found = true;
            for (int i = 0; i < parameters.length; i++) {
                if (!pars[i].getName().equals(parameters[i])) {
                    found = false;
                    break;
                }
            }

            if (found) {
                return method;
            }
        }

        return null;
    }

    public static Object invokeMethod(Method method, Object o, Object... args) {
        if (method == null)
            return null;

        try {
            return method.invoke(o, args);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
        return null;
    }

    public static Object invokeStaticMethod(Method method, Object... args) {
        if (method == null)
            return null;
        return invokeMethod(method, (Object) null, args);
    }

    public static boolean isCallingFrom(String className) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTraceElements) {
            if (element.getClassName().contains(className)) {
                return true;
            }
        }
        return false;
    }
}
