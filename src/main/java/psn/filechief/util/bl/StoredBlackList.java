package psn.filechief.util.bl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import psn.filechief.util.SingletonOM;

public abstract class StoredBlackList<V extends IElement> //extends BlackList<String,V> 
{
	private static final Logger log = LoggerFactory.getLogger(StoredBlackList.class.getName());
	public static final int LIMIT = 20;
	public static final int LIMIT2 = 1000;
	private BlackList<String,V> list = null;
	private int rowsDeleted = 0;// !!!!!!!!!!!
	private String fileName; // !!!!!!!!!!!
	private String dataDir;
	private String listName;
	private String fileSuffix;
	private int blackListWarnOn = 100;
	private int blackListWarnOff = 0;
	
	/**
	 * Показывает, существует ли файл, хранящий содержимое списка
	 */
	private boolean fileExists = false;
	public abstract Class<? extends IElement> getContainerClass();
	
	private String makeFileName(String agentName)
	{
		return dataDir + File.separator + agentName + fileSuffix;
	}

	protected StoredBlackList(String dataDir, String listName, String fileSuffix)
	{
		this.dataDir = dataDir;
		this.listName = listName;
		this.fileSuffix = fileSuffix;
	}
	
	private void setAgentName(String agentName)
	{
		fileName = makeFileName(agentName);
	}
	
	/**
	 * Удаляет файл, хранящий чёрный список, сбрасывает rowsDeleted в 0.
	 * @return true если успешно.
	 */
	private boolean deleteFile() 
	{
		rowsDeleted = 0;
    	File file = new File(fileName);
    	boolean del = file.delete();
    	if(!del) {
    		if(file.exists())
    			log.error("CRITICAL! file not deleted: '{}'", fileName);
    		else
    			log.error("attempt to delete - file does not exist: '{}'", fileName);
    	} else 
    		if(log.isDebugEnabled())
    			log.debug("deleted '{}'", file.getPath());
    	return del;
	}
	
	/**
	 * Добавляет представление объекта в json файл. Выставляет fileExists = true.
	 * @param d объект
	 * @return true если успешно
	 */
	private boolean appendToFile(V d) 
	{
		boolean ret = false;
    	try ( OutputStream out = new BufferedOutputStream(new FileOutputStream(fileName, true)) ) {
    		out.write('\n');
			SingletonOM.getDataWriterFor(getContainerClass()).writeValue(out, d);
			out.flush();
			ret = true;
			fileExists = true;
		} catch(Exception e) {
			log.error("CRITICAL! On append {} item to file: '{}' - {} : {}", new Object[] { listName, fileName, e.getClass().getSimpleName(), e.getMessage()});
		}
		return ret;
	}
	
	/**
	 * Записывает представление всех объектов в json файл.
	 * @return true если успешно
	 */
	private boolean writeAll() 
	{
		rowsDeleted = 0; // ????
		boolean ret = false;
		ByteArrayOutputStream bs = new ByteArrayOutputStream(2048); 
		ObjectWriter w = SingletonOM.getDataWriterFor(getContainerClass());
    	try(OutputStream  out = new BufferedOutputStream( new FileOutputStream(fileName, false)) ) {
			for(V d : this.list.getBlackMap().values())
			{
				d.setInsFlag(true);
				bs.write('\n');
				w.writeValue(bs, d);
				bs.writeTo(out);
				out.flush();
				bs.reset();
			}
			ret = true;
			fileExists = true;
			log.info("{} writeAll ok", listName);
		} catch(Exception e) {
			log.error("CRITICAL! On write records from {} to file: '{}' - exception: {}", new Object[] { listName, fileName, e.getMessage()} );
		} 
		return ret;
	}
	
	public int getSize()
	{
		return list.getSize();
	}
	
