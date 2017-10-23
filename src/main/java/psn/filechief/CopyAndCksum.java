package psn.filechief;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class CopyAndCksum extends Copy 
{
//	private static Logger log = LoggerFactory.getLogger(CopyAndCksum.class.getName());	

	public CopyAndCksum() 
	{
		super();
		shortName = "cpAndCksum";
	}
/*
	protected File endFile = null;
	protected boolean workZipFile(File curFile)
	{
		File zipFile = null;
		if(zip) zipFile = Zipper.zipSingleFileS(cacheDir, curFile);
		else if(gzip) zipFile = Zipper.gzipFile(cacheDir, curFile);
		if(zipFile==null) return false;
		File cs = makeCkSum(zipFile,null);
		if(cs==null) {
			deleteFile(zipFile); // если ошибка переноса, то удаляем zip файл
			return false;
		}
		boolean  mvx = moveToDst(cs); // переносим в dstDir, через cacheDir (если есть)
		if(!mvx) {
			deleteFile(cs);
			deleteFile(zipFile); // если ошибка переноса, то удаляем zip файл
			return false;
		}
		boolean  mv = moveToDst(zipFile); // переносим в dstDir, через cacheDir (если есть)
		if(!mv) deleteFile(zipFile); // если ошибка переноса, то удаляем zip файл
		else endFile = newFile;
		if( child==null ) { // нет следующего получателя
			if(mv) deleteFile(curFile); // если успешно перенесли zip файл, то удаляем оригинал 
			return mv;
		}	
		// есть следующий получатель
		if( mv ) 
			mv = moveFile(curFile, child.srcDir); // переносим в srcDir следующего получателя
		return mv;
	}

	protected boolean workFile(File curFile)
	{
		boolean ok = false;
		File cs = makeCkSum(curFile, cacheDir);
		ok = moveToDst(cs);
		if(!ok) return false;
		//if(child==null) { // нет следующего получателя
		if(child==null  || !is_MoveToNext()) { // нет следующего получателя, либо moveToNext==false			
			ok = moveToDst(curFile);
			if(ok) endFile = newFile;
			return ok;  // переносим в dstDir, через cacheDir (если есть)
		}	
		// есть следующий получатель
		ok = copyToDst(curFile);
		if( ok ) { // копируем в dstDir, через cacheDir (если есть)
			endFile = newFile;
			return moveFile(curFile, child.srcDir); // переносим в srcDir следующего получателя
		}	
		else return false;
	}

	private static final int bufSize = 8192;
	public static File makeCkSum(File file, String path)
	{
		CheckedInputStream crc = null;
		FileWriter outStream = null;
		File cks = null;
		//boolean ok = true;
		try {
			InputStream is = new BufferedInputStream(new FileInputStream(file),bufSize);
			crc = new CheckedInputStream(is, new jonelo.jacksum.algorithm.Cksum());
			byte[] buf = new byte[bufSize];
            while(crc.read(buf) >= 0);
            long cs = crc.getChecksum().getValue();
            if(path==null)
            	cks = new File(file.getPath()+".chk");
            else 
            	cks = new File(path,file.getName()+".chk");
            outStream = new FileWriter(cks,false);
            String s = "" + cs+ " " + file.length()+" "+file.getName();
            outStream.write(s);
		} catch(IOException e) {
			log.error("makeCkSum: "+e.getMessage());
			return null;
		} finally {
			try { crc.close(); } catch(Exception e1) {}
			try { outStream.close(); } catch(Exception e1) {}
		}
		return cks;
	}
	
	public int work()
	{
	    File fSrcDir = new File( srcDir );
	    
	    // List the files in srcDir
	    Filter flt = getFilter();
	    flt.clearList();
	    //File[] files = 
	    fSrcDir.listFiles(flt);
	    List<FileData> files = flt.getList();
	    if( files==null ) { 
	    	log.error("srcDir.listFiles - result is null, check directory"); 
	    	return -1; 
	    } else if( files.size()==0) { 
	    	log.debug("files not found !"); 
	    	return 0; 
	    }
	    
	    //Arrays.sort(files,this);
	    Collections.sort(files, flt);
	    if(log.isDebugEnabled()) {
	    	log.debug( "Number of files in srcDir: " + files.size() );
	    	log.debug("start work >>>>>");
	    }
	    int querySize = files.size();
		//try {
			for( int i=0; i<files.size(); i++ ) // для всех файлов
			{
				if( needForceStop() ) { 
					log.warn("break work"); 
					break; 
				}
				curFileData = files.get(i);
				File ff = new File(curFileData.getFullName());
				//long from = System.currentTimeMillis();
				if(zip||gzip) workZipFile(ff);
				else workFile(ff);
				//long to = System.currentTimeMillis();
				//writeStatInfo(endFile, (to - from)/1000);
				
			}
	    //} catch( Throwable e ) { log.error("Xmm:",e); }
	    //finally { }
		log.debug("stop work <<<<<");
		return querySize;
	}
	
	*/
}
