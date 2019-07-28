package com.tendwonder.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tendwonder.dao.UserDao;
import com.tendwonder.entity.User;

@Service
public class UserService {

	@Autowired UserDao userDao;
	
	public User getUserById(Long userId) {

		return userDao.findOne(userId);
	}

	public User getUserByOpenId(String openId) {

		return userDao.findByOpenid(openId);
	}

	public User saveUser(User user) {

		return userDao.save(user);
	}
}
