package psn.filechief;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import psn.filechief.log4j.EnvToMDC;
import psn.filechief.util.GlobalParamsStorage;
import psn.filechief.util.LocalParamsStorage;
import psn.filechief.util.PBECrypt;
import psn.filechief.util.Param;
import psn.filechief.util.ParamsStorage;
import psn.filechief.util.PortListener;
import psn.filechief.util.RefUtl;
import psn.filechief.util.ThreadList;
import psn.filechief.util.Utl;
import psn.filechief.util.bl.FileDataBlackList;
import psn.filechief.util.bl.UploadBlackList;
import psn.filechief.util.stat.StatInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="fileChief")
public class FileChief 
{
	private static final Logger log = LoggerFactory.getLogger(FileChief.class.getName());
    /**
     * разделитель в filePath 
     */
    private static final String FSEP = System.getProperty("file.separator");
    public static final String DEF_DATA_DIR = "data";
    public static final String AUTO_DST_PREFIX = "dst";
    public static final String AUTO_SRC_PREFIX = "src";
    public static final String AUTO_CACHE_PREFIX = "tmp";
    private static final String WRK_CFG = "workConfig";

    private AtomicBoolean stopFlag = new AtomicBoolean(false);

    /**
     * агенты-родители, находящиеся во главе цепочек
     */
	private ArrayList<Cfg> mainAgents = new ArrayList<>();
    /**
     * все агенты	
     */
    private HashMap<String,Cfg> allAgents = new HashMap<>();

    private String workDir = null;
    
    @XmlAttribute
    private String pathSymbol = null;
    
    private int lockPort = -1;
    /**
     * порт, на котором слушаем команды 
     */
    @XmlAttribute
    private String port = null;

    @XmlAttribute
    private String password = "^HPiky27z@5tI%;";
    
    private volatile boolean cleanStop = false; 
    
    private GlobalParamsStorage globalParams = new GlobalParamsStorage();

    private ArrayList<Param> globalParamList = new ArrayList<>(); 
    
    private LocalParamsStorage localParams = new LocalParamsStorage();
    
    private ArrayList<Cfg> patternList = new ArrayList<>();
    
    @XmlAttribute
    private static String tdnSeparator = "_";
    
    //@XmlAttribute
    private Integer defaultLagInterval = null;
    
    //@XmlAttribute
    private Integer defaultQueueWarnInterval = null;
    @XmlAttribute
    private Integer defaultQueueWarnOn = null;
    @XmlAttribute
    private Integer defaultQueueWarnOff = null;

    @XmlAttribute
    private String dataDir = null;
    @XmlAttribute
    private Boolean defaultSmartRemoteRename = null;
    @XmlAttribute
    private Boolean defaultSaveTime = null;
    
    public FileChief() {
    }

    @XmlElements({
        @XmlElement(name="copy", type = Copy.class ),
        @XmlElement(name="ftpDownload", type = FtpDownload.class ),
        @XmlElement(name="sftpDownload", type = SFtpDownload.class ),
        @XmlElement(name="ftpUpload", type = FtpUpload.class ),
        @XmlElement(name="sftpUpload", type = SFtpUpload.class ),
        @XmlElement(name="demux", type = Demux.class ),
        @XmlElement(name="link", type = Linker.class )
    })
    public ArrayList<Cfg> getAgents() {
		return mainAgents;
	}

	public void setAgents(ArrayList<Cfg> mainAgents) {
		this.mainAgents = mainAgents;
	}
	
    @XmlElements({
        @XmlElement(name="param", type = Param.class )
    })
	public ArrayList<Param> getGlobalParamList() {
		return globalParamList;
	}

	public void setGlobalParamList(ArrayList<Param> globalParamList) {
		this.globalParamList = globalParamList;
	}
	
	public void loadGlobalParams() {
		for(Param p : globalParamList)
			addParam(p);
	}

	@XmlElementWrapper(name="patterns")
    @XmlElements({
        @XmlElement(name="copy", type = Copy.class ),
        @XmlElement(name="ftpDownload", type = FtpDownload.class ),
        @XmlElement(name="sftpDownload", type = SFtpDownload.class ),
        @XmlElement(name="ftpUpload", type = FtpUpload.class ),
        @XmlElement(name="sftpUpload", type = SFtpUpload.class ),
        @XmlElement(name="demux", type = Demux.class ),
        @XmlElement(name="link", type = Linker.class )
    })
	public ArrayList<Cfg> getPatterns() {
		return patternList;
	}
	
	public void setPatterns(ArrayList<Cfg> patternList) {
		this.patternList = patternList;
	}
	
