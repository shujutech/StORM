package biz.shujutech.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class CryptoRsa {
	public static final String CRYPT_KEY = "Crypt-key";
	public static String BASE64_PRIVATE_KEY = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAIbHNotbUy5muk7j81xYiJ/bAEWw48Gr1+ifE81ioywO0KnXEOWIFf7Znahq03Qe0GKfGQamrq51rRFnrYh5zB13gTJDmxX24+cJREdV0HCZLaUyTdoLsGqs5F+4iDMbmTHIt3qHkQobAWXYkmfefdDw0xs6Kf2rjw4yWLtDbGPPAgMBAAECgYAycLXiG7Cayglp4ve+FMg1A1IrwtLKNZTdU0Om9wZYQ8cl+P4y2OZadXkCgdBXQfz9G8vS6GixlArMW7/fNq4Dwg/Sq8AT8Sl9Z65O3G9M5IFYo5UMIw/tu3m22pW5yeelDlJ6vDi7ZKRaHf6pQgukId9xLvDklzNS2MeTK2C0IQJBAL9oh6B0K5p5vqaxV83uwZ86hR9T1BxW3fejRQ08eOsJbLhEX5ENFtOubtxw0d2g8jBiWXgUMXo5ZkjH9ypQhhECQQC0Qn4IMH1m06di8Tjgq+f85qlT4/4bD6+N9oZkJd3ebLCYwesJhGc0Kd1QxWDujnZBu+KfK8X966RXd7zK1uvfAkBXwLada2UqNzKe/aNBuHpwY8XwbOBL9c9h3yqjPNw69WEwwbgGqjeS6N/OlLLCpy0h3ZUjJi7g2Dv6liLM9YnhAkBGeTB41yxPxTPN1O0duVerYiqVJ6+yuMEzyUvlX4CMcwXf0wCm6eLHXA6wg1pQKdZGAMSq4hw4F6Zao+9lhf0VAkAUGps+nqZloBROCcxTAlsc/KrqldXv0WKhfsBE/4d/LQ/nfh83pRj7NBEcblp/kgrlNhtsl2jf/sat07sKAYpe";
	public static String BASE64_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCGxzaLW1MuZrpO4/NcWIif2wBFsOPBq9fonxPNYqMsDtCp1xDliBX+2Z2oatN0HtBinxkGpq6uda0RZ62Iecwdd4EyQ5sV9uPnCURHVdBwmS2lMk3aC7BqrORfuIgzG5kxyLd6h5EKGwFl2JJn3n3Q8NMbOin9q48OMli7Q2xjzwIDAQAB";

	private Cipher cipher;

	public CryptoRsa() throws NoSuchAlgorithmException, NoSuchPaddingException {
		this.cipher = Cipher.getInstance("RSA");
	}

	public PrivateKey getPrivateFromStr(String aEncodedStr) throws Exception {
		byte[] keyBytes =  Base64.getDecoder().decode(aEncodedStr);
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}

	//https://docs.oracle.com/javase/8/docs/api/java/security/spec/PKCS8EncodedKeySpec.html
	public PrivateKey getPrivate(String filename) throws Exception {
		byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}

	public PublicKey getPublicFromStr(String aEncodedStr) throws Exception {
		byte[] keyBytes =  Base64.getDecoder().decode(aEncodedStr);
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePublic(spec);
	}

	//https://docs.oracle.com/javase/8/docs/api/java/security/spec/X509EncodedKeySpec.html
	public PublicKey getPublic(String filename) throws Exception {
		byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePublic(spec);
	}

	public void encryptFile(byte[] input, File output, PrivateKey key)
		throws IOException, GeneralSecurityException {
		this.cipher.init(Cipher.ENCRYPT_MODE, key);
		writeToFile(output, this.cipher.doFinal(input));
	}

	public void decryptFile(byte[] input, File output, PublicKey key) throws IOException, GeneralSecurityException {
		this.cipher.init(Cipher.DECRYPT_MODE, key);
		writeToFile(output, this.cipher.doFinal(input));
	}

	private void writeToFile(File output, byte[] toWrite) throws IllegalBlockSizeException, BadPaddingException, IOException {
		FileOutputStream fos = new FileOutputStream(output);
		fos.write(toWrite);
		fos.flush();
		fos.close();
	}

	public String encryptText(String msg, PublicKey key) throws NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
		this.cipher.init(Cipher.ENCRYPT_MODE, key);
		return Base64.getEncoder().encodeToString(cipher.doFinal(msg.getBytes("UTF-8")));
	}

	public String encryptText(String msg, String aBase64Key) throws NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, Exception {
		PublicKey publicKey = getPublicFromStr(aBase64Key);
		String result = encryptText(msg, publicKey);
		return(result);
	}

	public String decryptText(String msg, PrivateKey key) throws InvalidKeyException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException {
		this.cipher.init(Cipher.DECRYPT_MODE, key);
		return new String(cipher.doFinal(Base64.getDecoder().decode(msg)));
	}

	public String decryptText(String msg, String aBase64Key) throws InvalidKeyException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, Exception {
		PrivateKey privateKey = getPrivateFromStr(aBase64Key);
		String result = decryptText(msg, privateKey);
		return(result);
	}

	public String decryptText(String encryptedText) throws Exception {
		return(decryptText(encryptedText, BASE64_PRIVATE_KEY));
	}

	public String encryptText(String plainText) throws Exception {
		return(encryptText(plainText, BASE64_PUBLIC_KEY));
	}

	public byte[] getFileInBytes(File aFile) throws IOException {
		FileInputStream fis = new FileInputStream(aFile);
		byte[] fbytes = new byte[(int) aFile.length()];
		fis.read(fbytes);
		fis.close();
		return fbytes;
	}

	public static void main(String[] args) throws Exception {
		CryptoRsa cryptoRsa = new CryptoRsa();

		String msg = "This is testing cryptography";
		String encryptedMsg = cryptoRsa.encryptText(msg, BASE64_PUBLIC_KEY);
		String decryptedMsg = cryptoRsa.decryptText(encryptedMsg, BASE64_PRIVATE_KEY);
		System.out.println("Original Message: " + msg + "\nEncrypted Message: " + encryptedMsg + "\nDecrypted Message: " + decryptedMsg);

	}
}