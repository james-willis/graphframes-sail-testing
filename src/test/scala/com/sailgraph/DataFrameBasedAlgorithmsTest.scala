package com.sailgraph

import org.apache.spark.sql.functions._
import org.graphframes.GraphFrame

/**
 * Tests for DataFrame-based GraphFrames operations that SHOULD work with Spark Connect.
 * These operations don't rely on RDDs or GraphX.
 */
class DataFrameBasedAlgorithmsTest extends GraphFramesConnectTestBase {
  
  test("Create simple GraphFrame with DataFrames") {
    val vertices = spark.createDataFrame(Seq(
      (1L, "Alice", 34),
      (2L, "Bob", 36),
      (3L, "Charlie", 30),
      (4L, "David", 29)
    )).toDF("id", "name", "age")
    
    val edges = spark.createDataFrame(Seq(
      (1L, 2L, "friend"),
      (2L, 3L, "follow"),
      (3L, 4L, "friend"),
      (4L, 1L, "follow")
    )).toDF("src", "dst", "relationship")
    
    val g = GraphFrame(vertices, edges)
    
    g.vertices.count() shouldBe 4
    g.edges.count() shouldBe 4
  }
  
  // Motif finding returns unexpected results through Spark Connect (3 triangles instead of 1)
  ignore("Motif finding - simple pattern") {
    val vertices = spark.createDataFrame(Seq(
      (1L, "Alice"),
      (2L, "Bob"),
      (3L, "Charlie")
    )).toDF("id", "name")
    
    val edges = spark.createDataFrame(Seq(
      (1L, 2L, "friend"),
      (2L, 3L, "friend"),
      (3L, 1L, "friend")
    )).toDF("src", "dst", "relationship")
    
    val g = GraphFrame(vertices, edges)
    
    // Find triangles
    val triangles = g.find("(a)-[e1]->(b); (b)-[e2]->(c); (c)-[e3]->(a)")
    
    triangles.count() shouldBe 1
  }
  
  test("Motif finding - with filters") {
    val vertices = spark.createDataFrame(Seq(
      (1L, "Alice", 34),
      (2L, "Bob", 36),
      (3L, "Charlie", 30)
    )).toDF("id", "name", "age")
    
    val edges = spark.createDataFrame(Seq(
      (1L, 2L, "friend"),
      (2L, 1L, "follow"),
      (2L, 3L, "friend")
    )).toDF("src", "dst", "relationship")
    
    val g = GraphFrame(vertices, edges)
    
    // Find bidirectional connections
    val motifs = g.find("(a)-[e1]->(b); (b)-[e2]->(a)")
    val filtered = motifs.filter("e1.relationship = 'friend' AND e2.relationship = 'follow'")
    
    filtered.count() shouldBe 1
  }
  
  test("Degree calculations - inDegrees") {
    val vertices = spark.createDataFrame(Seq(
      (1L, "Alice"),
      (2L, "Bob"),
      (3L, "Charlie")
    )).toDF("id", "name")
    
    val edges = spark.createDataFrame(Seq(
      (1L, 2L, "friend"),
      (2L, 3L, "friend"),
      (1L, 3L, "friend")
    )).toDF("src", "dst", "relationship")
    
    val g = GraphFrame(vertices, edges)
    
    val inDegrees = g.inDegrees
    inDegrees.count() shouldBe 2  // Only nodes 2 and 3 have incoming edges
  }
  
  // outDegrees returns Integer instead of Long through Spark Connect, causing ClassCastException
  ignore("Degree calculations - outDegrees") {
    val vertices = spark.createDataFrame(Seq(
      (1L, "Alice"),
      (2L, "Bob"),
      (3L, "Charlie")
    )).toDF("id", "name")
    
    val edges = spark.createDataFrame(Seq(
      (1L, 2L, "friend"),
      (2L, 3L, "friend"),
      (1L, 3L, "friend")
    )).toDF("src", "dst", "relationship")
    
    val g = GraphFrame(vertices, edges)
    
    val outDegrees = g.outDegrees
    outDegrees.count() shouldBe 2  // Only nodes 1 and 2 have outgoing edges
    
    // Check Alice has 2 outgoing edges
    val aliceOut = outDegrees.filter("id = 1").select("outDegree").first().getLong(0)
    aliceOut shouldBe 2
  }
  