	public void loadPatterns() {
		Patterns p = new Patterns();
		for(Cfg c : patternList) {
			c.loadChildrenAndParams();
			p.addChild(c);
		}
	}
	
	
    private void initCfgDirsR2L(Cfg cfg) 
    {
		if(cfg.cacheDir==null && cfg.dstDir!=null) 
			cfg.setCacheDir(makeCacheDirName(cfg.dstDir, cfg.name));
		
		if( cfg.child!=null && cfg.dstDir!=null && cfg.child.srcDir==null ) // если не заполнен srcDir у потомка
			if( !Cfg.isBothInput(cfg, cfg.child) ) // и не взведены inputFlag у обоих
				cfg.child.setSrcDir(cfg.dstDir); // то автоматически заполняем SrcDir для child 
    }

    private void initCfgDirsL2L(Cfg cfg) 
    {
    	initCfgDirsL2R(cfg);
    	initCfgDirsR2L(cfg);
		if(cfg.cacheDir==null && cfg.dstDir!=null) 
			cfg.setCacheDir(makeCacheDirName(cfg.dstDir, cfg.name));
    }
     
    private void initCfgDirsL2R(Cfg cfg) 
    {
		if(cfg.child!=null && cfg.child.srcDir==null && cfg.is_MoveToNextDef() ) // следом есть агент, получающий те же данные ?
			cfg.child.setSrcDir(makeSrcDirName(cfg.srcDir, cfg.child.name));
			//cfg.child.setSrcDir(cfg.child.dstDir+"/"+autoSrcPrefix+cfg.child.name);
		
		if(cfg.ftpCacheDir==null && cfg.ftpDstDir!=null) 
			cfg.setFtpCacheDir(makeCacheDirName(cfg.ftpDstDir, cfg.name));
		
    }
    
    private void initCfgDirs(Cfg cfg) 
    {
    	if(cfg.is_dstDirTimeBased())
    	{
    		if(cfg.actionType.equals(ActionType.LOCAL2LOCAL) &&  Utl.trimToNull(cfg.cacheDir)==null) 
    				throw new IllegalArgumentException("agent '"+cfg.getName()+"' : cacheDir must be defined when dstDirTimeBased='true'");
    		else if(cfg.actionType.equals(ActionType.LOCAL2REMOTE) && Utl.trimToNull(cfg.ftpCacheDir)==null) {
       				throw new IllegalArgumentException("agent '"+cfg.getName()+"' : ftpCacheDir must be defined when dstDirTimeBased='true'");
    		}
    	}
    	
   		switch(cfg.actionType) {
    		case REMOTE2LOCAL : initCfgDirsR2L(cfg); break;
    		case LOCAL2REMOTE :	initCfgDirsL2R(cfg); break;
    		case LOCAL2LOCAL  :	initCfgDirsL2L(cfg); break;
    	}	
    	
   		boolean cacheEmpty = ( cfg.cacheDir==null || cfg.cacheDir.length()==0);
   		boolean l2lWithChild = cfg.actionType.equals(ActionType.LOCAL2LOCAL) && cfg.child!=null && cfg.is_MoveToNext();
   		boolean localWithNoDeleteAfter = cfg.isLocalSource() && cfg.child!=null && cfg.is_MoveToNext() && !cfg.is_deleteAfter();
   		boolean needCache = cfg.zip || cfg.gzip || cfg.unpack || l2lWithChild || localWithNoDeleteAfter;
   		
    	if( cacheEmpty && needCache ) {  // cfg.saveTime //&& cfg.dstDir!=null
    		String cd = null;
    		if(cfg.dstDir!=null) cd = makeCacheDirName(cfg.dstDir,cfg.name);
    			else if(cfg.srcDir!=null) cd = makeCacheDirName(cfg.srcDir,cfg.name);
    		cfg.setCacheDir(cd);
    	}
    }

    private void initChildSrcDirsForLink(Cfg cfg)
    {
    	Cfg y, prev;
    	Cfg x = cfg;
		while((y=x.getChild())!=null )
		{
			prev = x;
			x = y;
			if(x.is_MoveToNextDef() && !x.is_MoveToNextBlockedDef() ) // выставляем moveToNextBlocked 
		//	if(!x.is_MoveToNextBlockedDef()) // выставляем moveToNextBlocked ? && x.child!=null 
				x.moveToNextBlocked = true;
			if(x.getSrcDir()==null) // и srcDir
				x.setSrcDir(makeSrcDirName(prev.srcDir, x.name));
			if(!x.is_MoveToNextDef()) // если выбранный агент не должен передавать файл следующему в цепочке
				break;	// то заканчиваем работу
		}
    }

