package psn.filechief.util;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SubstituteWithTimeTest {
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	
	@Test
	public void test() throws ParseException {
		String format = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		String sample = "2017-01-31 23:53:21";
		Date d = sdf.parse(sample);
		
		String substitute = "s/^Info_(.*)$/yyyyMMdd_HHmmss_'$1'/";
		SubstituteWithTime swt = new SubstituteWithTime(substitute, true, TimeZone.getDefault());
		String res = swt.substitute("Info_cdr2876.dat", d);
		assertTrue(res.equals("20170131_235321_cdr2876.dat"));

		substitute = "s/^Info_//";
		swt = new SubstituteWithTime(substitute, false, null);
		res = swt.substitute("Info_cdr2876.dat", d);
		assertTrue(res.equals("cdr2876.dat"));
	}

}
