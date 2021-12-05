package blog.serilize.impl;

import blog.serilize.base.DMarshaller;
import blog.serilize.base.DSerialize;
import blog.serilize.test.StopEvent;
import blog.serilize.test.User;

import java.io.IOException;
import java.nio.BufferOverflowException;

public class EventJournal {
    private MMapFileStore store;
    private short magic = 12;
    private DMarshaller dMarshaller;

    public EventJournal(DMarshaller marshaller) throws IOException {
        this.store = new MMapFileStore("test.journal");
        this.store.setReserve(64);
        this.dMarshaller = marshaller;
    }

    public void write(DSerialize<?> data) {
        MMapOutput output = this.store.getOutput();
        final int mark = output.mark();
        try {
            output.writeShort(magic);

            final int p1 = output.mark();
            output.writeInt(0); //size
            dMarshaller.write(data, output);
            final int p2 = output.mark();
            int size = p2 - p1 - 4;
            output.reset(p1);
            output.writeInt(size);
            output.reset(p2);
        } catch (BufferOverflowException e) {
            output.reset(mark);
            output.writeShort((short) 0); // remove magic
            output.reset(mark);
            System.out.println("overflow");
        }
    }

    public DSerialize<?> read() {
        MMapInput input = this.store.getInput();
        try {
            short magic = input.readShort();
            input.readInt();
            if (magic == 0) return new StopEvent();
            return this.dMarshaller.read(input);
        } catch (BufferOverflowException e) {
            return new StopEvent();
        }
    }

    public MMapFileStore getStore() {
        return store;
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException, IOException {
        DMarshaller marshaller = new DMarshallerIml();
        EventJournal eventJournal = new EventJournal(marshaller);
        User demtv = new User("demtv");

        eventJournal.write(demtv);
        eventJournal.write(demtv);
        eventJournal.write(demtv);
        eventJournal.write(demtv);
        eventJournal.write(demtv);
        eventJournal.write(demtv);
        eventJournal.write(demtv);
        eventJournal.getStore().getInput().reset(0);

        while (true) {
            DSerialize serialize = eventJournal.read();
            if (serialize instanceof StopEvent) {
                break;
            }
            System.out.println(serialize);
        }


    }

}
