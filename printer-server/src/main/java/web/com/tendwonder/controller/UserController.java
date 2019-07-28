package com.tendwonder.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tendwonder.entity.User;
import com.tendwonder.service.UserService;

@RestController
public class UserController {

	@Autowired UserService userService;
	
	@RequestMapping("/saveUser")
	public User saveUser(@RequestBody User user) {
		
		return userService.saveUser(user);
	}
	
	@RequestMapping("/getUserByOpenId")
	public User getUserByOpenId(String openId) {
		
		User userInfo = userService.getUserByOpenId(openId);
		
		return userInfo;
	}
	
	@RequestMapping("/getUserById")
	public User getUserById(Long userId) {
		
		User userInfo = userService.getUserById(userId);
		
		return userInfo;
	}
	
}
