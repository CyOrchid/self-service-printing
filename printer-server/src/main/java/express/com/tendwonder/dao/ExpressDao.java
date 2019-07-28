package com.tendwonder.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.tendwonder.base.IBaseRepository;
import com.tendwonder.entity.Express;

@Repository
public interface ExpressDao extends IBaseRepository<Express, Long> {

	Express findByTradeNo(String tradeNo);

	List<Express> findByOpenId(String openId);

}
