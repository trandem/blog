package blog.common.messenger;

import blog.serialize.base.DInput;
import blog.serialize.base.DInstance;
import blog.serialize.base.DMarshaller;
import blog.serialize.base.DOutput;

import java.util.Objects;

public class DTransportTopic implements TransportTopic {
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public DTransportTopic(String name) {
        this.name = name;
    }

    public DTransportTopic() {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output) {
        output.writeString(name);
    }

    @Override
    public void read(DMarshaller marshaller, DInput input) {
        this.name = input.readString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DTransportTopic that = (DTransportTopic) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public String toString() {
        return "DTransportTopic{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public static class DTransportTopicInstance implements DInstance<DTransportTopic> {

        @Override
        public DTransportTopic instance() {
            return new DTransportTopic();
        }
    }
}
