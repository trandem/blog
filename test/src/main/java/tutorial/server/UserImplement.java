package tutorial.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import tutorial.proto.code.UserGrpc;
import tutorial.proto.code.UserReply;
import tutorial.proto.code.UserRequest;

import java.io.IOException;
import java.util.concurrent.Executors;

public class UserImplement extends UserGrpc.UserImplBase{

    @Override
    public void invoke(UserRequest request, io.grpc.stub.StreamObserver<UserReply> responseObserver) {
        System.out.println(request.getName() +"\t" + request.getID());


        UserReply reply = UserReply.newBuilder()
                .setMessage("reply from server ")
                .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("server listen in port " + args[0]);
        Server server = ServerBuilder.forPort(Integer.parseInt(args[0]))
                .addService(new UserImplement())
                .executor(Executors.newFixedThreadPool(10))
                .build()
                .start();
        server.awaitTermination();
    }
}
