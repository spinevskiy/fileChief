package psn.filechief.util;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 
 * 1. Generate Initialization Vector (use SecureRandom)
 * 2. IV convert to gcmSpec
 * 3. make SecretKey from password and IV
 * 4. init cipher with secretKey and gcmSpec
 * 5. use the cipher: encode message  
 */
public class PBECrypt8 {
	private static final String KEY_FUNCTION = "PBKDF2WithHmacSHA256"; 
	private static final String ALG_CRYPT  = "AES";
	private static final String ALG_IV_GEN  = "SHA1PRNG";
	private static final String TRANSFORM    = "AES/GCM/NoPadding"; 

	private static final int ITERATIONS  = 1000;
	
	private static final int KEY_BITS = 128;
	public static final int IV_BITS = 128;
	public static final int AUTH_TAG_BITS = 128;
	
	public static final int IV_BYTES = IV_BITS / 8;
	public static final int AUTH_TAG_BYTES = AUTH_TAG_BITS / 8;

	/**
	 * Generates an Initialization Vector.
	 * @throws GeneralSecurityException 
	 */
	private static byte[] generateIV() throws GeneralSecurityException {
		SecureRandom random = SecureRandom.getInstance(ALG_IV_GEN);
		byte[] iv = new byte[IV_BYTES];
		random.nextBytes(iv);
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

	private static Cipher initCipher(int mode, char[] password, byte[] iv) throws GeneralSecurityException 
	{
		GCMParameterSpec gcmSpec = new GCMParameterSpec(AUTH_TAG_BITS, iv);
		SecretKey key = deriveKey(password, iv);
		Cipher cipher = Cipher.getInstance(TRANSFORM);
		cipher.init(mode, key, gcmSpec);
		return cipher;
	}

	private static byte[] doFinal(int mode, char[] password, byte[] iv, byte[] message) throws GeneralSecurityException
	{
		Cipher cipher = initCipher(mode, password, iv);
		return cipher.doFinal(message);
	}

	public static byte[] encrypt(byte[] message, char[] password) throws GeneralSecurityException 
	{
		byte[] iv = generateIV();
		byte[] ciphertext = doFinal(Cipher.ENCRYPT_MODE, password, iv, message);
		byte[] result = new byte[iv.length + ciphertext.length];
		System.arraycopy(iv, 0, result, 0, iv.length);
		System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
		return result;
	}

	public static byte[] decrypt(byte[] encrypted, char[] password) throws GeneralSecurityException {
		byte[] iv = Arrays.copyOfRange(encrypted, 0, IV_BYTES);
		byte[] ciphertext = Arrays.copyOfRange(encrypted, IV_BYTES, encrypted.length);
		return doFinal(Cipher.DECRYPT_MODE, password, iv, ciphertext);
	}

}
