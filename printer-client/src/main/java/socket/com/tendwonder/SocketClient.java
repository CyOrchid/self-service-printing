package com.tendwonder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.tendwonder.controller.PrintController;


@Component
public class SocketClient implements CommandLineRunner{

	@Autowired PrintController printController;
	
	@Value("${printerCode}")
	private String printerCode;
	
	@Autowired
	private StringRedisTemplate redisTemplate;
	
	private InputStream in;
	
	@Override
    public void run(String... strings) throws Exception {
        client();
    }
	public int port = 8080;
	public static Socket socket = null;

	public void client() {
	
		try {
		    socket = new Socket("www.tantanba.cn", port);
		    socket.setKeepAlive(true);
		    System.out.println("打印服务连接成功");
		    //向服务端注册本打印机code
		    new Register().start();
		
		    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		    String serverMsg;
		    if((serverMsg = br.readLine()) != null) {
		        System.out.println(serverMsg);
		    }
		    
		    new ReadThread().start();
		    
		    //启动断线监测重连线程
		    new ReconnectThread().start();
		    
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class ReadThread extends Thread {
		
		public void run() {
			try {
				in = socket.getInputStream();
				
				while(in != null){
					//读取消息类型，2个字节
					byte[] msgType = new byte[2];
					in.read(msgType);
					String msgTypeStr = new String(msgType);
					System.out.println(msgTypeStr);
					if("01".equals(msgTypeStr)) {	//订单号传输消息
						//读取已支付订单号， 19个字节
						byte[] tradeNoByte = new byte[19];
						in.read(tradeNoByte);
						//开始打印
						printController.payPrint(new String(tradeNoByte).trim());
					}
					
					if("02".equals(msgTypeStr)) {	//文件传输消息
						//读取文件长度， 4个字节
						byte[] lData = new byte[4];
		                in.read(lData);
		                int length = byteArrayToInt(lData);
		                System.out.println("文件大小："+length+"字节");
		                
		                //读取文件， length个字节
		                byte[] dt2 = new byte[length];
		                readData(in,dt2);

		                //读取文件打印属性， 6个字节
		                byte[] type = new byte[6];
		                in.read(type);
		                String typeStr = new String(type);
		                System.out.println("打印属性："+typeStr.trim());
		                
		                //读取订单号， 19个字节
		                byte[] tradeNo = new byte[19];
		                in.read(tradeNo);
		                String tradeNoStr = new String(tradeNo).trim();
		                System.out.println("订单号："+tradeNoStr);
		                
		                
		                String filePath = "E:\\Printed\\";
		                File targetFile = new File(filePath);
		                if(!targetFile.exists()){    
		                    targetFile.mkdirs();    
		                }  
		                String finalPath = filePath + tradeNoStr + "_" + new Random().nextInt(999)+".pdf";
		                OutputStream o = null;
		                try {
		                	o = new FileOutputStream(finalPath);
		                	o.write(dt2);
			                o.flush();
			                
			                //将该订单号对应的打印数据存入缓存列表
			                redisTemplate.boundListOps(tradeNoStr).rightPush(finalPath + "|" + typeStr.trim());
			                
		                }catch(Exception e) {
		                	e.printStackTrace();
		                }finally{
		                	if( o != null )
		                		o.close();
		                }
					}
	            }
			} catch (IOException e1) {
				e1.printStackTrace();
			}
            
		}
	}
	
	class Register extends Thread {

		public void run() {
		    try {
		        PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
		        while (true) {
		            pw.println(printerCode);
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		}
	}

	class ReconnectThread extends Thread{
		public void run() {
			while(true) {
				if( socket != null && !socket.isClosed() ) {
					boolean status = true;
					while(status) {
						try{
							socket.sendUrgentData(0xFF);
							Thread.sleep(3000);
						}catch(Exception e1) {
							System.out.println("打印服务连接断开，尝试重连！断开信息：" + e1.getMessage());
							try {
								if( in != null) {
									in.close();
								}
								if( socket != null) {
									socket.close();
								}
							} catch (IOException e) {
								System.out.println("打印服务关闭出错！出错信息：" + e.getMessage());
							} finally {
								status = false;
							}
						}
					}
				}else {
					try {
						socket = new Socket("www.tantanba.cn", port);
						System.out.println("打印服务客户端重连成功");
						ReadThread readThread = new ReadThread();
				        readThread.start();
				        
				        new Register().start();
				        
				        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					    String serverMsg;
					    if((serverMsg = br.readLine()) != null) {
					        System.out.println(serverMsg);
					    }
					    
					} catch (IOException e) {
						System.out.println("打印服务连接失败，10秒后重连！错误信息：" + e.getMessage());
					}
				}
				try{
					Thread.sleep(10000);
				}catch(Exception e) {
					System.out.println(e.getMessage());
				}
			}
		}
	}
	
	private static int byteArrayToInt(byte[] b) {
	  return b[3] & 0xFF |
	          (b[2] & 0xFF) << 8 |
	          (b[1] & 0xFF) << 16 |
	          (b[0] & 0xFF) << 24;
	}
	
	/**
	* 
	* @param in
	* @param bData
	* @throws IOException
	*/
	private static void readData(InputStream in,byte[] bData) throws IOException{
	  int off = 0;
	  int length = bData.length;
	  int readLength = 0;
	  do{
	      off = readLength+off;
	      length = length-readLength;
	      readLength = in.read(bData, off, length);
	  }while(readLength!=length);
	}
}
