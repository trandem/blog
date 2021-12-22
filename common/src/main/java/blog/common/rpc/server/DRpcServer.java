package blog.common.rpc.server;

import blog.common.rpc.glosory.DService;

public interface DRpcServer {

    void addService(DService service);

    DService getService(String  name);

    DService removeService(String  name);

    void createService(String name,Object object);
}
