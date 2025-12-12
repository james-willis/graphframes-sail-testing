package com.sailgraph

import org.graphframes.GraphFrame

/**
 * Tests for GraphX/RDD-based GraphFrames operations that WON'T work with Spark Connect.
 * These tests are expected to fail or throw exceptions.
 */
class GraphXBasedAlgorithmsTest extends GraphFramesConnectTestBase {
  
  ignore("PageRank - EXPECTED TO FAIL") {
    // PageRank uses GraphX internally and won't work through Spark Connect
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
    
    // This should fail because PageRank uses GraphX
    assertThrows[Exception] {
      val results = g.pageRank.resetProbability(0.15).maxIter(10).run()
      results.vertices.show()
    }
  }
  
  ignore("Connected Components - EXPECTED TO FAIL") {
    // Connected Components uses GraphX/Pregel and won't work
    val vertices = spark.createDataFrame(Seq(
      (1L, "Alice"),
      (2L, "Bob"),
      (3L, "Charlie"),
      (4L, "Isolated")
    )).toDF("id", "name")
    
    val edges = spark.createDataFrame(Seq(
      (1L, 2L, "friend"),
      (2L, 3L, "friend")
    )).toDF("src", "dst", "relationship")
    
    val g = GraphFrame(vertices, edges)
    
    // This should fail
    assertThrows[Exception] {
      val cc = g.connectedComponents.run()
      cc.show()
    }
  }
  
  ignore("Strongly Connected Components - EXPECTED TO FAIL") {
    // Uses GraphX implementation
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
    
    assertThrows[Exception] {
      val scc = g.stronglyConnectedComponents.maxIter(10).run()
      scc.show()
    }
  }
  
  ignore("Shortest Paths - MAY FAIL") {
    // Shortest paths has both GraphX and GraphFrames implementations
    // The GraphFrames version might work, but needs testing
    val vertices = spark.createDataFrame(Seq(
      (1L, "Alice"),
      (2L, "Bob"),
      (3L, "Charlie"),
      (4L, "David")
    )).toDF("id", "name")
    
    val edges = spark.createDataFrame(Seq(
      (1L, 2L, "friend"),
      (2L, 3L, "friend"),
      (3L, 4L, "friend"),
      (1L, 4L, "friend")
    )).toDF("src", "dst", "relationship")
    
    val g = GraphFrame(vertices, edges)
    
    // Try the GraphFrames implementation (algorithm="graphframes")
    assertThrows[Exception] {
      val paths = g.shortestPaths.landmarks(Seq(1L, 4L)).run()
      paths.show()
    }
  }
  
  ignore("Label Propagation - MAY FAIL") {
    // Has both implementations, needs testing
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
    
    assertThrows[Exception] {
      val lp = g.labelPropagation.maxIter(5).run()
      lp.show()
    }
  }
  
  ignore("SVD++ - EXPECTED TO FAIL") {
    // GraphX only
    val vertices = spark.createDataFrame(Seq(
      (1L, "User1"),
      (2L, "User2"),
      (3L, "Movie1")
    )).toDF("id", "name")
    
    val edges = spark.createDataFrame(Seq(
      (1L, 3L, 5.0),
      (2L, 3L, 4.0)
    )).toDF("src", "dst", "rating")
    
    val g = GraphFrame(vertices, edges)
    
    assertThrows[Exception] {
      val model = g.svdPlusPlus.run()
      model.show()
    }
  }
}
