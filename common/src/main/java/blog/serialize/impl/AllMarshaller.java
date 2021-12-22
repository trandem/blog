package blog.serialize.impl;

import blog.common.cluster.glosory.RemoteEvent;
import blog.common.messenger.DTransportTopic;
import blog.common.messenger.TransportMessenger;
import blog.common.rpc.glosory.RpcRequest;
import blog.common.rpc.glosory.RpcResponse;
import blog.serialize.test.*;

public class AllMarshaller extends DMarshallerIml {

    public static AllMarshaller DEFAULT = new AllMarshaller();

    private AllMarshaller() {
        super();
        register(new UserModel.UserModelInstanceIml());
        register(new CompanyModel.CompanyInstanceImpl());
        register(new TransportMessenger.TransportMessengerInstance());
        register(new DTransportTopic.DTransportTopicInstance());
        register(new StartEvent.StartEventInstance());
        register(new StopEvent.StopEventInstance());
        register(new DataEvent.DataEventInstance());

        register(new RemoteEvent.RemoteEventInstance());

        register(new RpcRequest.RpcRequestInstance());
        register(new RpcResponse.RpcResponseInstance());
    }
}
