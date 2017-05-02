/*
 * Copyright 2017 M. Isuru Tharanga Chrishantha Perera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.chrishantha.perf.netty;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * Echoes back any received data from an HTTP client.
 */
public final class EchoServer {

    @Parameter(names = "--port", description = "Server Port")
    private int port = 8688;

    @Parameter(names = "--boss-threads", description = "Boss Threads")
    private int bossThreads = Runtime.getRuntime().availableProcessors();

    @Parameter(names = "--worker-threads", description = "Worker Threads")
    private int workerThreads = 200;

    @Parameter(names = "--enable-ssl", description = "Enable SSL")
    private boolean enableSSL = false;

    @Parameter(names = "--sleep-time", description = "Sleep Time in milliseconds")
    private int sleepTime = 0;

    @Parameter(names = "--help", description = "Display Help", help = true)
    private boolean help = false;

    public static void main(String[] args) throws Exception {
        EchoServer echoServer = new EchoServer();
        final JCommander jcmdr = new JCommander(echoServer);
        jcmdr.setProgramName(EchoServer.class.getSimpleName());
        jcmdr.parse(args);

        System.out.println(echoServer);

        if (echoServer.help) {
            jcmdr.usage();
            return;
        }

        echoServer.startServer();
    }

    private void startServer() throws SSLException, CertificateException, InterruptedException {
        // Configure SSL.
        final SslContext sslCtx;
        if (enableSSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(bossThreads);
        EventLoopGroup workerGroup = new NioEventLoopGroup(workerThreads);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc()));
                            }
                            p.addLast(new HttpServerCodec());
                            p.addLast("aggregator", new HttpObjectAggregator(1048576));
                            p.addLast(new EchoServerHandler(sleepTime));
                        }
                    });


            // Start the server.
            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync();

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public String toString() {
        return "EchoServer{" +
                "port=" + port +
                ", bossThreads=" + bossThreads +
                ", workerThreads=" + workerThreads +
                ", enableSSL=" + enableSSL +
                ", sleepTime=" + sleepTime +
                '}';
    }
}
