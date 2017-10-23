package psn.filechief.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import psn.filechief.ICommander;

public class PortListener extends Thread 
{
		private static final Logger log = LoggerFactory.getLogger(PortListener.class.getName());
		public static final String ADDR = "127.0.0.1";
		
		private boolean stopSignal = false;
		private String nam = "PortListener";
		private int port = 7077;
		private ServerSocket ss = null;
		private ICommander cmd;
		
		public PortListener(int port, ICommander cmd) {
			if(port >= 0)
				this.port = port;
			this.cmd = cmd;
		}
		
		public boolean bind() 
		{
			boolean ret = false;
			try {
				ss = new ServerSocket();
				ss.setSoTimeout(50);
				ss.bind(new InetSocketAddress(ADDR,port));
				ret = true;
			} catch(Exception e) {
				try { ss.close(); } catch(Exception ee) {}
				log.debug("on bind to {}:{} - {}", new Object[] {ADDR,port,e.getMessage()});
			} 
			return ret;
		}
		
		public boolean test() 
		{
			if(!bind()) return false;
			try { ss.close(); } catch(Exception ee) {}
			return true;
		}
		
		public void run()
		{   
			this.setName(nam);
			log.warn("started");
			do {
				try ( Socket s = ss.accept(); 
						InputStream in = new BufferedInputStream(s.getInputStream());
						OutputStream os = s.getOutputStream()) 
				{
						byte[] input = new byte[128]; 
						int bytes = in.read(input, 0, input.length);
						byte[] ib = Arrays.copyOf(input, bytes);
						byte ret = cmd.run(ib);
						os.write(ret);
						os.flush();
				} catch(SocketTimeoutException e) {  }
				catch(IOException e) { 
					  log.error("run()", e); 
				}
			} while(isStopSignal()==false); 
			try { ss.close(); } catch(Exception e) {}
			log.info("stopped, port {} is free", port);
		}

		public synchronized boolean isStopSignal() {
			return stopSignal;
		}

		public synchronized void setStopSignal(boolean stopSignal) {
			this.stopSignal = stopSignal;
		}

		public int getPort() {
			return port;
		}
	
}
