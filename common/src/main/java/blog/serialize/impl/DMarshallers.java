package blog.serialize.impl;

import blog.serialize.base.DMarshaller;
import blog.serialize.base.DOutput;
import blog.serialize.impl.io.DByteArrayInput;
import blog.serialize.impl.io.DByteArrayOutput;

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
