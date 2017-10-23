package psn.filechief;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import psn.filechief.util.bl.FileData;

public abstract class LocalSource extends Cfg 
{
	private static Logger log = LoggerFactory.getLogger(LocalSource.class.getName());	
	public static final int DEFAULT_DELAY = 5;
	
	/**
	 * Собственно обработка файла.
	 * Предварительная работа по подготовке файла к обработке делается LocalSource.workWithFile(FileData d)
	 * @param fd
	 * @param f
	 * @param dstName
	 * @return
	 * @throws IOException
	 */
    protected abstract boolean workWithFileOpt(FileData fd, File f,String dstName) throws IOException;
	
	private Filter filter = new Filter();

	public boolean isLocalSource() {
		return true;
	}

  //  protected File newFile = null; 
    
	public void beforeListing() throws IOException {
	}
	
	public boolean workWithFile(FileData d) throws IOException 
	{
		File file = new File(d.getFullName());
    	if( DEVNULL.equals(dstDir) ) { // Если в качестве целевого каталога указан /dev/null - удаляем файл.
    		if(file.delete()) { 
    			log.warn("ok delete {}", file.getPath());
    			return true;
    		}
    		log.error("can't delete {}", file.getPath());
    		return false;
    	}
		
		File f = unpackFile(file, getCacheDir() , false );
		if(f==null) // ошибка распаковки 
			return false;
		if(!f.getPath().equals(file.getPath())) { // распаковали
			log.info("unpacked {} to {}, size: {}", new Object[] {d.getName(), f.getPath(), f.length()});
			d.setWorkFullName(f.getPath());
		}
		if(zip||gzip) {
			File zipFile = zipper.packSingleFile(f, cacheDir, zip);
			if(zipFile==null) // ошибка паковки 
				return false;
			log.info("packed {} to {}, size: {}", new Object[] {f.getName(), zipFile.getPath(), zipFile.length()});
			f = zipFile;
			d.setWorkFullName(f.getPath());
		}
		Date xd = is_timeByFile() ? new Date(d.getModTime()) : new Date();
		String dstName = makeName(f.getName(), xd); // формируем имя выходного файла
		// если d.dirUpdated(), то файл уже в cacheDir
		// иначе, если cacheDir не пусто --------,и это локальное перемещение ---&& getActionType()==ActionType.LOCAL2LOCAL
		if(!d.dirUpdated() && cacheDir!=null && cacheDir.length()!=0 ) { // копируем в cacheDir, 
			//f = copyOrMoveOpt(f, cacheDir, false, dstName, false);
			f = copyOrMoveOpt(f, cacheDir, false, dstName, true);
			if(f==null)
				return false;
			d.setWorkFullName(f.getPath());
		}	
		// теперь файл (f) , готовый к доставке, находится в cacheDir;
		// либо, если cacheDir="", то в srcDir.
		return workWithFileOpt(d, f, dstName);
		//return afterAction(d, false);
	}
    
	static final String DEL_OK = "ok: delete file {}";
	static final String REN_OK = "ok: rename file from {} to {}";
	private String cacheDir2 = null;
	public boolean afterAction(FileData f, boolean fromBlackList) throws IOException 
	{
		if(getActionType().equals(ActionType.LOCAL2LOCAL) && !f.dirUpdated() && !Linker.isLinker(this)) // т.е. это последний агент, типа Copy, с cacheDir="" и, следовательно, файл уже перемещён в целевой каталог.
			return true;

		File src = new File(f.getFullName());
		if(child==null  || !is_MoveToNext()) { // нет следующего получателя, либо moveToNext==false
			if(!super.is_deleteAfter()) // запрет удаления -> включен batchMode, не удаляем.
				return true;
			return deleteFile(src, !fromBlackList); //!!!!
		}	
		// есть следующий получатель
		if(!is_deleteAfter())  // а если удалять нельзя, как обеспечить атомарность переноса ?
		{ // этот блок работает только при deleteAfter = false;
			if(cacheDir2==null) { // если ещё не использовали cache2 - надо создать, в child.srcDir 
				File d = new File(child.srcDir,"cache2_"+name);
				if(!d.mkdir()) {
					log.error("can't create dir: {}", d);
					return false;
				}
				log.info("dir created: {}",d);
				cacheDir2 = d.getAbsolutePath();
			}
			// cache2 существует, копируем туда файл
			src = copyOrMoveOpt(src, cacheDir2, false, null, true);
			if(src==null) // скопировать не удалось
				return false;
		}
		return moveFileUniq(src, child.srcDir); // переносим в srcDir следующего получателя
	}

	public Filter getFilter() {
		return filter;
	}
    
	public List<FileData> getFilesList() 
	{
		String workSrcDir = getCurrentSrcDir(srcDir, File.separator);
		Filter flt = getFilter();
		flt.clearList();
		if(srcSubDirList==null) {
			File fSrcDir = new File( workSrcDir );
			fSrcDir.listFiles(flt);
			return flt.getList();
		}
		for(String x : srcSubDirList)
		{
			flt.subDir = x;
			File fSrcDir = new File(workSrcDir,x);
			fSrcDir.listFiles(flt);
		}
		return flt.getList();
		
	}
	
    class Filter implements FileFilter//, ISmartFilter //Comparator<FileData>,
    {
    	List<FileData> list = new ArrayList<>(200);
    	
    	String subDir = null;

    	public void clearList()
    	{
    		subDir = null;
    		if(list!=null)
    			list.clear();
    	}
    	
    	public List<FileData> getList()
    	{
    		return list;
    	}

    	public boolean accept(File f) {
			if(!f.isFile() || !isValidName(f.getName())) 
				return false;
			FileData d = new FileData(f.getName(), f.lastModified(), FileData.T_FILE, f.length());
			d.setFullName(f.getAbsolutePath());
			d.setWorkFullName(f.getAbsolutePath());
			if(subDir!=null)
				d.setAltName(subDir + File.separator + d.getName());
			try {
				fts.updateTimestamp(d);
			} catch (ParseException e) {
				throw new IllegalArgumentException("ParseDate: agent="+getName()+" : "+e.getMessage()); 
			} 
			if( !inBlackList(d) )
				list.add(d);
			return false;
		}
    }

}
