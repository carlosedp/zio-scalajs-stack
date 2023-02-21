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
  val zio             = "2.0.9"
  val ziometrics      = "2.0.6"
  val ziologging      = "2.1.9"
  val ziohttp         = "0.0.4+6-79413b91-SNAPSHOT"
  val sttp            = "3.8.11"
  val organizeimports = "0.6.0"
  val scalajsdom      = "2.3.0"
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

// -----------------------------------------------------------------------------
// Projects
// -----------------------------------------------------------------------------

object backend
  extends Common    // Base config for the backend
  with NativeImage  // Build binary based on GrallVM Native Image
  with DockerModule // Build Docker images based on JVM using the app .jar
  with DockerNative // Build Docker images with app binary (GraalVM Native Image)
  with NativeImageConfig { // Uses config for Native image
  def scalaVersion         = versions.scala3
  def nativeImageClassPath = runClasspath()
  override def scalacOptions = T {
    super.scalacOptions() ++ Seq("-Wunused:imports") // Can be removed once it's integrated into tpolecat
  }
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"dev.zio::zio:${versions.zio}",
    ivy"dev.zio::zio-http:${versions.ziohttp}",
    ivy"dev.zio::zio-metrics-connectors:${versions.ziometrics}",
    ivy"dev.zio::zio-logging:${versions.ziologging}",
  )

  def dockerImage = "docker.io/carlosedp/zioscalajs-backend"
  def dockerPort  = 8080
  object dockerNative extends DockerNativeConfig with NativeImageConfig {
    // Config for the Native binary (GraalVM) based Docker image
    def nativeImageClassPath = runClasspath()
    def baseImage            = "ubuntu:22.04"
    def tags                 = List(dockerImage + "-native")
    def exposedPorts         = Seq(dockerPort)
  }

  object docker extends DockerConfig {
    // Config for the JVM based Docker image
    def baseImage    = "eclipse-temurin:17-jdk"
    def tags         = List(dockerImage + "-jdk")
    def exposedPorts = Seq(dockerPort)
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

// Shared config trait for Native Image and DockerNative build
// Using the JavaModule to access the module's main class and name for the binary
trait NativeImageConfig extends NativeImage {
  def nativeImageName         = "backend"
  def nativeImageMainClass    = "com.carlosedp.zioscalajs.backend.Main"
  def nativeImageGraalVmJvmId = T(versions.graalvm)
  // Options required by ZIO to be built by GraalVM
  // Ref. https://github.com/jamesward/hello-zio-http/blob/graalvm/build.sbt#L97-L108
  def nativeImageOptions = Seq(
    "--no-fallback",
    "--enable-http",
    "--enable-url-protocols=http,https",
    "--install-exit-handlers",
    "-Djdk.http.auth.tunneling.disabledSchemes=",
    "--initialize-at-run-time=io.netty.channel.DefaultFileRegion",
    "--initialize-at-run-time=io.netty.channel.epoll.Native",
    "--initialize-at-run-time=io.netty.channel.epoll.Epoll",
    "--initialize-at-run-time=io.netty.channel.epoll.EpollEventLoop",
    "--initialize-at-run-time=io.netty.channel.epoll.EpollEventArray",
    "--initialize-at-run-time=io.netty.channel.kqueue.KQueue",
    "--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventLoop",
    "--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventArray",
    "--initialize-at-run-time=io.netty.channel.kqueue.Native",
    "--initialize-at-run-time=io.netty.channel.unix.Limits",
    "--initialize-at-run-time=io.netty.channel.unix.Errors",
    "--initialize-at-run-time=io.netty.channel.unix.IovArray",
    "--initialize-at-run-time=io.netty.handler.ssl.BouncyCastleAlpnSslUtils",
    "--initialize-at-run-time=io.netty.handler.codec.compression.ZstdOptions",
    "--initialize-at-run-time=io.netty.incubator.channel.uring.Native",
    "--initialize-at-run-time=io.netty.incubator.channel.uring.IOUring",
    "--initialize-at-run-time=io.netty.incubator.channel.uring.IOUringEventLoopGroup",
  ) ++ (if (sys.props.get("os.name").contains("Linux")) Seq("--static") else Seq.empty)
}

object frontend extends ScalaJSModule with Common {
  def scalaVersion   = versions.scala3
  def scalaJSVersion = versions.scalajs
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"org.scala-js::scalajs-dom::${versions.scalajsdom}",
    ivy"com.softwaremill.sttp.client3::core::${versions.sttp}",
  )

  def scalaJSUseMainModuleInitializer = true
  def moduleSplitStyle                = T(ModuleSplitStyle.SmallModulesFor(List("com.carlosedp.zioscalajs.frontend")))
  def moduleKind                      = T(ModuleKind.ESModule)

  // These two tasks are used by Vite to get update path
  def fastLinkOut() = T.command(println(fastLinkJS().dest.path))
  def fullLinkOut() = T.command(println(fullLinkJS().dest.path))

  object test extends Tests with Common with TestModule.ScalaTest {
    // Test dependencies
    def ivyDeps = Agg(
      ivy"org.scalatest::scalatest::${versions.scalatest}",
    )
    def moduleKind       = T(ModuleKind.NoModule)
    def moduleSplitStyle = T(ModuleSplitStyle.FewestModules)
    def jsEnvConfig      = T(JsEnvConfig.JsDom())
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
  if (alias == "") {
    println("Use './mill run [alias]'.\nAvailable aliases:");
    aliases.foreach(x => println(x._1 + " " * (15 - x._1.length) + " - Commands: (" + x._2.mkString(", ") + ")"))
    sys.exit(1)
  }
  aliases.get(alias) match {
    case Some(t) =>
      mill.main.MainModule.evaluateTasks(
        ev,
        t.flatMap(x => x +: Seq("+")).flatMap(x => x.split(" ")).dropRight(1),
        mill.define.SelectMode.Separated,
      )(identity)
    case None => println(s"${Console.RED}ERROR:${Console.RESET} The task alias \"$alias\" does not exist.")
  }
  ()
}
