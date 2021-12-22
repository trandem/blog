package blog.serialize.impl;

import blog.common.messenger.DTransportTopic;
import blog.common.messenger.TransportMessenger;
import blog.serialize.impl.DMarshallerIml;
import blog.serialize.test.CompanyModel;
import blog.serialize.test.UserModel;

public class DMessengerMarshaller extends DMarshallerIml {

    public DMessengerMarshaller() {
        super();
        register(new UserModel.UserModelInstanceIml());
        register(new CompanyModel.CompanyInstanceImpl());
        register(new TransportMessenger.TransportMessengerInstance());
        register(new DTransportTopic.DTransportTopicInstance());
    }
}
