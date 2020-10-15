package com.learn.nettystudy.netty.v1;

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
                .handler(new ClientInitializer());

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

class ClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        System.out.println("channel initialized!");
        ch.pipeline().addLast(new ClientHandler());
    }
}

class ClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //channel第一次连上可用，写出一个字符串
        System.out.println("channel is activated.");

        //因为NIO的ByteBuffer太难用，所以Netty自己封装了ByteBuf，并且效率特别高
        //Jvm有自己的内存，操作系统也有自己的内存；以前如果需要操作数据的话，需要将数据从操作系统的内存中读到jvm的内存中
        //现在ByteBuf可以直接访问操作系统的内存，也就是Direct Memory直接内存，所以效率高
        //而使用直接内存会跳过java的垃圾回收机制，会占用系统内存，需要进行释放
        ByteBuf buf = Unpooled.copiedBuffer("HelloNetty".getBytes());

        //因为ByteBuf是使用直接内存，使用完是要进行内存释放的(否则造成内存泄漏)；writeAndFlush方法会进行自动内存释放
        //ChannelFuture中有没有成功，需要
        final ChannelFuture f = ctx.writeAndFlush(buf);
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                System.out.println("msg send!");
                //ctx.close();
            }
        });


    }

    //Object不是泛型，因为Netty从低版本的时候没有使用泛型，也一直没有进行修改
    //SimpleChannelInboundHandler这个类是有泛型的，需要跟Codec配合使用才能实现泛型

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = null;
        try {
            //在netty中往外写的所有内容都要转成ByteBuf,所以需要将msg转成ByteBuf
            buf= (ByteBuf)msg;
            System.out.println(buf.toString());//PooledUnsafeDirectByteBuf(ridx: 0, widx: 10, cap: 1024)
            System.out.println(buf.refCnt());//查看有几个引用
        } finally {
            if(buf !=null){
                ReferenceCountUtil.release(buf);
            }
            System.out.println(buf.refCnt());
        }
    }
}
