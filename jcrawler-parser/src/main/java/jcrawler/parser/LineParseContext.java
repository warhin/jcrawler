package jcrawler.parser;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * 可基于文件读写的一套单线程版的数据处理方案。
 * 
 * usage sample1：
 * LineParseContext.build(1000).from(fileIn, "UTF-8").to(fileOut, "UTF-8")
 *				.parse(new LineParseContext.LineTransformer() {
 *					List<String> transform(String input) {
 *						//...
 *					}
 *				});
 *
 * usage sample2：
 * LineParseContext.build(1000).setLineProducer(lineProducer).setLineConsumer(lineConsumer)
 *				.parse(new LineParseContext.LineTransformer() {
 *					List<String> transform(String input) {
 *						//...
 *					}
 *				});
 * 
 * @author warhin wang
 * 
 */
public class LineParseContext {
	
	private int lineSize;
	
	private LineProducer lineProducer;
	
	private LineConsumer lineConsumer;

	private LineParseContext(int lineSize) {
		super();
		this.lineSize = lineSize;
	}

	/**
	 * 建立LineParseContext对象
	 * @param lineSize 内存中数据队列长度，缓存后一次性写出到指定输出文件
	 * @return LineParseContext
	 */
	public static LineParseContext build(int lineSize) {
		// 检验line buffer大小
		checkArgument(lineSize > 10, "The param lineSize should be greater then 10, but accept %s.", lineSize);
		return new LineParseContext(lineSize);
	}
	
	/**
	 * 通过指定数据来源文件继续构造LineParseContext对象
	 * @param fileIn 从哪个文件读入原始数据
	 * @param readEncoding 以何种编码读入字符
	 * @return LineParseContext
	 */
	public LineParseContext from(File fileIn, String readEncoding) {
		// 校验读文件编码
		Charsets.toCharset(readEncoding);
		// 校验not null
		checkNotNull(fileIn);
		// 校验是文件且可读
		LineIterator iter = null;
		try {
			iter = FileUtils.lineIterator(fileIn, readEncoding);
		} catch (IOException e) {
			Throwables.propagate(e);
		}
		this.lineProducer = new SingleLineProducer(iter);
		return this;
	}
	
	/**
	 * 通过指定数据目的地文件继续构造LineParseContext对象
	 * @param fileOut 处理后的数据写出到哪个文件
	 * @param writeEncoding 以何种编码写出字符
	 * @return LineParseContext
	 */
	public LineParseContext to(File fileOut, String writeEncoding) {
		// 校验写文件编码
		Charsets.toCharset(writeEncoding);
		// 校验not null
		checkNotNull(fileOut);
		// 校验是文件且可写
		FileOutputStream out = null;
		try {
			out = FileUtils.openOutputStream(fileOut);
		} catch (IOException e) {
			Throwables.propagate(e);
		} finally {
			IOUtils.closeQuietly(out);
		}
		this.lineConsumer = new SingleLineConsumer(lineSize, fileOut, writeEncoding);
		return this;
	}
	
	/**
	 * 对from(File fileIn, String readEncoding)方法的替换，比如跨多行LineProducer的场景需要自定义，其他非文件来源的数据也需要自定义等
	 * @param lineProducer
	 * @return LineParseContext
	 */
	public LineParseContext setLineProducer(LineProducer lineProducer) {
		checkNotNull(lineProducer);
		this.lineProducer = lineProducer;
		return this;
	}

	/**
	 * 对to(File fileOut, String writeEncoding)方法的替换，比如输出到非文件的其他目的地需要自定义LineConsumer等
	 * @param lineConsumer
	 * @return LineParseContext
	 */
	public LineParseContext setLineConsumer(LineConsumer lineConsumer) {
		checkNotNull(lineConsumer);
		this.lineConsumer = lineConsumer;
		return this;
	}

