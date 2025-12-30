# GraphFrames + Sail Compatibility Test Project

This project tests GraphFrames algorithm compatibility with LakeSail via Spark Connect.

## Project Structure
- Scala 2.13 project using sbt build tool
- ScalaTest for test suites
- Spark 4.0 Connect client dependencies
- GraphFrames library integration

## Testing Strategy
Tests verify which GraphFrames algorithms work through Spark Connect by:
1. Testing DataFrame-based operations (motif finding, BFS, degrees)
2. Testing algorithms that use GraphX/RDDs (PageRank, connected components)
3. Documenting which algorithms succeed/fail

## Key Dependencies
- Scala 2.13
- Apache Spark 4.0 with Spark Connect
- GraphFrames 0.10.0
- ScalaTest 3.2+
