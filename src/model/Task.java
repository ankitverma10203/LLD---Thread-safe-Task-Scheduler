package model;

public class Task implements Comparable<Task> {
    final String id;
    final long executionTime;
    final Runnable task;
    boolean isCancelled = false;

    public Task(String id, long delayMillis, Runnable task) {
        this.id = id;
        this.executionTime = System.currentTimeMillis() + delayMillis;
        this.task = task;
    }

    @Override
    public int compareTo(Task task2) {
        return Long.compare(this.executionTime, task2.executionTime);
    }

    public String getId() {
        return id;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public Runnable getTask() {
        return task;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }
}
