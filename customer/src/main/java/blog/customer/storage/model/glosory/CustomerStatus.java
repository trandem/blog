package blog.customer.storage.model.glosory;

public enum CustomerStatus {

    ENABLE((byte) 1, "active"),
    DISABLE((byte) 0, "in active");

    private byte value;
    private String displayName;

    public byte getValue() {
        return value;
    }

    CustomerStatus(byte status, String displayName) {
        this.value = status;
        this.displayName = displayName;
    }

    public static CustomerStatus valueOf(byte value) {
        switch (value) {
            case 1:
                return ENABLE;
            case 0:
                return DISABLE;
            default:
                throw new RuntimeException("CustomerStatus is not found");
        }
    }

}
