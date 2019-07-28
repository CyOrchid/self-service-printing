package com.tendwonder.dao;

import org.springframework.stereotype.Repository;

import com.tendwonder.base.IBaseRepository;
import com.tendwonder.entity.User;

@Repository
public interface UserDao extends IBaseRepository<User, Long> {

	User findByOpenid(String openId);

}
