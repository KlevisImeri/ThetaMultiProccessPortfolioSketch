package main

import java.nio.file.Path
import java.nio.file.Paths

data class Xcfa(val name: String = "dummy")
data class Result(val success: Boolean = false, val output: String = "")

object Db {
  var xcfa: Xcfa? = null
  var result: Result? = null
}

fun CegarChecker(xcfa: Xcfa, partialRes: Result): Result {
  // Doing complex cegar in here with partialResult 
  if (System.getenv("FAIL_CEGAR") == "true") {
    return Result(success = false, output = "CEGAR failed")
  }
  return Result(success = true, output = "CEGAR completed for ${xcfa.name}");
}


fun CegarCheckerMain(args: Array<String>) {
  // You take the db through argumetns
  Logger.info("CegarCheckerMain: Starting CEGAR check...")
  val xcfa = Db.xcfa ?: run {
    Logger.error("CegarCheckerMain: No XCFA found in Db")
    return
  }
  val result = CegarChecker(xcfa, Result())
  Db.result = result
  Logger.result("CegarCheckerMain: Completed - %s", result.output)
}

// INFO: the object and @JvmStatic in only needed if you have two main fucntions
// in the same file. So we could just put the CegarCheckerMain and we would not
// have needed this part and more cleaner cause you can call the CegarCheckerMain
// directly.
object CegarCheckerKt {
  @JvmStatic
  fun main(args: Array<String>) = CegarCheckerMain(args)
}


class ParseXcfa {
  fun execute(path: Path): Xcfa {
    val xcfa = Xcfa(name = path.toString())
    Logger.info("ParseXcfa: Parsed XCFA from: %s", path)
    return xcfa
  }
}

class XcfaCegar {
  fun execute(xcfa: Xcfa): Result {
    Logger.info("XcfaCegar: Running CEGAR check for: %s", xcfa.name)
    val result = CegarChecker(xcfa, Result())
    Logger.debug("XcfaCegar: Result = %s (success=%b)", result.output, result.success)
    return result
  }
}

class XcfaCegarSpawnProcess {
  fun execute(xcfa: Xcfa, partialRes: Result): Result {
    Logger.info("XcfaCegarSpawnProcess: Spawning process for: %s", xcfa.name)
    
    val classpath = System.getProperty("java.class.path")
    val javaHome = System.getProperty("java.home")

    Db.xcfa = xcfa
    
    ProcessBuilder(
      "$javaHome/bin/java", "-cp", classpath,
      "main.CegarCheckerKt"
    ).inheritIO().start().waitFor()
    
    val result = Db.result ?: Result(false, "Result from spawned process")
    Logger.debug("XcfaCegarSpawnProcess: Result = %s", result.output)
    return result
  }
}

class StartNode(private val args: Array<String>) {
  fun execute(): Path {
    val path = args.firstOrNull() ?: error("No input file provided")
    Logger.info("StartNode: Parsing input file: %s", path)
    return Paths.get(path)
  }
}

class OutputNode {
  fun execute(result: Result) {
    Logger.result("OutputNode: Final result = %s", result.output)
  }
}

fun main(args: Array<String>) {
  // WARN: this code is only to show an example. 
  // It probalby has a lot of bugs and unnecessary operations.

  println("----------------------------START------------------------------")
  Logger.init("debug|info|result")
  // Logger.init("debug|info|result|portfolio")
  Logger.info("Portfolio main started with args: %s", args.joinToString(", "))

  val portfolio = PortfolioGraph()

  val start = StartNode(args)
  val parseXcfa = ParseXcfa()
  val cegar = XcfaCegar()
  val cegarProcess = XcfaCegarSpawnProcess()
  val output1 = OutputNode()
  val output2 = OutputNode()


  portfolio.connect(start, parseXcfa)
  portfolio.connect(parseXcfa, cegar)
  portfolio.connect(cegar, output1) { result ->
    (result as? Result)?.success == true
  }
  portfolio.connect(parseXcfa, cegarProcess)
  portfolio.connect(cegar, cegarProcess) { result ->
    (result as? Result)?.success != true
  }
  portfolio.connect(cegarProcess, output2)

  Logger.debug("Validating portfolio graph...")
  val errors = portfolio.validate()
  Logger.debug("Portfolio validation complete. Errors: %d", errors.size)
  errors.forEach { System.err.println("VALIDATION ERROR: $it") }

  println(portfolio.visualize())
  println()

  Logger.debug("Executing portfolio graph...")
  try {
    portfolio.execute()
    Logger.info("Portfolio execution complete")
  } catch (e: Exception) {
    Logger.error("Portfolio execution failed: %s", e.message)
  }
  println("-----------------------------END-------------------------------")
}
