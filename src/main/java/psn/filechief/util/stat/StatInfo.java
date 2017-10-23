package psn.filechief.util.stat;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import psn.filechief.util.SingletonOM;

public class StatInfo 
{
	private static Logger log = LoggerFactory.getLogger(StatInfo.class.getName());	
	public static final String LAG_DETECTED = "Lag detected, no files from {}";
	public static final String LAG_CONTINUES = "Lag continues, no files from {}";
	public static final String LAG_DONE = "Lag gone ({} minutes), filesInQueue={}";
	public static final String LAG_DISABLED = "Lag disabled, new lagInterval={}";
	
	public static final String QUEUE_TOO_BIG = "Queue too big, filesInQueue={}";
	public static final String QUEUE_SIZE = "Queue control, filesInQueue={}";
	public static final String QUEUE_OK = "Queue ok, filesInQueue={} ";
	public static final String QUEUE_DISABLED = "Queue disabled";
	
	public static final int MIN_QUEUE_WARN_ISEC = 60; // одна минута - минимальный интервал оповещения о состоянии очереди
	public static final int DEF_QUEUE_WARN_ISEC = 60*60; // один час - интервал оповещения о состоянии очереди по умолчанию
	
	public static final int NO_QUEUE = 0;
	public static final int FILES_LISTED = 1;
	public static final int FILE_RECEIVED = 2;
	public static final int CHECK_LAG = 3;
	
	public static final long UPDATE_NO_FILE = 60000;

	public static final String suffix_ST = ".stat.json";
	private static String dataDir;
	
	private String fileName;
	/**
     * Максимально допустимое время необработки файлов (миллисекунды).  
     */
	public long lagInterval = 0; 
	public long queueWarnInterval = 0;
	public int queueWarnOn = 0;
	public int queueWarnOff = 0;
	int queueWarnCount = 0;
	long queueDate = 0;
	public int queueSize = 0;
	
	private StatAll all = new StatAll(); 
	private StatDay day = new StatDay(); 
	private StatHour hour = new StatHour(); 
	
	// дата последнего принятого файла (на ftp сервере, только для ftpDownload)
	private Long lastFileTime = null;

	// дата "отсечки" 
	//private Long cutFileTime = null;
	
	public StatInfo() {
		init();
	}
	
	public void init() {
		all.setStatParams(new StatParams());		
	}
	
	public StatInfo(String dir) 
	{
		super();
		if(dataDir==null)
			dataDir = dir;
		//else throw new IllegalArgumentException("StatInfo - dataDir already inited !");
	}
	
	private static String makeFileName(String agentName)
	{
		return dataDir + File.separator + agentName + suffix_ST;
	}

	public void setAgentName(String agentName)
	{
		fileName = makeFileName(agentName);
	}	
	
//	private void copyStatParamsTo2(StatInfo si)
//	{
//		si.all.setStatParams(all.getStatParams());
//	}
	
	public static StatInfo loadFromJson(String agentName)
	{
		String fname = makeFileName(agentName);
		//setAgentName(agentName);
		File f = new File(fname);
		if( !f.exists() )
			return null;
		StatInfo st = null;
		try {
			st = SingletonOM.getStatInfoReader().readValue(new File(fname));
			log.info("statInfo loaded from '{}'", fname);
			//st.setDefData();
		} catch (Exception e) {
			log.error("CRITICAL! On load statInfo from file: '{}' - {} : {}", new Object[] { fname, e.getClass().getSimpleName(), e.getMessage()});
		}
		return st;
	}

	public void updateValues(Integer newLagInterval, Integer newQueueWarnInterval, Integer newQueueWarnOn, Integer newQueueWarnOff) 
	{
		long lagi = (newLagInterval==null) ? 0 : 1000L*newLagInterval;
		if(all.warnCount>0 && lagi!=lagInterval) // если ранее была обнаружена задержка, и затем поменялся lagInterval
			if( lagi==0 || (System.currentTimeMillis() - all.getLast() < lagi)) {
				all.cleanLagWarning();
				log.warn(StatInfo.LAG_DISABLED, lagi/1000);
			}
		lagInterval = lagi; 
		
		queueWarnInterval = (newQueueWarnInterval==null) ? 0 : 1000L*newQueueWarnInterval;
		int warnOn = (newQueueWarnOn==null) ? 0 : newQueueWarnOn;
		if(queueWarnCount>0 && warnOn==0) {  // ранее была обнаружена очередь, но теперь проверка очереди отключена
			cleanQueueWarning();
			log.warn(QUEUE_DISABLED);
		} 
		queueWarnOn = warnOn; 
		
		queueWarnOff = (newQueueWarnOff==null) ? 0 : newQueueWarnOff;
	}
	
