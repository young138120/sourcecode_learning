/*
 * Copyright 2015 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.sourcecode.learning.young.netty.http2.tls.openssl.twoway.server;

import io.netty.handler.codec.http2.*;

import static io.netty.handler.logging.LogLevel.INFO;

public final class SecureChatHttp2ServerHandlerBuilder
        extends AbstractHttp2ConnectionHandlerBuilder<SecureChatHttp2ServerHandler, SecureChatHttp2ServerHandlerBuilder> {

    private static final Http2FrameLogger logger = new Http2FrameLogger(INFO, SecureChatHttp2ServerHandler.class);

    public SecureChatHttp2ServerHandlerBuilder() {
        frameLogger(logger);
    }

    @Override
    public SecureChatHttp2ServerHandler build() {
        return super.build();
    }

    @Override
    protected SecureChatHttp2ServerHandler build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                                                                                                               Http2Settings initialSettings) {
       SecureChatHttp2ServerHandler handler = new SecureChatHttp2ServerHandler(decoder, encoder, initialSettings);
        frameListener(handler);
        return handler;
    }
}
