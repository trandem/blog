package http2.client.okhttp;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class OkhttpClient {
    public static void main(String[] args) throws Exception {
        OkHttpClient client = getUnsafeOkHttpClient();
        client.dispatcher().setMaxRequestsPerHost(1);
        Request request = new Request.Builder()
                .url("https://localhost:8082/test")
                .build();

        Response r1 = client.newCall(request).execute();
        System.out.println(r1.protocol());
        client.dispatcher().setMaxRequestsPerHost(20);
        CountDownLatch countDownLatch = new CountDownLatch(180);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 180; i++) {
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    response.close();
                    countDownLatch.countDown();
                }

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {

                }
            });
        }
        countDownLatch.await();
        long time = System.currentTimeMillis() - startTime;
        System.out.println("time run " + time);
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            ConnectionPool connectionPool = new ConnectionPool(1, 3000, TimeUnit.SECONDS);

            // Install the all-trusting trust manager

            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

            X509TrustManager manager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };

            TrustManager[] managers = new TrustManager[1];
            managers[0] = manager;

            sslContext.init(null, managers, null);


            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .followRedirects(false)
                    .sslSocketFactory(sslContext.getSocketFactory(), manager)
                    .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
                    .retryOnConnectionFailure(true)
                    .connectionPool(connectionPool)
                    .build();

            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
