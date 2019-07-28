package com.tendwonder.service;

import org.springframework.stereotype.Service;

import com.itextpdf.text.pdf.PdfReader;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

@Service
public class JacobTransition {

	private static final Integer WORD_TO_PDF_OPERAND = 17;
    private static final Integer PPT_TO_PDF_OPERAND = 32;
    private static final Integer EXCEL_TO_PDF_OPERAND = 0;
    
	public int doc2pdf(String srcFilePath, String pdfFilePath) {  
        ActiveXComponent app = null;  
        Dispatch doc = null;  
        try {  
            ComThread.InitSTA();  
            app = new ActiveXComponent("Word.Application");  
            app.setProperty("Visible", false);  
            Dispatch docs = app.getProperty("Documents").toDispatch();  
            doc = Dispatch.invoke(
            		docs,
            		"Open", 
            		Dispatch.Method,  
                    new Object[] { 
                    		 srcFilePath,   
                             new Variant(false),   
                             new Variant(true),//是否只读  
                             new Variant(false),   
                             new Variant("pwd") },  
                    new int[1]).toDispatch();  
//          Dispatch.put(doc, "Compatibility", false);  //兼容性检查,为特定值false不正确  
            Dispatch.put(doc, "RemovePersonalInformation", false);  
            Dispatch.call(doc, "ExportAsFixedFormat", pdfFilePath, WORD_TO_PDF_OPERAND); // word保存为pdf格式宏，值为17  
  
            PdfReader reader = new PdfReader(pdfFilePath);
            return reader.getNumberOfPages();
            
        } catch (Exception e) {  
        	System.out.println("获取页数失败");
        	e.printStackTrace();
        } finally {  
            if (doc != null) {  
                Dispatch.call(doc, "Close", false);  
            }  
            if (app != null) {  
                app.invoke("Quit", 0);  
            }  
            ComThread.Release();  
        }
		return 0;  
    } 
	
	public int ppt2pdf(String srcFilePath, String pdfFilePath) {  
        ActiveXComponent app = null;  
        Dispatch ppt = null;  
            try {  
                ComThread.InitSTA();  
                app = new ActiveXComponent("PowerPoint.Application");  
                Dispatch ppts = app.getProperty("Presentations").toDispatch();  
  
                // 因POWER.EXE的发布规则为同步，所以设置为同步发布  
                ppt = Dispatch.call(ppts, "Open", srcFilePath, true,// ReadOnly  
                        true,// Untitled指定文件是否有标题  
                        false// WithWindow指定文件是否可见  
                        ).toDispatch();  
  
                Dispatch.call(ppt, "SaveAs", pdfFilePath, PPT_TO_PDF_OPERAND); //ppSaveAsPDF为特定值32  
  
                PdfReader reader = new PdfReader(pdfFilePath);
                return reader.getNumberOfPages();
                
            } catch (Exception e) {  
            	System.out.println("获取页数失败");
                e.printStackTrace();
            } finally {  
                if (ppt != null) {  
                    Dispatch.call(ppt, "Close");  
                }  
                if (app != null) {  
                    app.invoke("Quit");  
                }  
                ComThread.Release();  
            }
			return 0;  
    }  
	
	public int excel2Pdf(String srcFilePath, String pdfFilePath) throws Exception {
        ActiveXComponent ax = null;
        Dispatch excel = null;
        try {
            ComThread.InitSTA();
            ax = new ActiveXComponent("Excel.Application");
            ax.setProperty("Visible", new Variant(false));
            ax.setProperty("AutomationSecurity", new Variant(3)); // 禁用宏
            Dispatch excels = ax.getProperty("Workbooks").toDispatch();

            Object[] obj = new Object[]{ 
            		srcFilePath, 
                    new Variant(false),
                    new Variant(false) 
             };
            excel = Dispatch.invoke(excels, "Open", Dispatch.Method, obj, new int[9]).toDispatch();
            
            // 转换格式
            Object[] obj2 = new Object[]{ 
                    new Variant(EXCEL_TO_PDF_OPERAND), // PDF格式=0
                    pdfFilePath, 
                    new Variant(0)  //0=标准 (生成的PDF图片不会变模糊) ; 1=最小文件
            };
            Dispatch.invoke(excel, "ExportAsFixedFormat", Dispatch.Method,obj2, new int[1]);

            PdfReader reader = new PdfReader(pdfFilePath);
            return reader.getNumberOfPages();
            
        } catch (Exception es) {
            es.printStackTrace();
        } finally {
            if (excel != null) {
                Dispatch.call(excel, "Close", new Variant(false));
            }
            if (ax != null) {
                ax.invoke("Quit", new Variant[] {});
                ax = null;
            }
            ComThread.Release();
        }
		return 0;
    }
}
