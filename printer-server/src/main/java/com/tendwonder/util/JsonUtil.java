package com.tendwonder.util;

import com.alibaba.fastjson.JSONObject;

/**
 * json操作类
 * 
 * @author liuxg
 *
 */
public class JsonUtil {

	private  JSONObject jo;

	private JsonUtil() {
	}
	
	/**
	 * 获取单例
	 * 
	 * @return
	 */
	public static JsonUtil getInstance() {
		return new JsonUtil();
	}

	/**
	 * 将数据装载到json里面支持链式
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public JsonUtil putData(String key, Object value) {
		if (jo == null) {
			jo = new JSONObject();
		}
		jo.put(key, value == null ? "" : value);
		return this;
	}

	/**
	 * 将JSONObject转成字符并置为空
	 * 
	 * @return
	 */
	public String pushData() {

		if (jo != null) {
			JSONObject _jo = jo;
			jo = null;
			return _jo.toString();
		} else {
			return null;
		}
	}

	/**
	 * 获取jsonObject
	 * s
	 * @return
	 */
	public JSONObject getJo() {
		return jo;
	}
	
     /**
     * 格式化date
     */
}
