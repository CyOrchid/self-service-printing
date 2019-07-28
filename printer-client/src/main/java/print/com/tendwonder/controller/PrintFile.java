package com.tendwonder.controller;


import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Sides;
import javax.servlet.http.HttpServletRequest;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.printing.PDFPageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


/**
* 调用打印机打印文件
* @author 寇航艇
*
*/
@RestController
public class PrintFile {
	
	/**
    * 调用打印机，设置打印份数，打印起始位置结束位置，双面打印
    * Prints using custom PrintRequestAttribute values.
    */
	@GetMapping("/print")
	public void printWithAttributes(String printFilePath, Map<Object, Object> map) {
   	
   	try {
			PrinterJob job = PrinterJob.getPrinterJob();
			PDDocument document = PDDocument.load(new File("C:\\Users\\Administrator\\Desktop\\2.pdf"));
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
			String printer = (String)map.get("printer");
			
			for (int i = 0; i < printService.length; i++) {
				if (printService[i].getName().equals("Canon TS6180")) {
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
				// 打印份数
				// attr.add(new Copies(1));
				if (map.get("Copies") != null) {
					attr.add(new Copies((int) map.get("Copies")));
				}
				// 纸张大小
				// attr.add(MediaSize.ISO.A4);
				// 双面打印
				if (map.get("duplex") != null) {
					if ((boolean)map.get("duplex")) {
						attr.add(Sides.DUPLEX);
					}
				} else {
					attr.add(Chromaticity.MONOCHROME);
					attr.add(new Copies(2));
					attr.add(Sides.ONE_SIDED);
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
	
	/**
	 * 获取打印参数
	 * @param request
	 * @return
	 */
	public Map<Object, Object> getPrintParams(HttpServletRequest request) {
		
		// 文件已生成则根据文件路径打印文件
		Map<Object, Object> map = new HashMap<Object, Object>();
		// 获取所请求的打印机
		String printer = request.getParameter("printer");
		if (printer != null && !"".equals(printer)) {
			map.put("printer", printer);
		}
		// 获取打印份数
		String pages = request.getParameter("printPages");
		if (pages != null && !"".equals(pages)) {
			map.put("Copies", Integer.valueOf(pages));
		}
		// 获取双面打印
		String Sides = request.getParameter("printSides");
		if (Sides != null && !"".equals(Sides)) {
			map.put("Sides", Integer.getInteger(Sides));
		}
		
		return map;

	}

}

