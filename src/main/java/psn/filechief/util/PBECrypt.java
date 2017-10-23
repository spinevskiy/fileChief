package psn.filechief.util;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PBECrypt {
	private static Logger log = LoggerFactory.getLogger(Utl.class.getName());
	
	private static final String KEY_FUNCTION = "PBKDF2WithHmacSHA1"; 
	private static final String ALG_CRYPT  = "AES";
	private static final String ALG_IV_GEN  = "SHA1PRNG";
	private static final String TRANSFORM    = "AES/CBC/PKCS5Padding";  
	
	private static final int ITERATIONS  = 1000;
	
	private static final int KEY_BITS = 128;
	public static final int IV_BITS = 128;
	public static final int IV_BYTES = IV_BITS / 8;
	
	/**
	 * Generates Initialization Vector.
	 * @throws GeneralSecurityException 
	 */
	private static byte[] generateIV() throws GeneralSecurityException {
		log.debug("generateIV");
		SecureRandom random = SecureRandom.getInstance(ALG_IV_GEN);
		byte[] iv = new byte[IV_BYTES];
		random.setSeed(System.nanoTime());
		random.nextBytes(iv);
		log.debug("random.nextBytes");
		return iv;
	}
	
	private static SecretKey deriveKey(char[] password, byte[] salt) throws GeneralSecurityException { 
		PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
		SecretKey pbeKey = null;
		byte[] keyBytes = null;

		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_FUNCTION);
			pbeKey = factory.generateSecret(pbeKeySpec);
			keyBytes = pbeKey.getEncoded();
			return new SecretKeySpec(keyBytes, ALG_CRYPT);
		}
		finally {
			pbeKeySpec.clearPassword();
			if (keyBytes != null)
				Arrays.fill(keyBytes, (byte) 0);
		}
	}

	public static byte[] concatArrays(byte[] a, byte[] b) {
		byte[] ret = new byte[a.length + b.length];
		System.arraycopy(a, 0, ret, 0, a.length);
		System.arraycopy(b, 0, ret, a.length, b.length);
		return ret;
	}

	public static byte[] concatArrays(byte[] a, byte[] b, byte[] c) {
		byte[] ret = new byte[a.length + b.length + c.length];
		System.arraycopy(a, 0, ret, 0, a.length);
		System.arraycopy(b, 0, ret, a.length, b.length);
		System.arraycopy(c, 0, ret, a.length+b.length, c.length);
		return ret;
	}
	
	private static byte[] crypt (Cipher cipher, byte[] message, boolean encrypt) throws GeneralSecurityException {
		int blocks = message.length/16;
		int xblocks = encrypt ? blocks+2 : blocks;
		byte[] out = new byte[xblocks*16];
		int pos = 0;
		for(int i = 0; i<blocks; i++)
			pos += cipher.update(message,i*16,16, out, pos);
		int off = blocks*16;
		int len = message.length-off;
		pos += cipher.doFinal(message,off,len, out, pos);
		return Arrays.copyOfRange(out, 0, pos); 
	}
	
	public static byte[] encrypt(byte[] message, char[] password) throws GeneralSecurityException 
	{
		byte[] iv = generateIV();
		SecretKey key = deriveKey(password, iv);
		Cipher cipher = Cipher.getInstance(TRANSFORM);
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
		byte[] ciphertext = crypt(cipher, message, true);
		return  concatArrays(iv, ciphertext);
	}

	public static byte[] decrypt(byte[] encrypted, char[] password) throws GeneralSecurityException {
		byte[] iv = Arrays.copyOfRange(encrypted, 0, IV_BYTES);
		byte[] ciphertext = Arrays.copyOfRange(encrypted, IV_BYTES, encrypted.length);
		SecretKey key = deriveKey(password, iv);
		Cipher cipher = Cipher.getInstance(TRANSFORM);
		cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
		return crypt(cipher, ciphertext, false);
	}

}
