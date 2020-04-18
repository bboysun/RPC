package com.darryl.test;

import com.darryl.producer.model.TestBean;
import com.darryl.producer.service.HelloService;
import com.darryl.rpc.ApplicationContext;
import com.darryl.rpc.config.ReferenceConfig;

import java.util.Collections;

/**
 * @Auther: Darryl
 * @Description: consumer测试
 * @Date: 2020/04/17
 */
public class TestConsumer {
	public static void main(String[] args) throws Exception {
		String connectionString = "zookeeper://127.0.0.1:2181";
		ReferenceConfig config = new ReferenceConfig(HelloService.class);
		ApplicationContext ctx = new ApplicationContext(connectionString, null, Collections.singletonList(config),
				8888);
		HelloService helloService = (HelloService) ctx.getService(HelloService.class);
		System.out.println("sayHello(TestBean)结果为：" + helloService.sayHello(new TestBean("张三", 20)));
	}
}
