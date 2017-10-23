package psn.filechief;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import psn.filechief.util.LocalParamsStorage;
import psn.filechief.util.Param;
import psn.filechief.util.RefUtl;
import psn.filechief.util.RegEx;
import psn.filechief.util.SubstituteWithTime;
import psn.filechief.util.Utl;
import psn.filechief.util.Zipper;
import psn.filechief.util.bl.FileData;
import psn.filechief.util.bl.FileDataBlackList;
import psn.filechief.util.bl.UploadBlackList;
import psn.filechief.util.stat.StatInfo;

@XmlAccessorType(XmlAccessType.NONE)
//@XmlType(propOrder = {"name", "pattern", "applyMethod"})
public abstract class Cfg implements Runnable 
{
	private static Logger log = LoggerFactory.getLogger(Cfg.class.getName());
	public static final int LocLoc = ActionType.LOCAL2LOCAL.ordinal();
	public static final int LocRem = ActionType.LOCAL2REMOTE.ordinal();
	public static final int RemLoc = ActionType.REMOTE2LOCAL.ordinal();
	
    public static final String CRIT = "CRITICAL";
    public static final String CRIT2 = "CRITICAL!";
    public static final String CRIT3 = "CRITICAL!!";
    public static final String NOT_DELETED = "not deleted";
    public static final String NOT_RENAMED = "not renamed";
    public static final String delErrMess = "deleteFile: {} - {}";
    
	public static final String START_DATE_FORMAT =  "yyyy-MM-dd HH:mm:ss";
	public static final int ST_STOPPED = 0;
	public static final int ST_STARTED = 1;
	public static final int ST_CLEANING = 2;

	public static final String DEVNULL = "/dev/null";
	public static final String PAT_DELIM = ";";
	public static final String PAT_DELIM_EXP = "\\s*;\\s*";
	
	public static final String PAUSED = "work paused";
	public static final String RESUMED = "work resumed";
	
	/**
	 * интервал проверки наличия файла (мс)
	 */
	public static final long checkPauseFlagInterval = 10000;
	
	public static final int waitCnt = 200;
	public static final int waitStepDelay = 50;
	public static final int waitDelay = waitCnt*waitStepDelay/1000;

	@XmlAttribute
	protected String name = null;
	@XmlAttribute
	protected String ftpServer = null;
	@XmlAttribute
	protected Integer ftpServerPort = null;
	@XmlAttribute
	protected String ftpSeparator = null; //"/";
	/**
	 * Указывает на необходимость немедленной остановки всех агентов
	 */
	private AtomicBoolean stopFlag = null;
	/**
	 * Указывает на необходимость немедленной остановки данного агента
	 */
	private volatile boolean personalStopFlag = false;
	/**
	 * При remoteVerification=true :
	 * For security purposes, all data connections to the client are verified to ensure that they originated from the intended party (host and port). 
	 * If a data connection is initiated by an unexpected party, the command will close the socket and throw an IOException. 
	 */
	@XmlAttribute
	protected Boolean ftpRemoteVerification = null;
	@XmlAttribute
	protected String ftpUser = null;
	@XmlAttribute
	protected String ftpPassword = null;
	@XmlAttribute
	protected String srcDir = null;
	@XmlAttribute
	protected String ftpSrcDir = null;
	@XmlAttribute
	protected String ftpDstDir = null;
	@XmlAttribute
	protected String ftpCacheDir = null;
	@XmlAttribute
	protected String dsc = null;

	private String pathSymbol = "@";

	@XmlAttribute
	protected Boolean ftpActiveMode = null;
	@XmlAttribute
	protected String ftpFileType = null;

	protected boolean pleaseWait = false;
	private String paramPrefix = null;
	private String paramSuffix = null;
    /**
     * маска файлов - регулярное выражение
     */
	@XmlAttribute
	protected String fileMask = null;
    /**
     * интервал между отправками, секунды
     */
	@XmlAttribute
    protected Integer delayBetween = null;
    /**
     * timeout для коннекта и выгрузки
     */
	@XmlAttribute
    protected Integer ftpTimeout = null;
	@XmlAttribute
    protected Boolean zip = null;
	@XmlAttribute
    protected Boolean gzip = null;
	@XmlAttribute
    protected Boolean reconnect = null;
	@XmlAttribute
    protected Boolean saveTime = null;
	@XmlAttribute
    protected String substitute = null;
	@XmlAttribute
    protected Boolean substTimeStamp = null;
	@XmlAttribute
    protected String suffix = null;
	@XmlAttribute
    protected String prefix = null;
	@XmlAttribute
    private String pauseFlagFile = null;
	@XmlAttribute
	protected String cacheDir = null;
	@XmlAttribute
	protected String dstDir = null;
	@XmlAttribute
	protected Boolean unpack = null;
	@XmlAttribute
    private String timeBasedSrcDir = null;
	@XmlAttribute
    private String srcTimeZone = null;
	@XmlAttribute
    private Boolean dstDirTimeBased = null;
	@XmlAttribute
    private String dstTimeZone = null;
	@XmlAttribute
    private Boolean dstDirTimeByFile = null;
	@XmlAttribute
    protected String statDir = null;
	@XmlAttribute
    protected Integer dstFileLimit = null;
	@XmlAttribute
    private Boolean inputFlag = null;
	@XmlAttribute
    private Boolean slowStop = null;
	@XmlAttribute
    private Boolean moveToNext = null;
	@XmlAttribute
    private Integer keepLastFiles = null;
	@XmlAttribute
    private Boolean replaceExistingFiles = null;
	@XmlAttribute
    protected Integer queueWarnOn = null;
	@XmlAttribute
    protected Integer queueWarnOff = null;
	@XmlAttribute
    protected String ftpServerTimeZone = null;
	@XmlAttribute
    private Boolean checkFtpFileLength = null;
	@XmlAttribute
    private Boolean batchMode = null;
	@XmlAttribute
    private Boolean deleteAfter = null;
	@XmlAttribute
    private Boolean timeByFile = null;
	@XmlAttribute
    private Integer sequenceLength = null;
	@XmlAttribute
    private String sequenceFormat = "%06d";
	@XmlAttribute
    private String startFileDate;
	@XmlAttribute
    private Long startFileDateLong = null;
	@XmlAttribute
    private Boolean smartRemoteRename = null;
	@XmlAttribute
    protected Boolean nonStrictOrder = null;

	@XmlAttribute
    protected String srcSubDirs = null;
   
    protected String[] srcSubDirList = null;
	
	//@XmlAttribute
    protected Integer lagInterval = null;
    
	//@XmlAttribute
    protected Integer queueWarnInterval = null;
	
	protected SimpleDateFormat substSDF = null;     
    protected SimpleDateFormat pref = null;     
    protected SimpleDateFormat suff = null;
    protected SubstituteWithTime substWithTime = null;
	private Boolean pauseFlagAttr = null;
    private boolean pauseFlag = false;
    
    private String prevTBSD = null;
    private SimpleDateFormat tbsdf = null;
    protected SimpleDateFormat tbddf = null; // time based dst dir format
    
    private String id = null;
    protected Cfg child = null;
    private ArrayList<Cfg> children = new ArrayList<>();
    protected boolean first = true;
    protected ActionType actionType;
    protected String shortName;
    protected String[] _patterns = null;
    protected String dstFileLimitAgent = null;
    
    protected long stFilesCount = 0;
    protected long stFilesSize = 0;
    protected long stFilesTime = 0;
    
    protected volatile int status = 0;
	/**
	 * Указывает на необходимость остановки данного агента с предварительной обработкой очереди
	 */
    protected volatile boolean cleanStopFlag = false;
    
    protected LocalParamsStorage localParams = new LocalParamsStorage(); 
    private ArrayList<Param> localParamList = new ArrayList<>();
    
    protected String applyMethods = null; //"SET"; 
    
    protected ApplyMethod[] _applyMethods = null; //ApplyMethod.SET;

    protected ArrayList<String[]> timeStamps = null;
    
    protected FileTimeStamps fts = null;
    
    protected StatInfo statInfo =  new StatInfo();
    /**
     * Максимально допустимое время необработки файлов (секунды). При превышении начинается информирование. 
     */
    
    private FileDataBlackList blackList = null; 
    protected  UploadBlackList upBlackList = null; 
    
    
    private int seqLength = 6;
    
    private String[] param = new String[5];
    
    protected Boolean moveToNextBlocked = null;
    
    protected Zipper zipper = null;
    
    public abstract boolean init();
    
    public abstract boolean isValid();

    public abstract boolean isLocalSource();
    
	public abstract void setDefaultValues();
	