	public void loadFromJson(String agentName)
	{
		rowsDeleted = 0;
		if(list==null)
			list = new BlackList<>(listName);
		else
			list.clearList();
		setAgentName(agentName);
		//String fname = makeFileName(agentName);
		File f = new File(fileName);
		if( !f.exists() )
			return; // null;
		ObjectReader rd = SingletonOM.getDataReaderFor(getContainerClass());
    	try( InputStream in = new BufferedInputStream( new FileInputStream( fileName )) ) {
			MappingIterator<V> mi = rd.readValues(in);
			while(mi.hasNext())
			 {
				 V d = mi.next();	
				 if(d!=null) {
					 if(d.isInsFlag())
						 appendToList(d);
					 else {
						 delFromList(d, false);
						 rowsDeleted++;
					 }
				 }
			}
			fileExists = true;
			log.warn("{} loaded from '{}' , {} records", new Object[] {listName, fileName, getSize()});
			
		} catch(Exception e) {
			log.error("CRITICAL! On load {} from file: '{}' - exception: {}", new Object[] {listName, fileName, e.getMessage()});
		}
	}

	/**
	 * Добавляет объект в список(но не в файл состояния).
	 * @param d объект.
	 */
	private void appendToList(V d)
	{
		list.addToList(d.getKeyFor(), d);
	}
	/**
	 * Добавляет объект в список и в файл состояния списка.
	 * @param d объект
	 */
	public void addToList(V d)
	{
		appendToList(d);
		d.setInsFlag(true);
		appendToFile(d);
		log.warn("added to {}: '{}'", listName, d.getKeyFor());
	}
	/**
	 * Очищает список и удаляет файл состояния списка.
	 */
	public void clearList() 
	{
		list.clearList();
		if(fileExists) {
			deleteFile();
			fileExists = false;
		}	
	}

	/**
	 * Очищает список, но не удаляет файл состояния списка.
	 */
	public void clearListFromMemory() {
		list.clearList();
		list = null;
	}
	
	/**
	 * Удаляет объект из списка.
	 * @param d объект 
	 * @param needLog нужно ли выводить сообщение в журнал
	 * @return true если такой объект был в списке
	 */
	private boolean delFromList(V d, boolean needLog) 
	{
		return list.removeFromList(d.getKeyFor(), needLog);
	}
	/**
	 * Удаляет объект из списка и сохраняет обновлённый список в файле.
	 * @param d объект
	 * @return true если такой объект был в списке
	 */
	public boolean removeFromList(V d) 
	{
		boolean ret  = delFromList(d, true);
		if(getSize()==0) // если список пуст - удаляем файл
			clearList();
		else {
			// если в файле немного удалённых записей(< LIMIT),
			// либо больше LIMIT, но менее 5% от общего числа записей
			if(++rowsDeleted < LIMIT || rowsDeleted*100/getSize()<10  ) {
			//if( ++rowsDeleted*100/getSize()<10  ) { // если в файле менее 10% от общего числа записей
				d.setInsFlag(false); // помечаем как удалённую 
				appendToFile(d); // дописываем в файл
			} else // иначе выгружаем весь список
			writeAll();
		}
		return ret;
	}

	public Collection<V> getFoundOnLastCheck() {
		return list.getFoundOnLastCheck();
	}

	public int getFound() {
		return list.getFound();
	}

	public void clearFound() {
		list.clearFound();
	}

	public boolean trimToLastCheckResult()
	{
		ArrayList<V> toRemove = list.getToTrimToLastCheckResult();
		if(toRemove==null)
			return true;
		for(V v : toRemove)
			removeFromList(v);
		return toRemove.size()>0;
	}

	/**
	 * Проверяет, есть ли данный объект в чёрном списке. Все найденные объекты складываются с спец. список.
	 * См. getFoundOnLastCheck() и clearFound(). 
	 * @return true если есть. 
	 */
	public boolean inList(V x) {
		return list.inList(x.getKeyFor());
	}
	
	public V getValue(String key) {
		return list.getValue(key);
	}

	public V getValue(V v) {
		return list.getValue(v.getKeyFor());
	}
	
	public int getBlackListWarnOn() {
		return blackListWarnOn;
	}

	public void setBlackListWarnOn(int blackListWarnOn) {
		this.blackListWarnOn = blackListWarnOn;
	}

	public int getBlackListWarnOff() {
		return blackListWarnOff;
	}

	public void setBlackListWarnOff(int blackListWarnOff) {
		this.blackListWarnOff = blackListWarnOff;
	}
	
}
