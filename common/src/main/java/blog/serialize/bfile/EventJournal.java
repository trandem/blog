//package blog.serialize.bfile;
//
//import blog.serialize.base.DMarshaller;
//import blog.serialize.base.DSerialize;
//import blog.serialize.bfile.io.BFileInput;
//import blog.serialize.bfile.io.BFileOutput;
//import blog.serialize.bfile.io.MMapInput;
//import blog.serialize.bfile.io.MMapOutput;
//import blog.serialize.bfile.store.MMapFileStore;
//import blog.serialize.impl.DMarshallerIml;
//import blog.serialize.test.StopEvent;
//
//import java.io.IOException;
//import java.nio.BufferOverflowException;
//
//public class EventJournal {
//    private MMapFileStore store;
//    private short magic = 12;
//    private DMarshaller dMarshaller;
//
//    public EventJournal(DMarshaller marshaller) throws IOException {
//        this.store = new MMapFileStore("test.journal");
//        this.store.setReserve(64);
//        this.dMarshaller = marshaller;
//    }
//
//    public void doStart() throws Exception {
//        this.store.doStart();
//    }
//
//    public void write(DSerialize<?> data) {
//        BFileOutput output = this.store.getOutput();
//        final int mark = output.mark();
//        try {
//            output.writeShort(magic);
//
//            final int p1 = output.mark();
//            output.writeInt(0); //size
//            dMarshaller.write(data, output);
//            final int p2 = output.mark();
//            int size = p2 - p1 - 4;
//            output.reset(p1);
//            output.writeInt(size);
//            output.reset(p2);
//        } catch (BufferOverflowException e) {
//            output.reset(mark);
//            output.writeShort((short) 0); // remove magic
//            output.reset(mark);
//            System.out.println("overflow");
//        }
//    }
//
//    public DSerialize<?> read() {
//        BFileInput input = this.store.getInput();
//        try {
//            short magic = input.readShort();
//            input.readInt();
//            if (magic == 0) return new StopEvent();
//            return this.dMarshaller.read(input);
//        } catch (BufferOverflowException e) {
//            return new StopEvent();
//        }
//    }
//
//    public MMapFileStore getStore() {
//        return store;
//    }
//
//    public static void main(String[] args) throws InstantiationException, IllegalAccessException, IOException {
//        DMarshaller marshaller = new DMarshallerIml();
//        EventJournal eventJournal = new EventJournal(marshaller);
//        eventJournal.getStore().getInput().reset(0);
//
//        while (true) {
//            DSerialize serialize = eventJournal.read();
//            if (serialize instanceof StopEvent) {
//                break;
//            }
//            System.out.println(serialize);
//        }
//
//
//    }
//
//}
