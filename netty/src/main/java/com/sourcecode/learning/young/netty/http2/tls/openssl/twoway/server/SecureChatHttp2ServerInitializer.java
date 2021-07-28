package com.sourcecode.learning.young.netty.http2.tls.openssl.twoway.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AsciiString;

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel.
 */
public class SecureChatHttp2ServerInitializer extends ChannelInitializer<SocketChannel> {


    private static final HttpServerUpgradeHandler.UpgradeCodecFactory upgradeCodecFactory = new HttpServerUpgradeHandler.UpgradeCodecFactory() {
        @Override
        public HttpServerUpgradeHandler.UpgradeCodec newUpgradeCodec(CharSequence protocol) {
            if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                return new Http2ServerUpgradeCodec(new SecureChatHttp2ServerHandlerBuilder().build());
            } else {
                return null;
            }
        }
    };

    private final SslContext sslCtx;
    private final int maxHttpContentLength;

    public SecureChatHttp2ServerInitializer(SslContext sslCtx) {
        this(sslCtx, 16 * 1024);
    }

    public SecureChatHttp2ServerInitializer(SslContext sslCtx, int maxHttpContentLength) {
        if (maxHttpContentLength < 0) {
            throw new IllegalArgumentException("maxHttpContentLength (expected >= 0): " + maxHttpContentLength);
        }
        this.sslCtx = sslCtx;
        this.maxHttpContentLength = maxHttpContentLength;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        configureSsl(ch);
    }

    private void configureSsl(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
        final ChannelPipeline p = ch.pipeline();
        final HttpServerCodec sourceCodec = new HttpServerCodec();
        final HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(new HttpServerCodec(), upgradeCodecFactory);
        final CleartextHttp2ServerUpgradeHandler cleartextHttp2ServerUpgradeHandler =
                new CleartextHttp2ServerUpgradeHandler(sourceCodec, upgradeHandler, new SecureChatHttp2ServerHandlerBuilder().build());
        p.addLast(cleartextHttp2ServerUpgradeHandler);
        p.addLast(new UserEventLogger());
    }


    /**
     * Class that logs any User Events triggered on this channel.
     */
    private static class UserEventLogger extends ChannelInboundHandlerAdapter {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            System.out.println("User Event Triggered: " + evt);
            ctx.fireUserEventTriggered(evt);
        }
    }
}
