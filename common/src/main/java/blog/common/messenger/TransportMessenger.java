package blog.common.messenger;

import blog.serialize.base.*;

public class TransportMessenger implements DMarshallable {

    private byte[] payload;

    private TransportTopic topic;

    public TransportMessenger() {
    }

    public TransportMessenger(byte[] payload, TransportTopic topic) {
        this.payload = payload;
        this.topic = topic;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public TransportTopic getTopic() {
        return topic;
    }

    public void setTopic(TransportTopic topic) {
        this.topic = topic;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output) {
        marshaller.write(topic, output);
        output.writeBytes(payload);
    }

    @Override
    public void read(DMarshaller marshaller, DInput input) {
        this.topic = marshaller.read(input);
        this.payload = input.readBytes();
    }


    public static class TransportMessengerInstance implements DInstance<TransportMessenger> {

        @Override
        public TransportMessenger instance() {
            return new TransportMessenger();
        }
    }
}
