package com.darryl.rpc.loadbalance;

import com.darryl.rpc.registry.RegistryInfo;

import java.util.List;

/**
 * @Auther: Darryl
 * @Description: 负载均衡
 * @Date: 2020/04/17
 */
public interface LoadBalancer {
	RegistryInfo choose(List<RegistryInfo> registryInfos);
}
