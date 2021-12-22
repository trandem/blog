package blog.serialize.bfile.store;

import blog.common.glosory.LifeCycle;
import blog.serialize.bfile.io.BFileInput;
import blog.serialize.bfile.io.BFileOutput;

public interface BFileStore extends LifeCycle {

    BFileInput getInput();

    BFileOutput getOutput();

    void reserve(int size);
}
