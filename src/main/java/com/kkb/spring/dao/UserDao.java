package com.kkb.spring.dao;

import java.util.List;

import com.kkb.spring.po.User;

public interface UserDao {

	List<User> queryUserList(String sqlId, Object param);
}
