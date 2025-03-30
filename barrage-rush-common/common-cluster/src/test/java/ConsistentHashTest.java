import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.spud.barrage.common.cluster.hash.ConsistentHash;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Spud
 * @date 2025/3/29
 */

public class ConsistentHashTest {

  private ConsistentHash<String> consistentHash;

  @BeforeEach
  void setUp() {
    consistentHash = new ConsistentHash<>();
  }

  @Test
  void testAddNode() {
    consistentHash.addNode("node1");
    assertTrue(consistentHash.getNodes().contains("node1"));
    assertEquals(1, consistentHash.getNodeCount());
  }

  @Test
  void testAddNodeWithWeight() {
    consistentHash.addNode("node1", 3);
    assertTrue(consistentHash.getNodes().contains("node1"));
    assertEquals(1, consistentHash.getNodeCount());
  }

  @Test
  void testAddNodeWithInvalidWeight() {
    assertThrows(IllegalArgumentException.class, () -> consistentHash.addNode("node1", 0));
  }

  @Test
  void testAddNullNode() {
    assertThrows(IllegalArgumentException.class, () -> consistentHash.addNode(null));
  }

  @Test
  void testRemoveNode() {
    consistentHash.addNode("node1");
    consistentHash.removeNode("node1");
    assertFalse(consistentHash.getNodes().contains("node1"));
    assertEquals(0, consistentHash.getNodeCount());
  }

  @Test
  void testRemoveNonExistentNode() {
    consistentHash.addNode("node1");
    consistentHash.removeNode("node2");
    assertTrue(consistentHash.getNodes().contains("node1"));
    assertEquals(1, consistentHash.getNodeCount());
  }

  @Test
  void testGetNode() {
    consistentHash.addNode("node1");
    consistentHash.addNode("node2");
    String key = "testKey";
    String node = consistentHash.getNode(key);
    assertNotNull(node);
    assertTrue(consistentHash.getNodes().contains(node));
  }

  @Test
  void testGetNodeFromEmptyRing() {
    assertNull(consistentHash.getNode("testKey"));
  }

  @Test
  void testGetNodes() {
    consistentHash.addNode("node1");
    consistentHash.addNode("node2");
    consistentHash.addNode("node3");
    Set<String> nodes = consistentHash.getNodes("testKey", 2);
    assertEquals(2, nodes.size());
    assertTrue(nodes.stream().allMatch(node -> consistentHash.getNodes().contains(node)));
  }

  @Test
  void testGetNodesWithCountGreaterThanNodeCount() {
    consistentHash.addNode("node1");
    consistentHash.addNode("node2");
    Set<String> nodes = consistentHash.getNodes("testKey", 3);
    assertEquals(2, nodes.size());
  }

  @Test
  void testIsNodeResponsible() {
    consistentHash.addNode("node1");
    consistentHash.addNode("node2");
    String key = "testKey";
    String responsibleNode = consistentHash.getNode(key);
    assertTrue(consistentHash.isNodeResponsible(responsibleNode, key));
  }

  @Test
  void testClear() {
    consistentHash.addNode("node1");
    consistentHash.addNode("node2");
    consistentHash.clear();
    assertEquals(0, consistentHash.getNodeCount());
    assertTrue(consistentHash.getNodes().isEmpty());
  }

  @Test
  void testAddNodes() {
    Set<String> nodes = new HashSet<>(Arrays.asList("node1", "node2"));
    consistentHash.addNodes(nodes);
    assertEquals(2, consistentHash.getNodeCount());
    assertTrue(consistentHash.getNodes().containsAll(nodes));
  }

  @Test
  void testAddWeightedNodes() {
    Map<String, Integer> nodeWeights = new HashMap<>();
    nodeWeights.put("node1", 2);
    nodeWeights.put("node2", 3);
    consistentHash.addWeightedNodes(nodeWeights);
    assertEquals(2, consistentHash.getNodeCount());
    assertTrue(consistentHash.getNodes().containsAll(nodeWeights.keySet()));
  }

  @Test
  void testCalculateBalanceMetric() {
    consistentHash.addNode("node1", 1);
    consistentHash.addNode("node2", 2);
    double balanceMetric = consistentHash.calculateBalanceMetric();
    assertTrue(balanceMetric >= 0);
  }

  @Test
  void testIsBalanced() {
    consistentHash.addNode("node1", 1);
    consistentHash.addNode("node2", 1);
    assertTrue(consistentHash.isBalanced());
  }

  @Test
  void testDifferentHashFunctions() {
    ConsistentHash<String> crc32Hash = new ConsistentHash<>(40, new ConsistentHash.Crc32Hash());
    ConsistentHash<String> md5Hash = new ConsistentHash<>(40, new ConsistentHash.MD5Hash());
    ConsistentHash<String> ketamaHash = new ConsistentHash<>(40, new ConsistentHash.KetamaHash());

    crc32Hash.addNode("node1");
    md5Hash.addNode("node1");
    ketamaHash.addNode("node1");

    String key = "testKey";
    assertNotNull(crc32Hash.getNode(key));
    assertNotNull(md5Hash.getNode(key));
    assertNotNull(ketamaHash.getNode(key));
  }

  @Test
  void testConcurrentOperations() throws InterruptedException {
    consistentHash.addNode("node1");
    consistentHash.addNode("node2");

    Thread t1 = new Thread(() -> {
      for (int i = 0; i < 100; i++) {
        consistentHash.getNode("key" + i);
      }
    });

    Thread t2 = new Thread(() -> {
      for (int i = 0; i < 100; i++) {
        consistentHash.addNode("node" + i);
      }
    });

    t1.start();
    t2.start();
    t1.join();
    t2.join();

    assertTrue(consistentHash.getNodeCount() > 0);
  }
}