package com.tendwonder.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tendwonder.dao.ExpressDao;
import com.tendwonder.dao.UserDao;
import com.tendwonder.entity.Express;
import com.tendwonder.entity.User;

@Service
public class ExpressService {

	@Autowired ExpressDao expressDao;
	@Autowired UserDao userDao;
	
	public Express saveExpress(Express express) {
		
		String[] expressCodeList = express.getCode().split(",");
		BigDecimal cost = new BigDecimal(0);
		BigDecimal bigExpressCost = new BigDecimal(3);
		BigDecimal smallExpressCost = new BigDecimal(1);
		for(String code : expressCodeList) {
			String shelf = code.split("-")[0];
			switch(shelf) {
			case "15":
				cost = cost.add(bigExpressCost);
				break;
			default:
				cost = cost.add(smallExpressCost);
				break;
			}
		}
		String openId = express.getOpenId();
		User user = userDao.findByOpenid(openId);
		if(user.getExpressCount() < 3) {
			cost = new BigDecimal(0);
		}
		
		express.setCost(cost);
		
		user.setExpressCount(user.getExpressCount() + expressCodeList.length);
		userDao.saveAndFlush(user);
		
		return expressDao.save(express);
	}

}