	public abstract String makeId();

	public abstract List<FileData> getFilesList() throws IOException;
	
    @XmlElements({
        @XmlElement(name="copy", type = Copy.class ),
        @XmlElement(name="ftpDownload", type = FtpDownload.class ),
        @XmlElement(name="sftpDownload", type = SFtpDownload.class ),
        @XmlElement(name="ftpUpload", type = FtpUpload.class ),
        @XmlElement(name="sftpUpload", type = SFtpUpload.class ),
        @XmlElement(name="demux", type = Demux.class ),
        @XmlElement(name="link", type = Linker.class )
    })
	public ArrayList<Cfg> getChildren() {
		return children;
	}

	public void setChildren(ArrayList<Cfg> children) {
		this.children = children;
	}
	
	private void loadChildren() {
		for(Cfg c : children)
			addChild(c);
	}
	
    @XmlElements({
        @XmlElement(name="param", type = Param.class )
    })
	public ArrayList<Param> getlocalParamList() {
		return localParamList;
	}

	public void setlocalParamList(ArrayList<Param> list) {
		localParamList = list;
	}

	private void loadLocalParams() {
		for(Param p : localParamList)
			localParams.addParam(p);
	}
	
	public void loadChildrenAndParams() {
		loadChildren();
		loadLocalParams();
	}
	
	protected void addToBlackList(FileData f)
	{
		blackList.addToList(f);
	}	

	protected void removeFromBlackList(FileData f)
	{
		blackList.removeFromList(f);
	}
	
	protected void clearBlackList()
	{
		blackList.clearList();
	}	

	protected boolean inBlackList(FileData x)
	{
		return blackList.inList(x);
	}	

	protected FileData getFromBlackList(FileData x)
	{
		return blackList.getValue(x);
	}	
	
	protected void loadAllBlackLists()
	{
		if(blackList==null)
			blackList = new FileDataBlackList();
		if(upBlackList==null)
			upBlackList = new UploadBlackList();
		
		log.debug("blackList.loadFromJson(name)");
		blackList.loadFromJson(name);
		log.debug("upBlackList.loadFromJson(name)");
		upBlackList.loadFromJson(name);
		log.debug("loaded");
	}

	protected void loadStatInfo()
	{
		StatInfo st = StatInfo.loadFromJson(name);
		if(st!=null) {
			statInfo = st;
			statInfo.updateValues(lagInterval, queueWarnInterval, queueWarnOn, queueWarnOff);
		}	
		statInfo.setAgentName(name);
	}
	
	private long lastUpdateSI = 0;
	protected void updateStatInfo(int flag, String fileName, long size, long fileDate)
	{
		statInfo.next(flag, fileName, size, fileDate);
		long t = System.currentTimeMillis();
		if(flag==StatInfo.FILE_RECEIVED || t-lastUpdateSI>StatInfo.UPDATE_NO_FILE) {
			statInfo.saveToJson();
			lastUpdateSI = t; 
		}
	}
	
	public void addChild(Cfg agent)
	{
		if(child==null) child = agent;
		else child.addChild(agent);

		agent.first = false;
	}
	
	public String getCurrentTimeBasedSrcDir() 
	{
		if(tbsdf==null) return null;
		if(prevTBSD==null) {
			Calendar c = Calendar.getInstance(tbsdf.getTimeZone());
			c.add(Calendar.DAY_OF_MONTH, -1);
			prevTBSD = tbsdf.format(c.getTime());
			return prevTBSD;
		}
		String t = tbsdf.format(new Date());
		if(!prevTBSD.equals(t)) {
			String tt = prevTBSD;
			prevTBSD = t;
			return tt;
		}
		return t;
	}
	
	public boolean isSrcDirTimeBased() {
		return tbsdf!=null;
	}

	public boolean is_dstDirTimeBased() {
		return (dstDirTimeBased==null) ? false : dstDirTimeBased; 
	}
	
	public String getCurrentSrcDir(String parentDir, String separator)
	{
		if(!parentDir.endsWith(separator))
			parentDir = parentDir + separator;
		String tbd = getCurrentTimeBasedSrcDir();
		if(tbd!=null) 
			return parentDir + tbd + separator;
		return parentDir;
	}

    public void writeStatInfo(String fileName, long loadTime)
    {
    	if(statDir==null || fileName==null) return;
    	File x = new File(statDir,fileName+".ftp");
    	if(x.exists())
    	{
    		log.warn("stat file '{}' not created, already exists", x.getPath());
            return;
    	}
    	int ret = -1;
    	try ( PrintWriter pw = new PrintWriter(new BufferedOutputStream(new FileOutputStream(x,false))) ) {
            pw.println(loadTime);
            pw.flush();
            ret = 0;
    	} catch(IOException e) {
    		log.error("Can't create stat file "+x.getPath(),e);
    	}
    	if(ret==0 && log.isDebugEnabled())
    		log.debug("ok create stat file {}", x.getPath());
    }
    
    /**
     * Проверяет соответствие имени файла маскам
     * @param name имя файла
     * @return true если соответствует
     */
    public boolean isValidName(String name)
    {
		if(fileMask==null || fileMask.length()==0) 
			return true;
		if(log.isDebugEnabled()) log.debug("check by mask: "+fileMask);
		Matcher m = Pattern.compile(fileMask).matcher(name);
		if( m.find()) 
			return true; 
    	return false;
    }
    
	
	public String getFtpServer() {
		return ftpServer;
	}

	public static final String USLASH = "/"; 
	public static final String DSLASH = "\\"; 
	
	public static String detectSeparator(String path) 
	{
		if(path==null) return null;
		if(path.startsWith(USLASH)) return USLASH;
		if(path.startsWith(DSLASH)) return DSLASH;
		if(path.indexOf(USLASH)!= -1) return USLASH;
		if(path.indexOf(DSLASH)!= -1) return DSLASH;
		return null;
	}

	public static String detectSeparator(String path, String path2) 
	{
		String sep = detectSeparator(path);
		if(sep!=null) 
			return sep;
		sep = detectSeparator(path2);
		if(sep==null) 
			sep = USLASH;
		return sep;
	}
	
	public static String normalizeTail(String path, String sep) {
		if(path==null || path.length()==0) return path;
    	if(path.lastIndexOf(sep)!=path.length()-1)
    		path = path + sep;
		return path;
	}
	
	public static String normalizePath(String path) {
		if(path==null || path.length()==0) return path;
		return new File(path).getPath();
	}

	public static String updateCache(String cache, String pathSymb, String path) 
	{
    	if(cache==null || path==null) 
    		return cache;
		
    	if(cache.startsWith(pathSymb) && cache.length()>2) 
    		return path + cache.substring(2);
    	
		return cache;
	}
	/**
	 * Изменяет имя файла, если необходимо.
	 * Порядок действий:<br> 1.добавление префикса;<br>
	 * 2.изменение в соответствии с выражением substitute;<br>
	 * 3.добавление суффикса.
	 * @param origName - исходное имя файла
	 * @param dat - текущая дата
	 * @return новое значение имени файла
	 */
	protected String makeName(String origName, Date dat) 
	{
		StringBuilder sb = new StringBuilder();
		String tmp = origName;
		if(pref!=null)
			sb.append(pref.format(dat));
		if(substWithTime!=null) {
			tmp = substWithTime.substitute(tmp, dat);
		}	
		sb.append(tmp);
		if(suff!=null)
			sb.append(suff.format(dat));
		String seq = statInfo.getSequence(sequenceFormat);
		int off = seq.length()-seqLength;
		if(off>0)
			seq = seq.substring(off);
		return sb.toString().replaceAll("%seq%", seq);
	}

	/**
	 * Распаковывает файлы с расширениями <b>.gz</b> и <b>.zip</b>.<br>
	 * Zip архив должен содержать только единственный файл. 
	 * @param file архив
	 * @param dstDir целевой каталог, если не задан, используется каталог архива. 
	 * @param delArcFile удалять архив после успешной распаковки.
	 * @return распакованный файл, если распаковали; оригинальный файл, если не те расширения; null при ошибке распаковки.
	 */
    protected File unpackFile(File file, String dstDir, boolean delArcFile)
    {
		if(!unpack) // если не положено - назад
			return file;
    	File unpacked = Zipper.unpackSingleFile(file, dstDir); // пытаемся распаковать
    	if(unpacked==null) // unpack error
    		return null;
		// unpacked OK or not packed
		if(unpacked.equals(file)) // not packed
			return file;
		// unpacked OK
		if(log.isDebugEnabled())
			log.debug("ok unpack file: {} to {}", file, unpacked.getPath());
		if(delArcFile && !file.delete())
			log.error("CRITICAL error delete file {} after unpacking", file.getAbsolutePath());
		return unpacked;
    }
	
