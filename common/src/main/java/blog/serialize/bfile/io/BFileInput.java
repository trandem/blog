package blog.serialize.bfile.io;

import blog.serialize.base.DInput;

import java.io.Closeable;

public interface BFileInput extends DInput, Closeable {

    void reset(int position);

    int mark();

    void skipBytes(int numBytes);
}
