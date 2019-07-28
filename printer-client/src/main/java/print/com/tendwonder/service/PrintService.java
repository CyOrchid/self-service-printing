package com.tendwonder.service;

import org.springframework.stereotype.Service;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

@Service
public class PrintService {

	public void printWord(String path) {
		
		System.out.println("开始打印");
		ComThread.InitSTA();
        ActiveXComponent word=new ActiveXComponent("Word.Application");
        Dispatch doc=null;
        Dispatch.put(word, "Visible", new Variant(false));
        Dispatch docs=word.getProperty("Documents").toDispatch();
        doc=Dispatch.call(docs, "Open", path).toDispatch();
        
        try {
            Dispatch.call(doc, "PrintOut");//打印
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("打印失败");
        }finally{
            try {
                if(doc!=null){
                    Dispatch.call(doc, "Close",new Variant(0));
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            //释放资源
            ComThread.Release();
        }
	}
	
	
	public void printExcel(String path){
        /**
         * 功能:实现excel打印工作
         */
        ComThread.InitSTA();
        ActiveXComponent xl = new ActiveXComponent("Excel.Application");
        try {
            // System.out.println("version=" +
            // xl.getProperty("Version"));
            // 不打开文档
            Dispatch.put(xl, "Visible", new Variant(false));
            Dispatch workbooks = xl.getProperty("Workbooks").toDispatch();
            // 打开文档
            Dispatch excel = Dispatch.call(workbooks, "Open", path).toDispatch();
            // 横向打印(2013/05/24)
            // Dispatch currentSheet = Dispatch.get(excel,
            // "ActiveSheet")
            // .toDispatch();
            // Dispatch pageSetup = Dispatch
            // .get(currentSheet, "PageSetup").toDispatch();
            // Dispatch.put(pageSetup, "Orientation", new Variant(2));
            // 每张表都横向打印2013-10-31
            Dispatch sheets = Dispatch.get((Dispatch) excel, "Sheets").toDispatch();
            // 获得几个sheet
            int count = Dispatch.get(sheets, "Count").getInt();
            // System.out.println(count);
            for (int j = 1; j <= count; j++) {
                Dispatch sheet = Dispatch
                        .invoke(sheets, "Item", Dispatch.Get, new Object[] { new Integer(j) }, new int[1])
                        .toDispatch();
                Dispatch pageSetup = Dispatch.get(sheet, "PageSetup").toDispatch();
                Dispatch.put(pageSetup, "Orientation", new Variant(2));
                Dispatch.call(sheet, "PrintOut");
            }
            // 开始打印
            if (excel != null) {
                // Dispatch.call(excel, "PrintOut");
                // 增加以下三行代码解决文件无法删除bug
                Dispatch.call(excel, "save");
                Dispatch.call(excel, "Close", new Variant(true));
                excel = null;
            }
            xl.invoke("Quit", new Variant[] {});
            xl = null;
 
        } catch (Exception e) {
            e.printStackTrace();
 
        } finally {
            // 始终释放资源
            ComThread.Release();
        }
    }

}
