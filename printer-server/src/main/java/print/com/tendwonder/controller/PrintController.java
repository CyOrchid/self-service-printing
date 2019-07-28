package com.tendwonder.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.weixin4j.pay.PayUtil;
import org.weixin4j.pay.SignUtil;
import org.weixin4j.pay.UnifiedOrder;
import org.weixin4j.util.MapUtil;

import com.tendwonder.SocketServer;
import com.tendwonder.entity.User;
import com.tendwonder.service.JacobTransition;
import com.tendwonder.service.WechatService;

@Controller
@RequestMapping("/print")
public class PrintController {

	@Autowired WechatService wechatService;
	@Autowired Weixin weixin;
	
	@Autowired JacobTransition transition;
	
	@Autowired
	private StringRedisTemplate redisTemplate;
	@Value("${upload.rootPath}")
	private String rootPath;
	
	private OutputStream out;
	
	@GetMapping("/index")
    public String print(Model model, String code, HttpServletRequest request) {
		
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
		
		//获取sign
		String url = "http://www.tantanba.cn" + request.getRequestURI() + "?" + request.getQueryString();
		
		Map<String, String> configSign;
		configSign = wechatService.getSign(url);
		model.addAttribute("configSign", configSign);
		
		//获取jsApiList
		ArrayList<String> jsApiList = new ArrayList<String>();
		jsApiList.add("getLocation");
		jsApiList.add("scanQRCode");
		jsApiList.add("chooseWXPay");
		model.addAttribute("jsApiList", jsApiList);
		
		return "print/index";
    } 
	
	@GetMapping("/order")
	public String pay(Model model, String openId, String price, String printerCode, String tradeNo, HttpServletRequest request) {
		//获取用户info
		User userInfo = (User) request.getSession().getAttribute("userInfo");
		
		try {
			BigDecimal penny = new BigDecimal(price);
			BigDecimal conversion = new BigDecimal("100");
			model.addAttribute("price", penny.divide(conversion, 2, BigDecimal.ROUND_HALF_UP));
		}catch(Exception e) {
			model.addAttribute("error", e.getMessage());
			return "public/error";
		}
		
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
		unifiedOrder.setBody("自助打印");
		unifiedOrder.setOut_trade_no(tradeNo);
		unifiedOrder.setTotal_fee(price);
		unifiedOrder.setSpbill_create_ip(ip);
		unifiedOrder.setNotify_url("www.tantanba.cn/print/notify");
		unifiedOrder.setTrade_type("JSAPI");
		unifiedOrder.setOpenid(openId==null?userInfo.getOpenid():openId);
		unifiedOrder.setDevice_info(printerCode);
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
		//将订单号对应的打印机id存入缓存，且30分钟内付款有效
		if(printerCode != null) {
			redisTemplate.boundValueOps(tradeNo).set(printerCode, 30, TimeUnit.MINUTES);
		}
		
		return "print/order";
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
        String tradeNo = returnMsg.get("out_trade_no");
        if(!redisTemplate.hasKey(tradeNo)) {
        	return;
        }
        String printerCode = redisTemplate.boundValueOps(tradeNo).get();
    	redisTemplate.delete(tradeNo);
        
        boolean realPay = PayUtil.verifySign(sb.toString(), Configuration.getProperty("weixin4j.pay.partner.key"));
        if(!realPay) 
        	return;
        
		Map<String, Socket> clients = SocketServer.clients;
		if(clients.isEmpty())
			return;
		Socket socket = clients.get(printerCode);
		if(socket == null || socket.isClosed()) {
			return;
		}
		try {
			out = clients.get(printerCode).getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
			try {
				if(out != null)
					out.close();
				if(socket != null)
					socket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return;
		}
		
		byte[] sendMsgType = "01".getBytes();
		byte[] sendOrderData = tradeNo.getBytes();
		byte[] sendData = new byte[sendMsgType.length+sendOrderData.length];
		
		System.arraycopy(sendMsgType, 0, sendData, 0, sendMsgType.length);
		System.arraycopy(sendOrderData, 0, sendData, sendMsgType.length, sendOrderData.length);
		
		try {
			out.write(sendData);
			out.flush();
		} catch(Exception e) {
			e.printStackTrace();
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
	
	
	@RequestMapping("/prePrint")
	public @ResponseBody void socket(HttpServletRequest request, String printerCode, String tradeNo) {
		
		Map<String, Socket> clients = SocketServer.clients;
		
		if(clients.isEmpty())
			return;
		
		Socket socket = clients.get(printerCode);
		if(socket == null || socket.isClosed()) {
			return;
		}
		
		try {
			out = clients.get(printerCode).getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
			try {
				if(out != null)
					out.close();
				if(socket != null)
					socket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return;
		}
		
		String[] filePathArr = request.getParameterValues("filePath");
		if(filePathArr == null) {
			return;
		}
		for(String printData:filePathArr) {
			String[] fileData = printData.split("[*]");
			String filePath = fileData[0];
			String printGather = fileData[1];
			
			File f = new File(rootPath + filePath);
			if(!f.exists()){
	            System.out.println("文件不存在");
	        }else{
	            InputStream in = null;
				try {
					in = new FileInputStream(f);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
					break;
				}
	            int length = (int) f.length();
	            if(length>1024*1024*20){ //限制文件小于20M
	                System.out.println("文件过大");
	            }else{

	                byte[] printGatherData = printGather.getBytes();
	                byte[] sendprintGatherData = new byte[6];
	                System.arraycopy(printGatherData, 0, sendprintGatherData, 0, printGatherData.length);

	                byte[] tempData = new byte[length];
	                try {
						in.read(tempData);
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
	                
	                byte[] lengthData = intToByteArray(length);
	                
	                byte[] tradeNoData = tradeNo.getBytes();
	                byte[] sendTradeNoData = new byte[19];
	                System.arraycopy(tradeNoData, 0, sendTradeNoData, 0, tradeNoData.length);
	                
	                //消息类型
	                byte[] sendMsgType = "02".getBytes();
	                
	                byte[] sendData = new byte[sendMsgType.length+tempData.length+lengthData.length+sendprintGatherData.length+sendTradeNoData.length];

	                System.arraycopy(sendMsgType, 0, sendData, 0, sendMsgType.length);
	                System.arraycopy(lengthData, 0, sendData, sendMsgType.length, lengthData.length);
	                System.arraycopy(tempData, 0, sendData, sendMsgType.length+lengthData.length, tempData.length);
	                System.arraycopy(sendprintGatherData, 0, sendData, sendMsgType.length+lengthData.length+tempData.length, sendprintGatherData.length);
	                System.arraycopy(sendTradeNoData, 0, sendData, sendMsgType.length+lengthData.length+tempData.length+sendprintGatherData.length, sendTradeNoData.length);

	                try {
						out.write(sendData);
						out.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
	            }
	        }	
		}
	}
	
	private static byte[] intToByteArray(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
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
