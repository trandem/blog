package blog.customer.engine.pipeline.response;

public enum CustomerResponseResult implements Response.Result {

    SUCCESS((byte) 0),
    FAIL((byte) 1),
    INVALID((byte) 2);

    private byte code;

    CustomerResponseResult(byte id) {
        this.code = id;
    }

    @Override
    public boolean isSuccess() {
        return this.equals(SUCCESS);
    }

    public byte getCode() {
        return code;
    }

    public static CustomerResponseResult parse(byte code) {
        switch (code) {
            case 0:
                return SUCCESS;
            case 1:
                return FAIL;
            case 2:
                return INVALID;
            default:
                throw new RuntimeException("wrong code number");
        }
    }
}