    private void initChildSrcDirsForLinkRS(RemoteSource cfg)
    {
    	if(!cfg.is_CreateLinks())
    		return;
    	Cfg x = cfg;
    	boolean found = false;
    	while((x=x.getChild())!=null) {
    		if( x instanceof LocalSource && cfg.dstDir.equals(x.srcDir)) {
    			found = true;
    			break;
    		}
    	}
    	if(!found) 
    		return;
		do 
		{
			if(x.is_MoveToNextDef() && !x.is_MoveToNextBlockedDef() ) // выставляем moveToNextBlocked 
				x.moveToNextBlocked = true;
			if(!x.is_MoveToNextDef()) // если выбранный агент не должен передавать файл следующему в цепочке
				break;	// то заканчиваем работу
			x=x.getChild();
		} while(x!=null && x instanceof LocalSource );
    }
    
    private static String makeDstDirName(String baseDir,String name)
    {
    	return baseDir+"/"+AUTO_DST_PREFIX+getTdnSeparator()+name;
    }
    
    private static String makeSrcDirName(String baseDir,String name)
    {
    	return baseDir+"/"+AUTO_SRC_PREFIX+getTdnSeparator()+name;
    }
    
    private static String makeCacheDirName(String baseDir,String name)
    {
    	return baseDir+"/"+AUTO_CACHE_PREFIX+getTdnSeparator()+name;
    }
    
    private Integer superLagInterval = null;
    
    /**
     * Инициализация агента-родителя и потомков
     * @param cfg
     * @return
     */
    private boolean initCfg(Cfg cfg)
    {
    	if(cfg.isFirst())
    		superLagInterval = cfg.getLagIntervalI();
    	
    	if(cfg.getPathSymbol()==null)
    		cfg.setPathSymbol(pathSymbol);
    	
    	cfg.setStopFlag(stopFlag);
    	
        if(defaultSaveTime!=null && cfg.getSaveTime()==null)
        	cfg.setSaveTime(defaultSaveTime.booleanValue());
    	
    	cfg.setDefaultValues();
    	if(Linker.isLinker(cfg))
    		initChildSrcDirsForLink(cfg);
    	initCfgDirs(cfg);
    	if(cfg instanceof RemoteSource) 
    		initChildSrcDirsForLinkRS((RemoteSource) cfg);

    	// если в программе задано значение по умолчанию для lagInterval,
    	// и для данного агента не задан явно интервал обнаружения задержек,
    	if(defaultLagInterval!=null && cfg.getLagIntervalI()==null)
    	{
    		if(cfg.isFirst()) // и если это первый узел в цепочке
    			cfg.setLagIntervalI(defaultLagInterval); // то назначаем значение по умолчанию
    		else if(cfg.getActionType().isRemote() || Utl.boolEq(cfg.getInputFlag(), true) ) { // иначе, если (он работает с удалённым сервером) либо (это агент-источник) 
    			Integer lagInt = (superLagInterval!=null) ? superLagInterval : defaultLagInterval; // назначаем значение родительского агента 
    			cfg.setLagIntervalI(lagInt);
    		}
    	}	
    	
    	// для всех узлов (не только первых) назначаем значение по умолчанию (если задано) для интервала оповещения о состоянии очереди.
        if(defaultQueueWarnInterval!=null && cfg.getQueueWarnIntervalI()==null ) // && cfg.isFirst() )
        	cfg.setQueueWarnIntervalI(defaultQueueWarnInterval);
    	// для всех узлов (не только первых), у которых не задан порог прекращения информирования о состоянии очереди,
        // назначаем значение по умолчанию (если задано)
        if(defaultQueueWarnOff!=null && cfg.getQueueWarnOff()==null ) // && cfg.isFirst() )
        	cfg.setQueueWarnOff(defaultQueueWarnOff);

    	// если это узел первый в цепочке, назначаем значение по умолчанию (если задано) для начала информирования о состоянии очереди.
        if(defaultQueueWarnOn!=null && cfg.getQueueWarnOn()==null && cfg.isFirst() )
        	cfg.setQueueWarnOn(defaultQueueWarnOn);
        //   
        if(defaultSmartRemoteRename!=null && cfg.getSmartRemoteRename()==null && cfg.getActionType().equals(ActionType.LOCAL2REMOTE))
        	cfg.setSmartRemoteRename(defaultSmartRemoteRename.booleanValue());

        if(defaultSaveTime!=null && cfg.getSaveTime()==null)
        	cfg.setSaveTime(defaultSaveTime.booleanValue());
        
    	cfg.init();
    	if(!cfg.isValid()) {
    		return false;
    	}
    	boolean ret = true;
    	if(cfg.child!=null) {
    		ret = initCfg(cfg.child);
    	}
    	return ret;
    }

    public void addParam(Param p)
    {
    	globalParams.addParam(p);
    }

