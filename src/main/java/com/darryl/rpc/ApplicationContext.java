package com.darryl.rpc;

import com.alibaba.fastjson.JSONObject;
import com.darryl.rpc.config.ReferenceConfig;
import com.darryl.rpc.config.ServiceConfig;
import com.darryl.rpc.invoke.Invoker;
import com.darryl.rpc.invoke.impl.DefaultInvoker;
import com.darryl.rpc.loadbalance.LoadBalancer;
import com.darryl.rpc.loadbalance.impl.RandomLoadBalancer;
import com.darryl.rpc.netty.NettyClient;
import com.darryl.rpc.netty.NettyServer;
import com.darryl.rpc.registry.Registry;
import com.darryl.rpc.registry.RegistryInfo;
import com.darryl.rpc.registry.ZookeeperRegistry;
import com.darryl.rpc.res.RpcResponse;
import com.darryl.rpc.utils.InvokeUtils;
import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

/**
 * @Auther: Darryl
 * @Description: 我们自定义RPC框架的入口，也可以理解为上下文
 * @Date: 2020/04/15
 */
public class ApplicationContext<T> {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	//服务提供者相关的成员变量
	// 服务提供者配置信息
	private List<ServiceConfig> serviceConfigs;
	// netty port
	private final static Integer PORT =50071;
	// 服务提供者暴露的接口方法
	private Map<String, Method> interfaceMethods = new ConcurrentHashMap<String, Method>();
	// 注册中心
	private Registry registry;
	// netty
	private NettyServer nettyServer;

	// RPC消费者配置信息
	private List<ReferenceConfig> referenceConfigs;
	// 缓存每个接口对应的注册信息
	private Map<Class, List> interfacesMethodRegistryList = new ConcurrentHashMap<>();
	// 缓存每个注册信息对应的一个netty网络链接
	private Map<RegistryInfo, ChannelHandlerContext> channels = new ConcurrentHashMap<>();
	// 服务消费者接收到的response响应队列
	private Queue<RpcResponse> responses = new ConcurrentLinkedQueue<>();
	// 负责生成requestId的类，用来唯一标示一个请求，因为同一个接口方法可能会被不同的应用服务调用，要用这个自增ID区别
	private LongAdder requestIdWorker = new LongAdder();
	// 缓存远程请求对应的接口实现类，接口实现类通过动态代理生成的匿名类
	private Map<String, Object> inProgressInvoker = new ConcurrentHashMap<>();
	// 随机负载均衡
	private LoadBalancer loadBalancer = new RandomLoadBalancer();
	// 响应列表
	private List<ResponseProcessor> processors = Lists.newArrayList();


	// 通用构造函数
	public ApplicationContext(String registryUrl, List<ServiceConfig> serviceConfigs,
	                          List<ReferenceConfig> referenceConfigs, int port) throws Exception {
		// step 1: 保存服务提供者和消费者
		this.serviceConfigs = serviceConfigs == null ? new ArrayList<>() : serviceConfigs;
		this.referenceConfigs = referenceConfigs == null ? new ArrayList<>() : referenceConfigs;
		// step 2: 实例化注册中心
		initRegistry(registryUrl);

		// step 3: 将接口注册到注册中心，从注册中心获取接口，初始化服务接口列表，需要将netty暴露的端口注册到zookeeper中
		RegistryInfo registryInfo = null;
		InetAddress addr = InetAddress.getLocalHost();
		String hostname = addr.getHostName();
		String hostAddress = addr.getHostAddress();
		registryInfo = new RegistryInfo(hostname, hostAddress, port);
		doRegistry(registryInfo);

		// step 4：初始化Netty服务器，接受到请求，直接打到服务提供者的service方法中
		if (!this.serviceConfigs.isEmpty()) {
			// 需要暴露接口才暴露
			nettyServer = new NettyServer(this.serviceConfigs, interfaceMethods);
			nettyServer.init(port);
		}

		// step 5: 启用处理响应的processer
		initProcessor();
	}

	// 服务提供者构造函数
	public ApplicationContext(String registryUrl, List<ServiceConfig> serviceConfigs) throws Exception {
		// 1. 保存需要暴露的接口配置
		this.serviceConfigs = serviceConfigs == null ? new ArrayList<ServiceConfig>() : serviceConfigs;

		// step 2: 实例化注册中心
		initRegistry(registryUrl);

		// step 3: 将接口注册到注册中心，从注册中心获取接口，初始化服务接口列表，需要将netty暴露的端口注册到zookeeper中
		RegistryInfo registryInfo = null;
		InetAddress addr = InetAddress.getLocalHost();
		String hostname = addr.getHostName();
		String hostAddress = addr.getHostAddress();
		registryInfo = new RegistryInfo(hostname, hostAddress, PORT);
		doRegistry(registryInfo);

		// step 4：初始化Netty服务器，接受到请求，直接打到服务提供者的service方法中
		if (!this.serviceConfigs.isEmpty()) {
			// 需要暴露接口才暴露
			nettyServer = new NettyServer(this.serviceConfigs, interfaceMethods);
			nettyServer.init(PORT);
		}
	}

