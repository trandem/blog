package blog.serialize.impl;

import blog.common.cluster.glosory.RemoteEvent;
import blog.common.messenger.DTransportTopic;
import blog.common.messenger.TransportMessenger;
import blog.common.rpc.glosory.RpcRequest;
import blog.common.rpc.glosory.RpcResponse;
import blog.core.storage.ResponseEvent;
import blog.serialize.base.DInput;
import blog.serialize.base.DMarshaller;
import blog.serialize.base.DOutput;
import blog.serialize.impl.io.DByteArrayInput;
import blog.serialize.impl.io.DByteArrayOutput;
import blog.serialize.test.*;

import java.util.ArrayList;
import java.util.List;

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
        register(new ResponseEvent.ResponseInstance());
    }

    public static void main(String[] args) {
        DMarshaller marshaller = AllMarshaller.DEFAULT;

        ResponseEvent<UserModel> response1 = new ResponseEvent<>();
        response1.setId(1000);
        List<UserModel> userModels = new ArrayList<>();
        userModels.add(new UserModel("demtv",11));
        userModels.add(new UserModel("demtv2",12));
        response1.setResponse(userModels);

        DOutput output = new DByteArrayOutput();
        marshaller.write(response1,output);

        DInput input = new DByteArrayInput(output.toArrayBytes());
        ResponseEvent<UserModel> response2 = marshaller.read(input);

        System.out.println(response2.getResponse());
        System.out.println(response2.getId());


    }
}
