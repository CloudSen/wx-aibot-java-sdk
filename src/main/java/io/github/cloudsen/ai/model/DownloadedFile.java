package io.github.cloudsen.ai.model;

/**
 * 下载并解密后的文件内容。
 */
public class DownloadedFile {
	private final byte[] buffer;
	private final String filename;

	public DownloadedFile(byte[] buffer, String filename) {
		this.buffer = buffer;
		this.filename = filename;
	}

	public byte[] buffer() {
		return buffer;
	}

	public String filename() {
		return filename;
	}
}
