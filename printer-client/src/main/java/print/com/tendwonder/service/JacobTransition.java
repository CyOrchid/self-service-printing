package com.tendwonder.service;

import org.springframework.stereotype.Service;

import com.itextpdf.text.pdf.PdfReader;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComFailException;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

@Service
public class JacobTransition {

	public String doc2pdf(String srcFilePath, String pdfFilePath) {  
        ActiveXComponent app = null;  
        Dispatch doc = null;  
        try {  
            ComThread.InitSTA();  
            app = new ActiveXComponent("Word.Application");  
            app.setProperty("Visible", false);  
            Dispatch docs = app.getProperty("Documents").toDispatch();  
            doc = Dispatch.invoke(docs, "Open", Dispatch.Method,  
                    new Object[] { srcFilePath,   
                                                 new Variant(false),   
                                                 new Variant(true),//是否只读  
                                                 new Variant(false),   
                                                 new Variant("pwd") },  
                    new int[1]).toDispatch();  
//          Dispatch.put(doc, "Compatibility", false);  //兼容性检查,为特定值false不正确  
            Dispatch.put(doc, "RemovePersonalInformation", false);  
            Dispatch.call(doc, "ExportAsFixedFormat", pdfFilePath, 17); // word保存为pdf格式宏，值为17  
  
            PdfReader reader = new PdfReader(pdfFilePath);
			System.out.println(reader.getNumberOfPages());
			
            return pdfFilePath; // set flag true;  
        } catch (ComFailException e) {  
            return null;  
        } catch (Exception e) {  
            return null;  
        } finally {  
            if (doc != null) {  
                Dispatch.call(doc, "Close", false);  
            }  
            if (app != null) {  
                app.invoke("Quit", 0);  
            }  
            ComThread.Release();  
        }  
    } 
	
	public boolean ppt2pdf(String srcFilePath, String pdfFilePath) {  
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
  
                Dispatch.call(ppt, "SaveAs", pdfFilePath, 32); //ppSaveAsPDF为特定值32  
  
                return true; // set flag true;  
            } catch (ComFailException e) {  
                return false;  
            } catch (Exception e) {  
                return false;  
            } finally {  
                if (ppt != null) {  
                    Dispatch.call(ppt, "Close");  
                }  
                if (app != null) {  
                    app.invoke("Quit");  
                }  
                ComThread.Release();  
            }  
    }  
}
