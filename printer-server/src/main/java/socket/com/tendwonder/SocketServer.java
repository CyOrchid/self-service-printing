package com.tendwonder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
public class SocketServer implements CommandLineRunner{
   
    public static Map<String, Socket> clients = new HashMap<String, Socket>();
    
    ServerSocket server;
    int port = 8080;

    @Override
    public void run(String... strings) throws Exception {
        server();
    }

    public void server() {
        try {

            server = new ServerSocket(port);
            while (true) {
            	
                Socket socket = server.accept();
                
                //启动接收打印机code注册线程
                Mythread mythread = new Mythread(socket);
                mythread.start();
            }

        } catch (Exception ex) {
        	ex.printStackTrace();
        }
    }

    class Mythread extends Thread {
        private Socket socket;
        private BufferedReader br;
        private PrintWriter pw;
        public String printerCode;

        public Mythread(Socket s) {
            this.socket = s;
        }

        public void run() {

            try {
            	br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                if ((printerCode = br.readLine()) != null) {
                	clients.put(printerCode, socket);
                }
            	

            	pw = new PrintWriter(clients.get(printerCode).getOutputStream(), true);
                pw.println("打印机："+ printerCode +" 已上线");
                System.out.println("打印机："+ printerCode +" 已上线");
                pw.flush();
            	
            } catch (Exception ex) {
            	ex.printStackTrace();
            	try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        }

    }

}
