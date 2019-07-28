package com.tendwonder.controller;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Sides;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.printing.PDFPageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrintController {

	@Autowired
	private StringRedisTemplate redisTemplate;
	
	@RequestMapping("payPrint")
	public void payPrint(String tradeNo) {
		
		while(redisTemplate.boundListOps(tradeNo).size() > 0) {
			String printData = (String) redisTemplate.boundListOps(tradeNo).leftPop();
			String[] printDataArr = printData.split("[|]");
			String filePath = printDataArr[0];
			String doublePrint = printDataArr[1];
			String colorPrint = printDataArr[2];
			String copies = printDataArr[3];
			
			try {
				System.out.println("正在打印："+filePath);
//				 PrintPDF.main(new String[]{
//				       "-silentPrint",//静默打印
//				       "-orientation","auto",//打印方向，三种可选
//				       filePath//打印PDF文档的路径
//				});
				printWithAttributes(filePath, doublePrint, colorPrint, copies);
				
	        }catch(Exception e) {
	         	System.out.println("打印出错"+e.getMessage());
	        }
		}
	}
	
	private void printWithAttributes(String printFilePath, String doublePrint, String colorPrint, String copies) {
	   	
	   	try {
				PrinterJob job = PrinterJob.getPrinterJob();
				PDDocument document = PDDocument.load(new File(printFilePath));
				job.setPageable(new PDFPageable(document));
				
				// 构建打印请求属性集
				PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
				// 设置打印格式，因为未确定文件类型，这里选择AUTOSENSE
				DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
				// 查找所有的可用打印服务
				// [发送至 OneNote 2013, NPI82AACC (HP LaserJet M1536dnf MFP), Microsoft XPS Document Writer, Fax, CutePDF Writer]
				PrintService printService[] = PrintServiceLookup.lookupPrintServices(flavor, pras);
				// 定位默认的打印服务
				PrintService defaultService = null;
				// 获取打印机
				
				for (int i = 0; i < printService.length; i++) {
					if (printService[i].getName().equals("Canon TS6180 series")) {
						defaultService = printService[i];
						break;
					}
				}
				
				if (defaultService != null) {
					// 设置打印机
					job.setPrintService(defaultService);
					// 设置打印机属性
					PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
					
					// 打印范围，第几页到第几页
					// attr.add(new PageRanges(1, 1));
					// 纸张大小
					// attr.add(MediaSize.ISO.A4);
					
					// 双面打印
					if ("Y".equals(doublePrint)) {
						attr.add(Sides.DUPLEX);
					} else {
						attr.add(Sides.ONE_SIDED);
					}
					//彩色打印
					if("Y".equals(colorPrint)) {
						attr.add(Chromaticity.COLOR);
					} else {
						attr.add(Chromaticity.MONOCHROME);
					}
					// 打印份数
					if (copies != null) {
						attr.add(new Copies(Integer.valueOf(copies)));
					}else {
						attr.add(new Copies(1));
					}
					
					job.print(attr);
					document.close();
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			} catch (InvalidPasswordException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (PrinterException e) {
				e.printStackTrace();
			}
	   	
	   }
}
