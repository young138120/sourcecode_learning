package com.sourcecode.learning.young.netty.http2.tls.openssl.twoway.client;

import com.sourcecode.learning.young.netty.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.handler.codec.http.HttpMethod.POST;

/**
 *
 */
public final class SecureChatHttp2Client {

    public static final List<String> CIPHERS;

    private static final List<String> CIPHERS_JAVA_MOZILLA_MODERN_SECURITY = Collections.unmodifiableList(Arrays
            .asList(
                    "TLS_ECDSA_WITH_AES_256_GCM_SHA384",
                    "TLS_RSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
                    "TLS_RSA_WITH_CHACHA20_POLY1305_SHA256",
                    "TLS_ECDSA_WITH_AES_128_GCM_SHA256",
                    "TLS_RSA_WITH_AES_128_GCM_SHA256"
            ));

    static {
        CIPHERS = Collections.unmodifiableList(new ArrayList<>(CIPHERS_JAVA_MOZILLA_MODERN_SECURITY));
    }

    public static void main(String[] args) throws Exception {

        File certChainFile = new File("D:\\openssl\\client\\client.crt");
        File keyFile = new File("D:\\openssl\\client\\pkcs8_client.key");
        File rootFile = new File("D:\\openssl\\client\\ca.crt");

        final SslContext sslCtx = SslContextBuilder.forClient()
                .keyManager(certChainFile, keyFile).trustManager(rootFile)
                .sslProvider(SslProvider.JDK)
                .ciphers(CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        ApplicationProtocolConfig.Protocol.ALPN,
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2))
                .build();

        SecureChatHttp2ClientInitializer initializer = new SecureChatHttp2ClientInitializer(sslCtx);
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(initializer);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.remoteAddress(Constants.HOST, Constants.PORT);


            // Start the client.
            Channel channel = b.connect().syncUninterruptibly().channel();

            SecureChatHttp2ClientSettingsHandler http2SettingsHandler = initializer.getSettingsHandler();
            http2SettingsHandler.awaitSettings(5, TimeUnit.SECONDS);

            SecureChatHttp2ClientResponseHandler responseHandler = initializer.getResponseHandler();


            int streamId = 3;


            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            for (; ; ) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                FullHttpRequest httpRequest = createPostRequest(line);
                responseHandler.put(streamId, channel.write(httpRequest), channel.newPromise());
                channel.flush();
                responseHandler.awaitResponses(5, TimeUnit.SECONDS);
                streamId = streamId + 2;
                if ("bye".equalsIgnoreCase(line)) {
                    channel.close().syncUninterruptibly();
                    break;
                }
            }

        } finally {
            group.shutdownGracefully();
        }
    }

    private static FullHttpRequest createPostRequest(String message) {
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.valueOf("HTTP/2.0"), POST, "/",
                message == null ? Unpooled.EMPTY_BUFFER : wrappedBuffer(message.getBytes(CharsetUtil.UTF_8)));
        request.headers().add(HttpHeaderNames.HOST, new AsciiString(Constants.HOST + ':' + Constants.PORT));
        request.headers().add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), HttpScheme.HTTPS);
        request.headers().add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
        request.headers().add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.DEFLATE);
        return request;
    }
}
