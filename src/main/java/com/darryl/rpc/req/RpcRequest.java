package com.darryl.rpc.req;

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @Auther: Darryl
 * @Description: 我们自己RPC框架的请求类定义，是服务提供者收到的远程请求的request
 * @Date: 2020/04/16
 */
public class RpcRequest {
	// 请求过来的具体需要掉用哪个接口的唯一ID
	private String interfaceIdentity;
	// 用于本地缓存每个参数的具体实例对象
	private Map<String, Object> parameterMap = new HashMap<>();
	// netty每个channel的上下文
	private ChannelHandlerContext ctx;
	// 每个请求的唯一ID
	private String requestId;

	public static RpcRequest parse(String message, ChannelHandlerContext ctx) throws ClassNotFoundException {
		/*
		 * {
		 *   "interfaces":"interface=com.study.rpc.test.producer.HelloService&method=sayHello&parameter=java.lang
		 * .String,com.study.rpc.test.producer.TestBean",
		 *   "parameter":{
		 *      "java.lang.String":"haha",
		 *      "com.study.rpc.test.producer.TestBean":{
		 *              "name":"darryl",
		 *              "age":18
		 *        }
		 *    }
		 * }
		 */
		JSONObject jsonObject = JSONObject.parseObject(message);
		String interfaces = jsonObject.getString("interfaces");

		JSONObject parameter = jsonObject.getJSONObject("parameter");
		Set<String> strings = parameter.keySet();

		RpcRequest request = new RpcRequest();
		request.setInterfaceIdentity(interfaces);
		Map<String, Object> parameterMap = new HashMap<>(16);

		String requestId = jsonObject.getString("requestId");

		for (String key : strings) {
			if (key.equals("java.lang.String")) {
				parameterMap.put(key, parameter.getString(key));
			} else {
				Class clazz = Class.forName(key);
				Object object = parameter.getObject(key, clazz);
				parameterMap.put(key, object);
			}
		}
		request.setParameterMap(parameterMap);
		request.setCtx(ctx);
		request.setRequestId(requestId);
		return request;
	}

	public String getInterfaceIdentity() {
		return interfaceIdentity;
	}

	public void setInterfaceIdentity(String interfaceIdentity) {
		this.interfaceIdentity = interfaceIdentity;
	}

	public Map<String, Object> getParameterMap() {
		return parameterMap;
	}

	public void setParameterMap(Map<String, Object> parameterMap) {
		this.parameterMap = parameterMap;
	}

	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	public void setCtx(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
}
