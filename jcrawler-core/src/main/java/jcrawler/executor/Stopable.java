package jcrawler.executor;

import java.util.concurrent.atomic.AtomicBoolean;

public class Stopable {

  private AtomicBoolean stopFlag = new AtomicBoolean(false);

  public void stop() {
    stopFlag.set(true);
  }

  public boolean isStop() {
    return stopFlag.get();
  }

}
