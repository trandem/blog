package blog.serialize.bfile;

import blog.serialize.Event;
import blog.serialize.bfile.journal.FileJournalReader;
import blog.serialize.bfile.journal.FileJournalWriter;
import blog.serialize.impl.AllMarshaller;
import blog.serialize.test.DataEvent;
import blog.serialize.test.StopEvent;
import blog.serialize.test.UserModel;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class JournalReadWriterTest {


    @Test
    public void testWriter() {
        File file = new File("dev-writer.dev");
        FileJournalWriter writer = new FileJournalWriter(file);
        writer.setMarshaller(AllMarshaller.DEFAULT);
        writer.start();
        int startId = 19999;
        for (int i = 0; i < 10; i++) {
            DataEvent event = new DataEvent();
            event.setId(startId++);
            UserModel userModel = new UserModel("demtv" + i, i);
            event.setSomeTestData(userModel);

            writer.write(event);
        }
        writer.flush();

        for (int i = 10; i < 20; i++) {

            DataEvent event = new DataEvent();
            event.setId(startId++);
            UserModel userModel = new UserModel("demtv" + i, i);
            event.setSomeTestData(userModel);

            writer.write(event);
        }
        writer.stop();
    }


    @Test
    public void testSeekReader() {
        File file = new File("dev-writer.dev");
        FileJournalReader reader = new FileJournalReader(file);
        reader.setMarshaller(AllMarshaller.DEFAULT);
        reader.start();
        Event event = reader.seek(1000000);
        System.out.println(event);
    }


    @Test
    public void testRead() {
        File file = new File("D:\\IdeaProjects\\blog\\concurrent-rw.dev");
        FileJournalReader reader = new FileJournalReader(file);
        reader.setMarshaller(AllMarshaller.DEFAULT);
        reader.start();
        while (true) {
            Event event = reader.read(100, TimeUnit.MILLISECONDS);
            System.out.println(event);
            if (event instanceof StopEvent) {
                break;
            }
        }
    }
}
