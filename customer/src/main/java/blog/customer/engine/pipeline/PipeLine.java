package blog.customer.engine.pipeline;

import blog.customer.engine.signal.RequestSignal;

public interface PipeLine {

    boolean submit(RequestSignal t);
}