	private boolean checkPauseFlag() 
	{
		File f = new File(pauseFlagFile);
		return f.exists();
	}
	
	protected String curWorkDir = null;
	/**
	 * Действия, которые должны быть выполнены перед получением списка файлов из исходного каталога. 
	 * @throws IOException
	 */
	abstract public void beforeListing() throws IOException;
	/**
	 * Действия, которые должны быть выполнены после получения списка файлов из исходного каталога.
	 * @throws IOException
	 */
	abstract public void afterListing() throws IOException; 
	abstract public int applyDstFileLimit(int fileCount) throws IOException;
	/**
	 * Действия, которые должны быть выполнены перед началом цикла обработки обнаруженных файлов.
	 * @throws IOException
	 */
	abstract public void beforeLoop() throws IOException; 
	/**
	 * Предварительная работа по подготовке файла к обработке:
	 * переименование, упаковка/распаковка и установка времени ...
	 * @param d
	 * @return
	 * @throws IOException
	 */
	abstract public boolean workWithFile(FileData d) throws IOException;
	
	/**
	 * После успешной обработки и передачи исходный файл должен быть<br>
	 * удалён,<br>
	 * либо
	 * либо оставлен как есть - при deleteAfter="false"   
	 * @param f
	 * @param fromBlackList
	 * @return
	 * @throws IOException
	 */
	abstract public boolean afterAction(FileData f, boolean fromBlackList) throws IOException;
	abstract public void finallyAction(boolean flag);

	/**
	 * 
	 */
	private void trimBlackList() 
	{
		if(tbsdf!=null) // если используется TimeBasedSrcDir - ничего не делаем.
			return;
		if( blackList.getFound()==0) // если при последней проверке файлов из чёрного списка в исходном каталоге не обнаружено - сбрасываем чёрный список. 
			clearBlackList();
		else if(tbsdf==null) // иначе пытаемся ужать список, отбросив информацию о файлах, которых уже нет 
			blackList.trimToLastCheckResult();
	}
	
	public int work()
	{
		boolean ex = false;
		int querySize = 0;
		List<FileData> files = null;
		try
	    {
			statInfo.queueSize = -1;
			updateStatInfo(StatInfo.CHECK_LAG, null, 0, 0); // не слишком ли давно обработан последний файл ?
			beforeListing();
			blackList.clearFound();
			files = getFilesList();
			if( files.size()==0 ) { // файлов нет
				updateStatInfo(StatInfo.NO_QUEUE, null, 0, 0);
				log.debug("files not found");
				//trimBlackList();
				return 0; 
			}
			
			querySize = files.size();
			statInfo.queueSize = files.size();
			updateStatInfo(StatInfo.FILES_LISTED, null, 0, 0);
			Collections.sort(files, new FDComparator());
			if(log.isDebugEnabled()) 
				log.debug( "Number of files in srcDir: {}\nstart work >>>>>", querySize );
		
			afterListing();
			
			int fileCount = files.size() - get_KeepLastFiles();
			if( fileCount<=0 ) { // файлы есть, но мы не можем забирать последние keepLastFiles, так что файлов нет.
				updateStatInfo(StatInfo.NO_QUEUE, null, 0, 0);
				if(log.isDebugEnabled())
					log.debug("found "+files.size()+" files, but keepLastFiles="+get_KeepLastFiles());
				//trimBlackList();
			}
			
			if(fileCount>0 && dstFileLimit!=null && dstFileLimit > 0) { // если включена проверка кол-ва файлов в целевом каталоге
				fileCount = applyDstFileLimit(fileCount);	// то определяем, сколько можно загрузить файлов
				if(fileCount==0 && log.isDebugEnabled())  
					log.debug("found "+files.size()+" files, but after applyDstFileLimit fileCount=0");
			}
			beforeLoop();
			boolean lastResult = false;
			for( int i=0; i< fileCount; i++ ) // для всех файлов
			{
				lastResult = false;
				if( needForceStop() ) { 
					log.warn("break, filesInQueue={}", statInfo.queueSize); 
					break; 
				}
				FileData cfd = files.get(i);
				long from  = System.currentTimeMillis();
				if(workWithFile(cfd))  // обрабатываем файл, если успешно, то ...
				{
					if(getStartFileDateLong() != null) // если включён спец. режим забора последних файлов с запоминанием времени 
						lastResult = true;
					else // исходный файл надо, как правило переместить/удалить
						lastResult = afterAction(cfd,false);  
					// если не получилось - заносим в чёрный список, что-бы больше этот файл не обрабатывать,
					// а также если запрещено удаление и надо использовать чёрный список для исключения повторов;
					// а также если включён спец. режим забора последних файлов с запоминанием времени, причём в этом случае не надо пытаться проводить операции заново - см. (***) 
					if(!lastResult || !is_deleteAfter() || getStartFileDateLong()!=null ) 
						addToBlackList(cfd);
					updateStatInfo(StatInfo.FILE_RECEIVED, cfd.getName(), cfd.getSize(), cfd.getModTime());
					long to  = System.currentTimeMillis();
					writeStatInfo(cfd.getName(), (to-from)/1000);
					// если afterAction не был выполнен успешно и источник данных - не локальный, пересоединяемся.
					if(!lastResult && !this.isLocalSource())
						throw new LoopBreakException("afterAction - failed");
				} else {
					//если файл не был обработн при помощи workWithFile(), к следующему переходить нельзя - нарушаем порядок обработки
					//исключение - если явно разрешён нестрогий порядок обработки, т.е. nonStrictOrder = "true"
					if(is_nonStrictOrder()==false)
						throw new LoopBreakException("workWithFile - failed");
				}
			} // end for
			// если последняя операция была выполнена успешно, и не включён пакетный режим с запретом удаления, и не режим забора последних файлов с запоминанием времени
			if(lastResult && is_deleteAfter() && getStartFileDateLong()==null) { // (***)
				// проверяем чёрный список, и пытаемся провести заново операции, закончившиеся неудачно.
				int aMax = 200;
				ArrayList<FileData> array = new ArrayList<>(aMax);
				for(FileData b : blackList.getFoundOnLastCheck()) { // отбираем не более 200 за раз.
					array.add(b);
					if(array.size()>=aMax) break;
				}
				for(FileData b : array) {
					if(!afterAction(b, true)) // по первой же ошибке прекращаем попытки
						break;
					removeFromBlackList(b); // если успешно - удаляем из чёрного списка.
				}
				if(blackList.getSize()>0 && array.size()==0) { // чёрный список не пуст, но при последнем просмотре каталога файлов из списка не обнаружено - чистим список
					trimBlackList();
				}
			}
	    }catch( LoopBreakException e ) { // это просто сигнал о необходимости прерывания цикла и установке нового соединения.
	    	ex = true;
	    	log.warn("LoopBreak. Reason: "+e.getMessage());
	    }catch( IOException e ) {
	    	ex = true;
	    	if(statInfo.queueSize==-1) { // если queueSize=-1 , то была ошибка входа/соединения     
	    		log.error("Hmm: {}", e.getMessage());
	    	}
	    	else 
	    		log.error("Hmm: {} , filesInQueue={}", e.getMessage(), statInfo.queueSize);
	  //  	if(loginOk==false)
	   // 		pleaseWait = true;
	    }
	    finally {
	    	if(files!=null)
	    		files.clear();
	    	blackList.clearFound();
	    	finallyAction(ex);
	    }
		log.debug("stop work <<<<<");
		return querySize;
	}
	
