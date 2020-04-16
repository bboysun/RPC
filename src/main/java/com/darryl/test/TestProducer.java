package com.darryl.test;

import com.darryl.producer.service.HelloService;
import com.darryl.producer.service.impl.HelloServiceImpl;
import com.darryl.rpc.ApplicationContext;
import com.darryl.rpc.config.ServiceConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: Darryl
 * @Description: producer测试
 * @Date: 2020/04/16
 */
public class TestProducer {

	public static void main(String[] args) throws Exception {
		String connectionString = "zookeeper://127.0.0.1:2181";
		HelloService service = new HelloServiceImpl();
		ServiceConfig config = new ServiceConfig<>(HelloService.class, service);
		List serviceConfigList = new ArrayList<>();
		serviceConfigList.add(config);
		ApplicationContext ctx = new ApplicationContext(connectionString, serviceConfigList);
	}
}
