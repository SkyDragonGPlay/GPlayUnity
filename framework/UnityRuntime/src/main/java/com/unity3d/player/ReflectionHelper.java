package com.unity3d.player;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

public class ReflectionHelper {
    protected static boolean LOG = false;
    protected static final boolean LOGV = false;
    private static MethodInfo[] methodInfos = new MethodInfo[4096];

    private static boolean hasLoaded(MethodInfo info) {
        MethodInfo mtdInfo = methodInfos[info.hashCode() & methodInfos.length - 1];
        if (!info.equals(mtdInfo)) {
            return false;
        }
        info.member = mtdInfo.member;
        return true;
    }

    private static void cacheMethodInfo(MethodInfo methodInfo, Member member) {
        methodInfo.member = member;
        ReflectionHelper.methodInfos[methodInfo.hashCode() & ReflectionHelper.methodInfos.length - 1] = methodInfo;
    }

    public static Constructor getConstructorID(Class class_, String paramsType) {
        Constructor constructor = null;
        MethodInfo mtdInfo = new MethodInfo(class_, "", paramsType);
        if (ReflectionHelper.hasLoaded(mtdInfo)) {
            constructor = (Constructor)mtdInfo.member;
        } else {
            Class[] arrclass = ReflectionHelper.paramTypes2Classes(paramsType);
            float f2 = 0.0f;
            for (Constructor ctor : class_.getConstructors()) {
                float f3 = ReflectionHelper.compareParamTypes(Void.TYPE, ctor.getParameterTypes(), arrclass);
                if (f3 <= f2) continue;
                constructor = ctor;
                f2 = f3;
                if (f2 == 1.0f) break;
            }
            ReflectionHelper.cacheMethodInfo(mtdInfo, constructor);
        }
        if (constructor == null) {
            throw new NoSuchMethodError("<init>" + paramsType + " in class " + class_.getName());
        }
        return constructor;
    }

    public static Method getMethodID(Class _class, String methodName, String paramsType, boolean isMemberMethod) {
        Method method = null;
        MethodInfo methodInfo = new MethodInfo(_class, methodName, paramsType);
        if (ReflectionHelper.hasLoaded(methodInfo)) {
            method = (Method)methodInfo.member;
        } else {
            Class[] arrclass = ReflectionHelper.paramTypes2Classes(paramsType);
            float f2 = 0.0f;
            while (_class != null) {
                for (Method method2 : _class.getDeclaredMethods()) {
                    float f3;
                    if (isMemberMethod != Modifier.isStatic(method2.getModifiers()) || method2.getName().compareTo(methodName) != 0 || (f3 = ReflectionHelper.compareParamTypes(method2.getReturnType(), method2.getParameterTypes(), arrclass)) <= f2) continue;
                    method = method2;
                    f2 = f3;
                    if (f2 == 1.0f) break;
                }
                if (f2 == 1.0f || _class.isPrimitive() || _class.isInterface() || _class.equals(Object.class) || _class.equals(Void.TYPE)) break;
                _class = _class.getSuperclass();
            }
            ReflectionHelper.cacheMethodInfo(methodInfo, method);
        }
        if (method == null) {
            Object[] arrobject = new Object[4];
            arrobject[0] = isMemberMethod ? "non-static" : "static";
            arrobject[1] = methodName;
            arrobject[2] = paramsType;
            arrobject[3] = _class.getName();
            throw new NoSuchMethodError(String.format("no %s method with name='%s' signature='%s' in class L%s;", arrobject));
        }
        return method;
    }

    public static Field getFieldID(Class class_, String string, String string2, boolean bl) {
        Field field = null;
        MethodInfo a2 = new MethodInfo(class_, string, string2);
        if (ReflectionHelper.hasLoaded(a2)) {
            field = (Field)a2.member;
        } else {
            Class[] arrclass = ReflectionHelper.paramTypes2Classes(string2);
            float f2 = 0.0f;
            while (class_ != null) {
                for (Field field2 : class_.getDeclaredFields()) {
                    float f3;
                    if (bl != Modifier.isStatic(field2.getModifiers()) || field2.getName().compareTo(string) != 0 || (f3 = ReflectionHelper.compareParamTypes(field2.getType(), null, arrclass)) <= f2) continue;
                    field = field2;
                    f2 = f3;
                    if (f2 == 1.0f) break;
                }
                if (f2 == 1.0f || class_.isPrimitive() || class_.isInterface() || class_.equals(Object.class) || class_.equals(Void.TYPE)) break;
                class_ = class_.getSuperclass();
            }
            ReflectionHelper.cacheMethodInfo(a2, field);
        }
        if (field == null) {
            Object[] arrobject = new Object[4];
            arrobject[0] = bl ? "non-static" : "static";
            arrobject[1] = string;
            arrobject[2] = string2;
            arrobject[3] = class_.getName();
            throw new NoSuchFieldError(String.format("no %s field with name='%s' signature='%s' in class L%s;", arrobject));
        }
        return field;
    }

