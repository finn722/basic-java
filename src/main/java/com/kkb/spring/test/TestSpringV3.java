package com.kkb.spring.test;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import com.kkb.spring.bean.factory.support.DefaultListableBeanFactory;
import com.kkb.spring.po.User;
import com.kkb.spring.resource.ClasspathResource;
import com.kkb.spring.resource.Resource;
import com.kkb.spring.service.UserService;
import com.kkb.springframework.reader.XmlBeanDefinitionReader;

public class TestSpringV3 {

	@Test
	public void test() throws Exception {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

		Resource resource = new ClasspathResource("beans.xml");

		InputStream inputStream = resource.getResource();

		xmlBeanDefinitionReader.loadBeanDefinitions(inputStream);

		UserService userService = (UserService) beanFactory.getBean("userService");

		User user = new User();
		user.setUsername("王五");
		List<User> users = userService.queryUsers(user);
		System.out.println(users);
	}
}
