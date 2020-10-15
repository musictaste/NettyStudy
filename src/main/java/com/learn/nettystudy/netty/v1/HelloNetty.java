package com.learn.nettystudy.netty.v1;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
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
        //两个线程池：大管家(负责客户端连接，也就是accept() )、工人（负责连接以后的IO处理）
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();

        //通道类型：NioServerSocketChannel
        //通道(SocketChannel)要连接时，给一个监听器：childHandler；
        // 这个监听器的处理过程是：当通道初始化以后，在这个通道上加一个通道的处理器Handler（又是一个监听器）
        //Netty的好处就是将连接的代码和业务的代码分开

        //NioServerSocketChannel.class指定Channel类型
        //handler()是在加在了Server整个面板上以及Client连上来的所有SocketChannel上
        //childHandler是SocketChannel已经连接了，加在它的孩子们身上，也就是加在Client客户端上面;这时有worker来负责
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
//                .handler()
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        System.out.println(ch);//Server的IP+端口，Client的IP+端口（端口是随机的）

                        //ChannelPipeline管道，这个管道上会加一个一个的handler（过滤器，在讲责任链模式的时候说过）
                        //看ChannelPipeline的类中，的图
                        //ChannelPipeline管道中Inbound Handler和Outbound Handler
                        ch.pipeline().addLast(new ServerChildHandler());
                    }
                });

        try {
            ChannelFuture f = b.bind(port).sync();

            //如果没有下面这句话，程序会继续执行，也就是说程序启动以后，执行完就结束了
            //这句话起到一个阻塞的作用
            //f.channel()返回Server上的channel
            //f.channel()调用close() 返回ChannelFuture，如果没有调用close方法，那么ChannelFutrue会一直等待
            //相当于餐厅关门的时候，有一个专门负责关门的机器人，来把餐厅关门；现在有三个角色：Boss、worker、CloseFuture
            //执行了这句话，finally中的才能正确优雅的将程序关闭
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //两个线程池的关闭没有先后顺序
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }


    }
}

//ChannelInboundHandlerAdapter不是适配器模式，只是做了一个骨架（空方法），只是一种比较方便的编程方式；具体可以看Adapter的内容
class ServerChildHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(Thread.currentThread().getId());
    }

    //读数据的操作，handler处理器都不需要处理；读数据由Worker负责
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


        /*ByteBuf buf = null;
        try {
            buf = (ByteBuf) msg;
            byte[] bytes = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(),bytes);
            System.out.println(new String(bytes));
        }finally {
            if(buf != null){
                ReferenceCountUtil.release(buf);
            }
        }*/

    }


    //通道的异常处理，一般要将通道关闭
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        ctx.close();
    }
}
