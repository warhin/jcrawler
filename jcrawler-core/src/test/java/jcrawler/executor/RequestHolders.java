package jcrawler.executor;

public class RequestHolders {
  
  public static RequestHolder queueRequestHolder() {
    return new QueueRequestHolder();
  }

  public static RequestHolder queueRequestHolderWithSetReserver() {
    return new QueueRequestHolder(new SetReserver());
  }

  public static RequestHolder queueRequestHolderWithBloomReserver() {
    return new QueueRequestHolder(new BloomReserver());
  }
  
  public static RequestHolder priorityRequestHolder() {
    return new PriorityRequestHolder();
  }

  public static RequestHolder priorityRequestHolderWithSetReserver() {
    return new PriorityRequestHolder(new SetReserver());
  }

  public static RequestHolder priorityRequestHolderWithBloomReserver() {
    return new PriorityRequestHolder(new BloomReserver());
  }

}
