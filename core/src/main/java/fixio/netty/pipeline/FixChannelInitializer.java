/*
 * Copyright 2014 The FIX.io Project
 *
 * The FIX.io Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package fixio.netty.pipeline;

import fixio.fixprotocol.FixMessage;
import fixio.fixprotocol.FixMessageBuilder;
import fixio.handlers.FixApplication;
import fixio.netty.codec.FixMessageDecoder;
import fixio.netty.codec.FixMessageEncoder;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * FixChannelInitializer
 *
 * @author Konstantin Pavlov
 */
public abstract class FixChannelInitializer<C extends Channel> extends ChannelInitializer<C> {

    protected static final String TAG_DECODER_HANDLER_NAME = "tagDecoder";
    private static final FixMessageEncoder ENCODER = new FixMessageEncoder();
    private final EventLoopGroup workerGroup;

    private final FixApplication fixApplication;

    private final TestRequestHandler testRequestHandler = new TestRequestHandler();

    protected FixChannelInitializer(EventLoopGroup workerGroup, FixApplication fixApplication) {
        this.workerGroup = workerGroup;
        this.fixApplication = fixApplication;
    }

    @Override
    public void initChannel(C ch) throws Exception {
        final ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(TAG_DECODER_HANDLER_NAME, new DelimiterBasedFrameDecoder(1024, Unpooled.wrappedBuffer(new byte[]{1})));
        pipeline.addLast("fixMessageDecoder", new FixMessageDecoder());
        pipeline.addLast("fixMessageEncoder", ENCODER);
        pipeline.addLast("logging", new LoggingHandler("fix", LogLevel.DEBUG));
        pipeline.addLast("session", createSessionHandler()); // handle fix session
        pipeline.addLast("testRequest", testRequestHandler); // process test requests
        if (fixApplication != null) {
            pipeline.addLast(workerGroup, "app", fixApplication); // process application events events
        }

    }

    protected abstract MessageToMessageCodec<FixMessage, FixMessageBuilder> createSessionHandler();

    protected FixApplication getFixApplication() {
        return fixApplication;
    }
}