	public void run() 
	{
		log.warn("*** Begin work ***");
		long olddt = 0;
		long lastCheckPauseFlag = 0;
		boolean prevPF = pauseFlag;
		if(prevPF) log.warn(PAUSED);
		long dBetween = (long)delayBetween*1000;
		long dBetweenX = dBetween;
		int querySize = -2; // начальное состояние очереди не 0, что-бы отличать первый цикл обработки.
		status = ST_STARTED;
		cleanStopFlag = false;
		personalStopFlag = false;
		int nodelay = 0;
		
		if(is_batchMode())
			log.warn("batchModeStart");
		try 
		{		
		loadAllBlackLists();
		loadStatInfo();
			
		while(true)
		{
			if(stopFlag.get()) { // выставлен глобальный флаг останова
				if(is_slowStop()) // при slowStop - надо попробовать разгрести очередь. Будет работать пока файлы не кончатся/тред не отстрелят.  
					setCleanStopFlag();
				else // иначе сразу завершаем работу
					break;
			}
			
			if( is_batchMode() && querySize==0 ) 
				break;
			if(cleanStopFlag ) {
				if( status == ST_CLEANING ) // если уже выставлен статус "очистка" 
				{
					nodelay--;
					if( querySize==0 ) // и нет файлов на входе - завершаем работу
						break;
				} else { // переходим из состояния "работа" в состояние "очистка" 
					nodelay = 2;
					status = ST_CLEANING;
					if(personalStopFlag) 
						break;
					querySize = -1;
				}
			}
			long dt = System.currentTimeMillis();
			if(pauseFlagFile!=null && dt-lastCheckPauseFlag > checkPauseFlagInterval) { // не пора ли проверить наличие файла, который обозначает необходимость паузы в работе  
				lastCheckPauseFlag = dt;	
				pauseFlag = checkPauseFlag();
			} 
			if(prevPF!=pauseFlag) { // если состояние pauseFlag изменилось
				if(pauseFlag) log.warn(PAUSED);
				else log.warn(RESUMED);
				prevPF = pauseFlag;
			}
			
			if(!pauseFlag && (nodelay>0 || dt-olddt > dBetweenX)) { // если нет паузы в обработке и ( после включения режима cleanStop проверили очередь < 3 раз или прошло время ожидания) то запускаем work()
				olddt = dt;
				if(dBetweenX!=dBetween) // восстанавливаем изменённое значение 
					dBetweenX = dBetween;
				try { 
					log.debug("checkWork");
					querySize = work();
					if(pleaseWait) {
						dBetweenX = dBetween*2;
						pleaseWait = false;
					}
				} catch(Throwable e) { log.error(CRIT3+" on run(): ",e); }
			} 
			try { Thread.sleep(50); } 
			catch(InterruptedException e) {
				Thread.currentThread().interrupt();
				break; 
			}
		}// end while
		if(child!=null && cleanStopFlag ) 
			child.setCleanStopFlag();
		
	} catch(Throwable e) {
		log.error(CRIT3+" on_run(): ",e); 
	}
		String clean = cleanStopFlag && status==ST_CLEANING && querySize==0 ? "(clean) " : "";
		status = ST_STOPPED;
		if(is_batchMode()) {
			if(child!=null) 
				child.setCleanStopFlag();			
			log.warn("batchModeStop");
		}	
		log.warn("*** Stop work {}***", clean);
		blackList.clearListFromMemory();
		upBlackList.clearListFromMemory();
		blackList = null; 
		upBlackList = null;
	}
	
	public static boolean isBothInput(Cfg a, Cfg b)
	{
		//boolean aa = a.isFirst() || a.isInputFlag();
		//boolean bb = b.isFirst() || b.isInputFlag();
		return a.is_InputFlag() && b.is_InputFlag();
	}
	
	public static void setInputFlagsToChildren(Cfg cfg)
	{
		if(!cfg.isFirst()) return;
		Cfg parent = cfg;
		while(parent.child!=null)
		{
			int atp = parent.getActionType().ordinal();
			int atc = parent.child.getActionType().ordinal();
			//if( (parent.isFirst() || parent.isInputFlag()) && !parent.isMoveToNext() && !parent.child.isMoveToNext() )
		//	boolean a1 = parent.is_InputFlag();
		//	boolean a2 = parent.is_MoveToNext();
		//	boolean a3 = parent.child.is_MoveToNext();
			if( parent.is_InputFlag() && !parent.is_MoveToNext() && !parent.child.is_MoveToNext() )
			{
				boolean a = Utl.in(atp, LocLoc, LocRem) && Utl.in(atc, LocLoc, LocRem) && parent.srcDir!=null && parent.srcDir.equals(parent.child.getSrcDir()); // если у агентов локальный источник, и исходные каталоги совпадают
				boolean b = Utl.in(atp, LocLoc, RemLoc) && Utl.in(atc, LocLoc, RemLoc) && parent.dstDir!=null && parent.dstDir.equals(parent.child.getDstDir()); // если у агентов локальная цель, и целевые каталоги совпадают
				if( a || b ) // пока совпадают - помечаем агент, как обрабатывающий данные на входе цепочки   
					parent.child.setInputFlag(true);
				else break; // перестали совпадаить - прекращаем поиск, это уже внутренние агенты
			} 
			parent = parent.child;
		}
	}
	
	public String makeIdString(String tail) 
	{
		StringBuilder sb = new StringBuilder();
		RefUtl ru = new RefUtl(this);
		for(String prop : FileChief.PROP_NAMES)
		{
			try {
				if(ru.hasGetter(prop))
					sb.append(ru.get(prop));
			} catch(Exception e) {
				log.error("makeIdString, property="+prop, e);
			} 
		}
		sb.append(tail);
		return sb.toString();
	}
	
	private List<String> getPropNames()
	{
		List<String> lst = new ArrayList<String>();
		for(String prop : FileChief.PROP_NAMES) {
			if("ftpPassword".equalsIgnoreCase(prop)) 
				continue;
			if("proxyPassword".equalsIgnoreCase(prop)) 
				continue;
			if("applyMethod".equalsIgnoreCase(prop) && getPattern()==null) 
				continue;
			lst.add(prop);
		}	
		return lst; 
	}
	
	private static final String formatString = "*+ %-21s: '%s'";
	private static final String parentFormat = "++++++++++++  %s  ++++++++++++";
	private static final String childFormat  = "------------  %s  ------------";
	private static final String chainBegin  = "++++++++++++  CHAIN BEGIN  ++++++++++++";
	private static final String chainEnd  = "-------------   CHAIN END   -------------";
	private StringBuilder workCfg = new StringBuilder(); 
	public void printCfg()
	{
		RefUtl ru = new RefUtl(this);
		if(isFirst())
			log.warn(chainBegin);
		String format = isFirst() ? parentFormat : childFormat;
		log.warn(String.format(format,this.getClass().getSimpleName()));
		for(String prop : getPropNames() ) {
			Object x = null;
			try {
				if(ru.hasGetter(prop))
					x = ru.get(prop);
			} catch(Exception e) {
				log.error("printCfg, property="+prop, e);
			}
			if(x==null) continue;
			workCfg.append("\t");
			workCfg.append(prop).append(" = \"").append(Utl.escapeXML2(x.toString())).append("\"\n");
			log.warn(String.format(formatString, prop,x));
		}
		log.warn("*-------------------------------");
		if(getChild()==null)
			log.warn(chainEnd);
	}

	static void getWorkConfig(StringBuilder sb, Cfg cfg)
	{
		Cfg c = cfg;
		String tail = null;
		do {
			boolean hasChild = c.isFirst() && (c.getChild()!=null);
			if(hasChild)
				sb.append("<!-- xxxxxxxx  ").append(c.getName()).append("  xxxxxxxx -->\n");
			sb.append("<").append(Utl.uncapFirstTwo(c.getClass().getSimpleName())).append("\n");
			sb.append(c.workCfg);
			if(!hasChild)
				sb.append("/>\n\n");
			else {
				sb.append(">\n");
				tail = c.getClass().getSimpleName();
			}
		} while((c=c.getChild())!=null);
		if(tail!=null) {
			sb.append("</").append(tail).append(">\n");
			sb.append("<!-- ================= -->\n\n");
		}
	}
	
