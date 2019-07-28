package com.tendwonder.controller;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.weixin4j.Configuration;
import org.weixin4j.Weixin;
import org.weixin4j.WeixinException;
import org.weixin4j.http.HttpClient;
import org.weixin4j.spi.HandlerFactory;
import org.weixin4j.spi.IMessageHandler;
import org.weixin4j.util.TokenUtil;

import com.alibaba.fastjson.JSON;
import com.tendwonder.entity.TemplateMsg;
import com.tendwonder.service.WechatService;
import com.tendwonder.util.RandomValidateCodeUtil;

@RestController
public class WechatController{

	@Autowired WechatService wechatService;
	@Autowired Weixin weixin;
	@Autowired HttpClient httpclient;
	
//	@Autowired
//	private RedisTemplate<String, Object> redisTemplate;
	
	@GetMapping("/sendTemplateMsg")
	public String sendTemplateMsg(String userOpenId, String goodsTitle, String message) throws WeixinException {
		weixin.login(Configuration.getOAuthAppId(), Configuration.getOAuthSecret());
		String token = weixin.getOAuthToken().getAccess_token();
		String url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token="+token;

		TreeMap<String,TreeMap<String,String>> params = new TreeMap<String,TreeMap<String,String>>();   
        //根据具体模板参数组装  
        params.put("buyer", TemplateMsg.item("您的户外旅行", "#000000"));  
        params.put("goods", TemplateMsg.item("发现旅行圣地", "#000000"));  
        params.put("message", TemplateMsg.item("5000元", "#000000"));  
        TemplateMsg wechatTemplateMsg = new TemplateMsg();  
        wechatTemplateMsg.setTemplate_id("MtgZ0W8Ux27fakIOv2f-EDLE9aDCxvgH5uIZmXEfja4");    
        wechatTemplateMsg.setTouser("or0Y7w7kDXA1juxomzm3NF8KT7YE");  
        wechatTemplateMsg.setUrl("http://music.163.com/#/song?id=27867140");  
        wechatTemplateMsg.setData(params); 
        String data = JSON.toJSONString(wechatTemplateMsg); 
        
        return httpclient.httpRequest(url, "POST", data).asString();
        
	}
	
	/**
	 * 生成验证码
	 */
	@RequestMapping(value = "/getVerify")
	public void getVerify(HttpServletRequest request, HttpServletResponse response) {
	    try {
	        response.setContentType("image/jpeg");//设置相应类型,告诉浏览器输出的内容为图片
	        response.setHeader("Pragma", "No-cache");//设置响应头信息，告诉浏览器不要缓存此内容
	        response.setHeader("Cache-Control", "no-cache");
	        response.setDateHeader("Expire", 0);
	        RandomValidateCodeUtil randomValidateCode = new RandomValidateCodeUtil();
	        randomValidateCode.getRandcode(request, response);//输出验证码图片方法
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	}
	
	/**
	 * 校验验证码
	 */
	@RequestMapping(value = "/checkVerify", method = RequestMethod.POST, headers = "Accept=application/json")
	public boolean checkVerify(String securityCode, HttpSession session) {
	    try{
	        //从session中获取随机数
	        String random = (String) session.getAttribute("RANDOMVALIDATECODEKEY");
	        if (random == null) {
	            return false;
	        }
	        if (random.equals(securityCode)) {
	            return true;
	        } else {
	            return false;
	        }
	    }catch (Exception e){
	    	e.printStackTrace();
	        return false;
	    }
	}
	
	@RequestMapping(value = "/weixin", method = RequestMethod.GET)
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (Configuration.isDebug()) {
            System.out.println("获得微信请求:" + request.getMethod() + " 方式");
            System.out.println("微信请求URL:" + request.getServletPath());
        }
        //消息来源可靠性验证
        String signature = request.getParameter("signature");// 微信加密签名
        String timestamp = request.getParameter("timestamp");// 时间戳
        String nonce = request.getParameter("nonce");       // 随机数
        //Token为weixin4j.properties中配置的Token
        String token = TokenUtil.get();
        //1.验证消息真实性
        //http://mp.weixin.qq.com/wiki/index.php?title=验证消息真实性
        //URL为http://www.weixin4j.org/api/公众号
        //成为开发者验证
        String echostr = request.getParameter("echostr");   //
        //确认此次GET请求来自微信服务器，原样返回echostr参数内容，则接入生效，成为开发者成功，否则接入失败
        if (TokenUtil.checkSignature(token, signature, timestamp, nonce)) {
            response.getWriter().write(echostr);
        }
    }

    @RequestMapping(value = "/weixin", method = RequestMethod.POST)
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/xml");
            //获取POST流
            ServletInputStream in = request.getInputStream();
            if (Configuration.isDebug()) {
                System.out.println("接收到微信输入流,准备处理...");
            }

            //处理输入消息，返回结果
            IMessageHandler messageHandler = HandlerFactory.getMessageHandler();
            //处理输入消息，返回结果
            String xml = messageHandler.invoke(in);
            //返回结果
            response.getWriter().write(xml);
        } catch (Exception ex) {
            ex.printStackTrace();
            response.getWriter().write("");
        }
    }
	    
}
