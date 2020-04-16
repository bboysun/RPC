package com.darryl.rpc;

import com.darryl.rpc.config.ServiceConfig;
import com.darryl.rpc.netty.NettyServer;
import com.darryl.rpc.registry.Registry;
import com.darryl.rpc.registry.RegistryInfo;
import com.darryl.rpc.registry.ZookeeperRegistry;
import com.darryl.rpc.utils.InvokeUtils;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Auther: Darryl
 * @Description: 我们自定义RPC框架的入口，也可以理解为上下文
 * @Date: 2020/04/15
 */
public class ApplicationContext<T> {

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
}
