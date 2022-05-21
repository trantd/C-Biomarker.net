package kcore.plugin.rcore.parallel;

import java.util.concurrent.atomic.AtomicInteger;

public class Atomic {
	private final AtomicInteger count = new AtomicInteger();
    public  void increment() {
        count.incrementAndGet();
    }
    public  void decrement() {
        count.decrementAndGet();
    }
    public int getCount() {
        return count.get();
    }   
}
