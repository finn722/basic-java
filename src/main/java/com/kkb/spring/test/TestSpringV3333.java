//package com.kkb.spring.test;
//
//import java.io.InputStream;
//import java.util.List;
//
//import org.junit.Test;
//
//import com.kkb.spring.bean.factory.support.DefaultListableBeanFactory;
//import com.kkb.spring.bean.resource.ClasspathResource;
//import com.kkb.spring.bean.resource.Resource;
//import com.kkb.spring.bean.xml.XmlBeanDefinitionReader;
//import com.kkb.spring.po.User;
//import com.kkb.spring.service.UserService;
//
//public class TestSpringV3333 {
//
//	@Test
//	public void test() throws Exception {
//		String location = "beans.xml";
//
//		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
//
//		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
//
//		// 将资源抽象为一个接口，通过该接口，可以获取不同地方（网络、文件系统、classpath）的资源
//		Resource resource = new ClasspathResource(location);
//		InputStream inputStream = resource.getResource();
//		beanDefinitionReader.loadBeanDefinitions(inputStream);
//
//		UserService userService = (UserService) beanFactory.getBean("userService");
//
//		User user = new User();
//		user.setUsername("王五");
//		List<User> users = userService.queryUsers(user);
//		System.out.println(users);
//	}
//}
