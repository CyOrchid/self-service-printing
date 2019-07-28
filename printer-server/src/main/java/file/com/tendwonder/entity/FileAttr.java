package com.tendwonder.entity;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.tendwonder.base.IdEntity;


@Entity
@Table(name = "file")
public class FileAttr extends IdEntity{

    private String fileName;        
    private String fileType;  
    private long fileSize;
    private String relativePath;
    private int pages;
    
    public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	public long getFileSize() {
		return fileSize;
	}
	public void setFileSize(long l) {
		this.fileSize = l;
	}
	public String getRelativePath() {
		return relativePath;
	}
	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}
	public int getPages() {
		return pages;
	}
	public void setPages(int pages) {
		this.pages = pages;
	}
}