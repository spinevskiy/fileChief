package psn.filechief.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import psn.filechief.util.bl.FileData;
import psn.filechief.util.bl.IElement;
import psn.filechief.util.bl.NotRenamedFile;
import psn.filechief.util.stat.StatInfo;

public class SingletonOM {

	private static final SingletonOM om = new SingletonOM();
	private ObjectMapper mapper;
	
	private ObjectReader fileDataReader;
	private ObjectWriter fileDataWriter;

	private ObjectReader nrfDataReader;
	private ObjectWriter nrfDataWriter;
	
	private ObjectReader statInfoReader;
	private ObjectWriter statInfoWriter;
	
	private static ObjectReader makeReaderFor(ObjectMapper m, Class<? extends IElement> c) 
	{
		return m.readerFor(c); //reader
	}

	private static ObjectWriter makeWriterFor(ObjectMapper m, Class<? extends IElement> c) 
	{
		ObjectWriter w = m.writerFor(c); //writerWithType
		w = w.without(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, SerializationFeature.CLOSE_CLOSEABLE);
		return w;
	}
	
	private SingletonOM() 
	{
		mapper = new ObjectMapper();
		
		statInfoReader = mapper.readerFor(StatInfo.class); //reader
		statInfoWriter = mapper.writerFor(StatInfo.class); //writerWithType
		
		fileDataReader = makeReaderFor(mapper, FileData.class);
		fileDataWriter = makeWriterFor(mapper, FileData.class);

		nrfDataReader = makeReaderFor(mapper, NotRenamedFile.class);
		nrfDataWriter = makeWriterFor(mapper, NotRenamedFile.class);
	}

	public static ObjectReader getDataReaderFor(Class<? extends IElement> c) 
	{
		if(c.equals(FileData.class))
			return om.fileDataReader;
		if(c.equals(NotRenamedFile.class))
			return om.nrfDataReader;
		return null;
	}

	public static ObjectWriter getDataWriterFor(Class<? extends IElement> c) 
	{
		if(c.equals(FileData.class))
			return om.fileDataWriter;
		if(c.equals(NotRenamedFile.class))
			return om.nrfDataWriter;
		return null;
	}
	
	
	public static ObjectReader getStatInfoReader() {
		return om.statInfoReader;
	}
	
	public static ObjectWriter getStatInfoWriter() {
		return om.statInfoWriter;
	}

}