package blog.common.cluster.glosory;

public interface Event {
    String getTopic();
    String getType();
}
