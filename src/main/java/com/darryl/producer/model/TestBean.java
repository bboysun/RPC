package com.darryl.producer.model;

/**
 * @Auther: Darryl
 * @Description: 服务提供者的model bean
 * @Date: 2020/04/15
 */
public class TestBean {
	private String name;
	private Integer age;

	public TestBean(String name, Integer age) {
		this.name = name;
		this.age = age;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}
}
