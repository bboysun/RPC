package com.darryl.rpc.loadbalance.impl;

import com.darryl.rpc.loadbalance.LoadBalancer;
import com.darryl.rpc.registry.RegistryInfo;

import java.util.List;
import java.util.Random;

/**
 * @Auther: Darryl
 * @Description: 负载均衡实现类，随机选择服务提供者机器
 * @Date: 2020/04/17
 */
public class RandomLoadBalancer implements LoadBalancer {
	@Override
	public RegistryInfo choose(List<RegistryInfo> registryInfos) {
		Random random = new Random();
		int index = random.nextInt(registryInfos.size());
		return registryInfos.get(index);
	}
}
