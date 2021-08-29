package blog.serilize.impl;


import blog.serilize.base.DInput;
import blog.serilize.base.DMarshaller;
import blog.serilize.base.DOutput;
import blog.serilize.base.DSerialize;
import blog.serilize.base.anotation.Marshaller;

import java.util.HashMap;
import java.util.Map;

public class DMarshallerIml implements DMarshaller {
    private final Map<String , DSerialize<?>> register;
    private final Map<Integer, DSerialize<?>> registerInt;

    public DMarshallerIml() {
        this.register = new HashMap<>();
        this.registerInt = new HashMap<>();
    }

    public void register(DSerialize<?> x) {
        register.put(x.getClass().getName(), x);
        Marshaller ano = x.getClass().getAnnotation(Marshaller.class);
        if (ano == null) {
            throw new RuntimeException("Class serialize need Marshaller annotation ");
        }
        if (registerInt.containsKey(ano.number())) {
            throw new RuntimeException("duplicate number");
        }
        registerInt.put(ano.number(), cast(x));
    }


    @Override
    public void register(Class<?> x) throws IllegalAccessException, InstantiationException {
        Object y = x.newInstance();
        if (y instanceof DSerialize<?>) {
            register.put(((DSerialize)y).getClasses().getName(), cast(y));
            Marshaller ano = x.getAnnotation(Marshaller.class);
            if (ano == null) {
                throw new RuntimeException("Class serialize need Marshaller annotation ");
            }
            if (registerInt.containsKey(ano.number())) {
                throw new RuntimeException("duplicate number");
            }
            registerInt.put(ano.number(), cast(y));
        }
    }

    public void write(Object x, DOutput output) {
        String className = x.getClass().getName();
        DSerialize<?> serialize =register.get(className);
        Marshaller ano = serialize.getClass().getAnnotation(Marshaller.class);
        output.writeIntOptimise(ano.number());
        if (register.containsKey(x.getClass().getName())) {
            register.get(x.getClass().getName()).write(this, output, cast(x));
        } else {
            throw new RuntimeException("object need to register first " + className);
        }
    }

    public static <T> T cast(Object x) {
        return (T) x;
    }

    public <T> T read(DInput input) {
        int number = input.readIntPositiveOptimise();
        if (registerInt.containsKey(number)) {
            return cast(registerInt.get(number).read(this, input));
        } else {
            throw new RuntimeException("object need to register first");
        }
    }
}
