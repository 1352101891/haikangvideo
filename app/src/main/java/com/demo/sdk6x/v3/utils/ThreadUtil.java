package com.demo.sdk6x.v3.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadUtil {
    private static final int CPU_COUNT=6;
    private static final int CORE_POOL_SIZE=Math.max(2,Math.min(CPU_COUNT-1,4));
    private static final int MAXIMUN_POOL_SIZE = CPU_COUNT*2+1;
    private static final int KEEP_ALIVE_SECONDS =30;
    private static Executor THREAD_POOL_EXECUTOR;
    private static ThreadUtil threadUtil=null;
    private BlockingDeque<Runnable> sPoolWorkQueue;
    private ThreadFactory sThreadFactory;

    public static synchronized void init(Context context){
        if (threadUtil == null) {
            threadUtil= new ThreadUtil(context);
        }
    }


    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private ThreadUtil(Context context){
        sThreadFactory=new ThreadFactory() {
            private final AtomicInteger mCount=new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r,"ThreadUtil's thread #"+mCount.getAndIncrement());
            }
        };
        sPoolWorkQueue=new LinkedBlockingDeque<Runnable>(128);
        ThreadPoolExecutor threadPoolExecutor=new ThreadPoolExecutor(CORE_POOL_SIZE
                ,MAXIMUN_POOL_SIZE,KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,sPoolWorkQueue
                , sThreadFactory);
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        THREAD_POOL_EXECUTOR=threadPoolExecutor;
    }


    public static void execute(Runnable runnable){
        if (threadUtil!=null){
            THREAD_POOL_EXECUTOR.execute(runnable);
        }else {
            new Thread(runnable).start();
        }
    }
}