    /**
     * Список атрибутов агента, которые<br>
     * - могут быть заполнены из шаблона<br>
     * - выводятся в журнал при инициализации<br>
     * - могут содержать ссылки на локальные параметры
     */
	public static final String[] PROP_NAMES = {"name","pattern","applyMethod","ftpServer","ftpServerPort","ftpSeparator","ftpRemoteVerification",
		"ftpUser","reconnect","ftpPassword","ftpSrcDir","ftpDstDir","ftpCacheDir","ftpActiveMode","ftpTimeout","ftpFileType","saveTime","srcDir",
		"dstDir","cacheDir","fileMask","statDir","delayBetween","zip","gzip","substitute","substTimeStamp","suffix","prefix","unpack",
		"pauseFlagFile","timeBasedSrcDir","srcTimeZone","dstFileLimit","moveToNext","inputFlag","keepLastFiles", "timeStampInName", "replaceExistingFiles",
		"lagInterval", "queueWarnInterval", "queueWarnOn", "queueWarnOff", "srcSubDirs","checkFtpFileLength","dstDirTimeBased","dstTimeZone",
		"dstDirTimeByFile","ftpServerTimeZone","batchMode","deleteAfter","timeByFile","sequenceLength","smartRemoteRename","param","param1","param2","param3","param4",
		"pauseFlag","startFileDate","moveToNextBlocked","createLinks","proxyType","proxyHost","proxyPort","proxyUser","proxyPassword","dstDirList","nonStrictOrder","slowStop"};
    //removed: "timeZone",
	
	
    /**
     * Список атрибутов агента, в которых разрешена замена ссылок на параметры значениями глобальных параметров.
     */
    private static final String  dirProp[] = {"ftpSrcDir","ftpDstDir","ftpCacheDir","srcDir","dstDir","cacheDir","suffix","prefix","pauseFlagFile","statDir","dstDirList"};

    private void replaceXParams(Cfg cfg, ParamsStorage store, String[] names)
    {
		RefUtl ru = new RefUtl(cfg);
		String pre = cfg.getParamPrefix();
		String suf = cfg.getParamSuffix();
		String pt = store.getPatternRegEx(pre, suf);
		Matcher m = Pattern.compile(pt).matcher("");

    	for(String prop : names) 
    	{
    		try {
    			Object x = ru.hasGetter(prop) ? ru.get(prop) : null; 
    			String tmp = null;
    			if (x instanceof String) 
        			tmp = store.findAndReplace( (String) x, pre, suf);
				else 
					continue; 
    			ru.set(prop, tmp); 
    			m.reset(tmp);
    			if(m.find())
    				throw new IllegalArgumentException("found unknown param : '"+tmp+"'");
    			
    		} catch(Exception e) {
    			throw new IllegalArgumentException("Agent '"+cfg.name + "'. On replaceParams(), property '"+prop+"'.", e);
    		}
    	}
    }

    private void replaceParams(Cfg cfg)
    {
    	replaceXParams(cfg, globalParams, dirProp);
    	replaceXParams(cfg, localParams, PROP_NAMES);
    }
    
    public void init(boolean testMode, boolean stopMode) 
    {
    	loadGlobalParams();
    	loadPatterns();
    	//check main agents
    	List<Cfg> old = mainAgents;
    	mainAgents = new ArrayList<>();
    	for(Cfg c : old) {
    		c.loadChildrenAndParams();
    		addMainAgent(c);
    	}	
    	
		if(port!=null && port.length()>0) {
			try {
				lockPort = Integer.parseInt(port);
			} catch(NumberFormatException e) {
				log.error("Invalid port : {}", port);
			}
		}
		if(stopMode) return;

		if(Utl.trimToNull(dataDir)==null)
			dataDir = DEF_DATA_DIR;

		if(!Utl.makeDirs(dataDir))
			throw new IllegalArgumentException("dataDir='"+dataDir+"' does not exist");

		// грузим dataDir в статические поля классов
		new FileDataBlackList(dataDir);
		new UploadBlackList(dataDir);
		new StatInfo(dataDir);
		
		listener = new PortListener(lockPort, new Commander(this));
    	
    	if(!testMode && !listener.test()) {
    		return;
    	}
    	log.warn("--------------------------------------");
    	localParams.setLogAddParam(true);
    	StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n\n");
    	sb.append("<").append(pName).append(" port=\"").append(lockPort).append("\" >\n\n");
    	
    	for(Cfg cfg : mainAgents) // перебираем агентов-родителей
    	{
    		localParams.clear(); // 
    		Param sup = new Param(LocalParamsStorage.SUPER, cfg.getName());
    		localParams.addParam(sup);
    		//log.info("add local parameter name='{}' , value='{}'", sup.getName(), sup.getValue());
    		
    		// применяем шаблоны(ы)
    		Cfg child = cfg;
    		do {
    			PatternStore.getInstance().applyPatterns( child );
    		} while( (child=child.getChild()) !=null); 
    		
    		// подмена параметров
    		child = cfg;
    		do {
    			localParams.set(child.getLocalParams());
    			replaceParams(child);  // заменяем ссылки на параметры значениями, для агента-родителя и потомков
    			if(child.getName() == null) {
        			String x = "";
        			if(child.srcDir!=null) x = "srcDir="+child.srcDir;
        			else if(child.dstDir!=null) x = "dstDir="+child.dstDir;
    				throw new IllegalArgumentException("undefined name of agent: "+x);
    			}	
        		if(allAgents.containsKey(child.getName()))  // составляем список всех агентов, проверяем на уникальность имени
        		{
        			String x = "";
        			if(child.srcDir!=null) x = "srcDir="+child.srcDir;
        			else if(child.dstDir!=null) x = "dstDir="+child.dstDir;
        			throw new IllegalArgumentException("duplicate name of agent : '"+child.getName()+"'  "+x);
        		}	
        		allAgents.put(child.getName(), child);
    		} while( (child=child.getChild()) !=null); 
    		
    		Cfg.setInputFlagsToChildren(cfg);
    		if( !initCfg(cfg) ) { // проводим инициализацию агента-родителя и потомков 
    			throw new IllegalArgumentException("agent '"+cfg.name+"' not inited !");
    		}
    		Cfg.getWorkConfig(sb, cfg);
    	}
    	sb.append("</").append(pName).append(">\n");
    	if(!testMode) {
			try(FileWriter w = new FileWriter("workConfig", false)) {
	    		w.write(sb.toString());
			} catch (IOException e) {
				log.error("on write "+WRK_CFG+" : ", e);
			}
    	}
    }
   
