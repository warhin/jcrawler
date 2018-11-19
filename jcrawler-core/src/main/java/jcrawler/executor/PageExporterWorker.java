package jcrawler.executor;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jcrawler.Envirenment;
import jcrawler.Page;
import jcrawler.exporter.Exporter;
import jcrawler.support.Threads;

public class PageExporterWorker extends Stopable implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(PageExporterWorker.class);
  
  private BlockingQueue<Page> pageQueue;
  
  private List<Exporter> exporters;
  
  private long pageExporterPauseMills;

  public PageExporterWorker(BlockingQueue<Page> pageQueue, List<Exporter> exporters,
      long pageExporterPauseMills) {
    super();
    this.pageQueue = pageQueue;
    this.exporters = exporters;
    this.pageExporterPauseMills = pageExporterPauseMills;
  }

  @Override
  public void run() {
    while (true) {
      // 被显式终止时，执行线程退出。
      if (isStop()) {
        break;
      }

      // 从pageQueue中取出一个page对象，如果经过指定时间后未取到page对象，循环该过程直到取到为止。
      Page page = null;
      try {
        page = pageQueue.poll(Envirenment.DEFAULT_PAGE_PULL_TIMEOUT, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      // 只有在当前线程本次循环中未取到page对象时才考虑要不要休眠暂停：pageExporterPauseMills需要显式设置过大于0，以此防止exporter线程空转浪费资源
      if (page == null) {
        Threads.sleep(pageExporterPauseMills, true);
        continue;
      }

      // 对于取到的page对象，依次通过每一个Exporter执行其export过程。
      for (Exporter exporter : exporters) {
        try {
          exporter.export(page);
          logger.info("export page success. page : {} ", page);
        } catch (Exception e) {
          logger.error("export page error : ", e);
        }
      }
    }
  }

}
