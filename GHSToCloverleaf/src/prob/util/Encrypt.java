package prob.util;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import com.sun.jersey.core.util.Base64;

public class Encrypt {
	//Variables
	private static String ALGORITHM = "AES";
	private static byte[] keyValue = "8%jK3$2)8%jK3$2)".getBytes();
	
	//Getter/Setter methods
	public static String getAlthorithm(){
		return ALGORITHM;
	}
	public static void setAlgorithm(String algorithm){
		ALGORITHM = algorithm;
	}
	/**
	 * Change the default encryption key to be used for this class by using this method.
	 * @param new_key The new encryption key. The requirements for this key changes depending on the algorithm used. Default algorithm is AES.
	 */
	public static void setEncryptionKey(String new_key){
		keyValue = new_key.getBytes();
	}
	
	
    public static String encrypt(String valueToEnc){
        Key key = generateKey();
        Cipher c;
        String encryptedValue = null;
		try {
			c = Cipher.getInstance(ALGORITHM);
	        c.init(Cipher.ENCRYPT_MODE, key);
	        byte[] encValue = c.doFinal(valueToEnc.getBytes("UTF-8"));
	        byte[] encryptedByteValue = Base64.encode(encValue);
	        encryptedValue = new String(encryptedByteValue,"UTF-8");
		} catch (Exception e) {
			throw new RuntimeException("Error encrypting string.");
		}
		return encryptedValue;
    }

    public static String decrypt(String encryptedValue) {
        Key key = generateKey();
        byte[] decryptedVal = null;
        try{
	        Cipher c = Cipher.getInstance(ALGORITHM);
	        c.init(Cipher.DECRYPT_MODE, key);
	        byte[] decodedValue = Base64.decode(encryptedValue.getBytes());
	        decryptedVal = c.doFinal(decodedValue);
	        return new String(decryptedVal);
        } catch (Exception e) {
			throw new RuntimeException("Error decrypting string.");
		}
    }
    
    private static Key generateKey(){
        Key key = new SecretKeySpec(keyValue, ALGORITHM);
        return key;
    }
}
