package jcrawler.parser;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Throwables;

import jcrawler.parser.LineParseContext.LineTransformer;

/**
 * 基于文件读写的一套多线程版的数据处理方案。
 * 
 * usage sample：
 * LineParseContext.build(6).from(fileIn, "UTF-8").to(fileOut, "UTF-8")
 *				.parse(new LineParseContext.LineTransformer() {
 *					List<String> transform(String input) {
 *						//...
 *					}
 *				});
 * 
 * @author warhin wang
 * 
 */
public class LineParseContext2 {
	
	private int nThreads;
	
	private BufferedReader reader;
	
	private BufferedWriter writer;

	private LineParseContext2(int nThreads) {
		super();
		this.nThreads = nThreads;
	}
	
	public static LineParseContext2 build(int nThreads) {
		// 检验threads大小
		checkArgument(nThreads > 2, "The param nThreads should be greater then 2, but accept %s.", nThreads);
		return new LineParseContext2(nThreads);
	}
	
	public LineParseContext2 from(File fileIn, String readEncoding) {
		// 校验读文件编码
		Charsets.toCharset(readEncoding);
		// 校验not null
		checkNotNull(fileIn);
		try {
			this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileIn), readEncoding));
		} catch (UnsupportedEncodingException e) {
			Throwables.propagate(e);
		} catch (FileNotFoundException e) {
			Throwables.propagate(e);
		}
		return this;
	}
	
	public LineParseContext2 to(File fileOut, String writeEncoding) {
		// 校验写文件编码
		Charsets.toCharset(writeEncoding);
		// 校验not null
		checkNotNull(fileOut);
		try {
			this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileOut), writeEncoding));
		} catch (UnsupportedEncodingException e) {
			Throwables.propagate(e);
		} catch (FileNotFoundException e) {
			Throwables.propagate(e);
		}
		return this;
	}
	
	public void parse(final LineTransformer lineTransformer) {
		checkNotNull(this.reader, "reader is null!");
		checkNotNull(this.writer, "writer is null!");
		ExecutorService executor = Executors.newFixedThreadPool(nThreads);
		BlockingQueue<String> queueIn = new LinkedBlockingQueue<String>();
		BlockingQueue<String> queueOut = new LinkedBlockingQueue<String>();
		// start read task
		CountDownLatch readerCount = new CountDownLatch(1);
		ReaderTask readerTask = new ReaderTask(reader, queueIn, readerCount);
		executor.submit(readerTask);
		// start work tasks
		CountDownLatch workerCount = new CountDownLatch(nThreads - 2);
		for (int i = 0, counter = nThreads - 2; i < counter; i++) {
			WorkerTask workerTask = new WorkerTask(queueIn, queueOut, readerCount, workerCount, lineTransformer);
			executor.submit(workerTask);
		}
		// start write task
		CountDownLatch writerCount = new CountDownLatch(1);
		WriterTask writerTask = new WriterTask(writer, queueOut, workerCount, writerCount);
		executor.submit(writerTask);
		// await and stop
		try {
			readerCount.await();
			workerCount.await();
			writerCount.await();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			executor.shutdown();
			try {
				executor.awaitTermination(3, TimeUnit.MINUTES);
			} catch (Exception e) {
				e.printStackTrace();
			}
			IOUtils.closeQuietly(reader);
			IOUtils.closeQuietly(writer);
		}
	}
	
	public static class ReaderTask implements Runnable {
		private BufferedReader reader;
		private BlockingQueue<String> readerQueue;
		private CountDownLatch readerCountDownLatch;
		
		public ReaderTask(BufferedReader reader,
				BlockingQueue<String> readerQueue,
				CountDownLatch readerCountDownLatch) {
			super();
			this.reader = reader;
			this.readerQueue = readerQueue;
			this.readerCountDownLatch = readerCountDownLatch;
		}

		public void run() {
			try {
				String line = null;
				// 从源文件中读入每一行原始数据存入第一个队列
				while (true) {
					try {
						line = reader.readLine();
						if (line == null) break;
						readerQueue.put(line);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} finally {
				readerCountDownLatch.countDown();
			}
		}
	}
	
	public static class WorkerTask implements Runnable {
		private BlockingQueue<String> readerQueue;
		private BlockingQueue<String> writerQueue;
		private CountDownLatch readerCountDownLatch;
		private CountDownLatch workerCountDownLatch;
		private LineTransformer lineTransformer;

		public WorkerTask(BlockingQueue<String> readerQueue,
				BlockingQueue<String> writerQueue,
				CountDownLatch readerCountDownLatch,
				CountDownLatch workerCountDownLatch,
				LineTransformer lineTransformer) {
			super();
			this.readerQueue = readerQueue;
			this.writerQueue = writerQueue;
			this.readerCountDownLatch = readerCountDownLatch;
			this.workerCountDownLatch = workerCountDownLatch;
			this.lineTransformer = lineTransformer;
		}

		public void run() {
			try {
				while (true) {
					try {
						if (readerQueue.isEmpty() && readerCountDownLatch.getCount() <= 0L) {
							break;
						}
						// 从第一个队列读入
						String input = readerQueue.poll(20, TimeUnit.MILLISECONDS);
						if (StringUtils.isBlank(input)) {
							continue;
						}
						// 处理input到outputs
						List<String> outputs = null;
						try {
							outputs = lineTransformer.transform(input);
						} catch (Exception e) {
							e.printStackTrace();
						}
						// 将处理结果放到第二个队列中
						if (outputs == null || outputs.isEmpty()) {
							continue;
						}
						for (String output : outputs) {
							if (StringUtils.isNotBlank(output)) {
								writerQueue.put(output);
							}
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} finally {
				workerCountDownLatch.countDown();
			}
		}
	}

	public static class WriterTask implements Runnable {
		private BufferedWriter writer;
		private BlockingQueue<String> writerQueue;
		private CountDownLatch workerCountDownLatch;
		private CountDownLatch writerCountDownLatch;
		
		public WriterTask(BufferedWriter writer,
				BlockingQueue<String> writerQueue,
				CountDownLatch workerCountDownLatch,
				CountDownLatch writerCountDownLatch) {
			super();
			this.writer = writer;
			this.writerQueue = writerQueue;
			this.workerCountDownLatch = workerCountDownLatch;
			this.writerCountDownLatch = writerCountDownLatch;
		}

		public void run() {
			try {
				while (true) {
					try {
						if (writerQueue.isEmpty() && workerCountDownLatch.getCount() <= 0L) {
							break;
						}
						// 从第二个队列取出处理后的数据写出到目标文件
						String rst = writerQueue.poll(20, TimeUnit.MILLISECONDS);
						if (StringUtils.isNotBlank(rst)) {
							writer.write(rst);
							writer.write(IOUtils.LINE_SEPARATOR);
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} finally {
				writerCountDownLatch.countDown();
			}
		}
	}

}
