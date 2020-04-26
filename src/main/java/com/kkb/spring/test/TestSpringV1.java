package com.kkb.spring.test;

import java.util.List;

import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Test;

import com.kkb.spring.dao.UserDaoImpl;
import com.kkb.spring.po.User;
import com.kkb.spring.service.UserServiceImpl;

public class TestSpringV1 {

	@Test
	public void test() throws Exception {
		UserServiceImpl service = new UserServiceImpl();
		UserDaoImpl userDao = new UserDaoImpl();
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://111.231.106.221:3306/kkb");
		dataSource.setUsername("kkb");
		dataSource.setPassword("kkb111111");
		
		userDao.setDataSource(dataSource);
		service.setUserDao(userDao);
		// 调用UserService的方法
		User user = new User();
		user.setUsername("王五");
		List<User> users = service.queryUsers(user);

		System.out.println("结果：" + users);
	}
}