    public boolean init2()
    {
    	if(sequenceLength!=null && sequenceLength>0) {
    		sequenceFormat = "%0" + sequenceLength + "d";
    		seqLength = sequenceLength;
    	}
    	setDefaultValues();
    	if(unpack || zip || gzip )
    		zipper = new Zipper();
    	statInfo.validate();
    	getId(); //????????????????????

    	TimeZone srcTZ = null;
		TimeZone dstTZ = null;
		if(srcTimeZone!=null) 
			srcTZ = TimeZone.getTimeZone(srcTimeZone);
		if(dstTimeZone!=null) 
			dstTZ = TimeZone.getTimeZone(dstTimeZone);
    	
    	if(substitute!=null) {
    		substWithTime = new SubstituteWithTime(substitute, is_substTimeStamp(), dstTZ);
    		if(!substWithTime.isValid())
    			substWithTime = null;
    	}
    	
    	if(prefix!=null) {
    		pref = new SimpleDateFormat(prefix);
    		if(dstTZ!=null)
    			pref.setTimeZone(dstTZ);
    	}	
    	if(suffix!=null) {
    		suff = new SimpleDateFormat(suffix);
    		if(dstTZ!=null)
    			pref.setTimeZone(dstTZ);
    	}	
    	
    	if(timeBasedSrcDir!=null && timeBasedSrcDir.length()>0) {
    		tbsdf = new SimpleDateFormat(timeBasedSrcDir);
    		if(srcTZ!=null)
    			tbsdf.setTimeZone(srcTZ);
    	}
    	
    	if(is_dstDirTimeBased())
    	{
    		String dir;
    		if(actionType.equals(ActionType.LOCAL2LOCAL)) {
    			dir = dstDir;
    			if(cacheDir.contains(pathSymbol))
    				throw new IllegalArgumentException("agent '"+getName()+"' : attribute 'dstDirTimeBased' incompatible with using pathSymbol ('"+pathSymbol+"') in chachDir");
    		}	
    		else if(actionType.equals(ActionType.LOCAL2REMOTE)) {
        			dir = ftpDstDir;
        			if(ftpCacheDir.contains(pathSymbol))
        				throw new IllegalArgumentException("agent '"+getName()+"' : attribute 'dstDirTimeBased' incompatible with using pathSymbol ('"+pathSymbol+"') in ftpChachDir");
    		}
    		else throw new IllegalArgumentException("agent '"+getName()+"' : attribute 'dstDirTimeBased' incompatible with (s)ftpDownload");
    		
    		tbddf = new SimpleDateFormat(dir);
    		if(dstTZ!=null)
    			tbddf.setTimeZone(dstTZ);
    	}
    	
   		fts = new FileTimeStamps(timeStamps, fileMask, srcTZ);	
    	
    	if(ftpSeparator==null && ( ftpDstDir!=null || ftpSrcDir!=null) ) 
   			ftpSeparator = detectSeparator(ftpDstDir, ftpSrcDir);

    	if(ftpSeparator!=null) {
    		ftpSrcDir = normalizeTail(ftpSrcDir, ftpSeparator);
    		if(!is_dstDirTimeBased()) {
    			ftpDstDir = normalizeTail(ftpDstDir, ftpSeparator);
    			ftpCacheDir = updateCache(ftpCacheDir, pathSymbol, ftpDstDir);
    		}	
    		ftpCacheDir = normalizeTail(ftpCacheDir, ftpSeparator);
    	}	

    	// к этому моменту srcDir и dstDir окончательно настроены, делаем подмену @ в cacheDir 
    	String dd = null;
    	if(dstDir!=null ) {
    		if(!DEVNULL.equals(dstDir))
    			dd = FilenameUtils.normalizeNoEndSeparator(dstDir);
    	} else 	if(srcDir!=null ) {
    			dd = FilenameUtils.normalizeNoEndSeparator(srcDir);
    	}	
    	if(dd!=null)
    		cacheDir = updateCache(cacheDir, pathSymbol, dd + File.separator);
    	else 
    		cacheDir = null;

        srcDir = normalizePath(srcDir);
        cacheDir = normalizePath(cacheDir);
        if(!DEVNULL.equals(dstDir))
        	dstDir = normalizePath(dstDir);
        
        if(isLocalSource() && getStartFileDateLong()!=null)
        	throw new IllegalArgumentException("agent '"+getName()+"' : attribute 'startFileDate' compatible only with (s)ftpDownload");
        // настройка агента завершена, выводим конфигурацию
    	printCfg();
        if(!Utl.makeDirs(srcDir)) return false;
        if(!Utl.makeDirs(cacheDir)) return false;
        if(!DEVNULL.equals(dstDir) && !is_dstDirTimeBased()) 
        	if(!Utl.makeDirs(dstDir)) return false;
        
        return true;
    }
	/**
	 * 
	 * @param file
	 * @param errorLevel 2 critical, 1 error, 0 warn
	 * @return
	 */
    public static boolean deleteFile(File file, int errorLevel)
    {
    	boolean del = false;
    	del = file.delete();
    	if(!del) {
    		if(errorLevel>0) {
    			String mesPref = (errorLevel==2) ? CRIT3 + " " : "";
    			log.error(mesPref + delErrMess, file , NOT_DELETED);
    		}	
    		else
    			log.warn(delErrMess, file, NOT_DELETED);
    	} else 
    		if(log.isDebugEnabled())
    			log.debug("deleted '{}'", file.getPath());
    		
    	return del;
    }

    public static boolean deleteFile(File file, boolean firstAttempt)
    {
    	int z = firstAttempt ? 1 : 0; 
    	return deleteFile(file, z);
    }
	
	public static boolean deleteFile(File file)
	{
		return deleteFile(file, 2);
	}

    static final int maxUniqIndex = 10000000;
    public static File makeUniqFileName(File f) 
    {
    	File ret = null;
    	String parent = f.getParent();
    	String name = f.getName();
    	for(int idx=1; idx<maxUniqIndex ;idx++) 
    	{
    		ret = new File(parent, makeIdxName(name, idx));
    		if(!ret.exists()) 
    			return ret;
    	}
    	log.error("CRITICAL!! maxUniqIndex overload for {}", f);
    	return null;
    }

    /**
     * Проверка существования целевого файла.
     * Если существует, и replaceIfExist=true, то будет удалён.
     * @param file проверяемый файл.
     * @param replaceIfExist можно ли замещать существующий файл.
     * @param opName имя операции (для журнала).
     * @param renamed если было переименование, получает значение true.
     * @return Если файл не существует, либо был удалён, то file.
     * Иначе новый файл с уникальным именем. 
     * При ошибке null.
     */
    public static File checkFileExists(File file, boolean replaceIfExist, String opName, AtomicBoolean renamed )
    {
    	if(renamed!=null)
    		renamed.set(false);
    	if( ! file.exists() )
    		return file;
    	
    	if( !replaceIfExist ) { // если запрещено замещать файлы, формируем новое имя файла
    		log.error("CRITICAL on try {} - file already exists : {}", opName, file);
    		File x = makeUniqFileName(file);
    		if(x==null) return null;
    		if(renamed!=null)
    			renamed.set(true);
    		return x;
    	}
    	else // т.к. FileUtils.moveFile не удаляет существующий файл
			if(!deleteFile(file)) // удаляем файл
				return null;
    	return file;
    }
    
    /**
     * 
     * @param f исходный файл
     * @param dir целевой каталог 
     * @param move copy/move
     * @param newName новое имя файла
     * @param replaceIfExist замещать ли сущестующий файл
     * @return null при ошибке, результирующий файл если операция выполнена
     * @throws IOException
     */
    public static File copyOrMoveOpt(File f, String dir, boolean move, String newName, boolean replaceIfExist) throws IOException
    {
    	String opName = move ? "move" : "copy";
    	newName = (newName==null) ? f.getName() : newName; 
    	File z = new File(dir, newName);
    	AtomicBoolean renamed = new AtomicBoolean();
    	
    	z = checkFileExists(z, replaceIfExist, opName, renamed );
    	if(z==null)
    		return null;
    	boolean r = copyOrMove(f, dir, move, z.getName());  // переносим / копируем в dir   		
    	if(r==false) return null;
    	if(renamed.get())
    		log.warn("file '{}' : change name to '{}'", f.getName(), z.getName() );
    	return z;
    }
	
	public static boolean copyOrMove(File f, String dstDir, boolean move)
	{
		return 	copyOrMove(f, dstDir, move, f.getName());
	}
	
	public static boolean copyOrMove(File f, String dstDir, boolean move, String newName)
    {
    	String opName = move ? "move" : "copy";
   		//File dst = new File(dstDir, f.getName());
    	File dst = new File(dstDir, newName);
   		try {
   			if(move) 
   				Utl.moveFile(f, dst);
   			else 
   				Utl.copyFile(f, dst);
			if(log.isDebugEnabled())
				log.debug("ok {} '{}' to '{}'" , new Object[] {opName, f.getPath(),dstDir});
   		} catch (IOException e) {
   			log.error("{} '{}' to '{}' : {}" , new Object[] {opName, f.getPath(), dstDir, e.getMessage()});
   			return false;
   		}
   		return true;
    }
    
	public static boolean copyFile(File f, String dstDir) {
    	return copyOrMove(f, dstDir, false);
    }

	public static boolean moveFile(File f, String dstDir) {
    	return copyOrMove(f, dstDir, true);
    }

	public static String makeIdxName(String name, int idx)
	{
		String[] x = RegEx.getGroupsS(name, "^(.*)(\\.(?i)(?:zip|gz))$");
		if(x.length==2)
			return x[0]+"."+idx+x[1];
		return name+"."+idx;
	}
	
