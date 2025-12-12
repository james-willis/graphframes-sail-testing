package com.sailgraph

import scala.sys.process._
import java.io.File

/**
 * Manages starting and stopping a Sail server for tests.
 * 
 * This requires Sail to be installed (cargo install lakesail).
 * Set SKIP_SAIL_SERVER=true environment variable to skip auto-start
 * if you're managing the server manually.
 */
object SailServerManager {
  
  private var sailProcess: Option[Process] = None
  private val skipAutoStart = sys.env.getOrElse("SKIP_SAIL_SERVER", "false").toLowerCase == "true"
  
  def startServer(host: String = "127.0.0.1", port: Int = 50051): Unit = {
    if (skipAutoStart) {
      println(s"Skipping Sail server auto-start (SKIP_SAIL_SERVER=true)")
      return
    }
    
    if (sailProcess.isDefined) {
      println("Sail server already running")
      return
    }
    
    println(s"Starting Sail server on $host:$port...")
    
    // Check if Sail is installed
    val sailCheck = "which sail".!
    if (sailCheck != 0) {
      throw new RuntimeException(
        "Sail not found. Install with: pip install pysail\n" +
        "Or set SKIP_SAIL_SERVER=true and start the server manually."
      )
    }
    
    // Start Sail server in background
    val processBuilder = Process(Seq("sail", "spark", "server", "--ip", host, "--port", port.toString))
    sailProcess = Some(processBuilder.run())
    
    // Wait a bit for server to start
    println("Waiting for Sail server to start...")
    Thread.sleep(5000)
    
    println(s"Sail server started at sc://$host:$port")
  }
  
  def stopServer(): Unit = {
    if (skipAutoStart) {
      return
    }
    
    sailProcess.foreach { process =>
      println("Stopping Sail server...")
      process.destroy()
      process.exitValue() // Wait for process to terminate
      println("Sail server stopped")
    }
    sailProcess = None
  }
  
  def isRunning: Boolean = sailProcess.exists(_.isAlive())
}
