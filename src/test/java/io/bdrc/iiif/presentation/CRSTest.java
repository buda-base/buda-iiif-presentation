package io.bdrc.iiif.presentation;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.BeforeClass;
import org.junit.Test;

public class CRSTest {

    static ExecutorService es = Executors.newSingleThreadExecutor();

    @BeforeClass
    public static void before() {

    }

    @Test
    public void simpleTest() throws InterruptedException {
        Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                try {
                    String res = RSMock.Instance.getAsync("id1").get();
                    System.out.println(res);
                    assert (res.equals("success"));
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };

        Runnable runnable2 = new Runnable() {
            @Override
            public void run() {
                try {
                    String res = RSMock.Instance.getAsync("id1").get();
                    System.out.println(res);
                    assert (res.equals("success"));
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread t1 = new Thread(runnable1);
        Thread t2 = new Thread(runnable2);

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

}
