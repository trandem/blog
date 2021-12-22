package blog.common.rpc.glosory;

import blog.serialize.base.DInstance;

public class RpcServerException extends  RuntimeException {
    private String msg;

    public RpcServerException() {
    }



    public static class RpcServerExceptionInstance implements DInstance<RpcServerException>{

        @Override
        public RpcServerException instance() {
            return new RpcServerException();
        }
    }
}
