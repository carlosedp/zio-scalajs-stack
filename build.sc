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

object libVersion {
  val scala           = "3.2.0"
  val scalajs         = "1.10.1"
  val zio             = "2.0.2"
  val zhttp           = "2.0.0-RC11"
  val sttp            = "3.8.0"
  val organizeimports = "0.6.0"
  val scalajsdom      = "2.3.0"
  val scalatest       = "3.2.13"
}

trait Common extends ScalaModule with TpolecatModule with ScalafmtModule with ScalafixModule {
  override def scalaVersion = libVersion.scala
  def scalafixIvyDeps       = Agg(ivy"com.github.liancheng::organize-imports:${libVersion.organizeimports}")
  // Add repositories for snapshot builds
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
// Global commands
// -----------------------------------------------------------------------------

def lint(ev: eval.Evaluator) = T.command {
  mill.main.MainModule.evaluateTasks(
    ev,
    Seq("__.fix", "+", "mill.scalalib.scalafmt.ScalafmtModule/reformatAll", "__.sources"),
    mill.define.SelectMode.Separated,
  )(identity)
}

def deps(ev: eval.Evaluator) = T.command {
  mill.scalalib.Dependency.showUpdates(ev)
}

// -----------------------------------------------------------------------------
// Projects
// -----------------------------------------------------------------------------

object shared extends Common

object backend extends Common with DockerModule {
  // Runtime dependencies
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"dev.zio::zio:${libVersion.zio}",
    ivy"io.d11::zhttp:${libVersion.zhttp}",
  )

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
