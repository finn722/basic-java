package com.kkb.spring.service;

import java.util.List;

import com.kkb.spring.po.User;

public interface UserService {

	List<User> queryUsers(User user);
}
