package psn.filechief.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author psn
 *
 */
public class Zipper {
	private static final Logger log = LoggerFactory.getLogger(Zipper.class.getName());

	public static final String ZIP_EXT = ".zip";
	public static final String GZIP_EXT = ".gz"; 
	public static final int BUF_SIZE = 65536; 
	public final byte[] BUF = new byte[BUF_SIZE];
	
	private ZipOutputStream zipOut;
	private String zipName;
	private boolean isOpen = false;

	private String setZipName(String name)
	{
		zipName = name + ZIP_EXT;
		return zipName;
	}
	
	public int addFile(File fl,String newName)
	{
	  String fileName = fl.getName();
	  String fullName = fl.getAbsolutePath(); 
	  if(!fl.isFile()) { 
		  log.error("{} is not file", fullName); 
		  return 1;
	  } 
	  ZipEntry entry;  
	  try {
		 entry = new ZipEntry(fileName);
		 entry.setTime(fl.lastModified());
		 zipOut.putNextEntry(entry);
	  } catch (IOException e) {
		  log.error("Couldn't add new entry {} in {}", fileName, zipName);  
		  return -1;	
	  }
	  // Compress the file.
	  try( InputStream in = new BufferedInputStream( new FileInputStream(fl) , BUF_SIZE) )  {
		  int length;
		  while ((length = in.read(BUF, 0, BUF_SIZE)) != -1)
			  zipOut.write(BUF, 0, length);
		  zipOut.closeEntry();
	  } catch (IOException e) {
		  log.error("Couldn't compress {}", fileName); 
		  return -1;
	  }
	 return 0;
	}

	public int open(String zname)
	{
		if(isOpen==true) close();
		setZipName(zname);
		try {
			zipOut = new ZipOutputStream(new BufferedOutputStream( new FileOutputStream(zipName), BUF_SIZE));	
			isOpen=true;
		 } catch (IOException e) { 
			 log.error("Couldn't create {}", zipName);
			 zipOut = null;
			 isOpen=false;
			 return -1;
		 }
		return 0;	 
	} 

	public int close()  
	{
		int ret = 0;
		if(isOpen==true) {
			try {
				zipOut.close();
			} catch (IOException e) {
				log.error("Couldn't close Zip {}, {}", zipName, e.getMessage());
				ret = -1;
			}
		}
		isOpen = false;
		zipOut = null;
		return ret;
	}
	
	private File zipSingleFile(String dir, File file, String newName)
	{
		String nam = (newName == null) ? file.getName() : newName;
	    if(open(new File(dir, nam).getPath()) == 0) {
	    	if(addFile(file, newName) == 0)
	    		if(close() == 0) 
		      return new File(this.zipName);
	    }

	    File z = new File(this.zipName);
	    if (z.exists())
	      z.delete();
	    return null;
	}
/*	
	public File zipSingleFile(String dir, File file, String newName, boolean renameOnPack)
	{
		String arcName = (newName==null) ? file.getName() : newName; // имя файла архива
		if ( open((new File(dir,arcName)).getPath())==0) { // если создали файла архива
			if(addFile(file,newName)==0) // и добавили в него файл  
				if(close()==0) // и закрыли  
					return new File(zipName); // то всё хорошо
		}
		File z = new File(zipName); // иначе удаляем файла архива, если есть 
		if(z.exists())
			z.delete();
		return null;
	}
*/
	private static File zipSingleFileS(String dir, File file) 
	{
		return new Zipper().zipSingleFile(dir, file, null);
	}
	
	private static File gzipFileS(String dir, File file)
	{
		return gzipFileS(dir, file, null);
	}
	
	private static File gzipFileS(String dir, File file, String newName)
	{
		File gzFile = null;
		try (InputStream in = new BufferedInputStream( new FileInputStream(file) , BUF_SIZE) ) {
		    String fname = (newName==null) ? file.getName() : newName; 
			String gzName = fname + GZIP_EXT;
			gzFile = new File(dir,gzName);
			try( OutputStream out = new GZIPOutputStream( new FileOutputStream(gzFile), BUF_SIZE ) ) {
				byte[] buffer = new byte[BUF_SIZE];
				int length;
				while ((length = in.read(buffer, 0, BUF_SIZE)) != -1)
					out.write(buffer, 0, length);
				out.flush();
				out.close();
				gzFile.setLastModified(file.lastModified());
				return gzFile;
			}
		} catch (IOException e) {
			log.error("Couldn't gzip file: " + file);
			if(gzFile!=null && gzFile.exists())
				gzFile.delete();
			return null;
		}
	}
	
