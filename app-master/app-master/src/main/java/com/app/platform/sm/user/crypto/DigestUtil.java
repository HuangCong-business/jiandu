package com.app.platform.sm.user.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 与历史库密码字段对齐的摘要工具（UTF-8 输入、十六进制小写输出）。 */
public final class DigestUtil {

	private DigestUtil() {
	}

	public static String md5HexLower(String plain) {
		return digestHex("MD5", plain);
	}

	public static String sha256HexLower(String plain) {
		return digestHex("SHA-256", plain);
	}

	/** JDK MessageDigest 计算并格式化为连续小写 hex 字符串。 */
	private static String digestHex(String algorithm, String plain) {
		try {
			MessageDigest md = MessageDigest.getInstance(algorithm);
			byte[] raw = md.digest(plain.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(raw);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(algorithm, e);
		}
	}
}