    public void addMainAgent(Cfg agent) 
    {
		if( Utl.isEmpty(agent.name) ) {
			String ext = "";
			if(!Utl.isEmpty(agent.getSrcDir()))
				ext = "srcDir ="+agent.getSrcDir();
			else if(!Utl.isEmpty(agent.getDstDir()))
				ext = "dstDir ="+agent.getDstDir();
			throw new IllegalArgumentException("found unnamed agent !  "+ ext);
		}
		mainAgents.add(agent);
    }
    
    public void addChild(Cfg src) {
    	addMainAgent(src);
    }

    public Cfg getAgentByName(String name) {
    	return allAgents.get(name);
    }
    
    private volatile boolean iamstopped = false;
    /**
     * Обработчик стоп-сигнала
     */
    private boolean stopAbnormal = false;
    public void stopWork()
    {
    	if(iamstopped)
    		return;
    	stopFlag.set(true);
    	if(!stopAbnormal) log.warn("Halt signal !"); 
    	// подождём завершения work(), но не дольше 15 сек
    	for(int i=0;i<300 && !iamstopped;i++)
    		try { Thread.sleep(50); 
    		} catch(Exception e) { break; }
    }
    
    private void stopThreadList(boolean comment)
    {
        ArrayList<Thread> list = ((ThreadList)ThreadList.getInstance()).getList();
        for(Thread t : list) // сообщаем тредам, что необходимо остановиться 
        	if(t.isAlive()) {
        		t.interrupt();
      		  	if(comment)
      		  		log.info("interrupted {}", t.getName());
        	} 
        
        for(int i=1; list.size()>0 && i<201; i++) // ждём завершения тредов, зарегистрированных в ThreadList, не больше 10 сек. 
        {
      	  	try { Thread.sleep(50); } catch(Exception e) {
      	  		break;
      	  	}
      	  	Iterator<Thread> it = list.iterator();
      	  	if( i%40==0 && comment ) {
      	  		StringBuilder sb = new StringBuilder();
      	  		while(it.hasNext()) {
      	  			Thread t = it.next();
      	  			if(!t.isAlive()) 
      	  				it.remove();
      	  			else 
      	  				sb.append(t.getName()).append(" ");
      	  		}
      	  		log.info("waiting for stop: {}", sb.toString());
      	  	}	
        }
    }
    
    static String delim = ", ";
    private int getRunningAgentsInfo(boolean needLog)
    {
    	int running = 0;
    	int cleaning = 0;
    	int stopped = 0;
    	StringBuilder r = new StringBuilder();
    	StringBuilder c = new StringBuilder();
    	for( Cfg x : allAgents.values()) {
    		switch(x.getStatus())
  			{
  				case Cfg.ST_STOPPED :  	stopped++; 
  										break;
  				case Cfg.ST_STARTED :  	running++;
  										if(r.length()>0) r.append(delim);
  										r.append(x.name); 
  										break;
  				case Cfg.ST_CLEANING:  	cleaning++; 
										if(c.length()>0) c.append(delim);
										c.append(x.name); 
  										break;
  			}
		}
    	if(needLog) {
    		log.warn("agents stopped: {}", stopped);
    		log.warn("agents cleaning: {}; {}",cleaning, c.toString() );
    		log.warn("agents running: {}; {}",running, r.toString() );
    	}	
    	return running+cleaning;
    }
    
