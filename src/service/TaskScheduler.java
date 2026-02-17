package service;

import model.Task;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class TaskScheduler {
    private final PriorityQueue<Task> queue = new PriorityQueue<>();
    private final Map<String, Task> taskMap = new HashMap<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition available = lock.newCondition();

    private final List<Thread> workers = new ArrayList<>();
    private volatile boolean running = true;

    public TaskScheduler(int workerCount) {
        for (int i = 0; i < workerCount; i++) {
            Thread worker = new Thread(this::workerLoop);
            worker.start();
            workers.add(worker);
        }
    }

    public void schedule (String id, long delayMillis, Runnable task) {
        lock.lock();
        try {
            Task scheduleTask = new Task(id, delayMillis, task);
            queue.offer(scheduleTask);
            taskMap.put(id, scheduleTask);

            available.signal();
        } finally {
            lock.unlock();
        }
    }

    public void cancel (String id) {
        lock.lock();
        try {
            Task task = taskMap.get(id);
            if (task != null) {
                task.setCancelled(true);
            }
        } finally {
            lock.unlock();
        }
    }

    private void workerLoop() {
        while (running) {
            lock.lock();
            try {
                while (queue.isEmpty()) {
                    available.await();
                }

                Task task = queue.peek();
                long now = System.currentTimeMillis();

                if (task.getExecutionTime() > now) {
                    long waitTime = task.getExecutionTime() - now;
                    available.awaitNanos(waitTime * 1_000_000);
                    continue;
                }

                queue.poll();
                taskMap.remove(task.getId());

                if (!task.isCancelled()) {
                    task.getTask().run();
                }
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            } finally {
                lock.unlock();
            }
        }
    }

    public void shutdown() {
        running = false;
        for (Thread worker : workers) {
            worker.interrupt();
        }
    }
}
