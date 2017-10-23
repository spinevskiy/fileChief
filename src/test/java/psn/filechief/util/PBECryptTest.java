package psn.filechief.util;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PBECryptTest {
	private static final String message = "TestMessageForEncrypt98765";
	private static final String message2 = "0123456789abcdef";
	private static final String message3 = "0123456789abc";
	private static final String password = "superPassword";

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEncryptDecrypt26() throws UnsupportedEncodingException, GeneralSecurityException {
		byte[] src = message.getBytes();
		byte[] encrypted = PBECrypt.encrypt(src, password.toCharArray());
		assertFalse(new String(encrypted).contains(message));
		int len = (src.length/16+1)*16;
		assertEquals(len + PBECrypt.IV_BYTES, encrypted.length);
		byte[] dst = PBECrypt.decrypt(encrypted, password.toCharArray());
		assertTrue(Arrays.equals(src, dst));
	}

	@Test
	public void testEncryptDecrypt16() throws UnsupportedEncodingException, GeneralSecurityException {
		byte[] src = message2.getBytes();
		byte[] encrypted = PBECrypt.encrypt(src, password.toCharArray());
		assertFalse(new String(encrypted).contains(message2));
		int len = (src.length/16+1)*16;
		assertEquals(len + PBECrypt.IV_BYTES, encrypted.length);
		byte[] dst = PBECrypt.decrypt(encrypted, password.toCharArray());
		assertTrue(Arrays.equals(src, dst));
	}

	@Test
	public void testEncryptDecrypt13() throws UnsupportedEncodingException, GeneralSecurityException {
		byte[] src = message3.getBytes();
		byte[] encrypted = PBECrypt.encrypt(src, password.toCharArray());
		assertFalse(new String(encrypted).contains(message3));
		int len = (src.length/16+1)*16;
		assertEquals(len + PBECrypt.IV_BYTES, encrypted.length);
		byte[] dst = PBECrypt.decrypt(encrypted, password.toCharArray());
		assertTrue(Arrays.equals(src, dst));
	}
	
}
