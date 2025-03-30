package performance;

import com.spud.barrage.common.cluster.hash.ConsistentHash;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * @author Spud
 * @date 2025/3/29
 */
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class ConsistentHashPerformanceTest {

  // 测试参数配置
  @Param({"100", "1000", "5000"})  // 不同节点规模
  private int nodeCount;

  @Param({"4", "8"})              // 并发线程数
  private int threadCount;

  private ConsistentHash<String> hashRing;
  private ExecutorService executor;
  private final ThreadLocalRandom random = ThreadLocalRandom.current();

  @Setup(Level.Trial)
  public void init() {
    // 初始化哈希环并预填充节点
    hashRing = new ConsistentHash<>();
    for (int i = 0; i < nodeCount; i++) {
      hashRing.addNode("node_" + i);
    }

    // 创建固定线程池
    executor = Executors.newFixedThreadPool(threadCount);
  }

  @TearDown(Level.Trial)
  public void cleanup() {
    executor.shutdownNow();
  }

  /******************** 核心基准测试场景 ********************/

  @Benchmark
  @Threads(4)
  public void writeOperations() {
    // 模拟节点动态变化
    String node = "dynamic_node_" + random.nextInt(10000);
    executor.submit(() -> hashRing.addNode(node));
    executor.submit(() -> hashRing.removeNode(node));
  }

  @Benchmark
  @Group("read_write")
  @GroupThreads(6)
  public void readOperations(Blackhole bh) {
    // 模拟高并发查询
    String key = "key_" + random.nextInt(1_000_000);
    bh.consume(hashRing.getNode(key));
  }

  @Benchmark
  @Group("read_write")
  @GroupThreads(2)
  public void mixedWriteOperations() {
    // 混合场景写入操作
    String node = "mixed_node_" + random.nextInt(5000);
    if (random.nextBoolean()) {
      hashRing.addNode(node);
    } else {
      hashRing.removeNode(node);
    }
  }

  /******************** 哈希算法对比测试 ********************/

  @State(Scope.Benchmark)
  public static class HashAlgorithmState {

    ConsistentHash<String> crc32Ring;
    ConsistentHash<String> md5Ring;
    ConsistentHash<String> ketamaRing;

    @Setup
    public void init() {
      crc32Ring = new ConsistentHash<>(160, new ConsistentHash.Crc32Hash());
      md5Ring = new ConsistentHash<>(160, new ConsistentHash.MD5Hash());
      ketamaRing = new ConsistentHash<>(160, new ConsistentHash.KetamaHash());

      // 预填充1000个节点
      for (int i = 0; i < 1000; i++) {
        String node = "hash_node_" + i;
        crc32Ring.addNode(node);
        md5Ring.addNode(node);
        ketamaRing.addNode(node);
      }
    }
  }

  @Benchmark
  public void testCRC32(HashAlgorithmState state, Blackhole bh) {
    String key = "hash_key_" + random.nextInt(1_000_000);
    bh.consume(state.crc32Ring.getNode(key));
  }

  @Benchmark
  public void testMD5(HashAlgorithmState state, Blackhole bh) {
    String key = "hash_key_" + random.nextInt(1_000_000);
    bh.consume(state.md5Ring.getNode(key));
  }

  @Benchmark
  public void testKetama(HashAlgorithmState state, Blackhole bh) {
    String key = "hash_key_" + random.nextInt(1_000_000);
    bh.consume(state.ketamaRing.getNode(key));
  }

  /******************** 扩展场景测试 ********************/

  @Benchmark
  @Threads(8)
  public void highConcurrencyRead(Blackhole bh) {
    // 模拟超高并发读取
    String key = "hc_key_" + random.nextInt(5_000_000);
    bh.consume(hashRing.getNode(key));
  }

  @Benchmark
  @Group("scale_test")
  @GroupThreads(4)
  public void scaleOut() {
    // 节点扩容测试
    String node = "scale_node_" + random.nextInt(1000);
    hashRing.addNode(node);
  }

  @Benchmark
  @Group("scale_test")
  @GroupThreads(4)
  public void scaleIn() {
    // 节点缩容测试
    String node = "scale_node_" + random.nextInt(1000);
    hashRing.removeNode(node);
  }

  public static void main(String[] args) throws Exception {
    org.openjdk.jmh.Main.main(args);
  }
}
