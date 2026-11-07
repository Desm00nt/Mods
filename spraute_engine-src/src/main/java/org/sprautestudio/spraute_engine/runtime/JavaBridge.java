package org.sprautestudio.spraute_engine.runtime;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public final class JavaBridge {
    private static final int NO = 1000000;

    private JavaBridge() {
    }

    public static void require() {
        if (!JsEngine.unsafe()) {
            throw new SprauteRuntimeException("Java из сюжета выключена — оператор мира может разрешить: /sprautejs unsafe on");
        }
    }

    public static Class<?> findClass(String name) {
        String n = name.trim();

        while (true) {
            for (ClassLoader cl : new ClassLoader[]{Thread.currentThread().getContextClassLoader(), JavaBridge.class.getClassLoader()}) {
                if (cl != null) {
                    try {
                        return Class.forName(n, false, cl);
                    } catch (LinkageError | ClassNotFoundException var7) {
                    }
                }
            }

            int dot = n.lastIndexOf(46);
            if (dot < 0) {
                return null;
            }

            n = n.substring(0, dot) + "$" + n.substring(dot + 1);
        }
    }

    public static JavaBridge.JavaObj type(String name) {
        require();
        Class<?> c = findClass(name);
        if (c == null) {
            throw new SprauteRuntimeException("Java: нет класса " + name + " (мод не установлен или имя неверное)");
        } else {
            return new JavaBridge.JavaObj(c, true);
        }
    }

    public static JavaBridge.JavaObj wrap(Object v) {
        require();
        if (v instanceof JavaBridge.JavaObj j) {
            return j;
        } else {
            Object raw = unwrap(v);
            if (raw == null) {
                throw new SprauteRuntimeException("Java: пустое значение");
            } else {
                return new JavaBridge.JavaObj(raw, false);
            }
        }
    }

    public static JavaBridge.JavaObj target(Object v) {
        return v instanceof String s ? type(s) : wrap(v);
    }

    public static String className(Object v) {
        return target(v).type().getName();
    }

    public static Object get(JavaBridge.JavaObj t, String name) {
        require();
        Class<?> c = t.type();
        Field f = field(c, name, t.statics);

        try {
            if (f != null) {
                return toStory(f.get(t.statics ? null : t.value));
            }
        } catch (IllegalAccessException var10) {
            throw new SprauteRuntimeException("Java: нет доступа к полю " + name);
        }

        String cap = name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);

        for (String m : new String[]{name, "get" + cap, "is" + cap}) {
            Method found = pick(methods(c, m, t.statics), List.of());
            if (found != null) {
                return invoke(found, t.statics ? null : t.value, List.of());
            }
        }

        if (t.statics) {
            return get(new JavaBridge.JavaObj(t.value, false), name);
        } else {
            throw new SprauteRuntimeException("Java: у " + c.getName() + " нет поля или свойства " + name);
        }
    }

    public static Object set(JavaBridge.JavaObj t, String name, Object value) {
        require();
        Class<?> c = t.type();
        Field f = field(c, name, t.statics);
        if (f != null && !Modifier.isFinal(f.getModifiers())) {
            JavaBridge.Conv cv = convert(value, f.getType(), f.getGenericType());
            if (cv == null) {
                throw new SprauteRuntimeException("Java: поле " + name + " имеет тип " + f.getType().getSimpleName() + ", а не " + Eval.str(value));
            } else {
                try {
                    f.set(t.statics ? null : t.value, cv.value);
                    return value;
                } catch (IllegalAccessException var7) {
                    throw new SprauteRuntimeException("Java: нет доступа к полю " + name);
                }
            }
        } else {
            String cap = name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
            Method setter = pick(methods(c, "set" + cap, t.statics), List.of(value));
            if (setter != null) {
                invoke(setter, t.statics ? null : t.value, List.of(value));
                return value;
            } else {
                throw new SprauteRuntimeException("Java: у " + c.getName() + " нельзя поменять " + name);
            }
        }
    }

    public static Object call(JavaBridge.JavaObj t, String name, List<Object> args) {
        require();
        Method m = pick(methods(t.type(), name, t.statics), args);
        if (m != null) {
            return invoke(m, t.statics ? null : t.value, args);
        } else {
            if (t.statics) {
                Method own = pick(methods(Class.class, name, false), args);
                if (own != null) {
                    return invoke(own, t.value, args);
                }
            }

            throw new SprauteRuntimeException("Java: у " + t.type().getName() + " нет метода " + name + " для " + args.size() + " аргументов такого типа");
        }
    }

    public static Object create(String className, List<Object> args) {
        Class<?> c = type(className).type();
        Constructor<?> best = null;
        int bestCost = Integer.MAX_VALUE;

        for (Constructor<?> k : c.getDeclaredConstructors()) {
            int cost = cost(k.getParameterTypes(), k.getGenericParameterTypes(), args);
            if (cost < bestCost) {
                best = k;
                bestCost = cost;
            }
        }

        if (best == null) {
            throw new SprauteRuntimeException("Java: у " + c.getName() + " нет конструктора для " + args.size() + " аргументов такого типа");
        } else {
            try {
                best.trySetAccessible();
                return toStory(best.newInstance(arguments(best.getParameterTypes(), best.getGenericParameterTypes(), args)));
            } catch (InvocationTargetException var10) {
                throw failure(c.getSimpleName(), var10.getCause());
            } catch (Exception var11) {
                throw failure(c.getSimpleName(), var11);
            }
        }
    }

    public static List<Object> members(Object v) {
        JavaBridge.JavaObj t = target(v);
        TreeSet<String> out = new TreeSet<>();

        for (Class<?> c = t.type(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!f.isSynthetic() && Modifier.isStatic(f.getModifiers()) == t.statics) {
                    out.add(f.getName() + ": " + f.getType().getSimpleName());
                }
            }

            for (Method m : c.getDeclaredMethods()) {
                if (!m.isSynthetic() && !m.isBridge() && Modifier.isStatic(m.getModifiers()) == t.statics) {
                    out.add(signature(m));
                }
            }
        }

        if (!t.statics) {
            for (Method mx : t.type().getMethods()) {
                if (!Modifier.isStatic(mx.getModifiers()) && mx.getDeclaringClass() != Object.class) {
                    out.add(signature(mx));
                }
            }
        }

        return new ArrayList<>(out);
    }

    private static String signature(Method m) {
        StringJoiner j = new StringJoiner(", ", m.getName() + "(", "): " + m.getReturnType().getSimpleName());

        for (Class<?> p : m.getParameterTypes()) {
            j.add(p.getSimpleName());
        }

        return j.toString();
    }

    private static Field field(Class<?> c, String name, boolean statics) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                if (Modifier.isStatic(f.getModifiers()) == statics && (f.trySetAccessible() || Modifier.isPublic(f.getModifiers()))) {
                    return f;
                }
            } catch (NoSuchFieldException var5) {
            }
        }

        return null;
    }

    private static List<Method> methods(Class<?> c, String name, boolean statics) {
        Map<String, Method> out = new LinkedHashMap<>();

        for (Method m : c.getMethods()) {
            add(out, m, name, statics);
        }

        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            for (Method m : k.getDeclaredMethods()) {
                add(out, m, name, statics);
            }
        }

        return new ArrayList<>(out.values());
    }

    private static void add(Map<String, Method> out, Method m, String name, boolean statics) {
        if (m.getName().equals(name) && !m.isBridge() && Modifier.isStatic(m.getModifiers()) == statics) {
            out.putIfAbsent(name + Arrays.toString((Object[])m.getParameterTypes()), m);
        }
    }

    private static Method pick(List<Method> candidates, List<Object> args) {
        Method best = null;
        int bestCost = Integer.MAX_VALUE;

        for (Method m : candidates) {
            int cost = cost(m.getParameterTypes(), m.getGenericParameterTypes(), args);
            if (cost < bestCost) {
                best = m;
                bestCost = cost;
            }
        }

        return best;
    }

    private static Object invoke(Method m, Object self, List<Object> args) {
        Method callable = accessible(m);

        try {
            Object r = callable.invoke(self, arguments(m.getParameterTypes(), m.getGenericParameterTypes(), args));
            return m.getReturnType() == void.class ? "" : toStory(r);
        } catch (InvocationTargetException var5) {
            throw failure(m.getName(), var5.getCause());
        } catch (Exception var6) {
            throw failure(m.getName(), var6);
        }
    }

    private static Method accessible(Method m) {
        if (m.trySetAccessible()) {
            return m;
        } else {
            Deque<Class<?>> queue = new ArrayDeque<>(List.of(m.getDeclaringClass()));

            while (!queue.isEmpty()) {
                Class<?> c = queue.poll();
                if (Modifier.isPublic(c.getModifiers())) {
                    try {
                        return c.getMethod(m.getName(), m.getParameterTypes());
                    } catch (NoSuchMethodException var4) {
                    }
                }

                if (c.getSuperclass() != null) {
                    queue.add(c.getSuperclass());
                }

                queue.addAll(List.of(c.getInterfaces()));
            }

            return m;
        }
    }

    private static SprauteRuntimeException failure(String what, Throwable e) {
        String msg = e == null ? "" : (e.getMessage() == null ? e.getClass().getSimpleName() : e.getClass().getSimpleName() + ": " + e.getMessage());
        return new SprauteRuntimeException("Java: " + what + " — " + msg);
    }

    private static int cost(Class<?>[] types, Type[] generic, List<Object> args) {
        boolean varargs = types.length > 0 && types[types.length - 1].isArray() && args.size() != types.length;
        if (types.length == args.size() || varargs && args.size() >= types.length - 1) {
            int sum = 0;
            int fixed = varargs ? types.length - 1 : types.length;

            for (int i = 0; i < fixed; i++) {
                JavaBridge.Conv c = convert(args.get(i), types[i], generic[i]);
                if (c == null) {
                    return Integer.MAX_VALUE;
                }

                sum += c.cost;
            }

            if (varargs) {
                Class<?> el = types[types.length - 1].getComponentType();

                for (int i = fixed; i < args.size(); i++) {
                    JavaBridge.Conv c = convert(args.get(i), el, el);
                    if (c == null) {
                        return Integer.MAX_VALUE;
                    }

                    sum += c.cost + 1;
                }
            }

            return sum;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    private static Object[] arguments(Class<?>[] types, Type[] generic, List<Object> args) {
        Object[] out = new Object[types.length];
        boolean varargs = types.length > 0 && types[types.length - 1].isArray() && args.size() != types.length;
        int fixed = varargs ? types.length - 1 : types.length;

        for (int i = 0; i < fixed; i++) {
            out[i] = convert(args.get(i), types[i], generic[i]).value;
        }

        if (varargs) {
            Class<?> el = types[types.length - 1].getComponentType();
            Object arr = Array.newInstance(el, args.size() - fixed);

            for (int i = fixed; i < args.size(); i++) {
                Array.set(arr, i - fixed, convert(args.get(i), el, el).value);
            }

            out[fixed] = arr;
        }

        return out;
    }

    private static JavaBridge.Conv convert(Object v, Class<?> type, Type generic) {
        Object raw = unwrap(v);
        Class<?> box = boxed(type);
        if (raw instanceof Double d) {
            if (box == Double.class) {
                return new JavaBridge.Conv(d, 0);
            }

            if (box == Float.class) {
                return new JavaBridge.Conv(d.floatValue(), 1);
            }

            if (box == Long.class) {
                return new JavaBridge.Conv(d.longValue(), 1);
            }

            if (box == Integer.class) {
                return new JavaBridge.Conv(d.intValue(), 1);
            }

            if (box == Short.class) {
                return new JavaBridge.Conv(d.shortValue(), 2);
            }

            if (box == Byte.class) {
                return new JavaBridge.Conv(d.byteValue(), 2);
            }
        }

        if (raw instanceof Boolean b && box == Boolean.class) {
            return new JavaBridge.Conv(b, 0);
        } else {
            if (raw instanceof String s) {
                if (type == String.class || type == CharSequence.class) {
                    return new JavaBridge.Conv(s, 0);
                }

                if (box == Character.class && s.length() == 1) {
                    return new JavaBridge.Conv(s.charAt(0), 1);
                }

                if (type.isEnum()) {
                    for (Object c : type.getEnumConstants()) {
                        if (((Enum)c).name().equalsIgnoreCase(s)) {
                            return new JavaBridge.Conv(c, 1);
                        }
                    }

                    return null;
                }

                if (type == ResourceLocation.class) {
                    try {
                        return new JavaBridge.Conv(new ResourceLocation(s.contains(":") ? s : "minecraft:" + s), 1);
                    } catch (Exception var11) {
                        return null;
                    }
                }

                if (type == Component.class) {
                    return new JavaBridge.Conv(Component.literal(s), 1);
                }

                if (type == UUID.class) {
                    try {
                        return new JavaBridge.Conv(UUID.fromString(s), 1);
                    } catch (IllegalArgumentException var12) {
                        return null;
                    }
                }
            }

            if (raw instanceof List<?> l) {
                if (type.isArray()) {
                    Class<?> el = type.getComponentType();
                    Object arr = Array.newInstance(el, l.size());

                    for (int i = 0; i < l.size(); i++) {
                        JavaBridge.Conv cx = convert(l.get(i), el, el);
                        if (cx == null) {
                            return null;
                        }

                        Array.set(arr, i, cx.value);
                    }

                    return new JavaBridge.Conv(arr, 1);
                }

                if (type.isAssignableFrom(ArrayList.class)) {
                    Class<?> el = elementType(generic, 0);
                    List<Object> out = new ArrayList<>();

                    for (Object x : l) {
                        JavaBridge.Conv cx = convert(x, el, el);
                        if (cx == null) {
                            return null;
                        }

                        out.add(cx.value);
                    }

                    return new JavaBridge.Conv(out, 1);
                }

                if (type.isAssignableFrom(HashSet.class)) {
                    Set<Object> out = new LinkedHashSet<>();

                    for (Object x : l) {
                        out.add(unwrap(x));
                    }

                    return new JavaBridge.Conv(out, 2);
                }
            }

            if (raw instanceof Map<?, ?> m && type.isAssignableFrom(LinkedHashMap.class)) {
                Map out = new LinkedHashMap();

                for (Entry<?, ?> e : m.entrySet()) {
                    out.put(e.getKey(), unwrap(e.getValue()));
                }

                return new JavaBridge.Conv(out, 1);
            } else if (raw == null) {
                return type.isPrimitive() ? null : new JavaBridge.Conv(null, 1);
            } else if (type.isInstance(raw)) {
                return new JavaBridge.Conv(raw, raw.getClass() == type ? 0 : (type == Object.class ? 3 : 1));
            } else {
                return type == String.class ? new JavaBridge.Conv(Eval.str(v), 5) : null;
            }
        }
    }

    private static Class<?> elementType(Type generic, int i) {
        return generic instanceof ParameterizedType p && p.getActualTypeArguments().length > i && p.getActualTypeArguments()[i] instanceof Class<?> c
            ? c
            : Object.class;
    }

    private static Class<?> boxed(Class<?> t) {
        if (!t.isPrimitive()) {
            return t;
        } else if (t == int.class) {
            return Integer.class;
        } else if (t == long.class) {
            return Long.class;
        } else if (t == double.class) {
            return Double.class;
        } else if (t == float.class) {
            return Float.class;
        } else if (t == boolean.class) {
            return Boolean.class;
        } else if (t == short.class) {
            return Short.class;
        } else if (t == byte.class) {
            return Byte.class;
        } else {
            return t == char.class ? Character.class : t;
        }
    }

    static Object unwrap(Object v) {
        if (v instanceof JavaBridge.JavaObj j) {
            return j.value;
        } else {
            return v instanceof EntityRef r ? r.require() : v;
        }
    }

    static Object toStory(Object v) {
        if (v == null) {
            return "";
        } else if (v instanceof Number n) {
            return n.doubleValue();
        } else if (v instanceof Boolean || v instanceof String) {
            return v;
        } else if (v instanceof CharSequence || v instanceof Character) {
            return v.toString();
        } else if (v instanceof Entity e) {
            return new EntityRef(e);
        } else if (v instanceof Component c) {
            return c.getString();
        } else if (v instanceof Optional<?> o) {
            return toStory(o.orElse(null));
        } else if (v instanceof Collection<?> c) {
            List<Object> out = new ArrayList<>();

            for (Object x : c) {
                out.add(toStory(x));
            }

            return out;
        } else if (v.getClass().isArray()) {
            List<Object> out = new ArrayList<>();

            for (int i = 0; i < Array.getLength(v); i++) {
                out.add(toStory(Array.get(v, i)));
            }

            return out;
        } else if (!(v instanceof Map<?, ?> m)) {
            return new JavaBridge.JavaObj(v, false);
        } else {
            Map<String, Object> out = Values.map();

            for (Entry<?, ?> e : m.entrySet()) {
                out.put(String.valueOf(e.getKey()), toStory(e.getValue()));
            }

            return out;
        }
    }

    private record Conv(Object value, int cost) {
    }

    public static final class JavaObj {
        public final Object value;
        final boolean statics;

        JavaObj(Object value, boolean statics) {
            this.value = value;
            this.statics = statics;
        }

        Class<?> type() {
            return this.statics ? (Class)this.value : this.value.getClass();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof JavaBridge.JavaObj j && this.statics == j.statics && Objects.equals(this.value, j.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.value);
        }

        @Override
        public String toString() {
            return this.statics ? ((Class)this.value).getName() : String.valueOf(this.value);
        }
    }
}
