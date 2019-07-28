package com.tendwonder.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.weixin4j.Configuration;
import org.weixin4j.Weixin;
import org.weixin4j.WeixinException;
import org.weixin4j.http.HttpClient;
import org.weixin4j.pay.PayUtil;
import org.weixin4j.pay.SignUtil;
import org.weixin4j.pay.UnifiedOrder;
import org.weixin4j.util.MapUtil;

import com.tendwonder.dao.ExpressDao;
import com.tendwonder.entity.Express;
import com.tendwonder.entity.User;
import com.tendwonder.service.ExpressService;
import com.tendwonder.service.UserService;
import com.tendwonder.service.WechatService;
import com.tendwonder.util.JsonUtil;

@Controller
@RequestMapping("/express")
public class ExpressController{

	@Autowired WechatService wechatService;
	@Autowired ExpressService expressService;
	@Autowired UserService userService;
	
	@Autowired ExpressDao expressDao;
	
	@Autowired Weixin weixin;
	@Autowired HttpClient httpclient;
	
	@Autowired
	private StringRedisTemplate redisTemplate;
	
	@GetMapping("/index")
	public String express(Model model, String code, HttpServletRequest request) {
		
		//获取用户info
		try {
			User userInfo = (User) request.getSession().getAttribute("userInfo");
			if(userInfo == null) {
				userInfo = wechatService.getUserInfo(code);
				request.getSession().setAttribute("userInfo", userInfo);
			}
			model.addAttribute("userInfo", userInfo);
		} catch (WeixinException e) {
			model.addAttribute("error", e.getMessage());
			return "public/error";
		}
		
		return "express/index";
	}
	
	@GetMapping("/order")
	public String pay(Model model, String price, String tradeNo, HttpServletRequest request) {
		//获取用户info
		User userInfo = (User) request.getSession().getAttribute("userInfo");
		if(price == "0" || "0".equals(price)) {
			Express express = expressDao.findByTradeNo(tradeNo);
			express.setPaid(1);
			expressDao.saveAndFlush(express);
			
			List<Express> expressList = expressDao.findByOpenId(userInfo.getOpenid());
			
			model.addAttribute("expressList", expressList);
			return "express/expressRecord";
		}
		
		BigDecimal penny = new BigDecimal(price);
		penny = penny.multiply(new BigDecimal(100));
		model.addAttribute("price", price);
		
		String ip;
		if (request.getHeader("x-forwarded-for") == null) {
			ip = request.getRemoteAddr();
		}else {
			ip = request.getHeader("x-forwarded-for");
		}
		
		//统一下单
		UnifiedOrder unifiedOrder = new UnifiedOrder();
		unifiedOrder.setAppid(Configuration.getOAuthAppId());
		unifiedOrder.setMch_id(Configuration.getProperty("weixin4j.pay.partner.id"));
		unifiedOrder.setNonce_str(create_nonce_str());
		unifiedOrder.setBody("代取快递");
		unifiedOrder.setOut_trade_no(tradeNo);
		unifiedOrder.setTotal_fee(penny.toString());
		unifiedOrder.setSpbill_create_ip(ip);
		unifiedOrder.setNotify_url("www.tantanba.cn/express/notify");
		unifiedOrder.setTrade_type("JSAPI");
		unifiedOrder.setOpenid(userInfo.getOpenid());
        //统一下单签名
        String unifiedOrderSign = SignUtil.getSign(unifiedOrder.toMap(), Configuration.getProperty("weixin4j.pay.partner.key"));
        unifiedOrder.setSign(unifiedOrderSign);
		
		try {
			String prepayId = weixin.payUnifiedOrder(unifiedOrder).getPrepay_id();
			
			Map<String, String> sign = new HashMap<String, String>();
			sign.put("appId", Configuration.getOAuthAppId());
			sign.put("timeStamp", create_timestamp());
			sign.put("nonceStr", create_nonce_str());
			sign.put("package", "prepay_id="+prepayId);
			sign.put("signType", "MD5");

			String paySign = SignUtil.getSign(sign, Configuration.getProperty("weixin4j.pay.partner.key"));
			sign.put("paySign", paySign);
	        
			model.addAttribute("paySign", sign);
		} catch (WeixinException e1) {
			model.addAttribute("error", e1.getMessage());
			return "public/error";
		}
		
		model.addAttribute("openId", userInfo.getOpenid());
		model.addAttribute("tradeNo", tradeNo);
		return "express/order";
	}
	
