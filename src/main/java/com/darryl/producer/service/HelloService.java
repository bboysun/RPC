package com.darryl.producer.service;

import com.darryl.producer.model.TestBean;

/**
 * @Auther: Darryl
 * @Description: 服务提供者暴露的接口
 * @Date: 2020/04/15
 */
public interface HelloService {
	String sayHello(TestBean bean);
}
