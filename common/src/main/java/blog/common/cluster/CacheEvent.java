package blog.common.cluster;

import blog.common.cluster.glosory.Event;

public class CacheEvent implements Event {
    private String topic;
    private String type;


    public CacheEvent(String topic, String type) {
        this.topic = topic;
        this.type = type;
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public String getType() {
        return type;
    }
}
