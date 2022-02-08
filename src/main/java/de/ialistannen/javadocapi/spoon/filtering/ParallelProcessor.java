package de.ialistannen.javadocapi.spoon.filtering;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import spoon.reflect.declaration.CtElement;

public class ParallelProcessor {

  private final FluentFilter filter;
  private final ExecutorService pool;
  private final int parallelism;

  public ParallelProcessor(FluentFilter filter, int parallelism) {
    this.filter = filter;
    this.parallelism = parallelism;

    this.pool = Executors.newFixedThreadPool(parallelism, r -> {
      Thread thread = new Thread(r);
      thread.setDaemon(true);
      return thread;
    });
  }

  public void process(CtElement root, Consumer<CtElement> consumer) {
    CyclicBarrier barrier = new CyclicBarrier(parallelism);
    LinkedBlockingDeque<CtElement> workQueue = new LinkedBlockingDeque<>();
    List<Callable<Void>> tasks = new ArrayList<>();

    workQueue.add(root);

    for (int i = 0; i < parallelism; i++) {
      tasks.add(() -> {
        while (true) {
          CtElement item;
          while ((item = workQueue.pollFirst()) != null) {
            if (!filter.keep(item)) {
              continue;
            }
            try {
              consumer.accept(item);
            } catch (Exception e) {
              System.err.println("Error in parallel worker");
              e.printStackTrace();
            } finally {
              workQueue.addAll(item.getDirectChildren());
            }
          }

          try {
            barrier.await(100, TimeUnit.MILLISECONDS);
            return null;
          } catch (TimeoutException e) {
            barrier.reset();
          } catch (BrokenBarrierException ignored) {
          }
        }
      });
    }

    try {
      pool.invokeAll(tasks);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void shutdown() {
    pool.shutdown();
  }

}
