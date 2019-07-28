package com.tendwonder.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.itextpdf.text.pdf.PdfReader;
import com.tendwonder.entity.FileAttr;
import com.tendwonder.service.FileService;
import com.tendwonder.service.JacobTransition;
import com.tendwonder.util.FileTypeUtil;

@RestController
public class FileController {

	@Autowired FileService fileService;
	
	@Value("${upload.rootPath}")
	private String rootPath;
    
    
    /**
     * 多文件上传
     * @param request
     * @param folder 文件夹根据上传文件type自动生成
     * @param id 编号根据上传文件id
     * @return
     */
    @PostMapping(value="/uploadFile") 
    public List<FileAttr> uploadFile(HttpServletRequest request, String userName){
    	
        List<MultipartFile> files =((MultipartHttpServletRequest)request).getFiles("file"); 
        
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd");
        
        List<FileAttr> fileAttrList = new ArrayList<FileAttr>();
        for (MultipartFile file:files) { 
            if (!file.isEmpty()) { 
                	FileAttr fileAttr = new FileAttr();
                	Integer randomNum = new Random().nextInt(999999);
                    String originalFileName = file.getOriginalFilename();
                    //获取文件后缀名
                    String ext=FileTypeUtil.getFileSuffix(originalFileName);
                    //获取文件名
                    String fileName = FileTypeUtil.getFileName(originalFileName);
                    
                    String filePath= userName + "/" + sf.format(new Date()) +"/";
            		String originalFullName = randomNum +"_"+ originalFileName;
            		String pdfFullName = randomNum +"_"+ fileName + ".pdf";
            		
        			try {
						fileService.uploadFile(file.getBytes(), filePath, originalFullName);
						
						//获取页数
	                    int pages = 0;
	                    JacobTransition jacobTransition = new JacobTransition();
	                    switch(ext) {
	                    	case "pdf" :
								try {
									pages = new PdfReader(rootPath+filePath+originalFullName).getNumberOfPages();
								} catch (IOException e) {
									System.out.println("获取页数失败");
								}
		                    	break;
		                    	
	                    	case "docx" :
		                    case "doc" :
		                    	pages = jacobTransition.doc2pdf(rootPath+filePath+originalFullName, rootPath+filePath+pdfFullName);
		                    	break;
		                    	
		                    case "pptx" :
		                    case "ppt" :
		                    	pages = jacobTransition.ppt2pdf(rootPath+filePath+originalFullName, rootPath+filePath+pdfFullName);
		                    	break;
		                    	
		                    case "xlsx" :
		                    case "xls" :
		                    	pages = jacobTransition.excel2Pdf(rootPath+filePath+originalFullName, rootPath+filePath+pdfFullName);
		                    	break;
	                    }
	                    
	                    fileAttr.setFileName(originalFileName);
	                    fileAttr.setFileSize(file.getSize());
	                    fileAttr.setFileType(ext);
	                    fileAttr.setPages(pages);
	                    fileAttr.setRelativePath(filePath + pdfFullName);
	                    
	                    fileAttrList.add(fileAttr);
	                    
					} catch (Exception e) {
						e.printStackTrace();
					} 
                    
            } else { 
            	System.out.println("You failed to upload " + file.getName() + " because the file was empty.");
            } 
        }
        return fileAttrList;
    } 
    
    //文件下载相关代码
    @GetMapping("/downloadFile")
    public void downloadFile( String relativePath, HttpServletRequest request, HttpServletResponse response){
       
    	if (relativePath != null) {
    		
    		String filePath = rootPath + relativePath;
            File file = new File(filePath);
            if (file.exists()) {
            	String fileName = FileTypeUtil.getFileName(filePath);
                response.setContentType("application/force-download");// 设置强制下载不打开
                response.addHeader("Content-Disposition",
                        "attachment;fileName=" +  fileName);// 设置文件名
                byte[] buffer = new byte[1024];
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                try {
                    fis = new FileInputStream(file);
                    bis = new BufferedInputStream(fis);
                    OutputStream os = response.getOutputStream();
                    int i = bis.read(buffer);
                    while (i != -1) {
                        os.write(buffer, 0, i);
                        i = bis.read(buffer);
                    }
                    System.out.println("success");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (bis != null) {
                        try {
                            bis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