	@PostMapping("/notify")
	@ResponseBody
	public void getNotify(String xmlMsg, HttpServletRequest request, HttpServletResponse response) {
		StringBuffer sb = new StringBuffer("");
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream(), "utf-8"));  
	        String temp;  
	        while ((temp = br.readLine()) != null) {  
	            sb.append(temp);  
	        }  
		} catch (Exception e) {  
	        e.printStackTrace();    
	    }
        System.out.print(sb.toString());
        Map<String, String> returnMsg = MapUtil.xmlToMap(sb.toString());
        
        boolean realPay = PayUtil.verifySign(sb.toString(), Configuration.getProperty("weixin4j.pay.partner.key"));
        if(!realPay) {
        	return;
        } else {
        	//数据库写入已支付
        	String tradeNo = returnMsg.get("out_trade_no");
        	Express express = expressDao.findByTradeNo(tradeNo);
        	express.setPaid(1);
        	expressDao.saveAndFlush(express);
        	
        	//推送模板消息
        }
		
		//告诉微信服务器，我收到信息了，不要在调用回调action了
		try {
			response.getWriter().write(
					"<xml>" + 
					"<return_code><![CDATA[SUCCESS]]></return_code>" + 
					"<return_msg><![CDATA[OK]]></return_msg>" + 
					"</xml>");
        }catch(Exception e){
         	e.printStackTrace();
        }
	}
	
	
	@GetMapping("/expressRecord")
	public String expressRecord(Model model, String code, String openId, HttpServletRequest request) {
		if(openId != null) {
			List<Express> expressList = expressDao.findByOpenId(openId);
			if(expressList.isEmpty()) {
				return "express/other";
			}
			model.addAttribute("expressList", expressList);
			
			return "express/expressRecord";
		}
		//获取用户info
		try {
			User userInfo = (User) request.getSession().getAttribute("userInfo");
			if(userInfo == null) {
				userInfo = wechatService.getUserInfo(code);
				request.getSession().setAttribute("userInfo", userInfo);
			}
			List<Express> expressList = expressDao.findByOpenId(userInfo.getOpenid());
			if(expressList.isEmpty()) {
				return "express/other";
			}
			model.addAttribute("expressList", expressList);
			
		} catch (WeixinException e) {
			model.addAttribute("error", e.getMessage());
			return "public/error";
		}
		
		return "express/expressRecord";
	}
	
	
	@PostMapping("/saveExpress")
	@ResponseBody
	public String saveExpress(Express express, String deliveryTimeStr, String securityCode, HttpSession session) throws ParseException {

		SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		
        //从session中获取随机数
        String random = (String) session.getAttribute("RANDOMVALIDATECODEKEY");
        if (random == null) {
        	return JsonUtil.getInstance().putData("ret", -1).putData("msg", "验证码不能为空！").pushData();
        }
        if (!random.equals(securityCode)) {
        	return JsonUtil.getInstance().putData("ret", -1).putData("msg", "验证码错误！").pushData();
        }

        if("today".equals(express.getDate())) {
        	express.setDate(sdf.format(c.getTime()));
        }else {
        	c.add(Calendar.DAY_OF_MONTH, 1);
        	express.setDate(sdf.format(c.getTime()));
        }
        
		if(express.getPhone() == null || "".equals(express.getPhone()) ) {
			return JsonUtil.getInstance().putData("ret", -1).putData("msg", "收件人手机不能为空！").pushData();
		}
		if(express.getCode() == null || "".equals(express.getCode()) ) {
			return JsonUtil.getInstance().putData("ret", -1).putData("msg", "取件码不能为空！").pushData();
		}
		int i = express.getArea().indexOf("萝岗和苑");
		if(i < 0) {
			return JsonUtil.getInstance().putData("ret", -1).putData("msg", "此地区暂为开放，敬请期待！").pushData();
		}
		express.setPaid(0);
		express.setTradeNo(creat_trade_no());
		Express result = expressService.saveExpress(express);
		return JsonUtil.getInstance().putData("ret", 1).putData("data", result).pushData();
	}
	
	private static String creat_trade_no() {
		//格式化当前时间
		SimpleDateFormat sfDate = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String strDate = sfDate.format(new Date());
		
		Random random = new Random();
		String result = random.nextInt(100) + "";
		if(result.length() == 1) {
			result = "0" + result;
		}
		return strDate + result;
	}
	
	private static String create_nonce_str() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String create_timestamp() {
        return Long.toString(System.currentTimeMillis() / 1000);
    }
}
