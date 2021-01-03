package biz.shujutech.db.relational;

import biz.shujutech.base.App;
import biz.shujutech.base.Hinderance;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class FieldEncrypt extends FieldStr {
	private static final String DEFAULT_ENCRYPTING_KEY = "s2h0u1j7u0t6e1c8h";
	private static final int MAX_KEY_SIZE = 16; // must be multiple of 8
	private static final String ENCRYPTING_PROPERTY = "Systm.keyEncrypt";
	private static final int MAX_ENCRYPTED_SIZE = 64; // should only need 43 bytes, https://stackoverflow.com/questions/13378815/base64-length-calculation
	private static final int MAX_STR_SIZE = 32; // must be multiple of 8

	public FieldEncrypt(String aName) {
		super(aName, MAX_ENCRYPTED_SIZE);
		this.setFieldType(FieldType.ENCRYPT);
	}

	@Override
	public String getValueStr() throws Exception {
		String result;
		if (super.getValueStr() == null || super.getValueStr().isEmpty()) {
			result = super.getValueStr();	
		} else {
			String strKey = App.GetValue(ENCRYPTING_PROPERTY, DEFAULT_ENCRYPTING_KEY);
			result = Decrypt(super.getValueStr(), strKey);
		}
		return(result);
	}

	@Override
	public void setValue(Object value) throws Exception {
		this.setValueStr((String) value); // must call this set method so setModified is call
	}

	public void setEncryptedValue(String value) throws Exception {
		super.setValueStr((String) value); // must call this set method so setModified is call
	}

	@Override
	public void setValueStr(String value) throws Exception {
		if (value.length() > MAX_STR_SIZE) throw new Hinderance("Encrypted field length cannot be more than " + MAX_STR_SIZE + " char");
		String strKey = App.GetValue(ENCRYPTING_PROPERTY, DEFAULT_ENCRYPTING_KEY);
		String encryptedValue = Encrypt(value, strKey);
		super.setValueStr(encryptedValue);
	}

	public static byte[] Str2ByteOfSize(String aStr, int aMaxSize) throws Exception {
		byte[] result = new byte[aMaxSize];
		byte[] strInput = aStr.getBytes();
		for (int cntr = 0; cntr < strInput.length && cntr < aMaxSize; cntr++) {
			result[cntr] = strInput[cntr];
		}
		return(result);
	}

	public static String Encrypt(String strClearText, String strKey) throws Exception{
		String result;
		SecretKeySpec skeyspec = new SecretKeySpec(Str2ByteOfSize(strKey, MAX_KEY_SIZE), "Blowfish");
		Cipher cipher = Cipher.getInstance("Blowfish");
		cipher.init(Cipher.ENCRYPT_MODE, skeyspec);
		byte[] encrypted = cipher.doFinal(Str2ByteOfSize(strClearText, MAX_STR_SIZE));
		result = Base64.getEncoder().encodeToString(encrypted);
		return(result);
	}

	public static String Decrypt(String strEncrypted, String strKey) throws Exception{
		String result;
		SecretKeySpec skeyspec = new SecretKeySpec(Str2ByteOfSize(strKey, MAX_KEY_SIZE), "Blowfish");
		Cipher cipher = Cipher.getInstance("Blowfish");
		cipher.init(Cipher.DECRYPT_MODE, skeyspec);
		byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(strEncrypted));
		result = new String(TrimNull(decrypted));
		return(result);
	}

	public static byte[] TrimNull(byte[] bytes) {
		int cntr = bytes.length - 1;
		while (cntr >= 0 && bytes[cntr] == 0) {
				--cntr;
		}
		return Arrays.copyOf(bytes, cntr + 1);
	}

}

