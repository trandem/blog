package blog.rpc.service.impl;

import blog.rpc.service.SimpleService;
import blog.rpc.support.User;

public class ServerImpl implements SimpleService {
    @Override
    public int add(int a, int b) {
        return a+b;
    }

    @Override
    public User getUser() {
        return new User("demtv");
    }
}
