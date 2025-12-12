name := "sailgraph"

version := "0.1.0"

scalaVersion := "2.13.12"

// Spark Connect dependencies
libraryDependencies ++= Seq(
  // Use only Spark Connect client (not spark-sql which includes classic Spark)
  "org.apache.spark" %% "spark-connect-client-jvm" % "4.0.0",
  
  // GraphFrames
  "io.graphframes" % "graphframes-spark4_2.13" % "0.10.0",
  
  // Testing
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)

// Test configuration
Test / parallelExecution := false
Test / fork := true
// Add JVM options for Arrow compatibility with Java 17+
Test / javaOptions ++= Seq(
  "--add-opens=java.base/java.nio=ALL-UNNAMED"
)
