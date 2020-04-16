package com.darryl.rpc.config;

/**
 * @Auther: Darryl
 * @Description: RPC 服务提供者 config
 * 用于定义这个服务提供者具体暴露的是什么接口，和具体的实例对象
 * @Date: 2020/04/15
 */
public class ServiceConfig<T> {
	public Class type;

	public T instance;

	public ServiceConfig(Class type, T instance) {
		this.type = type;
		this.instance = instance;
	}

	public Class getType() {
		return type;
	}

	public void setType(Class type) {
		this.type = type;
	}

	public T getInstance() {
		return instance;
	}

	public void setInstance(T instance) {
		this.instance = instance;
	}
}
