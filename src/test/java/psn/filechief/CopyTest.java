package psn.filechief;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CopyTest extends FCTest {
	static final String AGENT = "testCopy";
	static final String COPY_WO_CACHE = "<copy name = '"+AGENT+"' srcDir = '@srcDir' dstDir = '@dstDir' @opt /> ";
	//static final String COPY_WITH_CACHE = "<copy name = '"+COPY+"' srcDir = '@srcDir' dstDir = '@dstDir' cacheDir='@cacheDir' /> ";

	String[][] subst;
	
	String srcDir;
	String dstDir;
	String cacheDir;
	
	Copy cp;

	@Before
	public void setUp() throws Exception {
		srcDir = makeNewSubDir(SRC); 
		dstDir = makeNewSubDir(DST); 
		cacheDir = makeNewSubDir(CACHE);
		subst = new String[][] { 
			{"@srcDir", srcDir},
			{"@dstDir", dstDir},
			{"@cacheDir", cacheDir} 
		};
		cp = new Copy();
		//System.setProperty("java.io.tmpdir", tmpDir.getAbsolutePath());
	}

	@After
	public void tearDown() throws Exception {
		clearAndRemoveDir(STAT_DATA_DIR);
		clearAndRemoveDir(CONF_DIR);
		clearAndRemoveDir(srcDir);
		clearAndRemoveDir(dstDir);
	}

	@Test
	public void testIsValid() {
		cp.setDstDir(dstDir);
		cp.setSrcDir(srcDir);
		assertTrue(cp.isValid());
		
		cp.setSrcDir(null);
		assertFalse(cp.isValid());
		
		cp.setDstDir(null);
		cp.setSrcDir(srcDir);
		assertFalse(cp.isValid());
	}
	
	@Test
	public void testSetDefaultValues() {
		cp.setDefaultValues();
		assertFalse(cp.getZip());
		assertFalse(cp.getGzip());
		assertFalse(cp.getUnpack());
		assertEquals(5, cp.getDelayBetween().intValue());
		assertTrue(cp.getSaveTime());
	}

	@Test
	public void testAutoMakeDirs() throws IOException {
		cp.setDstDir(dstDir);
		cp.setSrcDir(srcDir);
		cp.setCacheDir(cacheDir);
		File dst = new File(dstDir);
		clearAndRemoveDir(dst);
		File src = new File(srcDir);
		clearAndRemoveDir(src);
		File cache = new File(cacheDir);
		clearAndRemoveDir(cache);
		
		assertFalse(dst.exists());
		assertFalse(src.exists());
		assertFalse(cache.exists());
		cp.init();
		assertTrue(dst.exists());
		assertTrue(src.exists());
		assertTrue(cache.exists());
	}
	
	@Test
	public void testApplyDstFileLimit() throws IOException {
		cp.setDstDir(dstDir);
		new File(dstDir,"file1").createNewFile();
		new File(dstDir,"file2").createNewFile();
		
		cp.setDstFileLimit(Integer.valueOf(1));
		int ret = cp.applyDstFileLimit(5);
		assertEquals(0, ret);
		
		cp.setDstFileLimit(Integer.valueOf(4));
		ret = cp.applyDstFileLimit(5);
		assertEquals(2, ret);
	}

	/**
	 * 
	 */
	@Test(timeout=2000)
	public void testCopy() throws IOException {
   		File srcFile = makeFile(srcDir, "s_file_1");
   		String cfg = makeConfig(COPY_WO_CACHE, "", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg copy = fc.getAgentByName(AGENT);
   		assertTrue(srcFile.exists());
   		assertTrue(copy.getCacheDir()!=null);
   		assertTrue(copy.getCacheDir().startsWith(copy.getDstDir()));
   		Thread t = new Thread() { public void run(){ copy.run(); } }; 
   		t.start();
   		t.interrupt();
   		try { t.join(2000); } catch (InterruptedException e1) {}
   		assertFalse(srcFile.exists());
   		File dstFile = new File(dstDir,srcFile.getName());
   		assertTrue(dstFile.exists());
	}

	/**
	 * 
	 */
	@Test(timeout=2000)
	public void testCopy2() throws IOException {
   		File srcFile =  makeFile(srcDir, "s_file_2");
   		String cfg = makeConfig(COPY_WO_CACHE, "cacheDir = '' substitute = 's/^s_file/new_file/' ", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg copy = fc.getAgentByName(AGENT);
   		assertTrue(copy.getCacheDir().length()==0); // simple move, without copy to cacheDir 
   		Thread t = new Thread() { public void run(){ copy.run(); } }; 
   		t.start();
   		t.interrupt();
   		try { t.join(2000); } catch (InterruptedException e1) {}
   		assertFalse(srcFile.exists());
   		File dstFile = new File(dstDir,"new_file_2"); // check new name
   		assertTrue(dstFile.exists());
	}

	/**
	 * 
	 */
	@Test(timeout=2000)
	public void testCopy3() throws IOException {
   		File srcFile =  makeFile(srcDir, "s_file_3");
   		long ft = srcFile.lastModified() - 120_000;
   		srcFile.setLastModified(ft);
   		String cfg = makeConfig(COPY_WO_CACHE, "cacheDir = '' saveTime = 'true' gzip='true'", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg copy = fc.getAgentByName(AGENT);
   		assertTrue(copy.getCacheDir().startsWith(copy.getDstDir()));
   		Thread t = new Thread() { public void run(){ copy.run(); } }; 
   		t.start();
   		t.interrupt();
   		try { t.join(2000); } catch (InterruptedException e1) {}
   		assertFalse(srcFile.exists());
   		File dstFile = new File(dstDir,srcFile.getName()+".gz"); // check gzip
   		assertTrue(dstFile.exists());
   		assertEquals(ft, dstFile.lastModified()); // check saveTime
	}

	/**
	 * @throws InterruptedException 
	 * 
	 */
	@Test(timeout=2000)
	public void testCopy4() throws IOException, InterruptedException {
   		File srcFile =  makeFile(srcDir, "s_file_4");
   		long ft = srcFile.lastModified() - 120_000;
   		srcFile.setLastModified(ft);
   		String prefix = "pref_";
   		String cfg = makeConfig(COPY_WO_CACHE, "cacheDir = '' saveTime='false' zip='true' prefix = \"'"+prefix+"'\" ", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg copy = fc.getAgentByName(AGENT);
   		assertTrue(copy.getCacheDir().startsWith(copy.getDstDir())); // check cacheDir - not empty !
   		Thread t = new Thread() { public void run(){ copy.run(); } }; 
   		long before = System.currentTimeMillis()-1;
   		File dstFile = new File(dstDir, prefix+srcFile.getName()+".zip"); // check prefix and zip
   		assertFalse(dstFile.exists());
   		t.start();
   		t.interrupt();
   		t.join(2000);
   		assertFalse(srcFile.exists());
   		assertTrue(dstFile.exists());
   		assertFalse(copy.getSaveTime().booleanValue());
   		assertTrue(before < dstFile.lastModified()); // check no saveTime
	}

	
}