	/**
	 * Копирует или переносит файл.<br> Если в целевом каталоге уже есть файл с таким именем<br> - добавляет суффикс <b>.n</b> где <b>n</b> - индекс.  
	 * @param f - исходный файл.
	 * @param dstDir - целевой каталог.
	 * @param dstName - целевое имя, если null, то берём имя исходного файла.
	 * @param move - <b>true</b> перенос <b>false</b> копирование.
	 * @param fd - сюда сохраняем полное имя получившегося файла.
	 * @return - <b>true</b> успешно <b>false</b> нет.
	 */
	public static boolean copyOrMoveUniq(File f, String dstDir, String dstName, boolean move, FileData fd) 
	{
		String tryName = (dstName==null) ? f.getName() : dstName; 
		File ok = new File(dstDir, tryName);
		String newName = tryName;
		int idx;
		for(idx = 1; ok.exists(); idx++) // если файл с таким именем уже есть - добавляем к имени <точка><номер> и снова проверяем.  
		{
			if(idx==1) 
				log.error("CRITICAL : file '{}' already exists in target directory '{}'" , tryName, dstDir);
			newName = makeIdxName(tryName, idx);
			ok = new File(dstDir, newName);
		}
		if(idx > 1)
			log.warn("file '{}' : change name to '{}'", tryName, newName);
		boolean ret = copyOrMove(f, dstDir, move, newName);
		if(fd!=null) {
			fd.setWorkFullName(ok.getPath());
			if(ret) fd.setSize(ok.length());
		}
    	return ret;
    }
	
	/**
	 * Копирует или переносит файл.<br> Если в целевом каталоге уже есть файл с таким именем<br> - добавляет суффикс <b>.n</b> где <b>n</b> - индекс.  
	 * @param f - исходный файл
	 * @param dstDir - целевой каталог
	 * @param move - <b>true</b> перенос <b>false</b> копирование
	 * @param fd - сюда сохраняем полное имя получившегося файла  
	 * @return - <b>true</b> успешно <b>false</b> нет
	 */
	public static boolean moveFileUniq(File f, String dstDir) 
	{
		return copyOrMoveUniq(f, dstDir, null, true, null);		
   }

	public static boolean moveFileUniqFD(File f, String dstDir, String dstName, FileData fd) 
	{
		return copyOrMoveUniq(f, dstDir, dstName, true, fd);
	}
	
	/**
	 * Проверяет количество файлов в целевом каталоге, сравнивает со значением атрибута <b>dstFileLimit</b>.
	 * Определяет, сколько исходных файлов может быть обработано сейчас.  
	 * @param fileCount количество исходных файлов
	 * @return сколько файлов можно обработать
	 */
	protected int getAllowedFilesNumber(int fileCount)
	{
		ActionType at = getActionType();
		if( !at.equals(ActionType.LOCAL2LOCAL) && !at.equals(ActionType.REMOTE2LOCAL) )
			return fileCount;
		if(dstFileLimit!=null && dstFileLimit > 0) 
		{ // проверяем количество файлов в целевом каталоге
			File dDir = new File( dstDir );
			File[] dFiles = dDir.listFiles(new FileOnlyFilter());
			if(dFiles==null)
				return 0;
			if(dFiles.length >= dstFileLimit) {
				log.info("dstFileLimit: {}, files in dstDir: {}, waiting", dstFileLimit, dFiles.length );
				fileCount = 0;
				pleaseWait = true;
			} else {
				fileCount = Math.min(fileCount, dstFileLimit - dFiles.length);
			}
		}
		return fileCount;
	}	

	protected int getFileCount(String destDir)
	{
		File dDir = new File( destDir );
		File[] dFiles = dDir.listFiles(new FileOnlyFilter());
		return dFiles==null ? 0 : dFiles.length;
	}	
	
	protected boolean checkFileDate(long date)
	{
		Long x1 = getStartFileDateLong();
		if(x1 != null) { //
			long t1 = x1;
			Long x2 = statInfo.getLastFileTime();
			if(x2!=null)
				t1 = Math.max(t1, x2.longValue());
			if(date < t1)
				return false;
		}	
		return true;
	}
	
	public void setFtpServer(String server) 
	{
		log.debug("setFtpServer {}", server);
		this.ftpServer = server;
	}

	public String getName() 
	{
		return name;
	}

	public void setName(String name) 
	{
		log.debug("setName {}", name);
		this.name = Utl.trimToNull(name);
	}

	public String getFtpSeparator() {
		return ftpSeparator;
	}

	public void setFtpSeparator(String separator) {
		this.ftpSeparator = separator;
	}

	public Boolean getFtpRemoteVerification() {
		return ftpRemoteVerification;
	}

	public void setFtpRemoteVerification(Boolean remoteVerification) {
		this.ftpRemoteVerification = remoteVerification;
	}

	public String getFtpUser() {
		return ftpUser;
	}

	public void setFtpUser(String username) {
		this.ftpUser = username;
	}

	public String getFtpPassword() {
		return ftpPassword;
	}

	public void setFtpPassword(String password) {
		this.ftpPassword = password;
	}

	public String getSrcDir() {
		return srcDir;
	}

	public void setSrcDir(String srcDir) {
		this.srcDir = srcDir;
	}

	public String getFtpDstDir() {
		return ftpDstDir;
	}

	public void setFtpDstDir(String dstDir) {
		this.ftpDstDir = dstDir;
	}

	public String getFtpCacheDir() {
		return ftpCacheDir;
	}

	public void setFtpCacheDir(String cacheDir) {
		this.ftpCacheDir = cacheDir;
	}

	public Boolean getFtpActiveMode() {
		return ftpActiveMode;
	}


	public void setFtpActiveMode(Boolean activeMode) {
		this.ftpActiveMode = activeMode;
	}

	public String getFileMask() {
		return fileMask;
	}


	public void setFileMask(String fileMask) {
		this.fileMask = fileMask;
	}

	public Integer getDelayBetween() {
		return delayBetween;
	}

	public void setDelayBetween(Integer delayBetween) {
		this.delayBetween = delayBetween;
	}

	public Integer getFtpTimeout() {
		return ftpTimeout;
	}


	public void setFtpTimeout(Integer dataTimeout) {
		this.ftpTimeout = dataTimeout;
	}


//	public boolean isDelOldBackup() {
//		return delOldBackup;
//	}


//	public void setDelOldBackup(boolean delOldBackup) {
//		this.delOldBackup = delOldBackup;
//	}


//	public int getDelOldBackupDays() {
//		return delOldBackupDays;
//	}


//	public void setDelOldBackupDays(int delOldBackupDays) {
//		this.delOldBackupDays = delOldBackupDays;
//	}


//	public OldBackupCleaner getCleaner() {
//		return cleaner;
//	}


//	public void setCleaner(OldBackupCleaner cleaner) {
//		this.cleaner = cleaner;
//	}

	public void setZip(Boolean zip) {
		this.zip = zip;
	}

//	public boolean isZip() {
//		return zip;
//	}

	public Boolean getZip() {
		return zip;
	}
	
//	public AtomicBoolean getStopFlag() {
//		return stopFlag;
//	}

	public void setStopFlag(AtomicBoolean stopFlag) {
		this.stopFlag = stopFlag;
	}

	public Integer getFtpServerPort() {
		return ftpServerPort;
	}

	public void setFtpServerPort(Integer ftpServerPort) {
		this.ftpServerPort = ftpServerPort;
	}
	
	public String getPathSymbol() {
		return pathSymbol;
	}

	public void setPathSymbol(String pathSymbol) {
		this.pathSymbol = pathSymbol;
	}

	public String getFtpSrcDir() {
		return ftpSrcDir;
	}

