package blog.common.messenger;

import blog.common.concurrent.DThread;
import blog.common.concurrent.XFuture;
import blog.serialize.impl.DMarshallers;
import blog.serialize.impl.DMessengerMarshaller;
import blog.serialize.base.DMarshaller;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class KafkaMessenger<T> extends AbstractMessenger<T> {

    public static KafkaMessenger<Object> instance;


    public static synchronized KafkaMessenger<Object> getInstance()  {
        if (instance == null) {
            instance = new KafkaMessenger<>();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    private final Consumer<byte[], byte[]> kafkaConsumer = createConsumer();
    private final Producer<byte[], byte[]> kafkaProducer = createProducer();

    private final DMarshaller msgMarshaller = new DMessengerMarshaller();
    private final ExecutorService defaultExecutor = Executors.newFixedThreadPool(4);

    private volatile boolean isConsume;


    private KafkaMessenger() {
        isConsume = true;
        ConsumeMsg consumeMsg = new ConsumeMsg();
        consumeMsg.start();
    }

    public void onMsg(ConsumerRecord<byte[], byte[]> record) {
        final TransportMessenger messenger = DMarshallers.unMarshaller(msgMarshaller, record.value());
        TransportTopic topic = messenger.getTopic();
        List<DMessengerListener> listeners = listenerMap.get(topic);
        if (listeners == null) {
            System.out.println("service is not register ");
            return;
        }
        ExecutorService executorService = executorServiceMap.get(topic);

        final DMarshaller marshaller = marshallerMap.get(topic);
        if (marshaller == null) {
            System.out.println("marshaller is not register");
            return;
        }

        if (executorService == null) {
            executorService = defaultExecutor;
        }


        executorService.submit(() -> {
            byte[] payload = messenger.getPayload();

            for (DMessengerListener listener : listeners) {
                listener.onMessage(DMarshallers.unMarshaller(marshaller, payload));
            }
        });

    }


    @Override
    public Future<Void> send(T message, TransportTopic topic) {
        final DMarshaller marshaller = marshallerMap.get(topic);
        if (marshaller == null) {
            throw new RuntimeException("marshaller is not register when send");
        }
        try {
            byte[] payload = DMarshallers.marshaller(message, marshaller);
            final TransportMessenger transportMessenger = new TransportMessenger(payload, topic);
            ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(topic.getName(), null, DMarshallers.marshaller(transportMessenger, msgMarshaller));
            kafkaProducer.send(record);
            kafkaProducer.flush();
            return new XFuture<>();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static Consumer<byte[], byte[]> createConsumer() {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, System.getProperty("Group")+ System.currentTimeMillis());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 10);
//        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 30);
//        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 100);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

        final Consumer<byte[], byte[]> consumer = new KafkaConsumer<>(props);

        consumer.subscribe(Arrays.asList("blog", "cluster","rpc"));
        return consumer;
    }

    private static Producer<byte[], byte[]> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, System.getProperty("Group"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        return new KafkaProducer<>(props);
    }


    private class ConsumeMsg extends DThread {

        @Override
        public void run() {
            while (isConsume) {
                try {
                    ConsumerRecords<byte[], byte[]> records = kafkaConsumer.poll(Duration.ofMillis(10));
                    if (records.count() != 0) {
                        records.forEach(KafkaMessenger.this::onMsg);
                    }
                } catch (Throwable e) {
                    System.out.println("something is wrong " + e.getMessage());
                }
            }
        }

    }
}
