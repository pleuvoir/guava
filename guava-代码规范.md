guava 代码的规范



本文以 `guava ` 代码库为例，记录该库的代码规范包括命名。希望大家能写出更专业的代码。



## 字典表



```java
Throwable failureCause();

public void failed(State from, Throwable failure) {}

Throwable cause
```



## 规范

写代码不超线



可以使用单行注释



返回自身可以使用

```java
 @return this
```

如果是一个值就使用link

```
if the service is not {@link State#NEW}
```

如果后面还有注释则使用linkplain

```
 {@linkplain State#RUNNING running}
```

如果这个单词使用  e.g. if the {@code state} 



之前可以使用 `previous` 这个单词



换行的姿势

```java
StateSnapshot(
    State internalState, boolean shutdownWhenStartupFinishes, @Nullable Throwable failure) {
    checkArgument(
        !shutdownWhenStartupFinishes || internalState == STARTING,
        "shutdownWhenStartupFinishes can only be set if state is STARTING. Got %s instead.",
        internalState);
    checkArgument(
        !(failure != null ^ internalState == FAILED),
        "A failure cause should be set if and only if the state is failed.  Got %s and %s "
        + "instead.",
        internalState,
        failure);
    this.state = internalState;
    this.shutdownWhenStartupFinishes = shutdownWhenStartupFinishes;
    this.failure = failure;
}
```

这是一个构造方法，可以看出第一个参数是换行了的。



这块可以使用静态导入

```java
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
```



定义的全局变量不一定必须使用驼峰

```java
  private static final ListenerCallQueue.Event<Listener> STOPPING_FROM_STARTING_EVENT =
      stoppingEvent(STARTING);
  private static final ListenerCallQueue.Event<Listener> STOPPING_FROM_RUNNING_EVENT =
      stoppingEvent(RUNNING);

  private static final ListenerCallQueue.Event<Listener> TERMINATED_FROM_NEW_EVENT =
      terminatedEvent(NEW);
  private static final ListenerCallQueue.Event<Listener> TERMINATED_FROM_STARTING_EVENT =
      terminatedEvent(STARTING);
  private static final ListenerCallQueue.Event<Listener> TERMINATED_FROM_RUNNING_EVENT =
      terminatedEvent(RUNNING);
  private static final ListenerCallQueue.Event<Listener> TERMINATED_FROM_STOPPING_EVENT =
      terminatedEvent(STOPPING);
```

注意这个换行，也是按照分组的 STOP和TERMINATED的



如果在switch中不想写default可以抛出断言异常

```java
switch (previous) {
    case NEW:
        snapshot = new StateSnapshot(TERMINATED);
        enqueueTerminatedEvent(NEW);
        break;
    case STARTING:
        snapshot = new StateSnapshot(STARTING, true, null);
        enqueueStoppingEvent(STARTING);
        doCancelStart();
        break;
    case RUNNING:
        snapshot = new StateSnapshot(STOPPING);
        enqueueStoppingEvent(RUNNING);
        doStop();
        break;
    case STOPPING:
    case TERMINATED:
    case FAILED:
        // These cases are impossible due to the if statement above.
        throw new AssertionError("isStoppable is incorrectly implemented, saw: " + previous);
}
```

直接不带异常内容也是可以的

```java
 private void enqueueStoppingEvent(final State from) {
    if (from == State.STARTING) {
      listeners.enqueue(STOPPING_FROM_STARTING_EVENT);
    } else if (from == State.RUNNING) {
      listeners.enqueue(STOPPING_FROM_RUNNING_EVENT);
    } else {
      throw new AssertionError();
    }
  }
```





像这种还可以抛出状态异常

```java
public final Service startAsync() {
    if (monitor.enterIf(isStartable)) {
        try {
            snapshot = new StateSnapshot(STARTING);
            enqueueStartingEvent();
            doStart();
        } catch (Throwable startupFailure) {
            notifyFailed(startupFailure);
        } finally {
            monitor.leave();
            dispatchListenerEvents();
        }
    } else {
        throw new IllegalStateException("Service " + this + " has already been started");
    }
    return this;
}
```

可以看到单词和 this 之间分别有个空格



三目运算符这样换行比较好，因为写两个超过了线，写一个下一个换行太丑

```java
  public static <V> FluentFuture<V> from(ListenableFuture<V> future) {
    return future instanceof FluentFuture
        ? (FluentFuture<V>) future
        : new ForwardingFluentFuture<V>(future);
  }
```



## 良好的类

### Monitor

用来代替直接使用 sync或者ReentrantLock，因为它们容易出错，我觉得不错