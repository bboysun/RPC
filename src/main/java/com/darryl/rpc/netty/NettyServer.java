package com.darryl.rpc.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @Auther: Darryl
 * @Description: 服务提供者的netty server
 * @Date: 2020/04/15
 */
public class NettyServer {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * 负责调用方法的handler
	 */
	private RpcInvokeHandler rpcInvokeHandler;

	public NettyServer(List serverConfigs, Map interfaceMethods) throws InterruptedException {
		this.rpcInvokeHandler = new RpcInvokeHandler(serverConfigs, interfaceMethods);
	}

	public int init(int port) throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 1024)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ByteBuf delimiter = Unpooled.copiedBuffer("$$".getBytes());
						// 设置按照分隔符“&&”来切分消息，单条消息限制为 1MB
						ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024 * 1024, delimiter));
						ch.pipeline().addLast(new StringDecoder());
						ch.pipeline().addLast().addLast(rpcInvokeHandler);
					}
				});
		ChannelFuture sync = b.bind(port).sync();
		log.info("启动Netty Server，端口为：{}", port);
		return port;
	}
}
