package com.sourcecode.learning.young.netty.http2.tls.openssl.twoway.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2Settings;

import java.util.concurrent.TimeUnit;

/**
 * @author yangjw
 * @date 2020/12/1515:29
 */
public class SecureChatHttp2ClientSettingsHandler extends SimpleChannelInboundHandler<Http2Settings> {
    private final ChannelPromise promise;

    public SecureChatHttp2ClientSettingsHandler(ChannelPromise promise) {
        this.promise = promise;
    }

    public void awaitSettings(long timeout, TimeUnit unit) throws Exception {
        if (!promise.awaitUninterruptibly(timeout, unit)) {
            throw new IllegalStateException("Timed out waiting for settings");
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2Settings msg) throws Exception {
        promise.setSuccess();

        ctx.pipeline()
                .remove(this);
    }
}
