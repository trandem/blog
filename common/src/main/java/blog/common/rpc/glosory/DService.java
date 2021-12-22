package blog.common.rpc.glosory;

public interface DService {

    int getId();

    int getServerId();

    String getDomain();

    Object invoke(int methodSignal, Object[] args) throws Throwable;


    interface Factory {

        DService create(int serverId,String name, Object target);

    }
}
