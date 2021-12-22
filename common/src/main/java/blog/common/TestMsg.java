package blog.common;

import blog.common.messenger.DMessenger;
import blog.common.messenger.DMessengerListener;
import blog.common.messenger.KafkaMessenger;
import blog.common.messenger.TransportTopic;

import blog.serialize.impl.AllMarshaller;
import blog.serialize.test.StartEvent;
import blog.serialize.test.StopEvent;

import java.util.Date;
import java.util.concurrent.Executors;

public class TestMsg implements DMessengerListener {
    private DMessenger<Object> messenger = KafkaMessenger.getInstance();
    private final TransportTopic topic;

    public TestMsg() {
        this.topic = messenger.registerTopic("blog");
    }
    public void start(){
        messenger.addListener(topic, this);
        messenger.setMarshaller(topic, AllMarshaller.DEFAULT);
        messenger.setExecutor(topic, Executors.newSingleThreadExecutor());
    }


    public void sendStart() {
        StartEvent event = new StartEvent();
        messenger.send(event, topic);
    }

    public void sendStopEvent() {
        StopEvent event = new StopEvent();
        messenger.send(event, topic);
    }


    @Override
    public void onMessage(Object data) {
        if (data instanceof StartEvent) {
            System.out.println("onMessage start " + data);
        } else if (data instanceof StopEvent) {
            System.out.println("onMessage stop " + data);
        }
        System.out.println(new Date().getTime());
    }


    public static void main(String[] args) throws InterruptedException {
        TestMsg testMsg = new TestMsg();
        testMsg.start();
        testMsg.sendStart();
        testMsg.sendStopEvent();
    }
}
