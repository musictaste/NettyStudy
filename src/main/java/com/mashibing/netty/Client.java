package com.mashibing.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;

public class Client {
    public static void main(String[] args) {
        new Client().clientStart();
    }

    private void clientStart() {
        //线程池：EventLoopGroup可以指定数量
        //EventLoopGroup,循环不停处理事件的线程池，用于处理Channel上的所有事件，
        // 读源码发现，如果是一个线程池默认为你机器的内核数(CPU)*2
        EventLoopGroup workers = new NioEventLoopGroup();

        //辅助启动类，类似解鞋带
        //NioSocketChannel.class指定为非阻塞版，如果想指定阻塞版，换成BIO
        Bootstrap b = new Bootstrap();
        b.group(workers)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        System.out.println("channel initialized!");
                        ch.pipeline().addLast(new ClientHandler());
                    }
                });

        try {
            System.out.println("start to connect...");
            //connect方法是一个异步方法，所以想让Channel成功连接才能继续执行，需要加sync()；
            //sync()返回的是ChannelFuture（Future在多线程中有讲）
            //netty中所有方法都是异步方法

            //SocketChannel什么时候初始化，是在调用了connect()，才能触发handler中的initChannel
            ChannelFuture f = b.connect("127.0.0.1", 8888).sync();

            f.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();

        } finally {
            //优雅的关闭
            workers.shutdownGracefully();
        }

    }


}

class ClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channel is activated.");
        //ChannelFuture中有没有成功，需要
        final ChannelFuture f = ctx.writeAndFlush(Unpooled.copiedBuffer("HelloNetty".getBytes()));
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                System.out.println("msg send!");
                //ctx.close();
            }
        });


    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            ByteBuf buf = (ByteBuf)msg;
            System.out.println(buf.toString());
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
}
