package psn.filechief.util.stat;

import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;

import psn.filechief.util.stat.StatInfo.StatParams;

public class StatX 
{
	private static final Logger log = LoggerFactory.getLogger(StatX.class.getName());	
	public static final long IDAY = 60*60*24*1000;
	public static final long IHOUR = 60*60*1000;
	public static final long IMINUTE = 60*1000;
	public static final long IMAX = IDAY;
	public static final String DATE_FORMAT =  "yyyy-MM-dd HH:mm:ss";
	
	private SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
	@JsonIgnore
	private StatParams statParams = null;

	/**
	 * Интервал сброса статистики, миллисекунды. Значение 0 - не сбрасывать статистику.
	 */
	@JsonIgnore
	private long resetInterval = 0;

	@JsonIgnore
	private boolean needLog;
	
	/**
	 * Дата первого запуска, с начала интервала.
	 */
	private long first = -1;
	/**
	 * Дата, когда был обработан последний файл, с начала текущего интервала.
	 * Если файлов не было - содержит "время запуска"/"время сброса интервала".
	 */
	private long last = -1;
	/**
	 * Обработано файлов, с начала интервала.
	 */
	long files = 0;
	//long sumSize = 0;
	/**
	 * Средний размер файлов, обработанных с начала интервала.
	 */
	long avgSize = 0;
	/**
	 * Номер предыдущего интервала.
	 */
	long prevInterval = -1;
	long lastWarn = 0;
	int warnCount = 0;
	
	/**
	 * 
	 * @param interval
	 * @param log
	 */
	StatX(long interval, boolean log) //(int interval, boolean log)
	{
		resetInterval = interval;
		needLog = log;
	}
	/**
	 * 
	 */
	public static void getDuration(StringBuilder sb, long delta)
	{
		long t;
		sb.append(" ( over "); 
		if(delta > 2*IDAY) {
			t = delta/IDAY;
			sb.append(t).append(" DAYS )");
		}
		else if(delta > 2*IHOUR) {
			t = delta/IHOUR;
			sb.append(t).append(" HOURS )");
		} else {
			t = delta/IMINUTE;
			sb.append(t).append(" minutes )");
		} 
	}
	/**
	 * Отрабатываем событие: отсутствие нового файла.
	 * @param cur - текущая дата.
	 */
	void noFiles(long cur)
	{
		long curInterval = (resetInterval==0) ? 1 : cur/resetInterval;
		if(prevInterval!=curInterval) 
			clean(cur);
		prevInterval = curInterval;
		
		if(needLog) { // только для StatAll
			long lagi = statParams.getLagInterval();
			if( lagi>0 && (cur - last) > lagi && (cur - lastWarn) > Math.min(lagi*2, IMAX))
			{
				String mes = (warnCount==0) ? StatInfo.LAG_DETECTED : StatInfo.LAG_CONTINUES ;
				warnCount++;
				lastWarn = cur;
				StringBuilder sb = new StringBuilder(sdf.format(last));
				getDuration(sb, cur - last);
				log.warn(mes, sb.toString() );
			}
		}
	}

	/**
	 * Отрабатываем событие: поступление нового файла.
	 * @param size - размер файла, байт.
	 * @param cur - текущая дата.
	 */
	void next(long size, long cur)
	{
		long curInterval = (resetInterval==0) ? 1 : cur/resetInterval;
		
		if(prevInterval!=curInterval) 
			clean(cur);
		
		prevInterval = curInterval;
		avgSize = (avgSize*files + size)/(files+1);
		files++;
		long lagDuration = cur-last; 
		last = cur;
		if( needLog && warnCount>0) { // сообщаем о возобновлении потока файлов. Только StatAll.
			cleanLagWarning();
			log.warn(StatInfo.LAG_DONE, lagDuration/60000, statParams.getQueueSize());
		} 
	}

	void cleanLagWarning()
	{
		warnCount = 0;
		lastWarn = 0;
	}
	
	/**
	 * Сброс накопленной статистики.
	 * @param cur
	 */
	void clean(long cur) 
	{
		first = cur;
		last = cur;
		files = 0;
		avgSize = 0;
		prevInterval = -1;
		cleanLagWarning();
		//queueWarnCount = 0;
		//queueDate = 0;
	}
	
	public boolean isNeedLog() {
		return needLog;
	}
	public StatParams getStatParams() {
		return statParams;
	}
	public void setStatParams(StatParams statParams) {
		this.statParams = statParams;
	}
	public long getFirst() {
		return first;
	}
	public void setFirst(long first) {
		this.first = first;
	}
	public long getLast() {
		return last;
	}
	public void setLast(long last) {
		this.last = last;
	}
	public long getFiles() {
		return files;
	}
	public void setFiles(long files) {
		this.files = files;
	}
	public long getAvgSize() {
		return avgSize;
	}
	public void setAvgSize(long avgSize) {
		this.avgSize = avgSize;
	}
	public long getPrevInterval() {
		return prevInterval;
	}
	public void setPrevInterval(long prevInterval) {
		this.prevInterval = prevInterval;
	}
	public long getLastWarn() {
		return lastWarn;
	}
	public void setLastWarn(long lastWarn) {
		this.lastWarn = lastWarn;
	}
	public int getWarnCount() {
		return warnCount;
	}
	public void setWarnCount(int warnCount) {
		this.warnCount = warnCount;
	}
}