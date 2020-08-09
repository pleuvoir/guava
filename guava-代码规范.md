guava 代码的规范



本文以 `guava ` 代码库为例，记录该库的代码规范包括命名。希望大家能写出更专业的代码。



## 字典表



```java
Throwable failureCause();

public void failed(State from, Throwable failure) {}

Throwable cause
```



### 已确定的专业操作

比如有个类是做权重操作的的，怎么命名？

```java
public interface Weigher<K, V> {

  int weigh(K key, V value);
}
```

加了个`er`，找到了另外一个类，做哈希操作的叫：

```java
public interface Hasher extends PrimitiveSink {
  @Override
  Hasher putByte(byte b);

  @Override
  Hasher putBytes(byte[] bytes);
}    
```



所以这是一种通用的做法，显得很专业。




像这种的，如果不能按业务分组就全换行:

```java
 public EventBus(String identifier) {
    this(identifier,
        MoreExecutors.directExecutor(),
        Dispatcher.perThreadDispatchQueue(),
        LoggingHandler.INSTANCE);
  }
```

另外，这个` LoggingHandler.INSTANCE`是在`LoggingHandler`里创建了个对象。我觉得是尽量不在构造方法里直接`new`对象的想法？需要再观察下。

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


这个换行也挺有代表性：

```java
  void register(Object listener) {
    Multimap<Class<?>, Subscriber> listenerMethods = findAllSubscribers(listener); //通过方法获得的下面空一行

    for (Entry<Class<?>, Collection<Subscriber>> entry : listenerMethods.asMap().entrySet()) {
      Class<?> eventType = entry.getKey();			
      Collection<Subscriber> eventMethodsInListener = entry.getValue();

      CopyOnWriteArraySet<Subscriber> eventSubscribers = subscribers.get(eventType); //从别的数据结构里获取空行

      if (eventSubscribers == null) {
        CopyOnWriteArraySet<Subscriber> newSet = new CopyOnWriteArraySet<>();
        eventSubscribers =
            MoreObjects.firstNonNull(subscribers.putIfAbsent(eventType, newSet), newSet);
      }

      eventSubscribers.addAll(eventMethodsInListener); //空着更清晰 凭感觉
    }
  }

```

再来看这个：

```java
  Iterator<Subscriber> getSubscribers(Object event) {
    ImmutableSet<Class<?>> eventTypes = flattenHierarchy(event.getClass()); //这里换行和上面规则一致

    List<Iterator<Subscriber>> subscriberIterators =
        Lists.newArrayListWithCapacity(eventTypes.size());   //这里是新建List为什么也换？这里姑且认为创建都换。也可能是下面的代码没用到这个变量

    for (Class<?> eventType : eventTypes) {
      CopyOnWriteArraySet<Subscriber> eventSubscribers = subscribers.get(eventType);
      if (eventSubscribers != null) {
        // eager no-copy snapshot
        subscriberIterators.add(eventSubscribers.iterator());
      }
    }

    return Iterators.concat(subscriberIterators.iterator()); //空着更清晰 凭感觉
  }
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

注意这个换行，也是按照分组进行的换行的 STOP和TERMINATED的



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



不可能的异常

```java
public StringBuilder appendTo(StringBuilder builder, Iterator<? extends Entry<?, ?>> entries) {
    try {
        appendTo((Appendable) builder, entries);
    } catch (IOException impossible) {
        throw new AssertionError(impossible);
    }
    return builder;
}
```





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



##  新学到的用法

### 重写toString

```java
@Override
public String toString() {
    return MoreObjects.toStringHelper(this).addValue(identifier).toString();
}
```

这是`EventBus`中的方法，这样生成的样式是`EventBus(default)`，这样就不用输出JSON了，挺好。注意，`addValue`添加的是全局变量。



或者下面这个，可以有命名，按需使用

```java
 public String toString() {
    return MoreObjects.toStringHelper(this).add("source", source).add("event", event).toString();
  }
//DeadEvent{source=1, event=2}
```




### 创建时静态导入 Preconditions

```java
 SubscriberRegistry(EventBus bus) {
    this.bus = checkNotNull(bus);
  }
```