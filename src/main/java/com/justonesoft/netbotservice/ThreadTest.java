package com.justonesoft.netbotservice;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class ThreadTest extends Thread {
	
	@Override
	public void run() {
		System.out.println(getName() + " sleeping 3 sec");
		try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}// TODO Auto-generated method stub
		System.out.println(getName() + " finish");
	}
	
	public static void main(String[] args) {
		
		
		ThreadTest test = new ThreadTest();
		FutureTask<Void> futureTask = new FutureTask<Void>(test, null);
		
		futureTask.run();
		
		System.out.println(Thread.currentThread().getName() + " sleeping 2 sec");
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(Thread.currentThread().getName() + " awake");
		
		//futureTask.run();
		
		try {
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
