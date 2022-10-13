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

import $ivy.`com.carlosedp::mill-docker-nativeimage::0.1-SNAPSHOT`
import com.carlosedp.milldockernative.DockerNative

object libVersion {
  val scala           = "3.2.0"
  val scalajs         = "1.11.0"
  val zio             = "2.0.2"
  val zhttp           = "2.0.0-RC11"
  val sttp            = "3.8.2"
  val organizeimports = "0.6.0"
  val scalajsdom      = "2.3.0"
  val scalatest       = "3.2.14"
}

trait Common extends ScalaModule with TpolecatModule with ScalafmtModule with ScalafixModule {
  override def scalaVersion = libVersion.scala
  def scalafixIvyDeps       = Agg(ivy"com.github.liancheng::organize-imports:${libVersion.organizeimports}")
  def repositoriesTask = T.task { // Add snapshot repositories in case needed
    super.repositoriesTask() ++ Seq("oss", "s01.oss")
      .map(r => s"https://$r.sonatype.org/content/repositories/snapshots")
      .map(MavenRepository(_))
  }
  def sources = T.sources(
    millSourcePath / "src",
    millSourcePath / os.up / "shared" / "src",
  )
}

// -----------------------------------------------------------------------------
// Projects
// -----------------------------------------------------------------------------

// object shared extends Common

object backend extends Common with DockerModule with DockerNative {
  // Runtime dependencies
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"dev.zio::zio:${libVersion.zio}",
    ivy"io.d11::zhttp:${libVersion.zhttp}",
  )

  object dockerNative extends DockerNativeConfig {
    def nativeImageName = "backend"
    def nativeImageGraalVmJvmId = T {
      sys.env.getOrElse("GRAALVM_ID", "graalvm-java17:22.2.0")
    }
    def nativeImageClassPath = runClasspath()
    def nativeImageMainClass = "com.carlosedp.zioscalajs.backend.MainApp"
    def nativeImageOptions = super.nativeImageOptions() ++ Seq(
      "--no-fallback",
      "--enable-url-protocols=http,https",
      "-Djdk.http.auth.tunneling.disabledSchemes=",
      // "--static", // Does not work on MacOS
      "--no-fallback",
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
    )

    def tags         = List("docker.io/carlosedp/zioscalajs-backend")
    def exposedPorts = Seq(8080)
  }

  object docker extends DockerConfig {
    def tags         = List("docker.io/carlosedp/zioscalajs-backend")
    def exposedPorts = Seq(8080)
  }
  object test extends Tests with Common {
    // Test dependencies
    def ivyDeps = Agg(
      ivy"dev.zio::zio-test:${libVersion.zio}",
      ivy"dev.zio::zio-test-sbt:${libVersion.zio}",
    )
    def testFramework = T("zio.test.sbt.ZTestFramework")
  }
}

object frontend extends ScalaJSModule with Common {
  def scalaJSVersion = libVersion.scalajs
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"org.scala-js::scalajs-dom::${libVersion.scalajsdom}",
    ivy"com.softwaremill.sttp.client3::core::${libVersion.sttp}",
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
      ivy"org.scalatest::scalatest::${libVersion.scalatest}",
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
