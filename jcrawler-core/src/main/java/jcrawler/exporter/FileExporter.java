package jcrawler.exporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import jcrawler.Envirenment;
import jcrawler.Page;
import jcrawler.Request;
import jcrawler.support.Jsons_;

/**
 * file的导出器
 * 
 * 目标是能支持对本地磁盘IO的读写操作。
 * 
 * @author warhin.wang
 *
 */
public class FileExporter implements Exporter {
	
	private static final Logger logger = LoggerFactory.getLogger(FileExporter.class);
	private static final String DEFAULT_SIGNATURE = Envirenment.DEFAULT_OUTPUT_FILENAME;
	
	/**
	 * 文件导出器输出目录
	 */
	private File target;
	
	/**
	 * 文件导出器写文件编码
	 */
	private String charset;
	
	/**
	 * BufferedWriter集合，key为request的signature，value为该类型signature对应的BufferedWriter
	 */
	private Map<String, BufferedWriter> bufferedWriters = new HashMap<String, BufferedWriter>();
	
	public FileExporter(String dir) throws IOException {
		this(dir, Charsets.UTF_8.toString());
	}
	
	public FileExporter(String dir, String charset) throws IOException {
		super();
		this.target = FileUtils.getFile(dir);
		if (!target.exists()) {
			FileUtils.forceMkdir(target);
		} else if (target.isFile()) {
			throw new IOException(dir + " is not a directory but a file!");
		} else if (!target.canWrite()) {
			throw new IOException(dir + " is a directory but cann't be writed!");
		}
		this.charset = Request.checkCharset(charset);
	}

	@Override
	public void close() throws IOException {
		for (BufferedWriter writer : bufferedWriters.values()) {
			IOUtils.closeQuietly(writer);
		}
	}

	@Override
	public void export(Page page) {
		if (page.skipPageItems()) {
			logger.info("the page items {} is ignore!" + page.getPageItems());
			return;
		}
		
		// request对象的signature属性在导出器为FileExporter类型时作为该类型request对应response数据的输出文件名称，不同类别的request可分别输出到不同的目标文件中，便于后续数据处理
		String signature = page.request().signature();
		if (StringUtils.isBlank(signature)) {
			signature = DEFAULT_SIGNATURE;
		}
		
		BufferedWriter writer = null;
		synchronized (bufferedWriters) {
			writer = bufferedWriters.get(signature);
			if (writer == null) {
				File fileToWrite = FileUtils.getFile(target, signature);
				try {
					writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileToWrite, true), charset));
				} catch (UnsupportedEncodingException | FileNotFoundException e) {
					logger.error("create bufferedWriter with name {} error!", signature, e);
					throw new ExportException("create bufferedWriter with name "+signature+" error!", e);
				}
				bufferedWriters.put(signature, writer);
			}
		}
		
		doExport(page, writer);
	}
	
	protected void doExport(Page page, BufferedWriter writer) {
		Map<String, Object> items = page.getPageItems();
		if (items != null && !items.isEmpty()) {
			try {
				IOUtils.write(Jsons_.toString(items), writer);
				writer.newLine();
			} catch (IOException e) {
				throw new ExportException("write data to target error!", e);
			}
		}
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("target", target).add("charset", charset).toString();
	}

}