    private static final long NANO = 1000000000L;
    private static final long CHECK_CFG_INT = NANO*20; //checkConfigInterval

    /**
     * основной цикл
     */
    public void work()
    {
    // вешаем ShutdownHook для корректного завершения работы по стоп-сигналу
	  Runtime.getRuntime().addShutdownHook(new Thread () { public void run() { stopWork();} } );
	  log.warn("*** Begin work ***");
	  ArrayList<Thread> threads = new ArrayList<Thread>();
	  for(Cfg s :  allAgents.values()) {
		  Thread t = new Thread(s);
		  t.setName(s.getName());
		  threads.add(t);
		  t.start();
	  }
	  
	  long dt = System.nanoTime();
	  long olddt = dt;
	  long cleanStopBeginAt = 0;
	  cleanStop = false;
      for(int i=0;!stopFlag.get();i++)
      {
    	  dt = System.nanoTime();
    	  if(cleanStop)
    	  {
    		  if(cleanStopBeginAt==0) cleanStopBeginAt = System.currentTimeMillis();
    		  else {
    			  int z =  getRunningAgentsInfo(i%200==0);
    			  if(z==0) {
    				  stopFlag.set(true);
    				  break;
    			  }
    		  }	  
    	  }
    	  if(dt-olddt > CHECK_CFG_INT)
    	  {
    		  olddt = dt;	
    		  //checkConfig();
    	  }
    	  try { Thread.sleep(50); } catch(Exception e) { 
    		  stopFlag.set(true); 
    	  }
      }
      log.info("waiting threads ...");
      for(int i=0; threads.size()>0 && i<80; i++) // ждём завершения агентов 4 секунды
      {
    	  try { Thread.sleep(50); } catch(Exception e) {
    		  break;
    	  }
    	  Iterator<Thread> it = threads.iterator();
    	  while(it.hasNext()) 
    		  if(!it.next().isAlive())
    			  it.remove();
      }

      stopThreadList(true);
      
      //log.info("end wait treads ...");
      listener.setStopSignal(true);
      try { Thread.sleep(200); } catch(Exception e) {}      
      log.warn("*** Stop work ***");
      iamstopped = true;
    }

	static final String pName = "fileChief";
	static final String conf = pName+".xml";
	
	static final FileChief newInstance(String confFile, boolean testMode,boolean stopMode) 
	{
		File file = new File( confFile );
		long d = file.lastModified();
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			//Schema schema = schemaFactory.newSchema(new File("filechief.xsd")); // 
			Schema schema = schemaFactory.newSchema(FileChief.class.getResource("filechief.xsd"));
			JAXBContext jaxbContext = JAXBContext.newInstance(FileChief.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			jaxbUnmarshaller.setSchema(schema);
			FileChief fc = (FileChief) jaxbUnmarshaller.unmarshal(file);
			fc.init(testMode, stopMode);
			configFileTime = d;
			return fc;
		} catch( Throwable e ) { 
			log.error("CRITICAL Not created "+pName,e); 
			return null;
		} 
	}

	/**
	 * @return новый экземпляр FileChief
	 */
	public static final FileChief newInstance(boolean testMode, boolean stopMode) 
	{
		if(configFile==null) {
			String home = System.getProperty("fileChief.conf.dir");
			if(home!=null) home += FSEP + conf;
			else home = conf;
			configFile = home;
		}
		return newInstance(configFile, testMode, stopMode);  
	}
	
	private void checkConfig()
	{
		File f = new File(configFile);
		long d = f.lastModified();
		if(d!=configFileTime) {
			//FileChief fc = newInstance(false, false);			
		}
	}
	
	private static long checkConfigInterval = 20000; 
	private static String configFile = null; 
	private static long configFileTime = 0;
	private PortListener listener = null; 

    private void sendCmd(String signalName, String cmd)
    {
    	boolean res = sendCmdExt(signalName, cmd, false);
    	if(!res)
    		sendCmdExt(signalName, cmd, true);
    }	

    private boolean sendCmdExt(String signalName, String cmd, boolean warn)
    {
    	boolean ret = false;
    	try( Socket s = new Socket(PortListener.ADDR, lockPort); 
    			OutputStream os = s.getOutputStream(); 
    			InputStream is = s.getInputStream();) 
    	{
    		ByteBuffer buf = ByteBuffer.allocate(10); 
    		buf.putLong(System.currentTimeMillis());
    		byte[] cb = cmd.getBytes(); 
    		buf.put(cb,0,2);
    		byte[] res = PBECrypt.encrypt(buf.array(), password.toCharArray());
    		log.debug("command encrypted");
            os.write(res);
            os.flush();
    		log.debug("command flushed");
            for(int i=0; i<40; i++) {
            	Thread.sleep(50);
            	if(is.available()==1) {
            		if(is.read()==0) {
            			log.warn("send {} signal: OK ", signalName);
            			ret = true;
            		} else {
            			if(warn)
            				log.warn("send {} signal: error ", signalName);
            			else 
            				log.debug("send {} signal: error ", signalName);
            		}	
            		break;
            	}
            }
            
    	} catch(Exception e) { 
    		String mes = "on send "+signalName+" : " + e.getMessage()+" , port = "+lockPort;
    		System.out.println(mes);
    		log.error(mes, e);
    	}
    	return ret;
    }	
    
    
    private void stopByTcp() {
    	sendCmd("stop", "q0");
    }	
    
