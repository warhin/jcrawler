package jcrawler.executor;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jcrawler.Envirenment;
import jcrawler.Request;
import jcrawler.support.Threads;

public class RequestSuplierWorker extends Stopable implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(RequestSuplierWorker.class);

  private RequestSuplier requestSuplier;

  private BlockingQueue<Request> requestQueue;

  private int requestHolderThresholds;

  private long requestSuplierPauseMills;

  public RequestSuplierWorker(RequestSuplier requestSuplier, BlockingQueue<Request> requestQueue,
      int requestHolderThresholds, long requestSuplierPauseMills) {
    super();
    this.requestSuplier = requestSuplier;
    this.requestQueue = requestQueue;
    this.requestHolderThresholds = requestHolderThresholds;
    this.requestSuplierPauseMills = requestSuplierPauseMills;
  }

  @Override
  public void run() {
    while (true) {
      try {
        // 被显式终止时，执行线程退出。
        if (isStop()) {
          break;
        }

        // requestQueue中存储的待爬取request集合数量超出阀值后暂停request泵工作，休眠指定requestSuplierPauseMills时长后再判断是否继续。以此控制泵的速度和爬虫系统负载。
        if (requestQueue.size() >= requestHolderThresholds) {
          logger.warn(
              "request suplier run faster than crawler(the requestQueue current size {} >= the requestQueue thresholds {}), please adjust the threads relationed!",
              requestQueue.size(), requestHolderThresholds);
          Threads.sleep(requestSuplierPauseMills, true);
          continue;
        }

        // requestSuplier没有后续任务了将轮询
        if (!requestSuplier.hasNext()) {
          continue;
        }

        // request泵继续开工，导入一批新的待爬取request任务到requestQueue中。
        List<Request> newRequests = requestSuplier.nextBatch();
        if (newRequests != null && !newRequests.isEmpty()) {
          logger.info("retrive next batch requests from requestSuplier : {}", newRequests.size());
          for (Request newRequest : newRequests) {
            requestQueue.offer(newRequest, Envirenment.DEFAULT_REQUEST_PUSH_TIMEOUT,
                TimeUnit.MILLISECONDS);
          }
        }
      } catch (Exception e) {
        logger.error("RequestSuplierWorker run error : ", e);
      }
    }
  }

}
