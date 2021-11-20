package tutorial.offheap;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.util.concurrent.ConcurrentMap;

public class TestMapdb {
    public static void main(String[] args) {
        DB db = DBMaker.newMemoryDirectDB().transactionDisable().cacheDisable().make();

        ConcurrentMap<Integer, String> testMap = db.createHashMap("dev-test")
                .keySerializer(Serializer.INTEGER)
                .valueSerializer(Serializer.BYTE_ARRAY).make();
        testMap.put(111, "mot mot");
        testMap.put(112, "mot mot");
        testMap.get(111);
        testMap.get(113);
        System.out.println(testMap.remove(111));

    }
}
