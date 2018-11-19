package jcrawler.executor;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jcrawler.Envirenment;
import jcrawler.Page;
import jcrawler.Request;
import jcrawler.Response;
import jcrawler.extractor.Extractor;
import jcrawler.fetcher.Fetcher;
import jcrawler.fetcher.HttpFetcherFactory;
import jcrawler.support.Threads;

public class SpiderWorker extends Stopable implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(SpiderWorker.class);
  
  private BlockingQueue<Request> requestQueue;
  
  private BlockingQueue<Page> pageQueue;
  
  private Fetcher fetcher;
  
  private Extractor extractor;
  
  private int pageHolderThresholds;
  
  private long pageSuplierPauseMills;

  public SpiderWorker(BlockingQueue<Request> requestQueue, BlockingQueue<Page> pageQueue,
      Fetcher fetcher, Extractor extractor, int pageHolderThresholds, long pageSuplierPauseMills) {
    super();
    this.requestQueue = requestQueue;
    this.pageQueue = pageQueue;
    this.fetcher = fetcher;
    this.extractor = extractor;
    this.pageHolderThresholds = pageHolderThresholds;
    this.pageSuplierPauseMills = pageSuplierPauseMills;
  }

  @Override
  public void run() {
    while (true) {
      // 被显式终止时，执行线程退出。
      if (isStop()) {
        break;
      }

      // pageQueue中存储的待导出page集合数量超出阀值后暂停crawler工作，休眠指定pageSuplierPauseMills时长后再判断是否继续。以此控制爬虫的速度和爬虫系统负载。
      if (pageQueue.size() >= pageHolderThresholds) {
        logger.warn(
            "crawler run faster than page exporter(the pageQueue current size {} >= the pageQueue thresholds {}), please adjust the threads relationed!",
            pageQueue.size(), pageHolderThresholds);
        Threads.sleep(pageSuplierPauseMills, true);
        continue;
      }

      // 从RequestHolder中取出一个待爬取的Request对象，如果未取到，循环该过程直到取到为止。
      Request request = null;
      try {
        request =
            requestQueue.poll(Envirenment.DEFAULT_REQUEST_PULL_TIMEOUT, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      if (request == null || !request.validate()) {
        continue;
      }

      // 使用指定的fetcher对象下载该request对象，得到一个page对象，如果下载的page对象有误，判断是否需要重试
      Page page = fetch(request);
      if (page == null || page.hasError()) {
        continue;
      }

      // 如果下载的page对象无误，根据指定的extractor对象(如果用户指定了)处理page
      extract(page);
      if (page.skipPageItems()) {
        continue;
      }

      // 将处理后的page对象存入PageQueue，待下游线程进一步处理
      try {
        pageQueue.offer(page, Envirenment.DEFAULT_PAGE_PUSH_TIMEOUT, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      // 一次爬取结束后根据site的sleepTime配置决定是否需要暂停一段时间，以控制爬取频率：为0时表示不休息，尽可能努力抓取；值越大表明停歇时间越长，可以防反爬虫策略
      long sleepTime = (page.site() == null) ? 0 : page.site().sleepTime();
      Threads.sleep(sleepTime, true);
    }
  }

  private Page fetch(Request request) {
    Page page = Page.create().request(request);
    try {
      Fetcher fetcherToUse = fetcher;
      if (fetcherToUse == null) {
        fetcherToUse = HttpFetcherFactory.getInstance().getHttpFetcher(request.site());
      }
      Response response = fetcherToUse.fetch(request);
      page.response(response);
      logger.info("fetch response from request success : the request is {}, the response is {}.",
          request, response);
    } catch (Exception e) {
      logger.error("Fetch response error : ", e);
      page.exception(e);
    }
    return page;
  }

  private void extract(Page page) {
    if (extractor == null) {
      return;
    }

    try {
      this.extractor.extract(page);
      if (!page.skipPageItems() && page.hasPageItems()) {
        logger.info("extract page items success : {}", page.getPageItems());
      }
    } catch (Exception e) {
      logger.error("Extract page error : ", e);
    }

    if (page.skipPageLinks()) {
      return;
    }
    
    List<Request> newRequests = page.getPageLinks();
    if (newRequests != null && !newRequests.isEmpty()) {
      logger.info("retrive next batch requests inner this page : {}", newRequests.size());
      for (Request newRequest : newRequests) {
        try {
          this.requestQueue.offer(newRequest, Envirenment.DEFAULT_REQUEST_PUSH_TIMEOUT,
              TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

}
