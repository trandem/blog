package blog.common.rpc.service;

import blog.serialize.test.StartEvent;

public class SimpleImpl implements Simple{

    @Override
    public int add(int a, int b) {
        return a +b;
    }

    @Override
    public String showServerConfig() {
        return "demtv";
    }

    @Override
    public StartEvent getStartEvent() {
        StartEvent event = new StartEvent();
        event.setId(System.currentTimeMillis());
        return event;
    }
}
