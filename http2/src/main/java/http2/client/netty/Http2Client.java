package http2.client.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AsciiString;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public final class Http2Client {

    public static final String HOST = "localhost";
    public static final int PORT = 8082;
    static final String URL = "/test";
    static final String DATA = "{\"aps\":{\"alert\":{\"body\":\"Example!\"}}}";

    private static final AsciiString APNS_EXPIRATION_HEADER = new AsciiString("apns-expiration");
    private static final AsciiString APNS_TOPIC_HEADER = new AsciiString("apns-topic");
    private static final AsciiString APNS_PRIORITY_HEADER = new AsciiString("apns-priority");
    private static final AsciiString APNS_AUTHORIZATION = new AsciiString("authorization");
    private static final AsciiString APNS_ID_HEADER = new AsciiString("apns-id");
    private static final AsciiString APNS_PUSH_TYPE_HEADER = new AsciiString("apns-push-type");


    public static void main(String[] args) throws Exception {

        final SslContext sslCtx;

        sslCtx = SslContextBuilder.forClient()
                .sslProvider(SslProvider.JDK)
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        ApplicationProtocolConfig.Protocol.ALPN,
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1))
                .build();

        EventLoopGroup workerEventLoop = new NioEventLoopGroup(5);
        Http2ClientInitializer initializer = new Http2ClientInitializer(sslCtx, Integer.MAX_VALUE);


        try {
            // Configure the client.
            Bootstrap b = new Bootstrap();
            b.group(workerEventLoop);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.option(ChannelOption.TCP_NODELAY, true);
            b.remoteAddress(HOST, PORT);
            b.handler(initializer);

            // Start the client.
            Channel channel = b.connect().syncUninterruptibly().channel();
            System.out.println("Connected to [" + HOST + ':' + PORT + ']');

            // Wait for the HTTP/2 upgrade to occur.
            CountDownLatch countDownLatch = new CountDownLatch(180);
            Semaphore semaphore = new Semaphore(100);
            HttpResponseHandler responseHandler = initializer.responseHandler();
            responseHandler.setSemaphore(semaphore);
            responseHandler.setCountDownLatch(countDownLatch);
            AsciiString hostName = new AsciiString(HOST + ':' + PORT);



            System.err.println("Sending request(s)...");
            long start = System.currentTimeMillis();

            for (int i = 0; i < 10000; i++) {
                semaphore.acquire();
                FullHttpRequest fullHttpRequest = new DefaultFullHttpRequest(HTTP_1_1, GET, URL, Unpooled.EMPTY_BUFFER);
                fullHttpRequest.headers().add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), HttpScheme.HTTPS.name());
                fullHttpRequest.headers().add("host", hostName);
                channel.writeAndFlush(fullHttpRequest);
            }


            System.out.println("time send request : " + (System.currentTimeMillis() - start));

            countDownLatch.await();
            long end = System.currentTimeMillis();

            System.out.println("time run is " + (end - start));
            System.out.println("Finished HTTP/2 request(s)");
        } finally {
        }
    }
}
