package com.sourcecode.learning.young.netty.http2.tls.openssl.twoway.client;

import com.sourcecode.learning.young.netty.http2.tls.openssl.twoway.Http2Util;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.ssl.SslContext;

import javax.net.ssl.SSLException;

import static io.netty.handler.logging.LogLevel.INFO;

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel.
 */
public class SecureChatHttp2ClientInitializer extends ChannelInitializer<SocketChannel> {

    private static final Http2FrameLogger logger = new Http2FrameLogger(INFO, SecureChatHttp2ClientInitializer.class);
    private final SslContext sslCtx;
    private final int maxContentLength = 1024;
    private SecureChatHttp2ClientSettingsHandler settingsHandler;
    private SecureChatHttp2ClientResponseHandler responseHandler;

    public SecureChatHttp2ClientInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        settingsHandler = new SecureChatHttp2ClientSettingsHandler(ch.newPromise());
        responseHandler = new SecureChatHttp2ClientResponseHandler();
        configureSsl(ch);
    }


    private void configureSsl(SocketChannel ch) throws SSLException {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(sslCtx.newHandler(ch.alloc(), SecureChatHttp2Client.HOST, SecureChatHttp2Client.PORT));
        pipeline.addLast(Http2Util.getClientAPNHandler(maxContentLength, settingsHandler, responseHandler));
    }

    public SecureChatHttp2ClientResponseHandler getResponseHandler() {
        return responseHandler;
    }

    public SecureChatHttp2ClientSettingsHandler getSettingsHandler() {
        return settingsHandler;
    }
}
