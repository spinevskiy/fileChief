package psn.filechief;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CfgTest extends FCTest {
	public static final String BIGCFG = "<copy name=\"big\" srcDir='@srcDir1' batchMode='true' "
			+ " dstDir='@dstDir1' cacheDir='@cacheDir1' deleteAfter='false' checkFtpFileLength='false' "
			+ " delayBetween='7' saveTime='false' keepLastFiles='3' dstFileLimit='4' lagInterval='12h' "
			+ " dstTimeZone='Europe/Kaliningrad' fileMask='^test' gzip='true' zip='false' "
			+ " param=':dat=aa' param1=':dat1=bb' param2=':dat2=cc' param3=':dat3=dd' param4=':dat4=ee' "
			+ " pauseFlag='false' pauseFlagFile='@cacheDir1' prefix='yyyyMMdd' queueWarnInterval='30m' "
			+ " queueWarnOn='50' queueWarnOff='40' replaceExistingFiles='false' sequenceLength='7' "
			+ " slowStop='true' srcSubDirs = 'dir1;dir2' srcTimeZone='Europe/Moscow' substTimeStamp='true' "
			+ " substitute='s/^CDR//' suffix=\"'.cdr'\" timeBasedSrcDir = \"'dat'yyyyMMdd\" "
			+ " timeStampInName='(\\d{14}) , yyyyMMddHHmmss' unpack='true' nonStrictOrder='true' "
			+ "/>";
	
	public static final String CHAIN_CFG = "<copy name='main' srcDir='@srcDir1' dstDir='@dstDir1' cacheDir='@cacheDir1' >"
			+ "<ftpUpload name='child' srcDir='@srcDir2' ftpDstDir='/someDdstDir' ftpServer='127.0.0.1' ftpUser='a' ftpPassword='b' />"
			+ "</copy>";

	public static final String PATTERNS = "<patterns>"
			// simple pattern 1
			+ "<ftpDownload name='pDown1' ftpSrcDir='/data/{:in}' ftpServer='127.0.0.1' ftpUser='a1' ftpPassword='b1'"
			+ "   delayBetween='3' ftpServerPort='2121' ftpTimeout='10' />"
			// simple pattern 2
			+ "<ftpDownload name='pDown2' ftpServer='127.0.0.2' ftpUser='a2' ftpPassword='b2' delayBetween='4' "
			+ "ftpServerPort='2122' ftpTimeout='15' ftpActiveMode='false'/>"
			// complex pattern, use simple pattern pDown2 
			+ "<ftpDownload name='pChain' dstDir='@dstDir1/{:super}' >"
			+ "  <ftpUpload name='{:super}_to_backup' pattern='pDown2' ftpDstDir='/someDstDir/{:super}' />"
			+ "</ftpDownload>"
			+ "</patterns>";

	// use patterns pDown1, pChain: make chain from single agent
	public static final String PATTERN_CFG = "<param name='in' value='@srcDir1' />"
			+ "<param name='ftpDst' value='/some\\path' />"+PATTERNS
			+ " <ftpDownload name='main2' pattern='pDown1;pChain' applyMethod='set;add' dstDir='@dstDir1' cacheDir='@cacheDir1' >"
			+ "<param name=':in' value='mainRaw' />"
			+ "</ftpDownload>"
			// for test global params
			+ "<copy name='cp1' srcDir='{in}/cp1' dstDir='@dstDir1' />"
			+ "<ftpUpload name='up1' srcDir='@srcDir1' ftpDstDir='{ftpDst}' />";
	
	static final String AGENT = "testCopy";
//	static final String COPY0 = "<copy name = '"+AGENT+"' srcDir = '@srcDir' dstDir = '@dstDir' @opt >";
//	static final String COPY1 = "</copy>";
//	static final String COPY = "<copy name = '"+AGENT+"' srcDir = '@srcDir' dstDir = '@dstDir' @opt /> ";

//	+ " startFileDate = '2014-01-01 00:00:00' " ftpdown 

	
	String[][] subst;

	String srcDir1;
	String dstDir1;
	String cacheDir1;
	String srcDir2;
	String dstDir2;
	String cacheDir2;
	String srcDir3;
	String dstDir3;
	String cacheDir3;

	@Before
	public void setUp() throws Exception {
		srcDir1 = makeNewSubDir(SRC); 
		dstDir1 = makeNewSubDir(DST); 
		cacheDir1 = makeNewSubDir(CACHE);
		srcDir2 = makeNewSubDir(SRC+"2"); 
		dstDir2 = makeNewSubDir(DST+"2"); 
		cacheDir2 = makeNewSubDir(CACHE+"2");
		subst = new String[][] { 
			{"@srcDir1", srcDir1},
			{"@dstDir1", dstDir1},
			{"@cacheDir1", cacheDir1}, 
			{"@srcDir2", srcDir2},
			{"@dstDir2", dstDir2},
			{"@cacheDir2", cacheDir2} 
		};
		//cp = new Copy();
	}

	@After
	public void tearDown() throws Exception {
		clearAndRemoveDir(STAT_DATA_DIR);
		clearAndRemoveDir(CONF_DIR);
		clearAndRemoveDir(srcDir1);
		clearAndRemoveDir(srcDir2);
		clearAndRemoveDir(dstDir1);
		clearAndRemoveDir(dstDir2);
		clearAndRemoveDir(cacheDir1);
		clearAndRemoveDir(cacheDir2);
	}

	@Test
	public final void testAttributes() throws IOException, ParseException {
   		String cfg = makeConfig(BIGCFG, "", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName("big");
   		assertEquals(srcDir1, agent.getSrcDir());
   		assertEquals(dstDir1, agent.getDstDir());
   		assertEquals(cacheDir1, agent.getCacheDir());
   		assertEquals(7, agent.getDelayBetween().intValue());
   		assertEquals(3, agent.getKeepLastFiles().intValue());
   		assertEquals(4, agent.getDstFileLimit().intValue());
   		assertFalse(agent.getSaveTime().booleanValue());
   		assertFalse(agent.getDeleteAfter().booleanValue());
   		assertTrue(agent.getBatchMode().booleanValue());
   		assertFalse(agent.getCheckFtpFileLength().booleanValue());
   		assertEquals("Europe/Kaliningrad", agent.getDstTimeZone());
   		assertEquals("^test", agent.getFileMask());
   		assertTrue(agent.getGzip().booleanValue());
   		assertFalse(agent.getZip().booleanValue());
   		assertEquals(12*60*60, agent.getLagIntervalI().intValue());
   		assertEquals("aa", agent.getLocalParams().getValue(":dat"));
   		assertEquals("bb", agent.getLocalParams().getValue(":dat1"));
   		assertEquals("cc", agent.getLocalParams().getValue(":dat2"));
   		assertEquals("dd", agent.getLocalParams().getValue(":dat3"));
   		assertEquals("ee", agent.getLocalParams().getValue(":dat4"));
   		assertEquals(cacheDir1, agent.getPauseFlagFile());
   		assertFalse(agent.getPauseFlag().booleanValue());
   		assertEquals("yyyyMMdd", agent.getPrefix());
   		assertEquals(30*60, agent.getQueueWarnIntervalI().intValue());
   		assertEquals(50, agent.getQueueWarnOn().intValue());
   		assertEquals(40, agent.getQueueWarnOff().intValue());
   		assertFalse(agent.getReplaceExistingFiles().booleanValue());
   		assertEquals(7, agent.getSequenceLength().intValue());
   		assertTrue(agent.getSlowStop().booleanValue());
   		assertEquals("dir1;dir2", agent.getSrcSubDirs()); // !
   		assertEquals("Europe/Moscow", agent.getSrcTimeZone());
   		assertTrue(agent.getSubstTimeStamp().booleanValue());
   		assertEquals("s/^CDR//", agent.getSubstitute());
   		assertEquals("'.cdr'", agent.getSuffix());
   		assertEquals("'dat'yyyyMMdd", agent.getTimeBasedSrcDir());
   		assertEquals("(\\d{14}) , yyyyMMddHHmmss", agent.getTimeStampInName());
   		assertTrue(agent.getUnpack().booleanValue());
   		assertTrue(agent.getNonStrictOrder().booleanValue());
   		
   		//SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   		//assertEquals(sdf.parse("2014-01-01 00:00:00").getTime(), agent.getStartFileDateLong().longValue());

   		//applyMethod pattern
	}
	
	/*
	@Test
	public final void test() throws IOException {
   		File srcFile = makeFile(srcDir1, "s_file_1");
   		String cfg = makeConfig(COPY, "", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg copy = fc.getAgentByName(AGENT);
   		assertTrue(srcFile.exists());
   		assertTrue(copy.getCacheDir()!=null);
   		assertTrue(copy.getCacheDir().startsWith(copy.getDstDir()));
	}
*/
	
	@Test
	public final void makeIdxNameTest() {
		assertEquals("name.1", Cfg.makeIdxName("name", 1));
		assertEquals("name.2.gz", Cfg.makeIdxName("name.gz", 2));
		assertEquals("name.3.zip", Cfg.makeIdxName("name.zip", 3));
		assertEquals("name.4.ZIp", Cfg.makeIdxName("name.ZIp", 4));
		assertEquals("name.5.Gz", Cfg.makeIdxName("name.Gz", 5));
		assertEquals("namegz.6", Cfg.makeIdxName("namegz", 6));
		assertEquals("namezip.7", Cfg.makeIdxName("namezip", 7));
	}
	
	@Test
	public final void defaultValuesTest() throws IOException {
		String ext = " defaultLagInterval='25m' defaultQueueWarnInterval='1h' defaultQueueWarnOn='27' defaultQueueWarnOff='17' "
				+ " defaultSmartRemoteRename='false' defaultSaveTime='false' ";
   		String cfg = makeConfigExt(CHAIN_CFG, ext, "", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName("main");
   		assertEquals( 25*60, agent.getLagIntervalI().intValue());
   		assertEquals( 60*60, agent.getQueueWarnIntervalI().intValue());
   		assertEquals( 27, agent.getQueueWarnOn().intValue());
   		assertEquals( 17, agent.getQueueWarnOff().intValue());
   		assertNull(agent.getSmartRemoteRename()); // only for LOCAL2REMOTE
   		assertTrue(agent.is_smartRemoteRename());
   		assertFalse(agent.getSaveTime().booleanValue());
   		final Cfg child = agent.getChild();
   		assertEquals( 25*60, child.getLagIntervalI().intValue());
   		assertEquals( 60*60, child.getQueueWarnIntervalI().intValue());
   		assertNull( child.getQueueWarnOn()); // not first in the chain
   		assertEquals( 17, child.getQueueWarnOff().intValue());
   		assertFalse(child.getSmartRemoteRename().booleanValue());
   		assertFalse(child.getSaveTime().booleanValue());
	}

	@Test
	public final void patternsTest() throws IOException {
		String ext = "defaultSaveTime='false'";
   		String cfg = makeConfigExt(PATTERN_CFG, ext, "", subst);
   		final FileChief fc = FileChief.newInstance(cfg, false, false);
   		final Cfg agent = fc.getAgentByName("main2");
   		assertFalse(agent.getSaveTime().booleanValue());
   		assertEquals("127.0.0.1", agent.getFtpServer());
   		assertEquals(2121, agent.getFtpServerPort().intValue());
   		assertEquals("a1", agent.getFtpUser());
   		assertEquals("b1", agent.getFtpPassword());
   		assertEquals(3, agent.getDelayBetween().intValue());
   		assertEquals(10, agent.getFtpTimeout().intValue());
   		assertEquals("/data/mainRaw/", agent.getFtpSrcDir());
   		assertTrue(agent.getFtpActiveMode().booleanValue());
   		assertEquals(dstDir1, agent.getDstDir());
   		
   		final Cfg child = agent.getChild();
   		assertFalse(child.getSaveTime().booleanValue());
   		assertEquals("main2_to_backup", child.getName());
   		assertEquals("127.0.0.2", child.getFtpServer());
   		assertEquals(2122, child.getFtpServerPort().intValue());
   		assertEquals("a2", child.getFtpUser());
   		assertEquals("b2", child.getFtpPassword());
   		assertEquals(4, child.getDelayBetween().intValue());
   		assertEquals(15, child.getFtpTimeout().intValue());
   		assertEquals(child.getSrcDir(), agent.getDstDir());
   		assertFalse(child.getFtpActiveMode().booleanValue());
   		
   		final Cfg cp = fc.getAgentByName("cp1");
   		assertEquals(Paths.get(srcDir1, "cp1").toString(), cp.getSrcDir());
   		assertEquals(dstDir1, cp.getDstDir());
   		
   		final Cfg up = fc.getAgentByName("up1");
   		assertEquals(srcDir1, up.getSrcDir());
   		assertEquals("/some\\path/", up.getFtpDstDir());
   		
	}

	
}