    static float equalClass(Class class_, Class class_2) {
        if (class_.equals(class_2)) {
            return 1.0f;
        }
        if (!class_.isPrimitive() && !class_2.isPrimitive()) {
            try {
                if (class_.asSubclass(class_2) != null) {
                    return 0.5f;
                }
            }
            catch (ClassCastException v0) {}
            try {
                if (class_2.asSubclass(class_) != null) {
                    return 0.1f;
                }
            }
            catch (ClassCastException v1) {}
        }
        return 0.0f;
    }

    private static float compareParamTypes(Class returnType, Class[] sourceParamTypes, Class[] destParamTypes) {
        if (destParamTypes.length == 0) {
            return 0.1f;
        }
        if ((sourceParamTypes == null ? 0 : sourceParamTypes.length) + 1 != destParamTypes.length) {
            return 0.0f;
        }
        float f2 = 1.0f;
        int n2 = 0;
        if (sourceParamTypes != null) {
            for (Class class_2 : sourceParamTypes) {
                f2 *= ReflectionHelper.equalClass(class_2, destParamTypes[n2++]);
            }
        }
        return f2 * ReflectionHelper.equalClass(returnType, destParamTypes[destParamTypes.length - 1]);
    }

    private static Class[] paramTypes2Classes(String paramsType) {
        Class class_;
        int[] arrn = new int[]{0};
        ArrayList<Class> listTypes = new ArrayList<Class>();
        while (arrn[0] < paramsType.length() && (class_ = ReflectionHelper.getParameterType(paramsType, arrn)) != null) {
            listTypes.add(class_);
        }
        int index = 0;
        Class[] arrParamsClass = new Class[listTypes.size()];
        for (Class cls : listTypes) {
            arrParamsClass[index++] = cls;
        }
        return arrParamsClass;
    }

    private static Class getParameterType(String paramsType, int[] arrn) {
        while (arrn[0] < paramsType.length()) {
            int startPosition = arrn[0];
            arrn[0] = startPosition + 1;
            char c = paramsType.charAt(startPosition);
            if (c == '(' || c == ')') continue;
            if (c == 'L') {
                int pos = paramsType.indexOf(';', arrn[0]);
                if (pos == -1) break;
                paramsType = paramsType.substring(arrn[0], pos);
                arrn[0] = pos + 1;
                paramsType = paramsType.replace('/', '.');
                Class<?> classParam;
                try {
                    classParam = Class.forName((String)paramsType);
                }
                catch (ClassNotFoundException v2) {
                    break;
                }
                return classParam;
            }
            if (c == 'Z') {
                return Boolean.TYPE;
            }
            if (c == 'I') {
                return Integer.TYPE;
            }
            if (c == 'F') {
                return Float.TYPE;
            }
            if (c == 'V') {
                return Void.TYPE;
            }
            if (c == 'B') {
                return Byte.TYPE;
            }
            if (c == 'S') {
                return Short.TYPE;
            }
            if (c == 'J') {
                return Long.TYPE;
            }
            if (c == 'D') {
                return Double.TYPE;
            }
            if (c == '[') {
                return Array.newInstance(ReflectionHelper.getParameterType((String)paramsType, arrn), 0).getClass();
            }
            UnityLog.Log(5, "! parseType; " + c + " is not known!");
            break;
        }
        return null;
    }

    private static native Object nativeProxyInvoke(int nativeObjectId, String methodName, Object[] args);

    private static native void nativeProxyFinalize(int nativeObjectId);

    public static Object newProxyInstance(int nativeObjectId, Class class_) {
        return ReflectionHelper.newProxyInstance(nativeObjectId, new Class[]{class_});
    }

    public static Object newProxyInstance(final int nativeObjectId, final Class<?>[] interfaces) {
        return Proxy.newProxyInstance(ReflectionHelper.class.getClassLoader(), interfaces, new InvocationHandler(){

            @Override
            public final Object invoke(Object proxy, Method method, Object[] args) {
                return ReflectionHelper.nativeProxyInvoke(nativeObjectId, method.getName(), args);
            }

            protected final void finalize() {
                ReflectionHelper.nativeProxyFinalize(nativeObjectId);
                return;
            }
        });
    }

    private static class MethodInfo {
        private final Class classObject;
        private final String methodName;
        private final String paramsType;
        private final int id;
        public volatile Member member;

        MethodInfo(Class class_, String _methodName, String _paramsType) {
            this.classObject = class_;
            this.methodName = _methodName;
            this.paramsType = _paramsType;
            int n = 527 + this.classObject.hashCode();
            n = 31 * n + this.methodName.hashCode();
            this.id = 31 * n + this.paramsType.hashCode();
        }

        public final int hashCode() {
            return this.id;
        }

        public final boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (object instanceof MethodInfo) {
                MethodInfo obj = (MethodInfo)object;
                if (this.id == obj.id && this.paramsType.equals(obj.paramsType) && this.methodName.equals(obj.methodName) && this.classObject.equals(obj.classObject)) {
                    return true;
                }
                return false;
            }
            return false;
        }
    }

}

