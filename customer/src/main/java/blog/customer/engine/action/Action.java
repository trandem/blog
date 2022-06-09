package blog.customer.engine.action;

import blog.customer.engine.pipeline.request.Request;
import blog.customer.engine.pipeline.response.Response;

public interface Action<Req extends Request, Res extends Response> {

    Res doAction(Req req);

}