	public void setFtpSrcDir(String ftpSrcDir) {
		this.ftpSrcDir = ftpSrcDir;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

//	public String getTimeZone() {
//		return timeZone;
//	}

//	public void setTimeZone(String timeZone) {
//		this.timeZone = timeZone;
//	}

	public String getDstDir() {
		return dstDir;
	}

	public void setDstDir(String dstDir) {
		this.dstDir = dstDir;
	}

	public Boolean getUnpack() {
		return unpack;
	}

	public void setUnpack(Boolean unpack) {
		this.unpack = unpack;
	}

	public String getCacheDir() {
		return cacheDir;
	}
	
	public void setCacheDir(String cDir) {
		cacheDir = cDir;
	}

	public String getTimeBasedSrcDir() {
		return timeBasedSrcDir;
	}

	public void setTimeBasedSrcDir(String timeBasedDir) {
		this.timeBasedSrcDir = timeBasedDir;
	}

	public void setPauseFlagFile(String pauseFlagFile) {
		this.pauseFlagFile = pauseFlagFile;
	}

	public String getPauseFlagFile() {
		return pauseFlagFile;
	}

	public String getId() {
		if(id==null) id = makeId();
		return id;
	}

	public void setChild(Cfg child) {
		this.child = child;
	}

	public Cfg getChild() {
		return child;
	}

	public boolean isFirst() {
		return first;
	}

	public void setFirst(boolean first) {
		this.first = first;
	}

	public ActionType getActionType() {
		return actionType;
	}

	public String getDsc() {
		return dsc;
	}

	public void setDsc(String dsc) {
		this.dsc = dsc;
	}

	public String getPattern() 
	{
		if(_patterns==null || _patterns.length==0)
			return null;
			
		StringBuilder sb = new StringBuilder();
		for(String x : _patterns)
		{
			if(sb.length()>0) sb.append(PAT_DELIM);
			sb.append(x);
		}
		return sb.toString();
	}

	@XmlAttribute
	public void setPattern(String pattern) 
	{
		log.debug("setPattern: '{}'",pattern);
		
		pattern = Utl.trimToNull(pattern);
		if(pattern==null)
			return;
		ArrayList<String> p = new ArrayList<>();
		for(String x : pattern.split(PAT_DELIM_EXP) )
		{
			x = Utl.trimToNull(x);
			if(x==null)
				throw new IllegalArgumentException("agent '" + getName() + "' - setPattern() - invalid pattern string : '" + pattern + "'");
			p.add(x);
		}
		if(p.size()>0)
			_patterns = p.toArray(new String[0]);
		//if(name!=null)
		//	PatternStore.getInstance().applyPattern(this);
	}

	public String getFtpFileType() {
		return ftpFileType;
	}

	public void setFtpFileType(String fType) {
		ftpFileType = fType.toLowerCase();
	}

	public void setParamPrefix(String paramPrefix) {
		this.paramPrefix = paramPrefix;
	}

	public String getParamPrefix() {
		return paramPrefix;
	}

	public void setParamSuffix(String paramSuffix) {
		this.paramSuffix = paramSuffix;
	}

	public String getParamSuffix() {
		return paramSuffix;
	}

	public Boolean getReconnect() {
		return reconnect;
	}

	public void setReconnect(Boolean reconnect) {
		this.reconnect = reconnect;
	}

	public Boolean getSaveTime() {
		return saveTime;
	}

	public void setSaveTime(Boolean saveTime) {
		this.saveTime = saveTime;
	}

	public String getStatDir() {
		return statDir;
	}

	public void setStatDir(String statDir) {
		this.statDir = statDir;
	}

	public Boolean getGzip() {
		return gzip;
	}

	public void setGzip(Boolean gzip) {
		this.gzip = gzip;
	}

	public String getSrcTimeZone() {
		return srcTimeZone;
	}

	public void setSrcTimeZone(String srcTimeZone) {
		this.srcTimeZone = srcTimeZone;
	}

	public String getSubstitute() {
		return substitute;
	}

	public void setSubstitute(String subst) {
		this.substitute = subst;
	}

	public Boolean getSubstTimeStamp() {
		return substTimeStamp;
	}

	public boolean is_substTimeStamp() {
		return (substTimeStamp==null)? false : substTimeStamp;
	}
	
	public void setSubstTimeStamp(Boolean substTimeStamp) {
		this.substTimeStamp = substTimeStamp;
	}

	public Integer getDstFileLimit() {
		return dstFileLimit;
	}

	public void setDstFileLimit(Integer dstFileLimit) {
		if(dstFileLimit==null || dstFileLimit < 0) 
			this.dstFileLimit = null;
		else 
			this.dstFileLimit = dstFileLimit;
	}
	

	public String getDstFileLimitAgent() {
		return dstFileLimitAgent;
	}

	public void setDstFileLimitAgent(String dstFileLimitAgent) {
		this.dstFileLimitAgent = dstFileLimitAgent;
	}

    public void addParam(Param p)
    {
    	localParams.addParam(p);
    }

	
	public LocalParamsStorage getLocalParams() 
	{
		if(!localParams.isReady()) {
			for(String p : param) {
				if(p!=null) 
			 		localParams.addParam(Param.parseString(p));
			 }	
			localParams.setReady();
		}
		return localParams;
	}

	public String getApplyMethod() 
	{
		if(_applyMethods==null)
			return null;
		StringBuilder sb = new StringBuilder();
		for(ApplyMethod a : _applyMethods) {
			if(sb.length()>0) sb.append(PAT_DELIM);
			sb.append(a.name());
		}	
		return sb.toString();
	}

	@XmlAttribute
	public void setApplyMethod(String applyMethod) {
		applyMethod = Utl.trimToNull(applyMethod);
		if(applyMethod==null) return;
		String[] xx = applyMethod.toUpperCase().split(PAT_DELIM_EXP);
		ArrayList<ApplyMethod> lst = new ArrayList<ApplyMethod>();
		int idx = -1;
		for(String x : xx)
		{
			String[] zz = x.split("\\s*:\\s*");
			if( zz.length>1 )
			{
				boolean error = false;
				try {
					idx = Integer.parseInt(zz[1]);
					x = zz[0];
				} catch(NumberFormatException e) {
					error = true;
				}
				if( idx<0 || error)
					throw new IllegalArgumentException("agent '"+getName()+"' - setApplyMethod() - invalid index : '{" + idx + "}'");
			}
			ApplyMethod m;
			try {
				m = ApplyMethod.valueOf(x);
				if( idx>-1 )
					m.index = idx;
				lst.add(m);
			} catch(Throwable t) {
				throw new IllegalArgumentException("agent '"+getName()+"' - setApplyMethod() - invalid method name : '{" + x + "}'");
			}
		}
		_applyMethods = lst.toArray(new ApplyMethod[]{});
		applyMethods = getApplyMethod();
	}
	
	class FileOnlyFilter implements FileFilter 
	{
		public boolean accept(File pathname) 
		{
			return pathname.isFile();
		}
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public boolean isCleanStopFlag() {
		return cleanStopFlag;
	}

	public void setCleanStopFlag() {
		this.cleanStopFlag = true;
		if( is_InputFlag() && !is_slowStop() )
			personalStopFlag = true; 
	}
	
	public boolean needForceStop()
	{
		return (personalStopFlag || stopFlag.get()) && !is_slowStop();
	}
	
	public Boolean getMoveToNext() {
		return moveToNext;
	}

	/**
	 * 
	 * @return <b>false</b> if moveToNext==False;<br><b>true</b> if (moveToNext==null or moveToNext==True )    
	 */
	public boolean is_MoveToNextDef() { //  
		if( moveToNext!=null && moveToNext.booleanValue()==false )
			return false;
		return true;
	}

	public boolean is_MoveToNextBlockedDef() 
	{   
		if( moveToNextBlocked!=null && moveToNextBlocked.booleanValue()==true )
			return true;
		return false;
	}
	
	public boolean is_MoveToNext() 
	{
		return is_MoveToNextDef() && !is_MoveToNextBlockedDef();
	}
	
	public void setMoveToNext(Boolean moveToNext) {
		this.moveToNext = moveToNext;
	}

	public Boolean getInputFlag() {
		return inputFlag;
	}

	/**
	 * @return <b>true</b> для агента - источника данных для цепочки<br>
	 * <b>false</b> для агента - потребителя.
	 */
	public boolean is_InputFlag() { //
		if( isFirst() ) return true;
		return (inputFlag==null) ? false : inputFlag; 
	}
	
	public void setInputFlag(Boolean inputFlag) {
		this.inputFlag = inputFlag;
	}

	public Integer getKeepLastFiles() {
		return keepLastFiles;
	}

	public int get_KeepLastFiles() {
		if(keepLastFiles==null || keepLastFiles<0)
			return 0;
		return keepLastFiles;
	}
	
	public void setKeepLastFiles(Integer keepLastFiles) {
		this.keepLastFiles = keepLastFiles;
	}

	@XmlAttribute
	public String getTimeStampInName() {
		if(timeStamps==null) 
			return null;
		return Utl.listToString2(timeStamps);
	}

	public void setTimeStampInName(String timeStampInName) {
		timeStamps = Utl.stringToList2(timeStampInName);
		if(timeStamps.size()==0)
			timeStamps = null;
	}

	public Boolean getReplaceExistingFiles() {
		return replaceExistingFiles;
	}

	public boolean get_ReplaceExistingFiles() 
	{
		return (replaceExistingFiles==null)?false:replaceExistingFiles;
	}
	
	public void setReplaceExistingFiles(Boolean replaceExistingFiles) {
		this.replaceExistingFiles = replaceExistingFiles;
	}

//---------------------------------------------------------------------	
	
	@XmlAttribute
	public String getLagInterval() {
		return (lagInterval==null) ? null : ""+lagInterval;
	}

	public Integer getLagIntervalI() {
		return lagInterval;
	}
	
	public void setLagIntervalI(int interval) {
		lagInterval = interval;
		statInfo.lagInterval = lagInterval*1000L;
	}
	
	public void setLagInterval(String interval) {
		setLagIntervalI(Utl.str2interval(interval));
	}
	
//---------------------------------------------------------------------
	@XmlAttribute
	public String getQueueWarnInterval() 
	{
		return (queueWarnInterval==null) ? null : ""+queueWarnInterval;
	}
	
	public Integer getQueueWarnIntervalI() {
		return queueWarnInterval;
	}

	public void setQueueWarnIntervalI(Integer qwInterval) 
	{
		if(qwInterval==null)
			return;
		if(qwInterval>0 && qwInterval<StatInfo.MIN_QUEUE_WARN_ISEC)
			qwInterval = StatInfo.MIN_QUEUE_WARN_ISEC;
		queueWarnInterval = qwInterval;
		statInfo.queueWarnInterval = queueWarnInterval*1000L; 
	}

	public void setQueueWarnInterval(String interval) {
		setQueueWarnIntervalI(Utl.str2interval(interval));
	}
	
//---------------------------------------------------------------------	
	public Integer getQueueWarnOn() {
		return queueWarnOn;
	}
		
	public void setQueueWarnOn(Integer qWarnOn) 
	{
		if(qWarnOn==null || qWarnOn<0) 
		{
			queueWarnOn = null;
			statInfo.queueWarnOn = 0;
		} else {
			queueWarnOn = qWarnOn;
			statInfo.queueWarnOn = qWarnOn; 
		}
	}

//---------------------------------------------------------------------
	
	public Integer getQueueWarnOff() {
		return queueWarnOff;
	}

	public void setQueueWarnOff(Integer qWarnOff) 
	{
		if(qWarnOff==null || qWarnOff<0) 
		{
			queueWarnOff = null;
			statInfo.queueWarnOff = 0;
		} else {
			queueWarnOff = qWarnOff;
			statInfo.queueWarnOff = qWarnOff;
		}
	}

	public String getSrcSubDirs() {
		return srcSubDirs;
	}

	public void setSrcSubDirs(String srcSubDirs) 
	{
		this.srcSubDirs = srcSubDirs;
		if(srcSubDirs!=null && srcSubDirs.trim().length()>0)
		{
			String[] x = srcSubDirs.split(";");
			if(x!=null && x.length>0) {
				for(int i = 0; i<x.length; i++)
					x[i] = x[i].trim(); 
				srcSubDirList = x;
				return;
			} 
		}
		srcSubDirList = null;
	}

	public Boolean getCheckFtpFileLength() {
		return checkFtpFileLength;
	}

	public boolean is_checkFtpFileLength() {
		if(checkFtpFileLength==null)
			return true;
		return checkFtpFileLength;
	}
	
	public void setCheckFtpFileLength(Boolean checkFtpFileLength) {
		this.checkFtpFileLength = checkFtpFileLength;
	}

	public Boolean getDstDirTimeBased() {
		return dstDirTimeBased;
	}

	public void setDstDirTimeBased(Boolean dstDirTimeBased) {
		this.dstDirTimeBased = dstDirTimeBased;
	}

	public String getDstTimeZone() {
		return dstTimeZone;
	}

	public void setDstTimeZone(String dstTimeZone) {
		this.dstTimeZone = dstTimeZone;
	}

	public Boolean getDstDirTimeByFile() {
		return dstDirTimeByFile;
	}

	public boolean is_dstDirTimeByFile() {
		return (dstDirTimeByFile==null) ? true : dstDirTimeByFile ;
	}
	
	public void setDstDirTimeByFile(Boolean dstDirTimeByFile) {
		this.dstDirTimeByFile = dstDirTimeByFile;
	}

	public SimpleDateFormat getTbddf() {
		return tbddf;
	}

	public String getFtpServerTimeZone() {
		return ftpServerTimeZone;
	}

	public void setFtpServerTimeZone(String ftpServerTimeZone) {
		this.ftpServerTimeZone = ftpServerTimeZone;
	}

	public Boolean getBatchMode() {
		return batchMode;
	}

	public void setBatchMode(Boolean batchMode) {
		this.batchMode = batchMode;
	}

	public boolean is_batchMode() {
		if(batchMode==null) return false;
		return batchMode;
	}

	public Boolean getDeleteAfter() {
		return deleteAfter;
	}

	public void setDeleteAfter(Boolean deleteAfter) {
		this.deleteAfter = deleteAfter;
	}

	/**
	 * @return true если атрибут deleteAfter не задан;
	 * иначе вернёт значение атрибута deleteAfter.
	 */
	public boolean is_deleteAfter() 
	{
		if(deleteAfter==null) 
			return true;
		return deleteAfter;
	}

	/*
	public boolean is_batchModeWithBL() 
	{
		if(is_deleteAfter()) // если разрешено удаление - значит не "пакетный режим с чёрным списком".
			return false;
		if(batchModeWithBL==null) // если запрещено удаление и batchModeWithBL не задан, то по умолчанию работает "пакетный режим с чёрным списком". 
			return true;
		return batchModeWithBL; // иначе возвращаем batchModeWithBL.
	}
	 */

	public Boolean getTimeByFile() {
		return timeByFile;
	}

	public void setTimeByFile(Boolean timeByFile) {
		this.timeByFile = timeByFile;
	}

	public boolean is_timeByFile()	{
		if(timeByFile==null) return false;
		return timeByFile;
	}
	
	public Integer getSequenceLength() {
		return sequenceLength;
	}

	public void setSequenceLength(Integer sequenceLength) {
		this.sequenceLength = sequenceLength;
	}

	public Boolean getSmartRemoteRename() {
		return smartRemoteRename;
	}

	public void setSmartRemoteRename(Boolean smartRemoteRename) {
		this.smartRemoteRename = smartRemoteRename;
	}

	public boolean is_smartRemoteRename() 
	{
		if(smartRemoteRename==null)
			return true; // smartRemoteRename включен по умолчанию
		return smartRemoteRename;
	}

	public String[] getParamArray() {
		return param;
	}

	@XmlAttribute
	public String getParam() {
		return param[0];
	}

	public void setParam(String param) {
		this.param[0] = param;
	}

	@XmlAttribute
	public String getParam1() {
		return param[1];
	}

	public void setParam1(String param1) {
		this.param[1] = param1;
	}

	@XmlAttribute
	public String getParam2() {
		return param[2];
	}

	public void setParam2(String param2) {
		this.param[2] = param2;
	}

	@XmlAttribute
	public String getParam3() {
		return param[3];
	}

	public void setParam3(String param3) {
		this.param[3] = param3;
	}
	
	@XmlAttribute
	public String getParam4() {
		return param[4];
	}

	public void setParam4(String param4) {
		this.param[4] = param4;
	}

	/**
	* returns value of <b>pauseFlagAttr</b>
	*/
	public Boolean getPauseFlag() {
		return pauseFlagAttr;
	}

	/**
	* Set values of <b>pauseFlagAttr</b> and <b>pauseFlag</b> 
	*/
	@XmlAttribute
	public void setPauseFlag(Boolean pauseFlagAttr) {
		this.pauseFlagAttr = pauseFlagAttr;
		if(pauseFlagAttr!=null)
			pauseFlag = pauseFlagAttr.booleanValue(); 
	}

	public String getStartFileDate() {
		return startFileDate;
	}

	public void setStartFileDate(String startFileDate) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat(START_DATE_FORMAT);
		startFileDateLong = sdf.parse(startFileDate).getTime();
		this.startFileDate = startFileDate;
	}

	public Long getStartFileDateLong() {
		return startFileDateLong;
	}

	public Boolean getMoveToNextBlocked() {
		return moveToNextBlocked;
	}

	public boolean is_moveToNextBlocked() {
		return moveToNextBlocked==null ? false : moveToNextBlocked;
	}

	public Boolean getNonStrictOrder() {
		return nonStrictOrder;
	}

	public void setNonStrictOrder(Boolean nonStrictOrder) {
		this.nonStrictOrder = nonStrictOrder;
	}
	
	public boolean is_nonStrictOrder() {
		return nonStrictOrder==null ? false : nonStrictOrder;
	}

	public Boolean getSlowStop() {
		return slowStop;
	}

	public void setSlowStop(Boolean slowStop) {
		this.slowStop = slowStop;
	}
	
	public boolean is_slowStop() {
		return (slowStop==null) ? false : slowStop;
	}
	
}	
