package com.sourcecode.learning.young.netty.http.tls.openssl.twoway.client;

import com.sourcecode.learning.young.netty.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 */
public final class SecureChatClient {

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
        File certChainFile = new File("D:\\openssl\\client\\client.crt");
        File keyFile = new File("D:\\openssl\\client\\pkcs8_client.key");
        File rootFile = new File("D:\\openssl\\client\\ca.crt");
        SslContext sslCtx = SslContextBuilder.forClient().keyManager(certChainFile,keyFile).trustManager(rootFile)
                .ciphers(CIPHERS, SupportedCipherSuiteFilter.INSTANCE).build();
      //   Configure SSL.
//        final SslContext sslCtx = SslContextBuilder.forClient()
//                .trustManager(InsecureTrustManagerFactory.INSTANCE).build();

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new SecureChatClientInitializer(sslCtx));

            // Start the connection attempt.
            Channel ch = b.connect(Constants.HOST, Constants.PORT).sync().channel();

            // Read commands from the stdin.
            ChannelFuture lastWriteFuture = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            for (;;) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }

                // Sends the received line to the server.
                lastWriteFuture = ch.writeAndFlush(line + "\r\n");

                // If user typed the 'bye' command, wait until the server closes
                // the connection.
                if ("bye".equals(line.toLowerCase())) {
                    ch.closeFuture().sync();
                    break;
                }
            }

            // Wait until all messages are flushed before closing the channel.
            if (lastWriteFuture != null) {
                lastWriteFuture.sync();
            }
        } finally {
            // The connection is closed automatically on shutdown.
            group.shutdownGracefully();
        }
    }
}
