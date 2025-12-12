package com.sailgraph

import org.apache.spark.sql.SparkSession
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Base test class for GraphFrames compatibility tests with Spark Connect.
 * This provides setup for connecting to a Spark Connect server.
 * 
 * By default, this will attempt to start a Sail server automatically.
 * Set SKIP_SAIL_SERVER=true to manage the server manually.
 * Set SPARK_CONNECT_URL to override the default endpoint (sc://localhost:50051).
 * 
 * SPARK 4.0 + SAIL 0.4.3 COMPATIBILITY NOTES:
 * 
 * When using Spark 4.0 Connect client with Sail 0.4.3, several Spark 4.0 
 * configurations are not yet implemented in Sail and must be set client-side:
 * 
 * 1. spark.sql.execution.arrow.useLargeVarTypes (default: false)
 *    - Controls Arrow large variable-width types for strings/binary
 *    - Missing from Sail RuntimeConfig responses
 *    
 * 2. spark.sql.session.localRelationCacheThreshold (default: 67108864 bytes)
 *    - Size threshold for caching local relations at driver
 *    - Missing from Sail RuntimeConfig responses
 * 
 * These configs are set below to avoid NoSuchElementException errors.
 * This is a workaround until Sail implements these Spark 4.0 configurations.
 */
abstract class GraphFramesConnectTestBase extends AnyFunSuite with Matchers with BeforeAndAfterAll {
  
  var spark: SparkSession = _
  
  override def beforeAll(): Unit = {
    super.beforeAll()
    
    // Start Sail server (unless explicitly skipped)
    SailServerManager.startServer()
    
    // Configure Spark Connect endpoint
    // Default assumes Sail/Spark Connect server running on localhost:50051
    val connectEndpoint = sys.env.getOrElse("SPARK_CONNECT_URL", "sc://localhost:50051")
    
    println(s"Connecting to Spark Connect at $connectEndpoint")
    
    spark = SparkSession.builder()
      .remote(connectEndpoint)
      .appName("GraphFrames Sail Test")
      .config("spark.sql.execution.arrow.useLargeVarTypes", "false")
      .config("spark.sql.session.localRelationCacheThreshold", "67108864") // 64MB default
      .getOrCreate()
      
    println("Spark session created successfully")
  }
  
  override def afterAll(): Unit = {
    if (spark != null) {
      spark.stop()
    }
    
    // Stop Sail server
    SailServerManager.stopServer()
    super.afterAll()
  }
}
