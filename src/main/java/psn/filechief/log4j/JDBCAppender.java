package psn.filechief.log4j;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Iterator;

import org.apache.log4j.spi.LoggingEvent;

public class JDBCAppender extends org.apache.log4j.jdbc.JDBCAppender 
{
	private LogFilter filter = null;
	private String sqlTable;
	private String insertToTable;
	private String zone = "";
	private String server = "";
	private PreparedStatement ps = null;
	
	private void initFilter()
	{
		if(filter==null) {
			filter = new LogFilter();
			addFilter(filter);
		}	
	}
	
	@SuppressWarnings("unchecked")
	public void flushBuffer()
	  {
		removes.ensureCapacity(buffer.size());
		try {
			if(ps==null)
				ps = getConnection().prepareStatement(insertToTable);
			for (@SuppressWarnings("rawtypes")
			Iterator i = buffer.iterator(); i.hasNext(); ) 
			{
				LoggingEvent logEvent = (LoggingEvent)i.next();
				ps.setTimestamp(1, new Timestamp(logEvent.timeStamp));
				ps.setString(2, zone);
				ps.setString(3, server);
				ps.setString(4, logEvent.getLevel().toString());
				ps.setString(5, logEvent.getThreadName());
				ps.setString(6, logEvent.getRenderedMessage());
				ps.executeUpdate();
				connection.commit();
				ps.clearParameters();
				removes.add(logEvent);
			}
	    } catch (SQLException e) {
	        errorHandler.error("Failed to excute sql: '"+insertToTable+"'", e, 2);
	        try { 
	        	if(connection!=null) connection.close();
	        } catch(Exception ee) { }
	        connection = null;
	        ps = null;
	    }
	    buffer.removeAll(removes);
	    removes.clear();
	  }

	public void setDenyPattern(String pattern) 
	{
		initFilter();
		filter.setDenyPattern(pattern);
	}

	public void setAllowPattern(String pattern) 
	{
		initFilter();
		filter.setAllowPattern(pattern);
	}

	public void setMinLogLevel(String level)
	{
		initFilter();
		filter.setMinLogLevel(level);
	}

	public String getMinLogLevel()
	{
		return "";
	}
	
	public String getSqlTable() {
		return sqlTable;
	}

	public void setSqlTable(String sqlTable) {
		this.sqlTable = sqlTable;
		insertToTable = "insert into "+sqlTable+ " values(?,?,?,?,?,?)";
	}

	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

}