	public boolean saveToJson() 
	{
		try {
			SingletonOM.getStatInfoWriter().writeValue(new File(fileName), this);
			return true;
		} catch (Exception e) {
			log.error("CRITICAL! On save staInfo to file '"+fileName+"' :", e);
		}
		return false;
	}
	
	public void validate()
	{
		if(queueWarnOn<=0)
			return;
		
		if(queueWarnOn<queueWarnOff) {
			log.error("value queueWarnOff({}) great then queueWarnOn({}) , set to 0", queueWarnOff, queueWarnOn);
			queueWarnOff = 0;
		}
		if(queueWarnInterval==0)
			queueWarnInterval = 1000L*DEF_QUEUE_WARN_ISEC;
	}

	private void cleanQueueWarning()
	{
		queueWarnCount = 0;
		queueDate = 0;
	}	
	
	/**
	 * Контролирует состояние очереди, выдаёт сообщения в журнал:<br> 
	 * - предупреждает, если кол-во файлов в очереди >= <b>queueWarnOn</b>;<br>
	 * - после чего оповещает о состоянии очереди, пока кол-во файлов не будет <= <b>queueWarnOff</b>.  
	 * @param cur текущее время
	 */
	private void queueDetect(long cur)
	{
		if(queueWarnOn<=0) 
			return;
		if(queueWarnCount==0 && queueSize<queueWarnOn) // очередь ниже порога предупреждения, ничего не делаем
			return;
		if(queueWarnCount==0 && queueSize>=queueWarnOn) { // очередь возникла
			queueWarnCount = 1;
			queueDate = cur;
			log.warn(QUEUE_TOO_BIG, queueSize);
			return;
		} 
		if(queueWarnCount>0 && queueSize<=queueWarnOff) {  // очередь рассосалась
			cleanQueueWarning();
			log.warn(QUEUE_OK, queueSize);
			return;
		} 
		// очередь ещё не рассосалась
		if((cur-queueDate)/queueWarnCount > queueWarnInterval) { // проверяем, что прошло время не меньше queueWarnInterval 
			queueWarnCount++;
			log.warn(QUEUE_SIZE, queueSize);
		}
	}
	
	/**
	 * Обновляет статистику обработки файлов. 
	 * @param flag : FILE_RECEIVED файл успешно обработан; 
	 * NO_FILES - нет файлов, нужен для определения задержек поступления;
	 * FILES_LISTED - нужен для определения размера очереди файлов.  
	 * @param fileName имя обработанного файла (при flag=FILE_RECEIVED).
	 * @param size размер обработанного файла (при flag=FILE_RECEIVED).
	 */
	public void next(int flag, String fileName, long size, long fileDate)
	{
		long cur = System.currentTimeMillis();
		if(flag==FILE_RECEIVED && fileName != null)
		{
			lastFileTime = fileDate;
			all.next(size, cur);
			day.next(size, cur);
			hour.next(size, cur);
			if(queueSize>0) 
				queueSize--;
			return;
		} 
		if(flag==NO_QUEUE) {
			all.noFiles(cur);
			queueSize = 0;
			queueDetect(cur);
		} else if(flag==CHECK_LAG) {
			all.noFiles(cur);
		} else if(flag==FILES_LISTED) 
			queueDetect(cur);
	}
	
	@JsonIgnore
	public long getSequence()
	{
		return all.files;
	}
	
	@JsonIgnore
	public String getSequence(String format)
	{
		//"%05d"
		return String.format(format, all.files);
	}

	class StatParams implements IStatParams {

		public int getQueueSize() {
			return queueSize;
		}

		public long getLagInterval() {
			return lagInterval;
		}
		
	}

	public StatAll getAll() {
		return all;
	}

	public void setAll(StatAll all) {
		this.all = all;
		init();
	}

	public StatDay getDay() {
		return day;
	}

	public void setDay(StatDay day) {
		this.day = day;
	}

	public StatHour getHour() {
		return hour;
	}

	public void setHour(StatHour hour) {
		this.hour = hour;
	}

	public Long getLastFileTime() {
		return lastFileTime;
	}

	public void setLastFileTime(Long lastFileTime) {
		this.lastFileTime = lastFileTime;
	}

}
