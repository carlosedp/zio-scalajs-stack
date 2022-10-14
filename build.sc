import mill._, mill.scalalib._
import mill.scalajslib._, mill.scalajslib.api._
import scalafmt._
import coursier.maven.MavenRepository

import $ivy.`com.lihaoyi::mill-contrib-docker:$MILL_VERSION`
import contrib.docker.DockerModule
import $ivy.`com.goyeau::mill-scalafix::0.2.10`
import com.goyeau.mill.scalafix.ScalafixModule
import $ivy.`io.github.davidgregory084::mill-tpolecat::0.3.1`
import io.github.davidgregory084.TpolecatModule

import $ivy.`io.github.alexarchambault.mill::mill-native-image::0.1.21`
import io.github.alexarchambault.millnativeimage.NativeImage

import $ivy.`com.carlosedp::mill-docker-nativeimage::0.0.1`
import com.carlosedp.milldockernative.DockerNative

object versions {
  val scala213        = "2.13.10"
  val scala3          = "3.2.0"
  val scalajs         = "1.11.0"
  val zio             = "2.0.2"
  val zhttp           = "2.0.0-RC11"
  val sttp            = "3.8.2"
  val organizeimports = "0.6.0"
  val scalajsdom      = "2.3.0"
  val scalatest       = "3.2.14"
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
  // override def scalacOptions = super.scalacOptions() ++ Seq("-Xsource:3")
}

// -----------------------------------------------------------------------------
// Projects
// -----------------------------------------------------------------------------

// object shared extends Common

object backend
  extends Common    // Base config for the backend
  with NativeImage  // Build binary based on GrallVM Native Image
  with DockerModule // Build Docker images based on JVM using the app .jar
  with DockerNative // Build Docker images with app binary (GraalVM Native Image)
  with NativeImageConfig { // Uses config for Native image
  def scalaVersion         = versions.scala213
  def nativeImageClassPath = runClasspath()
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"dev.zio::zio:${versions.zio}",
    ivy"io.d11::zhttp:${versions.zhttp}",
  )
  override def scalacPluginIvyDeps =
    Agg(ivy"org.scalameta:::semanticdb-scalac:4.5.13")

  object dockerNative extends DockerNativeConfig with NativeImageConfig {
    def nativeImageClassPath = runClasspath()
    def baseImage            = "debian"
    def tags                 = List("docker.io/carlosedp/zioscalajs-backend")
    def exposedPorts         = Seq(8080)
  }

  object docker extends DockerConfig {
    def tags         = List("docker.io/carlosedp/zioscalajs-backend")
    def exposedPorts = Seq(8080)
  }

  object test extends Tests with Common {
    // Test dependencies
    def ivyDeps = Agg(
      ivy"dev.zio::zio-test:${versions.zio}",
      ivy"dev.zio::zio-test-sbt:${versions.zio}",
    )
    def testFramework = T("zio.test.sbt.ZTestFramework")
  }
}

// Shared config trait for Native Image and DockerNative build
trait NativeImageConfig extends NativeImage {
  def nativeImageName = "backend"
  def nativeImageGraalVmJvmId = T {
    sys.env.getOrElse("GRAALVM_ID", "graalvm-java17:22.2.0")
  }
  def nativeImageMainClass = "com.carlosedp.zioscalajs.backend.MainApp"
  def nativeImageOptions = Seq(
    "--no-fallback",
    "--enable-url-protocols=http,https",
    "-Djdk.http.auth.tunneling.disabledSchemes=",
    "--install-exit-handlers",
    "--enable-http",
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
    "--allow-incomplete-classpath",
  ) ++ (if (System.getProperty("os.name").contains("Linux")) Seq("--static") else Seq.empty)
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
  def moduleSplitStyle                = T(ModuleSplitStyle.SmallModulesFor(List("com.carlosedp.zioscalajs.frontend")))

  // These two tasks are used by Vite to get update path
  def fastLinkOut() = T.command {
    val target = fastLinkJS()
    println(target.dest.path)
  }
  def fullLinkOut() = T.command {
    val target = fullLinkJS()
    println(target.dest.path)
  }

  object test extends Tests with Common with TestModule.ScalaTest {
    // Test dependencies
    def ivyDeps = Agg(
      ivy"org.scalatest::scalatest::${versions.scalatest}",
    )
    def jsEnvConfig = T(JsEnvConfig.JsDom())
  }
}

// -----------------------------------------------------------------------------
// Global commands
// -----------------------------------------------------------------------------

// Toplevel commands
def runTasks(t: Seq[String])(implicit ev: eval.Evaluator) = T.task {
  mill.main.MainModule.evaluateTasks(
    ev,
    t.flatMap(x => x +: Seq("+")).flatMap(x => x.split(" ")).dropRight(1),
    mill.define.SelectMode.Separated,
  )(identity)
}
def lint(implicit ev: eval.Evaluator) = T.command {
  runTasks(Seq("__.fix", "mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources"))
}
def deps(ev: eval.Evaluator) = T.command {
  mill.scalalib.Dependency.showUpdates(ev)
}
