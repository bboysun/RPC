package com.darryl.rpc.invoke.impl;

import com.alibaba.fastjson.JSONObject;
import com.darryl.rpc.invoke.Invoker;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Auther: Darryl
 * @Description: 默认的invoker
 * @Date: 2020/04/17
 */
public class DefaultInvoker<T> implements Invoker {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	// netty上下文
	private ChannelHandlerContext ctx;
	// 请求ID
	private String requestId;
	// 生成的接口类方法的唯一ID
	private String identify;
	// 接口方法返回类型
	private Class<T> returnType;
	//
	private T result;

	// 构造函数
	public DefaultInvoker(Class returnType, ChannelHandlerContext ctx, String requestId, String identify) {
		this.returnType = returnType;
		this.ctx = ctx;
		this.requestId = requestId;
		this.identify = identify;
	}


	@SuppressWarnings("unckecked")
	@Override
	public T invoke(Object[] args) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("interfaces", identify);
		JSONObject param = new JSONObject();
		if (args != null) {
			for (Object obj : args) {
				param.put(obj.getClass().getName(), obj);
			}
		}
		jsonObject.put("parameter", param);
		jsonObject.put("requestId", requestId);
		log.info("服务消费者发送给服务端JSON为：{}", jsonObject.toJSONString());
		String msg = jsonObject.toJSONString() + "$$";
		ByteBuf byteBuf = Unpooled.buffer(msg.getBytes().length);
		byteBuf.writeBytes(msg.getBytes());
		ctx.writeAndFlush(byteBuf);
		// 线程挂起等待远程接口返回的结果
		waitForResult();
		return result;
	}

	@Override
	public void setResult(String result) {
		synchronized (this) {
			this.result = JSONObject.parseObject(result, returnType);
			notifyAll();
		}
	}


	private void waitForResult() {
		synchronized (this) {
			try {
				wait();
			} catch (InterruptedException e) {
				log.error("default invoker Exception: ", e);
			}
		}
	}
}