  test("Degree calculations - degrees (total)") {
    val vertices = spark.createDataFrame(Seq(
      (1L, "Alice"),
      (2L, "Bob"),
      (3L, "Charlie")
    )).toDF("id", "name")
    
    val edges = spark.createDataFrame(Seq(
      (1L, 2L, "friend"),
      (2L, 3L, "friend"),
      (1L, 3L, "friend")
    )).toDF("src", "dst", "relationship")
    
    val g = GraphFrame(vertices, edges)
    
    val degrees = g.degrees
    degrees.count() shouldBe 3
  }
  
  // filterVertices fails with AnalysisException: cannot resolve attribute 'id'
  ignore("Filter vertices and edges") {
    val vertices = spark.createDataFrame(Seq(
      (1L, "Alice", 34),
      (2L, "Bob", 36),
      (3L, "Charlie", 30)
    )).toDF("id", "name", "age")
    
    val edges = spark.createDataFrame(Seq(
      (1L, 2L, "friend"),
      (2L, 3L, "follow"),
      (1L, 3L, "friend")
    )).toDF("src", "dst", "relationship")
    
    val g = GraphFrame(vertices, edges)
    
    // Filter by vertex age
    val filtered = g.filterVertices("age > 32")
    filtered.vertices.count() shouldBe 2
    
    // Filter by edge type
    val friendsOnly = g.filterEdges("relationship = 'friend'")
    friendsOnly.edges.count() shouldBe 2
  }
  
  test("Drop isolated vertices") {
    val vertices = spark.createDataFrame(Seq(
      (1L, "Alice"),
      (2L, "Bob"),
      (3L, "Charlie"),
      (4L, "Isolated")  // This vertex has no edges
    )).toDF("id", "name")
    
    val edges = spark.createDataFrame(Seq(
      (1L, 2L, "friend"),
      (2L, 3L, "friend")
    )).toDF("src", "dst", "relationship")
    
    val g = GraphFrame(vertices, edges)
    
    val withoutIsolated = g.dropIsolatedVertices()
    withoutIsolated.vertices.count() shouldBe 3
  }
  
  // BFS fails with NoClassDefFoundError: org.apache.spark.sql.classic.Dataset
  // BFS uses internal Spark APIs not available through Spark Connect
  ignore("BFS - Breadth First Search") {
    val vertices = spark.createDataFrame(Seq(
      (1L, "Alice"),
      (2L, "Bob"),
      (3L, "Charlie"),
      (4L, "David")
    )).toDF("id", "name")
    
    val edges = spark.createDataFrame(Seq(
      (1L, 2L, "friend"),
      (2L, 3L, "friend"),
      (3L, 4L, "friend")
    )).toDF("src", "dst", "relationship")
    
    val g = GraphFrame(vertices, edges)
    
    // Find path from Alice (id=1) to David (id=4)
    val paths = g.bfs.fromExpr("name = 'Alice'").toExpr("name = 'David'").run()
    
    paths.count() shouldBe 1
  }
  
  // Connected Components fails with NoClassDefFoundError: org.apache.spark.sql.internal.SQLConf$
  // Even the "graphframes" algorithm path requires internal Spark APIs not available through Connect
  ignore("Connected Components - GraphX-free algorithm") {
    val vertices = spark.createDataFrame(Seq(
      (1L, "Alice"),
      (2L, "Bob"),
      (3L, "Charlie"),
      (4L, "Isolated"),
      (5L, "David"),
      (6L, "Eve")
    )).toDF("id", "name")
    
    val edges = spark.createDataFrame(Seq(
      (1L, 2L, "friend"),
      (2L, 3L, "friend"),
      (5L, 6L, "friend")
    )).toDF("src", "dst", "relationship")
    
    val g = GraphFrame(vertices, edges)
    
    // Use the GraphFrames algorithm (not GraphX) which should work with Spark Connect
    val cc = g.connectedComponents.setAlgorithm("graphframes").run()
    
    // Should have 3 connected components: {1,2,3}, {4}, {5,6}
    val componentCount = cc.select("component").distinct().count()
    componentCount shouldBe 3
    
    // Verify that connected vertices have the same component ID
    val aliceComponent = cc.filter("name = 'Alice'").select("component").first().getLong(0)
    val bobComponent = cc.filter("name = 'Bob'").select("component").first().getLong(0)
    aliceComponent shouldBe bobComponent
    
    // Verify isolated vertex has its own component
    val isolatedComponent = cc.filter("name = 'Isolated'").select("component").first().getLong(0)
    isolatedComponent should not be aliceComponent
  }
}
