# Thread-Safe Task Scheduler

A production-grade task scheduling system that safely manages and executes delayed tasks across multiple worker threads. This implementation demonstrates low-level design patterns and concurrent programming concepts in Java.

## Overview

The Task Scheduler is a multi-threaded system that allows you to schedule tasks for delayed execution with support for cancellation. It ensures thread safety through proper synchronization mechanisms and uses a priority queue to execute tasks in the correct order based on their scheduled execution time.

## Architecture

### Core Components

#### 1. **Task Model** (`src/model/Task.java`)
Represents a scheduled task with the following attributes:
- **id**: Unique identifier for the task
- **executionTime**: Absolute timestamp (in milliseconds) when the task should execute
- **task**: The actual `Runnable` to be executed
- **isCancelled**: Flag to track if the task has been cancelled

**Key Design**: The `Task` class implements `Comparable<Task>` to enable priority-based ordering in the priority queue. Tasks are compared by their execution time, ensuring earlier scheduled tasks are executed first.

#### 2. **TaskScheduler Service** (`src/service/TaskScheduler.java`)
The core scheduling engine that manages task execution. It features:
- Multiple worker threads for concurrent task execution
- A priority queue for maintaining task order
- A hash map for O(1) task lookup during cancellation

#### 3. **Main Entry Point** (`src/Main.java`)
Contains the application entry point for testing and demonstration.

## Technologies & Synchronization Mechanisms

### 1. **ReentrantLock** (`java.util.concurrent.locks.ReentrantLock`)
- **Why**: Provides explicit lock management, allowing more control than synchronized blocks
- **Usage**: Protects shared data structures (queue and task map) from concurrent modifications
- **Benefits**: 
  - Allows the same thread to acquire the lock multiple times
  - Better performance for contention scenarios
  - Works seamlessly with Condition variables for advanced signaling

### 2. **Condition Variables** (`java.util.concurrent.locks.Condition`)
- **Why**: Enables efficient communication between worker threads and the scheduler
- **Usage**: Workers wait on the `available` condition when the queue is empty, and are signaled when new tasks arrive
- **Benefits**:
  - More efficient than busy-waiting or sleep loops
  - Prevents CPU waste by putting idle threads to sleep
  - Allows precise control over thread wake-ups

### 3. **PriorityQueue** (`java.util.PriorityQueue`)
- **Why**: Automatically maintains tasks in sorted order by execution time
- **Usage**: Tasks are inserted and removed based on their scheduled execution time
- **Benefits**:
  - O(log n) insertion and removal complexity
  - Ensures tasks execute in correct temporal order
  - No manual sorting required

### 4. **HashMap** (`java.util.HashMap`)
- **Why**: Provides fast task lookup during cancellation
- **Usage**: Maps task IDs to Task objects for O(1) access
- **Benefits**:
  - Quick cancellation without scanning the entire queue
  - Efficient task tracking

## Key Design Decisions

### 1. **Absolute Execution Time**
Tasks store an absolute timestamp (current time + delay) rather than just the delay value. This approach:
- Eliminates timing drift that could accumulate with relative delays
- Makes task ordering deterministic and comparison-based

### 2. **Worker Thread Pattern**
Multiple worker threads continuously poll the queue:
- **Scalability**: Supports concurrent task execution
- **Resource Efficiency**: Threads sleep when queue is empty
- **Cancellation Support**: Can cancel tasks before they execute

### 3. **Thread Safety Through Locking**
All access to shared data (queue and task map) is protected by the `ReentrantLock`:
- **Schedule Operation**: Adds task to both queue and map under lock
- **Cancel Operation**: Marks task as cancelled under lock
- **Worker Loop**: Entire critical section (queue peek/poll) is locked

### 4. **Graceful Shutdown**
The `shutdown()` method:
- Sets the `volatile` running flag to false
- Interrupts all worker threads
- Allows clean termination of the scheduler

### 5. **Condition-Based Waiting**
Workers use `available.await()` and `available.awaitNanos()`:
- When queue is empty, threads wait indefinitely until signaled
- When waiting for task execution time, threads use timed wait with nano-second precision
- Prevents unnecessary CPU cycles from polling

## Thread Safety Guarantees

1. **Race Condition Prevention**: All shared state modifications are atomic operations protected by locks
2. **Visibility**: `volatile` flag ensures all threads see the latest `running` state
3. **Deadlock Avoidance**: Single lock per scheduler with consistent lock acquisition order
4. **Fairness**: ReentrantLock maintains FIFO ordering of thread access

## Usage Example

```java
// Create scheduler with 4 worker threads
TaskScheduler scheduler = new TaskScheduler(4);

// Schedule a task to execute after 2 seconds
scheduler.schedule("task1", 2000, () -> {
    System.out.println("Task executed!");
});

// Cancel the task before it executes
scheduler.cancel("task1");

// Shutdown the scheduler gracefully
scheduler.shutdown();
```

## Design Pattern

This implementation follows several design patterns:
- **Producer-Consumer Pattern**: Main thread produces tasks, worker threads consume and execute them
- **Thread Pool Pattern**: Fixed number of worker threads processing tasks from a shared queue
- **Monitor Pattern**: Uses locks and conditions to synchronize access to shared resources

## Complexity Analysis

- **Schedule Operation**: O(log n) where n is queue size (priority queue insertion)
- **Cancel Operation**: O(1) (hash map lookup)
- **Task Execution**: O(log n) (priority queue removal)
- **Space Complexity**: O(n) for storing n tasks in queue and map

## Future Enhancements

- Task priority levels independent of execution time
- Task completion callbacks
- Task execution statistics and monitoring
- Dynamic worker thread scaling based on load
