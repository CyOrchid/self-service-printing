package com.tendwonder.service;

import java.io.File;
import java.io.FileOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileService {

	@Value("${upload.rootPath}")
	private String rootPath;
	
	public void uploadFile(byte[] file, String filePath, String fileName) throws Exception { 
		String finalFilePath = rootPath + filePath;
        File targetFile = new File(finalFilePath);  
        if(!targetFile.exists()){    
            targetFile.mkdirs();    
        }       
        FileOutputStream out = new FileOutputStream(finalFilePath+fileName);
        out.write(file);
        out.flush();
        out.close();
    }
}
