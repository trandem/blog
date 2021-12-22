package blog.common.cluster.glosory;

import blog.serialize.base.*;

import java.util.Arrays;

public class RemoteEvent implements DMarshallable {
    private String topic;
    private String type;
    private int busId;
    private byte[] args;


    public RemoteEvent() {
    }

    public RemoteEvent(String topic, String type, int busId, byte[] args) {
        this.topic = topic;
        this.type = type;
        this.busId = busId;
        this.args = args;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getBusId() {
        return busId;
    }

    public void setBusId(int busId) {
        this.busId = busId;
    }

    public byte[] getArgs() {
        return args;
    }

    public void setArgs(byte[] args) {
        this.args = args;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output) {
        output.writeBytes(args);
        output.writeString(topic);
        output.writeString(type);
        output.writeIntOptimise(busId);
    }

    @Override
    public void read(DMarshaller marshaller, DInput input) {
        this.args = input.readBytes();
        this.topic = input.readString();
        this.type = input.readString();
        this.busId = input.readIntPositiveOptimise();
    }

    @Override
    public String toString() {
        return "RemoteEvent{" +
                "topic='" + topic + '\'' +
                ", type='" + type + '\'' +
                ", busId=" + busId +
                ", args=" + Arrays.toString(args) +
                '}';
    }

    public static class RemoteEventInstance implements DInstance<RemoteEvent> {

        @Override
        public RemoteEvent instance() {
            return new RemoteEvent();
        }
    }
}
