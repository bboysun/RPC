package com.darryl.rpc.registry;

import com.alibaba.fastjson.JSONArray;
import com.darryl.rpc.utils.InvokeUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.shaded.com.google.common.collect.Lists;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @Auther: Darryl
 * @Description: 使用zookeeper作为注册中心
 * @Date: 2020/04/15
 */
public class ZookeeperRegistry implements Registry {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private CuratorFramework client;
	private final static String ROOT_PATH = "/DARRYL_RPC";

	public ZookeeperRegistry(String connectionString) {
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000,3);
		client = CuratorFrameworkFactory.newClient(connectionString, retryPolicy);
		client.start();
		// 创建我们的RPC文件路径
		try {
			Stat stat = client.checkExists().forPath(ROOT_PATH);
			if (stat == null) {
				client.create().creatingParentsIfNeeded().forPath(ROOT_PATH);
			}
			log.info("zookeeper client init success...");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 为接口的每个方法生成一个唯一ID，注册到zookeeper中
	 *
	 * 例如：
	 * 生成的KEY：interface=com.darryl.producer.service.HelloService&method=sayHello&parameter=com.darryl.producer.model.TestBean
	 * 对应的VALUE：[{"hostname":"darryldeMacBook-Pro.local","ip":"11.241.8.140","port":50071}]
	 *
	 * @param clazz        类
	 * @param registryInfo 本机的注册信息
	 * @throws Exception
	 */
	public void register(Class clazz, RegistryInfo registryInfo) throws Exception {
		Method[] methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			String key = InvokeUtils.buildInterfaceMethodIdentify(clazz, method);
			String path = ROOT_PATH + "/" + key;
			Stat stat = client.checkExists().forPath(path);
			List registryInfos;
			if (stat != null) {
				// 这个接口已经被其他人注册了，我们需要先获取该节点的数据拿出来，然后把我们需要注册的信息添加进去
				byte[] bytes = client.getData().forPath(path);
				String data = new String(bytes, StandardCharsets.UTF_8);
				registryInfos = JSONArray.parseArray(data, RegistryInfo.class);
				// zookeeper断开链接后，并不会理解将临时借点销毁掉，这个在我的zookeeper文档中有提到
				if (registryInfos.contains(registryInfo))
					log.info("zookeeper 注册中心中已经注册了该 [" + key + "]");
				else {
					registryInfos.add(registryInfo);
					client.create().forPath(path, JSONArray.toJSONString(registryInfos).getBytes());
					log.info("zookeeper 注册该信息，路径：" + key + " , 数据信息： " + registryInfo);
				}
			} else {
				registryInfos = Lists.newArrayList();
				registryInfos.add(registryInfo);
				// 创建临时节点
				client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path,
						JSONArray.toJSONString(registryInfos).getBytes());
				log.info("zookeeper 注册该信息，路径：" + key + " , 数据信息： " + registryInfo);
			}
		}
	}

	@Override
	public List fetchRegistry(Class clazz) throws Exception {
		Method[] methods = clazz.getDeclaredMethods();
		List registryInfos = null;
		for (Method method : methods) {
			String key = InvokeUtils.buildInterfaceMethodIdentify(clazz, method);
			String path = ROOT_PATH + "/" + key;
			Stat stat = client.checkExists().forPath(path);
			if (stat == null) {
				// 可以利用zookeeper的watcher机制监听节点的变化
				log.warn("there is no infomation: {} on ZOOKEEPER!!!", key);
				continue;
			}
			// TODO: 这里如果一个接口类暴露了多个方法，此处处理需要验证调整
			if (registryInfos == null) {
				byte[] bytes = client.getData().forPath(path);
				String data = new String(bytes, StandardCharsets.UTF_8);
				registryInfos = JSONArray.parseArray(data, RegistryInfo.class);
			}
		}
		return registryInfos;
	}
}
