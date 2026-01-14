package fr.qsh.fmt.java.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CodeSnippet2 {

    static class NettyServer {
        public static void main(String[] args) throws IOException {


            EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(
                    1,
                    NioIoHandler.newFactory());
            EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(
                    NioIoHandler.newFactory());

            try {

                SimpleChannelInboundHandler<String> handler =
                        new SimpleChannelInboundHandler<>() {
                            @Override
                            protected void channelRead0(
                                    ChannelHandlerContext ctx,
                                    String msg) {
                                System.out.println("=>" + msg);
                                ctx.writeAndFlush(msg + "\n"); // echo
                            }
                        };


                ChannelInitializer<SocketChannel> childHandler = new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // inbound
                        pipeline.addLast(new LineBasedFrameDecoder(1024));
                        pipeline.addLast(new StringDecoder());
                        // outbound
                        pipeline.addLast(new StringEncoder());
                        // inbound
                        pipeline.addLast(handler);
                    }
                };

                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(childHandler);

                int port = 5042;
                ChannelFuture f = b.bind(port).sync();
                System.out.println("Started on port " + port);

                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }
    }

    static class NettyServerComplete {
        public static void main(String[] args) throws IOException {
            EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(1,
                    NioIoHandler.newFactory());
            EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(
                    NioIoHandler.newFactory());
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new LineBasedFrameDecoder(1024));
                                pipeline.addLast(new StringDecoder());
                                pipeline.addLast(new StringEncoder());
                                pipeline.addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(
                                            ChannelHandlerContext ctx,
                                            String msg) {
                                        System.out.println("=>" + msg);
                                        ctx.writeAndFlush(msg + "\n"); // echo
                                    }
                                });
                            }
                        });

                int port = 5042;
                ChannelFuture f = b.bind(port).sync();
                System.out.println("Started on port " + port);
                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }
    }

    static class NettyUnlimitedServer {
        public static void main(String[] args) throws IOException {
            int port = 5042;

            EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(
                    1,
                    NioIoHandler.newFactory());
            EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(
                    NioIoHandler.newFactory());

            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();

                                // 1️⃣ Decoder personnalisé "sans limite"
                                pipeline.addLast(new UnlimitedLineDecoder());
                                pipeline.addLast(new StringEncoder());
                                // 2️⃣ Handler echo
                                pipeline.addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                        System.out.println("Received line: " + msg);
                                        ctx.writeAndFlush(msg + "\n"); // echo
                                    }
                                });
                            }
                        });

                ChannelFuture f = b.bind(port).sync();
                System.out.println("Unlimited Line Echo Server started on port " + port);
                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }

        // --- Decoder qui lit jusqu'au '\n' sans limite fixe ---
        static class UnlimitedLineDecoder extends ByteToMessageDecoder {
            @Override
            protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
                int start = in.readerIndex();
                int readable = in.readableBytes();

                for (int i = 0; i < readable; i++) {
                    if (in.getByte(start + i) == '\n') {
                        ByteBuf lineBuf = in.readRetainedSlice(i); // prend i octets
                        in.skipBytes(1); // sauter le '\n'
                        out.add(lineBuf.toString(CharsetUtil.UTF_8));
                        return; // une ligne décodée, on sort
                    }
                }
                // Pas encore de '\n', on attend plus d'octets
            }
        }
    }

    static class NettyClient {
        public static void main(String[] args) throws IOException {
            EventLoopGroup eventLoop = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
            try {
                Bootstrap b = new Bootstrap();
                b.group(eventLoop)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new LineBasedFrameDecoder(1024));
                                pipeline.addLast(new StringDecoder());
                                pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
                                pipeline.addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                        System.out.println("Echo from server: " + msg);
                                    }
                                });
                            }
                        });

                ChannelFuture f = b.connect("localhost", 5042).sync();
                Channel channel = f.channel();
                for (int i = 1; i <= 1_000_0000; i++) {
                    channel.writeAndFlush("Hello World " + i + "\n");
                }

                // attendre un peu pour recevoir les echoes
                Thread.sleep(2000);

                channel.close().sync();

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                eventLoop.shutdownGracefully();
            }
        }
    }
}



