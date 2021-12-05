package blog.serilize.impl;

import blog.serilize.base.DMarshaller;
import blog.serilize.base.DOutput;

public class DMarshallers {


    public static <T> T unMarshaller(final DMarshaller marshaller, byte[] value) {
        return marshaller.read(new DByteArrayInput(value));
    }


    public static byte[] marshaller(Object data, final DMarshaller marshaller) {
        final DOutput output = cachedOutput.get();
        try {
            marshaller.write(data, output);
            return output.toArrayBytes();
        } finally {
            output.clear();
        }

    }

    private static final ThreadLocal<DOutput> cachedOutput = ThreadLocal.withInitial(() -> new DByteArrayOutput(1024, 32 * 1024 * 1024));
}
