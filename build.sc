import mill._, mill.scalalib._, mill.scalalib.scalafmt._
import mill.scalajslib._, mill.scalajslib.api._
import mill.scalalib.api.Util.isScala3
import coursier.maven.MavenRepository

import $ivy.`com.lihaoyi::mill-contrib-docker:$MILL_VERSION`
import contrib.docker.DockerModule
import $ivy.`com.goyeau::mill-scalafix::0.2.11`
import com.goyeau.mill.scalafix.ScalafixModule
import $ivy.`io.github.davidgregory084::mill-tpolecat::0.3.2`
import io.github.davidgregory084.TpolecatModule
import $ivy.`io.github.alexarchambault.mill::mill-native-image::0.1.23`
import io.github.alexarchambault.millnativeimage.NativeImage
import $ivy.`com.carlosedp::mill-docker-nativeimage::0.5.0`
import com.carlosedp.milldockernative.DockerNative

object versions {
  val scala3          = "3.3.0-RC3"
  val scalajs         = "1.13.0"
  val zio             = "2.0.10"
  val ziometrics      = "2.0.7"
  val ziologging      = "2.1.11"
  val ziohttp         = "0.0.5"
  val sttp            = "3.8.14"
  val organizeimports = "0.6.0"
  val scalajsdom      = "2.4.0"
  val scalatest       = "3.2.15"
  val graalvm         = "graalvm-java17:22.3.1"
}

trait Common extends ScalaModule with TpolecatModule with ScalafmtModule with ScalafixModule {
  def sources = T.sources(
    millSourcePath / "src",
    millSourcePath / os.up / "shared" / "src",
  )
  def scalafixIvyDeps = Agg(ivy"com.github.liancheng::organize-imports:${versions.organizeimports}")
  def repositoriesTask = T.task { // Add snapshot repositories in case needed
    super.repositoriesTask() ++ Seq("oss", "s01.oss")
      .map(r => s"https://$r.sonatype.org/content/repositories/snapshots")
      .map(MavenRepository(_))
  }
}

// Shared config trait for Native Image and DockerNative build
// Using the JavaModule to access the module's main class and name for the binary
trait NativeImageConfig extends NativeImage {
  def nativeImageName         = "backend"
  def nativeImageMainClass    = "com.carlosedp.zioscalajs.backend.Main"
  def nativeImageGraalVmJvmId = T(versions.graalvm)
  def nativeImageOptions = super.nativeImageOptions() ++
    (if (sys.props.get("os.name").contains("Linux")) Seq("--static") else Seq.empty)
}

// -----------------------------------------------------------------------------
// Projects
// -----------------------------------------------------------------------------

object backend
  extends Common    // Base config for the backend
  with NativeImage  // Build binary based on GrallVM Native Image
  with DockerModule // Build Docker images based on JVM using the app .jar
  with DockerNative // Build Docker images with app binary (GraalVM Native Image)
  with NativeImageConfig { // Uses config for Native image
  def scalaVersion    = versions.scala3
  def useNativeConfig = T.input(T.env.get("NATIVECONFIG_GEN").contains("true"))
  def forkArgs = T {
    if (useNativeConfig()) Seq(s"-agentlib:native-image-agent=config-merge-dir=${this}/resources/META-INF/native-image")
    else Seq.empty
  }
  def nativeImageClassPath = runClasspath()
  override def scalacOptions = T {
    super.scalacOptions() ++ Seq(
      "-Wunused:imports",
      "-Wvalue-discard",
    )
  }
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"dev.zio::zio:${versions.zio}",
    ivy"dev.zio::zio-http:${versions.ziohttp}",
    ivy"dev.zio::zio-metrics-connectors:${versions.ziometrics}",
    ivy"dev.zio::zio-logging:${versions.ziologging}",
  )

  def dockerImage = "docker.io/carlosedp/zioscalajs-backend"
  def dockerPorts = Seq(8080)
  object dockerNative extends DockerNativeConfig with NativeImageConfig {
    // Config for the Native binary (GraalVM) based Docker image
    def nativeImageClassPath = runClasspath()
    def baseImage            = "ubuntu:22.04"
    def tags                 = List(dockerImage + "-native")
    def exposedPorts         = dockerPorts
  }

  object docker extends DockerConfig {
    // Config for the JVM based Docker image
    def baseImage    = "eclipse-temurin:17-jdk"
    def tags         = List(dockerImage + "-jdk")
    def exposedPorts = dockerPorts
  }

  object test extends Tests with Common {
    def ivyDeps = Agg(
      ivy"dev.zio::zio-test:${versions.zio}",
      ivy"dev.zio::zio-test-sbt:${versions.zio}",
      ivy"dev.zio::zio-http-testkit:${versions.ziohttp}",
    )
    def testFramework = T("zio.test.sbt.ZTestFramework")
  }
}

object frontend extends ScalaJSModule with Common {
  def scalaVersion   = versions.scala3
  def scalaJSVersion = versions.scalajs
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"org.scala-js::scalajs-dom::${versions.scalajsdom}",
    ivy"com.softwaremill.sttp.client3::core::${versions.sttp}",
  )
  def scalaJSUseMainModuleInitializer = true
  def moduleKind                      = T(ModuleKind.ESModule)
  def jsEnvConfig                     = T(JsEnvConfig.JsDom(args = List("--dns-result-order=ipv4first")))
  def moduleSplitStyle = ModuleSplitStyle.SmallModulesFor(List("com.carlosedp.zioscalajs.frontend"))

  // These two tasks are used by Vite to get update path
  def fastLinkOut() = T.command(println(fastLinkJS().dest.path))
  def fullLinkOut() = T.command(println(fullLinkJS().dest.path))

  object test extends Tests with Common with TestModule.ScalaTest {
    // Test dependencies
    def ivyDeps = Agg(
      ivy"org.scalatest::scalatest::${versions.scalatest}",
    )
    def moduleKind = T(ModuleKind.NoModule)
    def moduleSplitStyle = T(ModuleSplitStyle.FewestModules)
  }
}

// -----------------------------------------------------------------------------
// Command Aliases
// -----------------------------------------------------------------------------
// Alias commands are run like `./mill run [alias]`
// Define the alias as a map element containing the alias name and a Seq with the tasks to be executed
val aliases: Map[String, Seq[String]] = Map(
  "lint"     -> Seq("__.fix", "mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources"),
  "checkfmt" -> Seq("mill.scalalib.scalafmt.ScalafmtModule/checkFormatAll __.sources"),
  "deps"     -> Seq("mill.scalalib.Dependency/showUpdates"),
  "testall"  -> Seq("__.test"),
)

// The toplevel alias runner
def run(ev: eval.Evaluator, alias: String = "") = T.command {
  aliases.get(alias) match {
    case Some(t) =>
      mill.main.MainModule.evaluateTasks(ev, t.flatMap(x => Seq(x, "+")).flatMap(_.split("\\s+")).init, false)(identity)
    case None =>
      Console.err.println("Use './mill run [alias]'."); Console.out.println("Available aliases:")
      aliases.foreach(x => Console.out.println(s"${x._1.padTo(15, ' ')} - Commands: (${x._2.mkString(", ")})"));
      sys.exit(1)
  }
}
