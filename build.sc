import mill._, mill.scalalib._, mill.scalalib.scalafmt._
import mill.scalajslib._, mill.scalajslib.api._
import mill.scalalib.api.ZincWorkerUtil.isScala3
import coursier.Repositories

import $ivy.`com.lihaoyi::mill-contrib-docker:$MILL_VERSION`
import contrib.docker.DockerModule
import $ivy.`com.goyeau::mill-scalafix::0.3.1`
import com.goyeau.mill.scalafix.ScalafixModule
import $ivy.`io.github.davidgregory084::mill-tpolecat::0.3.5`
import io.github.davidgregory084.TpolecatModule
import $ivy.`io.github.alexarchambault.mill::mill-native-image::0.1.25`
import io.github.alexarchambault.millnativeimage.NativeImage
import $ivy.`com.carlosedp::mill-docker-nativeimage::0.6.0`
import com.carlosedp.milldockernative.DockerNative
import $ivy.`com.carlosedp::mill-aliases::0.2.1`
import com.carlosedp.aliases._

object versions {
  val scala3          = "3.3.0"
  val scalajs         = "1.13.1"
  val zio             = "2.0.15"
  val ziometrics      = "2.0.8"
  val ziologging      = "2.1.13"
  val ziohttp         = "3.0.0-RC2"
  val sttp            = "3.8.15"
  val scalajsdom      = "2.6.0"
  val scalatest       = "3.2.16"
  val graalvm         = "graalvm-java17:22.3.2"
}

trait Common extends ScalaModule with TpolecatModule with ScalafmtModule with ScalafixModule {
  def sources = T.sources(
    millSourcePath / "src",
    millSourcePath / os.up / "shared" / "src",
  )
  def repositoriesTask = T.task {
    super.repositoriesTask() ++ Seq(Repositories.sonatype("snapshots"), Repositories.sonatypeS01("snapshots"))
  }
}

// Shared config trait for Native Image and DockerNative build
// Using the JavaModule to access the module's main class and name for the binary
trait NativeImageConfig extends NativeImage {
  def nativeImageName         = "backend"
  def nativeImageMainClass    = "com.carlosedp.zioscalajs.backend.Main"
  def nativeImageGraalVmJvmId = T(versions.graalvm)
  def nativeImageOptions = super.nativeImageOptions() ++
    // GraalVM initializes all classes at runtime, so lets ignore all configs from jars since some change this behavior
    Seq("--exclude-config", "/.*.jar", ".*.properties") ++
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
      // "-Wunused:imports",
      "-Wvalue-discard"
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

  object test extends ScalaTests with Common {
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
  def jsEnvConfig                     = T(JsEnvConfig.JsDom())
  def moduleSplitStyle                = ModuleSplitStyle.SmallModulesFor(List("com.carlosedp.zioscalajs.frontend"))

  // These two tasks are used by Vite to get update path
  def fastLinkOut() = T.command(println(fastLinkJS().dest.path))
  def fullLinkOut() = T.command(println(fullLinkJS().dest.path))

  object test extends ScalaJSTests with Common with TestModule.ScalaTest {
    // Test dependencies
    def ivyDeps = Agg(
      ivy"org.scalatest::scalatest::${versions.scalatest}"
    )
    def moduleKind       = T(ModuleKind.NoModule)
    def moduleSplitStyle = T(ModuleSplitStyle.FewestModules)
  }
}

// -----------------------------------------------------------------------------
// Command Aliases
// -----------------------------------------------------------------------------
// Alias commands are run with: `./mill Alias/run [alias]`
// Define the alias name with the `alias` type with a sequence of tasks to be executed
object MyAliases extends Aliases {
  def lint     = alias("__.fix", "mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources")
  def checkfmt = alias("mill.scalalib.scalafmt.ScalafmtModule/checkFormatAll __.sources")
  def deps     = alias("mill.scalalib.Dependency/showUpdates")
  def testall  = alias("__.test")
}