    private void haltByTcp() {
    	sendCmd("halt", "q1");
    }	
        
	public static void main(String[] args) throws IOException 
	{
		Thread.currentThread().setName("main");
		EnvToMDC.getInstance();
		log.warn("");
		log.warn("");
		log.warn("");
	    boolean testMode = false;
	    boolean stopMode = false;
	    boolean haltMode = false;
	    //boolean cleanStopMode = false;
	    //boolean breakMode = false;
	    String za = "@@@@@@@@-- FileChief, version {}. {} --@@@@@@@@";
	    String ver = "1.8.0";
	    String tstMode = "Running in test mode.";
	    String stpMode = "Running in stop mode.";
	    String hltMode = "Running in halt mode.";
	    //String cleanStpMode = "Running in clean stop mode.";
		if( args.length>0 ) {
			if("test".equalsIgnoreCase(args[0])) 
			{
				testMode = true;
				log.warn(za, ver, tstMode);
			}
			
			if("stop".equalsIgnoreCase(args[0])) 
			{
				stopMode = true;
				log.warn(za, ver, stpMode);
			}
			
			if("halt".equalsIgnoreCase(args[0])) 
			{
				haltMode = true;
				log.warn(za, ver, hltMode);
			}	
		} else log.warn(za, ver, "");;
		FileChief fldr = newInstance(testMode, stopMode||haltMode);
		if(fldr==null) {
			String m = "CRITICAL ERROR on FileChief.newInstance()";
			log.error(m);
			System.err.println(m+"\n");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { }
			return;
		}	
		if(stopMode) {
			fldr.stopByTcp();
			log.warn("Exit.");
			fldr.stopThreadList(false);
			return;
		}
		if(haltMode) {
			fldr.haltByTcp();
			log.warn("Exit.");
			fldr.stopThreadList(false);
			return;
		}
		
		if(testMode||haltMode||stopMode) {
			log.warn("Exit.");
			fldr.stopThreadList(false);
			return;
		}
		
		String mess;
		if(!fldr.listener.bind()) {
			mess = "Another instance is already running ( port "+fldr.listener.getPort()+" is busy)";
			log.error(mess);
			System.err.println(mess+"\n");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) { }
			fldr.stopThreadList(false);
			return;
		}
		fldr.listener.start();
		mess = "Started, locked port "+fldr.listener.getPort();
		System.err.println(mess+"\n");
		log.warn(mess);
		for(String s : EnvToMDC.INSTANCE.getInfo())
			log.warn(s);
		//Server server = new Server(fldr.listener.getPort() + 100);
		//ServletContextHandler handler = new ServletContextHandler(server, "/info");
		//handler.addServlet(XServlet.class, "/");
		//try {
//			server.start();
//		} catch(Exception e) {
//			log.error("On start Jetty: "+ e.getMessage());
//		}
		try {
			Thread.sleep(30);
		} catch (InterruptedException e1) {
			log.error("Interrupted "+ e1.getMessage());
			Thread.currentThread().interrupt();
		}
		fldr.work();
		/*
		ThreadGroup grp = Thread.currentThread().getThreadGroup();
		while(grp.getParent()!=null) grp = grp.getParent();
		Thread[] ta = new Thread[1000]; 
		int cnt = grp.enumerate(ta);
		for(int i = 0; i< cnt; i++)
		{
			log.info("tread : id={} name={}  state={} ", new Object[] { ta[i].getId(), ta[i].getName(), ta[i].getState().toString()} );
		}
		*/
//		try {
//			server.stop();
//		} catch(Exception e) {
//			log.error("On stop Jetty: "+ e.getMessage());
//		}
		System.exit(0);
//*/		
	}

	public String getWorkDir() {
		return workDir;
	}

	public void setWorkDir(String workDir) {
		this.workDir = workDir;
	}

	public void setPathSymbol(String pathSymbol) {
		this.pathSymbol = pathSymbol;
	}

	public String getPathSymbol() {
		return pathSymbol;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

//	public String getPwd() {
//		return pwd;
//	}

	public void setPassword(String pwd) {
		this.password = pwd;
	}

//	public String getParamSymbol() {
//		return paramSymbol;
//	}

//	public void setParamSymbol(String parSymbol) {
//		this.paramSymbol = parSymbol;
//	}

	@XmlTransient
	class Commander implements ICommander 
	{
		
		Commander(FileChief uploader) {
		}

		public byte run(byte[] cmd) 
		{
			try {
				long ct = System.currentTimeMillis();
				byte[] b = null;
				b = PBECrypt.decrypt(cmd, password.toCharArray());
				ByteBuffer buf = ByteBuffer.wrap(b);
				long ctime = buf.getLong();
				if(ct - ctime > 2000 ) {
					log.error("Commander.run(): invalid command time: " +  ctime);
					return 1;
				}
				byte[] bc = new byte[2];
				buf.get(bc);
				String command = new String(bc); 

				if(command.equals("q1")) { // halt
					Thread t = new Thread() { public void run() {System.exit(0);} };
					t.start();
				} else if(command.equals("q0")) { //stop
					if(cleanStop) {
						log.warn("Double stop signal => halt !");
						runs("q1");
					} else {
						log.warn("Stop signal !");
						cleanStop = true;
					}
					for(Cfg cfg : mainAgents) // перебираем агентов-родителей
						cfg.setCleanStopFlag();
				}
			} catch(Exception e) {
				log.error("Commander.run():", e);
				return 1;
			}
			return 0;
		}

		public void runs(String command) 
		{
			if(command.equals("q1")) { // halt
			Thread t = new Thread() { public void run() {System.exit(0);} };
			t.start();
		} else if(command.equals("q0")) { //stop
			if(cleanStop) {
				log.warn("Double stop signal => halt !");
				runs("q1");
			} else {
				log.warn("Stop signal !");
				cleanStop = true;
			}
	    	for(Cfg cfg : mainAgents) // перебираем агентов-родителей
	    		cfg.setCleanStopFlag();
		}
	}
	
}
	
	
	public Integer getDefaultLagIntervalI() {
		return defaultLagInterval;
	}

	@XmlAttribute	
	public void setDefaultLagInterval(String defLagInterval) 
	{
		defaultLagInterval = Utl.str2interval(defLagInterval);
		log.warn("defaultLagInterval = {} sec", defaultLagInterval);
		if(defaultLagInterval==0) defaultLagInterval = null;
	}

	public static String getTdnSeparator() {
		return tdnSeparator;
	}

	public Integer getDefaultQueueWarnIntervalI() {
		return defaultQueueWarnInterval;
	}

	@XmlAttribute
	public void setDefaultQueueWarnInterval(String defQueueWarnInterval) 
	{
		defaultQueueWarnInterval = Utl.str2interval(defQueueWarnInterval);
		if(defaultQueueWarnInterval>0 && defaultQueueWarnInterval<StatInfo.MIN_QUEUE_WARN_ISEC)
			defaultQueueWarnInterval = StatInfo.MIN_QUEUE_WARN_ISEC;
		log.warn("defaultQueueWarnInterval = {} sec", defaultQueueWarnInterval);
		if(defaultQueueWarnInterval==0) defaultQueueWarnInterval = null;
	}

	public Integer getDefaultQueueWarnOn() {
		return defaultQueueWarnOn;
	}

	public void setDefaultQueueWarnOn(Integer defQueueWarnOn) {
		if(defQueueWarnOn!=null && defQueueWarnOn<0)
			defQueueWarnOn = null;
		defaultQueueWarnOn = defQueueWarnOn;
		log.warn("defaultQueueWarnOn = {}", defaultQueueWarnOn);
	}

	public Integer getDefaultQueueWarnOff() {
		return defaultQueueWarnOff;
	}

	public void setDefaultQueueWarnOff(Integer defQueueWarnOff) {
		if(defQueueWarnOff!=null && defQueueWarnOff<0)
			defQueueWarnOff = null;
		defaultQueueWarnOff = defQueueWarnOff;
		log.warn("defaultQueueWarnOff = {}", defaultQueueWarnOff);
}

	public void setDataDir(String dataDir) {
		this.dataDir = dataDir;
	}

	public String getDataDir() {
		return dataDir;
	}

	public Boolean getDefaultSmartRemoteRename() {
		return defaultSmartRemoteRename;
	}
	
	public void setDefaultSmartRemoteRename(Boolean enableSmartRemoteRename) {
		this.defaultSmartRemoteRename = enableSmartRemoteRename;
	}

	public Boolean getDefaultSaveTime() {
		return defaultSaveTime;
	}

	public void setDefaultSaveTime(Boolean defaultSaveTime) {
		this.defaultSaveTime = defaultSaveTime;
		log.warn("defaultSaveTime = {}", defaultSaveTime);
	}
	
}
