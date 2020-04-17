package com.darryl.rpc.invoke;

/**
 * @Auther: Darryl
 * @Description: 服务消费者动态代理的invoker接口
 * @Date: 2020/04/17
 */
public interface Invoker<T> {
	T invoke(Object[] args);
	void setResult(String result);
}
