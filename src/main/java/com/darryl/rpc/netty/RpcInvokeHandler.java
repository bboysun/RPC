package com.darryl.rpc.netty;

import com.alibaba.fastjson.JSONObject;
import com.darryl.rpc.config.ServiceConfig;
import com.darryl.rpc.req.RpcRequest;
import com.darryl.rpc.res.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Auther: Darryl
 * @Description: RPC 服务提供者收到请求后，对远程请求做相应的处理，并返回给远程客户端
 * @Date: 2020/04/15
 */
public class RpcInvokeHandler extends ChannelInboundHandlerAdapter {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	//接口方法唯一标识对应的Method对象
	private Map<String, Method> interfaceMethods;
	//接口对应的实现类
	private Map<Class, Object> interfaceToInstance;

	/**
	 * 线程池，随意写的，参数随便定义的，具体的参数要看具体的项目所部署的服务器
	 */
	private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(3, 6,
			60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(50),
			new ThreadFactory() {
				AtomicInteger m = new AtomicInteger(0);
				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "Netty-Server-thread-" + m.incrementAndGet());
				}
			});

	public RpcInvokeHandler(List<ServiceConfig> serviceConfigList, Map<String, Method> interfaceMethods) {
		this.interfaceToInstance = new ConcurrentHashMap<>();
		this.interfaceMethods = interfaceMethods;
		// 将注册到zookeeper暴露的接口对应的类实例缓存到内存中，当远程请求过来时，可以方便快捷的调用到对应的接口方法
		for (ServiceConfig config : serviceConfigList) {
			interfaceToInstance.put(config.getType(), config.getInstance());
		}
	}

	// channelRead方法用于接收消息，接收到的就是我们前面分析的那个JSON格式的数据，接着我们将消息解析成RpcRequest
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			String message = (String) msg;
			// 这里拿到的是一串JSON数据，解析为Request对象，
			// 事实上这里解析网络数据，可以用序列化方式，定一个接口，可以实现JSON格式序列化，或者其他序列化
			// 本demo版本简单处理
			log.info("receive message: {}", msg);
			RpcRequest request = RpcRequest.parse(message, ctx);
			threadPoolExecutor.execute(new RpcInvokeTask(request));
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error("netty exception: ", cause);
		cause.printStackTrace();
		ctx.close();
	}

	public class RpcInvokeTask implements Runnable {

		private RpcRequest rpcRequest;

		RpcInvokeTask(RpcRequest rpcRequest) {
			this.rpcRequest = rpcRequest;
		}

		@Override
		public void run() {
			try {
				/*
				 * 数据大概是这样子的
				 * {"interfaces":"interface=com.study.rpc.test.producer.HelloService&method=sayHello¶meter=com
				 * .study.rpc.test.producer.TestBean","requestId":"3","parameter":{"com.study.rpc.test.producer
				 * .TestBean":{"age":20,"name":"张三"}}}
				 */
				// 这里希望能拿到每一个服务对象的每一个接口的特定声明
				String interfaceIdentity = rpcRequest.getInterfaceIdentity();
				Method method = interfaceMethods.get(interfaceIdentity);
				Map<String, String> map = string2Map(interfaceIdentity);
				String interfaceName = map.get("interface");
				// 通过反射拿到接口类，在我们内存中缓存的map中找到对应的接口实现类
				Class interfaceClass = Class.forName(interfaceName);
				Object o = interfaceToInstance.get(interfaceClass);
				String parameterString = map.get("parameter");
				Object result;
				if (parameterString != null) {
					String[] parameterTypeClass = parameterString.split(",");
					Map<String, Object> parameterMap = rpcRequest.getParameterMap();
					Object[] parameterInstance = new Object[parameterTypeClass.length];
					for (int i = 0; i < parameterTypeClass.length; i++) {
						String parameterClazz = parameterTypeClass[i];
						parameterInstance[i] = parameterMap.get(parameterClazz);
					}
					result = method.invoke(o, parameterInstance);
				} else {
					result = method.invoke(o);
				}
				// 写回响应
				ChannelHandlerContext ctx = rpcRequest.getCtx();
				String requestId = rpcRequest.getRequestId();
				RpcResponse response = RpcResponse.create(JSONObject.toJSONString(result), interfaceIdentity, requestId);
				String str = JSONObject.toJSONString(response) + "$$";
				ByteBuf byteBuf = Unpooled.copiedBuffer(str.getBytes());
				ctx.writeAndFlush(byteBuf);
				log.info("return response: {}", str);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public Map<String, String> string2Map(String str) {
			String[] split = str.split("&");
			Map<String, String> map = new HashMap<>(16);
			for (String s : split) {
				String[] split1 = s.split("=");
				map.put(split1[0], split1[1]);
			}
			return map;
		}
	}
}
