package com.google.common.os;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.base.Stopwatch;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Before;
import org.junit.Test;

/**
 * 局部性原理测试
 *
 * <ul>
 *   <li>时间局部性： 程序倾向于短时间内再次访问刚刚访问过的变量
 *   <li>空间局部性： 程序倾向于访问刚刚访问过内存地址附近的地址
 * </ul>
 *
 * <p>结论： 循环中在不改变原逻辑的情况下，尽可能保证每次跳跃步长为 1。
 *    一个可用的技巧是保证最内层循环右侧变量变得最快，如下所示：
 * <pre>
 *    for (int i = 0; i < ROW_1; i++) {
 *       for (int j = 0; j < ROW_2; j++) {
 *         sum += array[i][j];    //这里 j 变化的最快
 *       }
 *     }
 * </pre>
 */
public class LocalityTest {

  private final int ROW_1 = 200;
  private final int ROW_2 = 500000;
  private final int[][] array = new int[ROW_1][ROW_2];

  @Before
  public void before() {
    ThreadLocalRandom localRandom = ThreadLocalRandom.current();
    for (int i = 0; i < ROW_1; i++) {
      for (int j = 0; j < ROW_2; j++) {
        array[i][j] = localRandom.nextInt(10);
      }
    }

    // print for check.
//    for (int i = 0; i < ROW_1; i++) {
//      for (int j = 0; j < ROW_2; j++) {
//        System.out.println(String.format("array[%s][%s] = %s", i, j, array[i][j]));
//      }
//    }

  }

  @Test
  public void testLocality() throws InterruptedException {

    testStep1();
    // sum equal = 450008315, cost 35 ms.

    testStepN();
    // sum equal = 450008315, cost 145 ms.
  }

  /**
   * 步长为1
   */
  private void testStep1() {
    Stopwatch stopwatch = Stopwatch.createStarted();

    int sum = 0;
    for (int i = 0; i < ROW_1; i++) {
      for (int j = 0; j < ROW_2; j++) {
        sum += array[i][j];
      }
    }

    long elapsed = stopwatch.elapsed(MILLISECONDS);
    System.out.println(String.format("sum equal %s, cost %s ms.", sum, elapsed));
  }


  /**
   * 步长为 ROW_2 size
   */
  private void testStepN() {
    Stopwatch stopwatch = Stopwatch.createStarted();

    int sum = 0;
    for (int j = 0; j < ROW_2; j++) {
      for (int i = 0; i < ROW_1; i++) {
        sum += array[i][j];
      }
    }

    long elapsed = stopwatch.elapsed(MILLISECONDS);
    System.out.println(String.format("sum equal %s, cost %s ms.", sum, elapsed));
  }


}
