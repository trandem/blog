package blog.core.storage;

import blog.serialize.base.*;

import java.util.List;

public class ResponseEvent<T> implements Event, DMarshallable {
    private long id;

    private List<T> response;

    public ResponseEvent() {
    }

    public ResponseEvent(long id, List<T> response) {
        this.id = id;
        this.response = response;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<T> getResponse() {
        return response;
    }

    public void setResponse(List<T> response) {
        this.response = response;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Type getType() {
        return Type.RESPONSE;
    }

    @Override
    public void write(DMarshaller marshaller, DOutput output) {
        output.writeLong(id);
        marshaller.write(response, output);
    }

    @Override
    public void read(DMarshaller marshaller, DInput input) {
        this.id = input.readLong();
        this.response = marshaller.read(input);
    }

    public static class ResponseInstance implements DInstance<ResponseEvent>{
        @Override
        public ResponseEvent instance() {
            return new ResponseEvent<>();
        }
    }
}
