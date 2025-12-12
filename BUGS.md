# Sail 0.4.3 Spark 4.0 Compatibility Issues

This document details compatibility issues found when using Spark 4.0 Connect client with Sail 0.4.3.

## Environment

- **Sail Version**: 0.4.3 (pysail package)
- **Spark Connect Client**: 4.0.0
- **Scala Version**: 2.13.12
- **Java Version**: 17.0.15
- **OS**: macOS (Homebrew installation)

## Issue #1: Missing RuntimeConfig - spark.sql.execution.arrow.useLargeVarTypes

### Description
Spark 4.0 Connect client attempts to read `spark.sql.execution.arrow.useLargeVarTypes` configuration from the server, but Sail 0.4.3 does not provide this configuration in RuntimeConfig responses.

### Error
```
java.util.NoSuchElementException: spark.sql.execution.arrow.useLargeVarTypes
  at org.apache.spark.sql.connect.RuntimeConfig.$anonfun$get$1(RuntimeConfig.scala:51)
  at org.apache.spark.sql.connect.SparkSession.largeVarTypes(SparkSession.scala:xxx)
  at org.apache.spark.sql.connect.SparkSession.createDataFrame(SparkSession.scala:138)
```

### Impact
- **Severity**: High - Blocks all DataFrame creation operations
- **Affected Operations**: `createDataFrame`, any operation that serializes data to Arrow format
- **User Experience**: Cannot use basic DataFrame functionality without workaround

### Expected Behavior
According to [Sail's own documentation](https://docs.lakesail.com/sail/latest/guide/sql/data-types/compatibility.html):

> The SQL string types (except TEXT) are mapped to either the Utf8 or LargeUtf8 type in Arrow, **depending on the `spark.sql.execution.arrow.useLargeVarTypes` configuration option**.

This indicates Sail supports this configuration internally but doesn't expose it via RuntimeConfig.

### Workaround
Set configuration client-side during SparkSession creation:

```scala
SparkSession.builder()
  .remote("sc://localhost:50051")
  .config("spark.sql.execution.arrow.useLargeVarTypes", "false")
  .getOrCreate()
```

### Recommendation
Sail should return this configuration with default value `false` in RuntimeConfig responses to match Spark 4.0 behavior.

---

## Issue #2: Missing RuntimeConfig - spark.sql.session.localRelationCacheThreshold

### Description
Spark 4.0 Connect client attempts to read `spark.sql.session.localRelationCacheThreshold` configuration, but Sail 0.4.3 does not provide this configuration.

### Error
```
java.util.NoSuchElementException: spark.sql.session.localRelationCacheThreshold
  at org.apache.spark.sql.connect.RuntimeConfig.$anonfun$get$1(RuntimeConfig.scala:51)
  at org.apache.spark.sql.connect.SparkSession.$anonfun$createDataset$1(SparkSession.scala:120)
```

### Impact
- **Severity**: High - Blocks DataFrame creation after Issue #1 is resolved
- **Affected Operations**: `createDataFrame`, `createDataset`
- **User Experience**: Cannot create DataFrames without workaround

### Spark Documentation
From [Spark 4.0 Configuration](https://spark.apache.org/docs/latest/configuration.html):

> **spark.sql.session.localRelationCacheThreshold**: The threshold for the size in bytes of local relations to be cached at the driver side after serialization. Default: 67108864 (64 MiB)

### Workaround
Set configuration client-side:

```scala
SparkSession.builder()
  .remote("sc://localhost:50051")
  .config("spark.sql.session.localRelationCacheThreshold", "67108864")
  .getOrCreate()
```

### Recommendation
Sail should return this configuration with the default value `67108864` (64MB) in RuntimeConfig responses.

---

## Issue #3: Unimplemented RPC - releaseSession

### Description
Spark 4.0 Connect client attempts to call `releaseSession` RPC when stopping SparkSession, but Sail 0.4.3 does not implement this endpoint.

### Error
```
WARN SparkSession: session.stop: Failed to release session
org.apache.spark.SparkException: UNIMPLEMENTED: release session
```

### Impact
- **Severity**: Low - Non-blocking warning
- **Affected Operations**: `SparkSession.stop()`, test cleanup
- **User Experience**: Warning message but no functional impact

### Recommendation
Implement `releaseSession` RPC endpoint for proper session lifecycle management, or at minimum document that this is intentionally not implemented.

---

## Test Results Summary

With the above workarounds in place, GraphFrames compatibility testing shows:

**DataFrame-Based Operations: 5/8 PASSING (62.5%)**

✅ **Working:**
- Basic graph creation from DataFrames
- Motif finding with filters
- Vertex degree calculations (inDegrees, degrees total)
- Graph filtering (drop isolated vertices)

❌ **Not Working:**
- Motif finding simple pattern (result count mismatch)
- outDegrees calculation (Integer/Long type mismatch)
- Filter vertices and edges (column resolution issue)

**GraphX-Based Operations: EXPECTED TO FAIL**
- All GraphX algorithms require RDD operations not supported in Spark Connect

---

## Reproduction

This repository provides a complete test suite to reproduce these issues:
https://github.com/yourusername/sailgraph

Steps:
1. Install Sail 0.4.3: `pip install pysail`
2. Clone test repository
3. Run tests: `sbt test`
4. Observe failures without workarounds
5. Apply workarounds and see 5/8 tests passing

---

## Recommendations for Sail Team

### Short Term (Bug Fixes)
1. Add `spark.sql.execution.arrow.useLargeVarTypes` to RuntimeConfig with default `false`
2. Add `spark.sql.session.localRelationCacheThreshold` to RuntimeConfig with default `67108864`
3. Implement `releaseSession` RPC or document intentional omission

### Medium Term (Compatibility)
1. Create Spark 4.0 compatibility matrix documentation
2. Add integration tests with Spark 4.0 Connect client
3. Validate all Spark 4.0 RuntimeConfig expectations

### Long Term (Process)
1. Establish compatibility testing with each Spark minor version
2. Document which Spark configurations are supported/ignored/required
3. Provide migration guides between Spark versions

---

## Additional Context

### Why These Configs Matter

**useLargeVarTypes**: Spark 4.0 introduced better large data handling. Clients need to know if the server supports large Arrow types (>2GB per column) to choose appropriate serialization format.

**localRelationCacheThreshold**: Spark 4.0 optimizes small DataFrame transfers by caching at driver vs. sending to executors. Client needs this threshold to make efficient decisions about data movement.

Both configurations have sensible defaults and should be safe to add to Sail's RuntimeConfig responses without breaking existing functionality.

### Version Compatibility Statement

Sail documentation states:
> "Sail is designed to be compatible with Spark 3.5 and later versions"

This suggests Spark 4.0 should work, but these missing configurations indicate incomplete 4.0 support. More explicit version compatibility documentation would help users choose appropriate versions.
