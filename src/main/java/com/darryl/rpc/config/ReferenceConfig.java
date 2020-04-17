package com.darryl.rpc.config;

/**
 * @Auther: Darryl
 * @Description: RPC 服务消费者 config
 *               用于定义我们需要调用的接口类型
 * @Date: 2020/04/16
 */
public class ReferenceConfig {

	// 需要从远端调用的接口类型
	private Class type;

	public ReferenceConfig(Class type) {
		this.type = type;
	}

	public Class getType() {
		return type;
	}

	public void setType(Class type) {
		this.type = type;
	}
}
