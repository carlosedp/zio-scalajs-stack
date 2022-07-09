import mill._, mill.scalalib._
import mill.scalajslib._, mill.scalajslib.api._
import scalafmt._
import coursier.maven.MavenRepository

import $ivy.`com.goyeau::mill-scalafix::0.2.9`
import com.goyeau.mill.scalafix.ScalafixModule
import $ivy.`io.github.davidgregory084::mill-tpolecat::0.3.1`
import io.github.davidgregory084.TpolecatModule

object libVersion {
  val scala           = "3.1.3"
  val scalajs         = "1.10.1"
  val zio             = "2.0.0"
  val organizeimports = "0.6.0"
  val scalajsdom      = "2.2.0"
  val scalatest       = "3.2.11"
}

trait Common extends ScalaModule with Aliases with TpolecatModule {
  def scalaVersion    = libVersion.scala
  def scalafixIvyDeps = Agg(ivy"com.github.liancheng::organize-imports:${libVersion.organizeimports}")
  def scalacOptions   = T(super.scalacOptions() ++ Seq("-Xsemanticdb")) // Disable semanticDB since we use Scala 3

  // Add repositories for snapshot builds
  def repositoriesTask = T.task {
    super.repositoriesTask() ++ Seq("oss", "s01.oss")
      .map("https://" + _ + ".sonatype.org/content/repositories/snapshots")
      .map(MavenRepository(_))
  }
}

trait Aliases extends Module with ScalafmtModule with ScalafixModule {
  def style(): mill.define.Command[Unit] =
    T.command {
      reformat()()
      fix()()
    }
  // Format and fixes all files in all projects
  def lint(ev: mill.eval.Evaluator) = T.command {
    def findAllChildren(module: Module): Seq[Module] = {
      val children = module.millModuleDirectChildren
      if (children.isEmpty) Seq(module)
      else module +: children.flatMap(findAllChildren)
    }

    def eval[T](e: mill.define.Task[T]): T =
      ev.evaluate(mill.api.Strict.Agg(e)).values match {
        case Seq()     => throw new NoSuchElementException
        case Seq(e: T) => e
      }

    findAllChildren(ev.rootModule).collect { case mod: ScalafmtModule with ScalafixModule => mod }.foreach { mod =>
      println(s"Formatting module $mod...")
      eval(mod.fix())      // Organize imports
      eval(mod.reformat()) // Scalafmt
    }
  }

  // Checks for library updates in all projects
  def deps(ev: eval.Evaluator) = T.command {
    mill.scalalib.Dependency.showUpdates(ev)
  }
}

// -----------------------------------------------------------------------------
// Projects
// -----------------------------------------------------------------------------

object all extends Common {} // Dummy target for some mill commands line "deps", "lint", etc.

object backend extends Common {
  // Runtime dependencies
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"dev.zio::zio:${libVersion.zio}",
  )
  object test extends Tests {
    // Test dependencies
    def ivyDeps = Agg(
      ivy"dev.zio::zio-test:${libVersion.zio}",
      ivy"dev.zio::zio-test-sbt:${libVersion.zio}",
    )
    def testFramework = T("zio.test.sbt.ZTestFramework")
  }
}

object frontend extends ScalaJSModule with Common {
  def scalaJSVersion                  = libVersion.scalajs
  def scalaJSUseMainModuleInitializer = true
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"org.scala-js::scalajs-dom::${libVersion.scalajsdom}",
  )
  def jsEnvConfig = T(scalajslib.api.JsEnvConfig.JsDom())
  // def moduleKind  = T(scalajslib.api.ModuleKind.CommonJSModule)

  object test extends Tests with TestModule.ScalaTest {
    // Test dependencies
    def ivyDeps = Agg(
      ivy"org.scalatest::scalatest::${libVersion.scalatest}",
    )

    def testFramework = T("org.scalatest.tools.Framework")
    def jsEnvConfig   = T(scalajslib.api.JsEnvConfig.JsDom())
  }
}
