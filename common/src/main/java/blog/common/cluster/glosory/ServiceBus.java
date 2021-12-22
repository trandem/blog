package blog.common.cluster.glosory;

public interface ServiceBus extends DCycler{
    int getId();

    void registerListener(Object target);

    void unRegisterListener(Object target);

    void notify(Event event, Object... args);
}
