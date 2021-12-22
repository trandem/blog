package blog.serialize.bfile.io;

import blog.serialize.base.DOutput;

import java.io.Closeable;

public interface BFileOutput extends DOutput, Closeable {

    void reset(int position);

    int mark();

    void flush();
}
