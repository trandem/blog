package blog.core.storage;

public interface Event {

    long getId();

    Type getType();

    enum Type {
        STOP((byte) 1, ""),
        START((byte) 2, ""),
        RESPONSE((byte) 9, "");


        private final byte value;
        private final String displayName;

        Type(byte value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }
    }
}
