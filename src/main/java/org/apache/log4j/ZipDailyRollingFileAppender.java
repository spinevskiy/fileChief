package org.apache.log4j;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.log4j.helpers.LogLog;

public class ZipDailyRollingFileAppender extends DailyRollingFileAppender 
{
	public static final String zipExt = ".zip";
	public static final int BUF_SIZE = 65536;
	
	public boolean addFileZip(ZipOutputStream zipOut, File fl)
	{
	  int ret = -1;	
	  String fileName = fl.getName();
	  String fullName = fl.getAbsolutePath();  
	  if(!fl.isFile()) {
		  LogLog.error(fullName+ " is not file");
		  return false;
	  }	  
	  ZipEntry entry;  
	  try {
		 entry = new ZipEntry(fileName);
		 entry.setTime(fl.lastModified());
		 zipOut.putNextEntry(entry);
		} catch (IOException e) { 
		  LogLog.error("Couldn't add new entry "+fileName + " : " + e.getMessage());
		  return false;
		  }
	  byte[] buffer = new byte[BUF_SIZE];
	  try( InputStream in = new  BufferedInputStream( new FileInputStream(fl), BUF_SIZE) ) {
		int length;
		while ((length = in.read(buffer, 0, BUF_SIZE)) != -1) 
			zipOut.write(buffer, 0, length);
		zipOut.closeEntry();
		ret = 0;
		}
	  catch (IOException e) {
		  LogLog.error(e.getMessage()); 
	  }
	  if(ret!=0) {
		  LogLog.error("Couldn't compress " + fileName);
		  return false;
	  }	  
	  return true;
	}

	public ZipOutputStream openZip(String zname)
	{
		try { return new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zname), BUF_SIZE)); } 
		catch (IOException e) { 
			LogLog.error("Couldn't create " + zname+" : "+e.getMessage());
			return null;
		}
	} 

	public boolean closeZip(ZipOutputStream zipOut) {
		try {
			if(zipOut!=null)
				zipOut.close();
			return true;
		} catch (IOException e) { 
			LogLog.error("Couldn't close Zip : " + e.getMessage());
			return false;
		}
	}

	public boolean packFile(String zipFile, File t)
	{
		ZipOutputStream zipOut = openZip(zipFile);
		if(zipOut==null) return false;
		if(addFileZip(zipOut,t))
			if(closeZip(zipOut))
				return true;
		closeZip(zipOut);
		return false;
	}
	
 /**
    Rollover the current file to a new file.
 */
 void rollOver() throws IOException 
 {
	String oldValue = super.getScheduledFilename();
	super.rollOver();
	String newValue  = super.getScheduledFilename();

	if(oldValue==null || newValue==null || oldValue.equals(newValue))
		return;
		
	ZipThread t = new ZipThread(oldValue);
	t.start();
 }
 
 class ZipThread extends Thread 
 {
	 String fName;
	 
	 ZipThread(String scheduledFilename)
	 {
		 fName = scheduledFilename;
		 setName("Zipper");
		 setDaemon(true);
	 }
	 
	 public void run()
	 {
		 File target  = new File(fName);
		 if(!target.exists()) {
			 LogLog.error(fName + " does't exists, not packed");
			 return;
		 }	 
		 String zName = fName + zipExt; 
		 File zFile = new File(zName);
		 if(zFile.exists()) {
			 LogLog.error(zName+" already exists, not packed");
			 return;
		 }	
		 if(packFile(zName, target)) {
			 target.delete();
			 return;
		 }
		 if(zFile.exists()) 
			 zFile.delete();
	 }
 }
 
}
