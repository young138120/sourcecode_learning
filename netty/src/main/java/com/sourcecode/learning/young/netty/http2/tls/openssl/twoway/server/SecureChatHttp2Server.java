package com.sourcecode.learning.young.netty.http2.tls.openssl.twoway.server;

import com.sourcecode.learning.young.netty.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public final class SecureChatHttp2Server {

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
        CIPHERS = Collections.unmodifiableList(new ArrayList<String>(CIPHERS_JAVA_MOZILLA_MODERN_SECURITY));
    }

    public static void main(String[] args) throws Exception {

        File certChainFile = new File("D:\\openssl\\server\\server.crt");
        File keyFile = new File("D:\\openssl\\server\\pkcs8_server.key");
        File rootFile = new File("D:\\openssl\\server\\ca.crt");
        SslContext sslCtx = SslContextBuilder.forServer(certChainFile, keyFile).trustManager(rootFile).clientAuth(ClientAuth.REQUIRE)
                .sslProvider(SslProvider.JDK)
                .ciphers(CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        ApplicationProtocolConfig.Protocol.ALPN,
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2))
                .build();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new SecureChatHttp2ServerInitializer(sslCtx));

            Channel ch = b.bind(Constants.HOST, Constants.PORT).sync().channel();

            System.err.println("Open your HTTP/2-enabled web browser and navigate to " +
                    "https" + "://127.0.0.1:" + Constants.PORT + '/');

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}