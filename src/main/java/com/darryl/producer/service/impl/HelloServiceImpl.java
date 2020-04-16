package com.darryl.producer.service.impl;

import com.darryl.producer.model.TestBean;
import com.darryl.producer.service.HelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Auther: Darryl
 * @Description: 服务提供者具体实现类
 * @Date: 2020/04/16
 */
public class HelloServiceImpl implements HelloService {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	public String sayHello(TestBean bean) {
		log.info("receive info: name is {}, age is {}", bean.getName(), bean.getAge() );
		return "producer has received your message, this is my response info";
	}
}
