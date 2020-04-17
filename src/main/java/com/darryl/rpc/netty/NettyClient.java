package com.darryl.rpc.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Auther: Darryl
 * @Description: 服务消费者提供netty client，
 * 通过wait()和notifyall()来控制同步阻塞来创建netty网络链接
 * @Date: 2020/04/17
 */
public class NettyClient {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	// netty channel上下文
	private ChannelHandlerContext ctx;

	private MessageCallback messageCallback;

	public NettyClient(String ip, Integer port) {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group)
					.channel(NioSocketChannel.class)
					.option(ChannelOption.TCP_NODELAY, true)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							ByteBuf delimiter = Unpooled.copiedBuffer("$$".getBytes());
							// 设置按照分隔符“&&”来切分消息，单条消息限制为 1MB
							ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024 * 1024, delimiter));
							ch.pipeline().addLast(new StringDecoder());
							ch.pipeline().addLast(new NettyClientHandler());
						}
					});
			ChannelFuture sync = b.connect(ip, port).sync();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setMessageCallback(MessageCallback callback) {
		this.messageCallback = callback;
	}

	public ChannelHandlerContext getCtx() throws InterruptedException {
		log.info("服务消费者等待链接成功。。。");
		if (ctx == null) {
			// 没有netty client上下文，就挂起该链接线程。
			synchronized (this) {
				wait();
			}
		}
		return ctx;
	}

	// 用于服务消费者回调方法后的匿名类的接口类，用于接收response消息
	public interface MessageCallback {
		void onMessage(String message);
	}

	// 服务消费者netty client handler
	private class NettyClientHandler extends ChannelInboundHandlerAdapter {
		// 服务消费者读取信息，回调到ApplicationContext
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			try {
				String message = (String) msg;
				if (messageCallback != null) {
					messageCallback.onMessage(message);
				}
			} finally {
				ReferenceCountUtil.release(msg);
			}
		}
		// 服务消费者创建netty链接
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			NettyClient.this.ctx = ctx;
			log.info("服务消费者netty client链接成功，{}", ctx);
			synchronized (NettyClient.this) {
				NettyClient.this.notifyAll();
			}
		}
		// 服务消费者读取数据完成后，flush数据
		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
			ctx.flush();
		}
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			cause.printStackTrace();
		}
	}

}
