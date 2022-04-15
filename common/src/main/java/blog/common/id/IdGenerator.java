package blog.common.id;

import java.time.Instant;
import java.util.Date;

public class IdGenerator {

    private static final int NODE_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final int maxSequence = (int) (Math.pow(2, SEQUENCE_BITS) - 1);

    private static final long CUSTOM_EPOCH = 1420070400000L;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public static IdGenerator instance = new IdGenerator();

    public synchronized long nextId() {
        long id = nextId(188);
        return id;
    }


    private synchronized long nextId(int nodeId) {
        long currentTimestamp = timestamp();
        System.out.println(currentTimestamp);
        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Invalid System Clock!");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & maxSequence;
            if (sequence == 0) {
                // Sequence Exhausted, wait till next millisecond.
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            // reset sequence to start with zero for the next millisecond
            sequence = 0;
        }

        lastTimestamp = currentTimestamp;

        long id = currentTimestamp << (NODE_ID_BITS + SEQUENCE_BITS);

        id |= (nodeId << SEQUENCE_BITS);
        id |= sequence;
        return id;
    }


    private static long timestamp() {
        long current = Instant.now().toEpochMilli();
        System.out.println(current);
        return current - CUSTOM_EPOCH;
    }

    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp == lastTimestamp) {
            currentTimestamp = timestamp();
        }
        return currentTimestamp;
    }

    public short getNodeIdById(long id) {
        return (short) ((id >> SEQUENCE_BITS) & 0x2ff);
    }

    public long getTimestampById(long id) {
        return (id >> 22) + CUSTOM_EPOCH;
    }

    public static int generateCustomerId(short nationalId, int sequence) {
        int id = ((int) (nationalId & 0x1ff)) << 23;
        id |= sequence & 0x007FFFFF;
        return id;
    }

    public static short getNationalId(int customerId) {
        short id = (short) ((short) (customerId >> 23) & 0x1ff);
        return id;
    }

    public static void main(String[] args) {
//        IdGenerator instance = IdGenerator.instance;
//        System.out.println(instance.nextId());
//        System.out.println(instance.getNodeIdById(964064791746355200L));
//        System.out.println(instance.getNodeIdById(964064791750549504L));
//        System.out.println(instance.getNodeIdById(964064791750549505L));
//
//        System.out.println(instance.getNodeIdById(964064976837525504L));
//        System.out.println(instance.getNodeIdById(964064976837525505L));
//        System.out.println(instance.getNodeIdById(964064976837525506L));
//
//        System.out.println(new Date(instance.getTimestampById(964064791746355200L)));
//        System.out.println(new Date(instance.getTimestampById(964064791750549504L)));
//        System.out.println(new Date(instance.getTimestampById(964064791750549505L)));
//
//        System.out.println(new Date(instance.getTimestampById(964064976837525504L)));
//        System.out.println(new Date(instance.getTimestampById(964064976837525505L)));
//        System.out.println(new Date(instance.getTimestampById(964064976837525506L)));
        //System.out.println((18 << 10));


        int id1 = generateCustomerId((short)111,300);
        int id2 = generateCustomerId((short)112,300);
        int id3 = generateCustomerId((short)113,300);
        int id4 = generateCustomerId((short)114,300);
        int id5 = generateCustomerId((short)115,300);

        System.out.println(getNationalId(id1));
        System.out.println(getNationalId(id2));
        System.out.println(getNationalId(id3));
        System.out.println(getNationalId(id4));
        System.out.println(getNationalId(id5));
    }

}
