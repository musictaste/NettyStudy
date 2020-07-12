package com.mashibing.io.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

//单线程的AIO
public class Server {
    public static void main(String[] args) throws Exception {
        final AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open()
                .bind(new InetSocketAddress(8888));

        //现在accept是非阻塞的，连上以后就走了
        //把CompletionHandler的代码交给操作系统；一个客户端连接上来，由操作系统来调用CompletionHandler的代码
        //本身这个是一个observer观察者模式（也叫回调函数、钩子函数）
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            //completed说明客户端已经连上来了
            @Override
            public void completed(AsynchronousSocketChannel client, Object attachment) {
                serverChannel.accept(null, this);
                try {
                    System.out.println(client.getRemoteAddress());
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    //Client的读原先也是阻塞的，现在变成读了，就走了；当读完的时候，调CompletionHandler的代码
                    client.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                        @Override
                        public void completed(Integer result, ByteBuffer attachment) {
                            attachment.flip();
                            System.out.println(new String(attachment.array(), 0, result));
                            client.write(ByteBuffer.wrap("HelloClient".getBytes()));
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {
                            exc.printStackTrace();
                        }
                    });


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                exc.printStackTrace();
            }
        });

        //客户端连上以后，不能结束，也可以采用CountDownLatch线程池来做
        while (true) {
            Thread.sleep(1000);
        }

    }
}
