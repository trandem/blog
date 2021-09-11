package blog.common.transaction.base;

public class TnxError extends RuntimeException {

    public TnxError(String message) {
        super(message);
    }
}