	/**
	 * 启动“读数据源到内存-->内存中处理源数据到数据队列-->写数据队列到目的地”流程，直到数据源中全部line处理完毕后自动关闭所有数据流
	 * @param lineTransformer 对单行字符串表示的原始数据做出转换到数据列表的具体实现
	 */
	public void parse(final LineTransformer lineTransformer) {
		checkNotNull(this.lineProducer, "lineProducer is null!");
		checkNotNull(this.lineConsumer, "lineConsumer is null!");
		try {
			String input = null;
			List<String> outputs = null;
			while (this.lineProducer.hasNext()) {
				try {
					input = this.lineProducer.produce();
					if (StringUtils.isBlank(input)) {
						continue;
					}
					outputs = lineTransformer.transform(input);
					if (outputs == null || outputs.isEmpty()) {
						continue;
					}
					for (String output : outputs) {
						if (StringUtils.isNotBlank(output)) {
							this.lineConsumer.consume(output);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			this.lineProducer.close();
			this.lineConsumer.close();
		}
	}

	/**
	 * 数据产生源
	 * 
	 * 可以来自于文件，可以来自于应用程序等
	 * 
	 */
	public static interface LineProducer {
		/**
		 * 判断数据源是否还有下一条数据
		 * @return true if has next, false otherwise
		 */
		boolean hasNext();
		
		/**
		 * 获取下一条数据
		 * @return 生产下一条数据
		 */
		String produce();
		
		/**
		 * 关闭数据源
		 */
		void close();
	}
	
	/**
	 * 数据消费目的地
	 * 
	 * 可以输出到文件，也可以输出到应用程序等
	 *
	 */
	public static interface LineConsumer {
		/**
		 * 消费生产的单条数据
		 * @param data 待消费数据
		 * @throws IOException
		 */
		void consume(String data) throws IOException;
		
		/**
		 * 关闭数据目的地
		 */
		void close();
	}
	
	/**
	 * 数据转换器
	 * 
	 * 将源数据转换处理后输出到数据列表
	 * 
	 */
	public static interface LineTransformer {
		/**
		 * 实现类可实现所有对输入数据的处理逻辑
		 * 
		 * @param input 待处理源数据
		 * @return 处理后数据列表
		 */
		List<String> transform(String input);
	}
	
	public static class SingleLineProducer implements LineProducer {
		
		private LineIterator lineIterator;
		
		private SingleLineProducer(LineIterator lineIterator) {
			super();
			this.lineIterator = checkNotNull(lineIterator);
		}

		public boolean hasNext() {
			return lineIterator.hasNext();
		}

		public String produce() {
			return lineIterator.next();
		}

		public void close() {
			lineIterator.close();
		}
		
	}
	
	public static class SingleLineConsumer implements LineConsumer {

		private int lineSize;
		private File fileOut;
		private String writeEncoding;
		private List<String> lineBuffer;
		private static final String LINEENDING = SystemUtils.LINE_SEPARATOR;
		private static final boolean APPEND = true;
		
		private SingleLineConsumer(int lineSize, File fileOut, String writeEncoding) {
			super();
			this.lineSize = lineSize;
			this.fileOut = fileOut;
			this.writeEncoding = writeEncoding;
			this.lineBuffer = Lists.newArrayListWithExpectedSize(lineSize);
		}

		public void consume(String data) throws IOException {
			lineBuffer.add(data);
			if (lineBuffer.size() >= lineSize) {
				try {
					FileUtils.writeLines(fileOut, writeEncoding, lineBuffer, LINEENDING, APPEND);
				} finally {
					lineBuffer.clear();
				}
			}
		}
		
		public void close() {
			if (!lineBuffer.isEmpty()) {
				try {
					FileUtils.writeLines(fileOut, writeEncoding, lineBuffer, LINEENDING, APPEND);
				} catch (IOException e) {
					Throwables.propagate(e);
				} finally {
					lineBuffer.clear();
				}
			}
		}
		
	}
	
	public static class SingleLineTransformer implements LineTransformer {

		public List<String> transform(String input) {
			return Collections.singletonList(input);
		}
		
	}

}
