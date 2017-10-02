package io.rnkit.appparse.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Name: ApkUtil
 * Author: SimMan [liwei0990@gmail.com]
 * CreatedAt: 02/10/2017
 * Description:
 * Copyright (c) 2017 Toutoo, Inc.
 */
public class IconUtil {
	
	/**
	 * 从指定的apk文件里获取指定file的流
	 * @param apkpath
	 * @param fileName
	 * @return
	 */
	public static InputStream extractFileFromApk(String apkpath, String fileName) {
		try {
			ZipFile zFile = new ZipFile(apkpath);
			ZipEntry entry = zFile.getEntry(fileName);
			entry.getComment();
			entry.getCompressedSize();
			entry.getCrc();
			entry.isDirectory();
			entry.getSize();
			entry.getMethod();
			InputStream stream = zFile.getInputStream(entry);
			return stream;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void extractFileFromApk(String apkpath, String fileName, String outputPath) throws Exception {
		InputStream is = extractFileFromApk(apkpath, fileName);
		
		File file = new File(outputPath);
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file), 1024);
		byte[] b = new byte[1024];
		BufferedInputStream bis = new BufferedInputStream(is, 1024);
		while(bis.read(b) != -1){
			bos.write(b);
		}
		bos.flush();
		is.close();
		bis.close();
		bos.close();
	}

	public static String getIconMax(Map<String, String> applicationIcons) {
		Iterator<String> it = applicationIcons.keySet().iterator();
		int max = 0;
		while (it.hasNext()) {
			String name = it.next();
			String[] arr = name.split("-");
			int a = Integer.valueOf(arr[2]);
			if (a > max) {
				max = a;
			}
			System.out.println(applicationIcons.get(name));
		}
		return applicationIcons.get("application-icon-" + max);
	}
}
