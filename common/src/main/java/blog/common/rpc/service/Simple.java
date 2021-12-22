package blog.common.rpc.service;

import blog.serialize.test.StartEvent;

public interface Simple {

    int add(int a,int b);

    String showServerConfig();

    StartEvent getStartEvent();
}
