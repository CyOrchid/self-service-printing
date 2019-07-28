package com.tendwonder.util;

/**
 * 文件类型的工具类
 */
public class FileTypeUtil {

	
	
	/**
	 * 图片类型
	 */
	public final static String[] IMG_FILE_TYPE = { "jpg", "bmp", "png", "gif" ,"jpeg"};

	/**
	 * 视频类型
	 */
	public final static String[] VIDEO_FILE_TYPE = { "mp4", "gmf", "wmv", "avi" };

	/**
	 * 日志文件
	 */
	public final static String[] LOG_FILE_TYPE = { "log", "txt", "doc", "docx" };
	
	/**
	 * 检查是否日志文件
	 * 
	 * @param fileName
	 * @return
	 */
	public static boolean bLogFileType(String fileName) {
		String fileSuffix = getFileSuffix(fileName);
		for (int i = 0; i < LOG_FILE_TYPE.length; i++) {
			if (LOG_FILE_TYPE[i].equals(fileSuffix)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断文件名是否为图片类型
	 * 
	 * @param fileName
	 * @return
	 */
	public static boolean bImgFileType(String fileName) {
		String fileSuffix = getFileSuffix(fileName);
		for (int i = 0; i < IMG_FILE_TYPE.length; i++) {
			if (IMG_FILE_TYPE[i].equals(fileSuffix)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 获取文件后缀
	 * 
	 * @param fileName
	 * @return
	 */
	public static String getFileSuffix(String fileName) {
		return fileName.substring(fileName.lastIndexOf(".") + 1);
	}
	
	/**
	 * 获取文件名
	 * 
	 * @param fileName
	 * @return
	 */
	public static String getFileName(String fileName) {
		int dot = fileName.lastIndexOf('.'); 
		String filename = null;
		if ((dot >-1) && (dot < (fileName.length()))) { 
			 filename = fileName.substring(0, dot); 
		} 
		return filename;
	}
}
