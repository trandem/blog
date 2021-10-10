package tutorial.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import tutorial.proto.code.UserGrpc;
import tutorial.proto.code.UserReply;
import tutorial.proto.code.UserRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserLB {
    private final Map<String, UserGrpc.UserBlockingStub> blockingStubMap;
    private final Map<String, ManagedChannel> channelMap;


    public UserLB() {
        this.blockingStubMap = new ConcurrentHashMap<>();
        this.channelMap = new ConcurrentHashMap<>();
    }

    public UserGrpc.UserBlockingStub getClient(Integer userId) {
        if (userId < 100) {
            return blockingStubMap.get("localhost:8081");
        } else {
            return blockingStubMap.get("localhost:8080");
        }
    }

    public void updateListServer(List<String> hostAndPortList) {
        for (String hostAndPort : hostAndPortList) {
            String host = hostAndPort.split(":")[0];
            Integer port = Integer.parseInt(hostAndPort.split(":")[1]);
            ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
            ManagedChannel oldChannel = channelMap.get(hostAndPort);
            channelMap.put(hostAndPort, channel);
            UserGrpc.UserBlockingStub blockingStub = UserGrpc.newBlockingStub(channel);
            blockingStubMap.put(hostAndPort, blockingStub);
            try {
                if (oldChannel != null) {
                    oldChannel.shutdown();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void invoke(int userId) {
        UserRequest request = UserRequest.newBuilder()
                .setID(userId)
                .setName("data" + userId)
                .build();
        UserGrpc.UserBlockingStub stub = getClient(userId);
        UserReply reply = stub.invoke(request);
        System.out.println(reply.getMessage());
    }

    public static void main(String[] args) {
        UserLB userLB = new UserLB();
        userLB.updateListServer(Arrays.asList("localhost:8080","localhost:8081"));
        userLB.invoke(10);
        userLB.invoke(11);
        userLB.invoke(12);
        userLB.invoke(13);
        userLB.invoke(101);
        userLB.invoke(102);
        userLB.invoke(103);
        userLB.invoke(104);

    }
}
