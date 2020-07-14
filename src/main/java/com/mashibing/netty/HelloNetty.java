package com.mashibing.netty;

import com.mashibing.io.aio.Server;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

public class HelloNetty {
    public static void main(String[] args) {
        new NettyServer(8888).serverStart();
    }
}

class NettyServer {


    int port = 8888;

    public NettyServer(int port) {
        this.port = port;
    }

    public void serverStart() {
        //两个线程池：大管家(负责客户端连接)、工人（负责连接以后的IO处理）
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();

        //通道类型：NioServerSocketChannel
        //通道(SocketChannel)要连接时，给一个监听器：childHandler；这个监听器的处理过程是：当通道初始化以后，在这个通道上加一个通道的处理器Handler（又是一个监听器）
        //Netty的好处就是将连接的代码和业务的代码分开

        //handler()是在加在了Server整个面板上以及Client连上来的所有SocketChannel上
        //childHandler是SocketChannel已经连接了，加在它的孩子们身上，也就是加载Client客户端上面
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
//                .handler()
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new Handler());
                    }
                });

        try {
            ChannelFuture f = b.bind(port).sync();

            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }


    }
}

class Handler extends ChannelInboundHandlerAdapter {
    //读数据的操作，handler处理器都不需要处理
    //数据读进来以后，对数据进行解析；
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //super.channelRead(ctx, msg);
        System.out.println("server: channel read");
        ByteBuf buf = (ByteBuf)msg;

        System.out.println(buf.toString(CharsetUtil.UTF_8));

        ctx.writeAndFlush(msg);

        ctx.close();

        //buf.release();
    }


    //通道的异常处理，一般要将通道关闭
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        ctx.close();
    }
}