	// 将本地信息注册到注册中心
	private void doRegistry(RegistryInfo registryInfo) throws Exception {
		for (ServiceConfig config : serviceConfigs) {
			Class type = config.getType();
			// 将接口注册到注册中心
			registry.register(type, registryInfo);
			// 为接口中的每一个方法生成唯一ID，并保存到interfaceMehods本地内存，当服务提供者被调用到该接口，通过反射调用method
			Method[] declaredMethods = type.getDeclaredMethods();
			for (Method method : declaredMethods) {
				String identify = InvokeUtils.buildInterfaceMethodIdentify(type, method);
				interfaceMethods.put(identify, method);
			}
		}
		for (ReferenceConfig config : referenceConfigs) {
			List registryInfos = registry.fetchRegistry(config.getType());
			if (registryInfos != null) {
				interfacesMethodRegistryList.put(config.getType(), registryInfos);
				// 服务消费这初始化网络链接
				initChannel(registryInfos);
			}
		}
	}

	/**
	 * 服务消费者在获取到远程接口的注册信息后，进行网络链接的初始化
	 * @param registryInfos
	 */
	private void initChannel(List<RegistryInfo> registryInfos) throws InterruptedException {
		for (RegistryInfo info : registryInfos) {
			// 针对每一个唯一的registryinfo创建一个链接
			if (!channels.containsKey(info)) {
				log.info("服务消费者开始建立链接:{}:{}", info.getIp(), info.getPort());
				NettyClient client = new NettyClient(info.getIp(), info.getPort());
				// 当netty client接受到消息时，回调该方法来处理response响应消息
				client.setMessageCallback(message -> {
					// 这里收单服务端返回的消息，先压入队列
					RpcResponse response = JSONObject.parseObject(message, RpcResponse.class);
					responses.offer(response);
					synchronized (ApplicationContext.this) {
						ApplicationContext.this.notifyAll();
					}
				});

				// 等待连接建立
				ChannelHandlerContext ctx = client.getCtx();
				channels.put(info, ctx);
			}
		}
	}

	// 实例化注册中心
	private void initRegistry(String registryUrl) {
		if (registryUrl.startsWith("zookeeper://")) {
			registryUrl = registryUrl.substring(12);
			registry = new ZookeeperRegistry(registryUrl);
		} else {
			// TODO: 可扩展其他注册中心的配置
		}
	}

	private void initProcessor() {
		// 事实上，这里可以通过配置文件读取，启动多少个processor
		int num = 3;
		for (int i = 0; i < 3; i++) {
			processors.add(createProcessor(i));
		}
	}

	private ResponseProcessor createProcessor(int i) {
		ResponseProcessor rp = new ResponseProcessor();
		rp.start();
		return rp;
	}

	// 处理响应的线程
	private class ResponseProcessor extends Thread {
		@Override
		public void run() {
			log.info("启动响应处理线程：{}", getName());
			while (true) {
				// 多个线程在这里获取响应，只有一个成功
				RpcResponse response = responses.poll();
				if (response == null) {
					try {
						synchronized (ApplicationContext.this) {
							// 如果没有响应，先休眠
							ApplicationContext.this.wait();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					log.info("收到一个响应：{}" + response);
					String interfaceMethodIdentify = response.getInterfaceMethodIdentify();
					String requestId = response.getRequestId();
					String key = interfaceMethodIdentify + "#" + requestId;
					Invoker invoker = (Invoker) inProgressInvoker.remove(key);
					invoker.setResult(response.getResult());
				}
			}
		}
	}

	/**
	 * 获取调用服务，服务消费者动态代理生成一个匿名类来调用远端接口
	 */
	@SuppressWarnings("unchecked")
	public T getService(Class clazz) {
		return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{clazz}, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				String methodName = method.getName();
				if ("equals".equals(methodName) || "hashCode".equals(methodName)) {
					throw new IllegalAccessException("不能访问" + methodName + "方法");
				}
				if ("toString".equals(methodName)) {
					return clazz.getName() + "#" + methodName;
				}

				// step 1: 获取服务地址列表
				List registryInfos = interfacesMethodRegistryList.get(clazz);
				if (registryInfos == null) {
					throw new RuntimeException("无法找到服务提供者");
				}
				// step 2： 负载均衡
				RegistryInfo registryInfo = loadBalancer.choose(registryInfos);
				ChannelHandlerContext ctx = channels.get(registryInfo);
				String identify = InvokeUtils.buildInterfaceMethodIdentify(clazz, method);
				String requestId;
				synchronized (ApplicationContext.this) {
					requestIdWorker.increment();
					requestId = String.valueOf(requestIdWorker.longValue());
				}
				Invoker invoker = new DefaultInvoker(method.getReturnType(), ctx, requestId, identify);
				inProgressInvoker.put(identify + "#" + requestId, invoker);
				return invoker.invoke(args);
			}
		});
	}


}