	private File gzipFile(String dir, File file, String newName)
	{
		File gzFile = null;
		try ( InputStream in = new BufferedInputStream( new FileInputStream(file) , BUF_SIZE) ) {
		    String fname = (newName==null) ? file.getName() : newName; 
			gzFile = new File(dir, fname + GZIP_EXT);
			try (OutputStream out = new GZIPOutputStream( new FileOutputStream(gzFile), BUF_SIZE) ) { 
				int length;
				while ((length = in.read(BUF, 0, BUF_SIZE)) != -1)
					out.write(BUF, 0, length);
				out.flush();
				out.close();
				gzFile.setLastModified(file.lastModified());
				return gzFile;
			}	
		} catch (IOException e) {
			log.error("Couldn't gzip file: " + file);
			if(gzFile!=null && gzFile.exists())
				gzFile.delete();
			return null;
		}
	}
	
	
	private static File unZipSingleFileExt(File f, String dstDir)// throws IOException 
	{
		if(dstDir==null) 
			dstDir = f.getParent();
		File ret = null;
		try( ZipFile zf = new ZipFile(f) ) 
		{
			ArrayList<ZipEntry> list = new ArrayList<>();
			Enumeration<? extends ZipEntry> entries = zf.entries();
			ZipEntry ze = null;
			while(entries.hasMoreElements()) {
				ze = entries.nextElement(); 
				if(ze!=null)
					list.add(ze);
			}
			if(ze==null)
				throw new IOException("not found entry in archive");
			if(list.size()!=1 || ze.isDirectory())
				throw new IOException("zip archive contains many entries, or contains directory");

			try( InputStream is = new BufferedInputStream(zf.getInputStream(ze)) ) {
				File unzip = new File(dstDir, ze.getName());
				Utl.copyInputStreamToFile(is, unzip);
				long t = ze.getTime();
				if(t!=-1) unzip.setLastModified(t);
				return unzip;
			} 
		} catch(IOException e) {
			log.error("unzip: '"+f.getPath()+"' : "+e.getMessage()); 
		}
		return ret;
	}

	private static File unGzipSingleFile(File f, String dstDir)
	{
		if(dstDir==null) 
			dstDir = f.getParent();
		String fn = f.getName();
		String name = fn.substring(0, fn.length() - GZIP_EXT.length());
		try(InputStream is = new BufferedInputStream( new GZIPInputStream( new FileInputStream(f.getPath())))) {
			File ungzip = new File(dstDir, name);
			Utl.copyInputStreamToFile(is, ungzip);
			ungzip.setLastModified(f.lastModified());
			return ungzip;
		}catch (IOException e) {
			log.error("unGzip " + f.getPath() + " : " + e.getMessage());
		}
		return null;
	}
	
	public static File unpackSingleFile(File f, String dstDir)
	{
		String fn = f.getName().toLowerCase();
		if(fn.toLowerCase().endsWith(ZIP_EXT) && fn.length()>ZIP_EXT.length())
			return unZipSingleFileExt(f, dstDir);
		if(fn.toLowerCase().endsWith(GZIP_EXT) && fn.length()>GZIP_EXT.length())
			return unGzipSingleFile(f, dstDir);
		//log.error("unpack "+ f.getPath() + " : bad extension or too short name");
		return f;
	}
	
	/**
	 * Пакует одиночный файл <b>fileName</b> в архив <b>fileName.zip</b> при <b>zip = true</b>,<br>
	 * либо в <b>fileName.gz</b> при <b>zip = false</b>. 
	 * @param f исходный файл
	 * @param dir целевой каталог
	 * @param zip true zip, false gzip
	 * @return null при ошибке, иначе запакованный файл
	 */
	public static File packSingleFileS(File f, String dir, boolean zip)
	{
		if(zip) 
			return Zipper.zipSingleFileS(dir, f);
		return Zipper.gzipFileS(dir, f);
	}

	/**
	 * Пакует одиночный файл <b>fileName</b> в архив <b>fileName.zip</b> при <b>zip = true</b>,<br>
	 * либо в <b>fileName.gz</b> при <b>zip = false</b>. 
	 * @param f исходный файл
	 * @param dir целевой каталог
	 * @param zip true zip, false gzip
	 * @return null при ошибке, иначе запакованный файл
	 */
	public File packSingleFile(File f, String dir, boolean zip)
	{
		if(zip) 
			return zipSingleFile(dir, f, null);
		return gzipFile(dir, f, null);
	}
	
	
}
