package com.tendwonder.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.weixin4j.Configuration;
import org.weixin4j.OAuth2;
import org.weixin4j.OAuth2User;
import org.weixin4j.Sign;
import org.weixin4j.Weixin;
import org.weixin4j.WeixinException;
import org.weixin4j.pay.JsApiTicket;

import com.tendwonder.entity.User;

@Service
public class WechatService {

	@Autowired OAuth2 oauth2;
	@Autowired Weixin weixin;
	
	@Autowired UserService userService;
	
	public User getUserInfo(String code) throws WeixinException {
		
		String openId = oauth2.login(Configuration.getOAuthAppId(), Configuration.getOAuthSecret(), code).getOpenid();
		User userInfo = userService.getUserByOpenId(openId);
		
		if(userInfo == null) {
			OAuth2User oauth2User = oauth2.getUserInfo();
			User newUser = new User();
			newUser.setOpenid(oauth2User.getOpenid());
			newUser.setNickname(oauth2User.getNickname());
			newUser.setSex(oauth2User.getSex());
			newUser.setProvince(oauth2User.getProvince());
			newUser.setCity(oauth2User.getCity());
			newUser.setCountry(oauth2User.getCountry());
			newUser.setHeadimgurl(oauth2User.getHeadimgurl());
			newUser.setUnionid(oauth2User.getUnionid());
			newUser.setLanguage(oauth2User.getLanguage());
			userInfo = userService.saveUser(newUser);
			
		}
		return userInfo;
	}

	
	public Map<String, String> getSign(String url) {
		
		JsApiTicket jsApiTicket = null;
		//JSSDK
		try {
			weixin.login(Configuration.getOAuthAppId(), Configuration.getOAuthSecret());
			jsApiTicket = weixin.getJsApi_Ticket();
		} catch (WeixinException e) {
			e.printStackTrace();
		}
		return Sign.sign(jsApiTicket.getTicket(), url, Configuration.getOAuthAppId());
	}

}
