import mill._, mill.scalalib._, mill.scalalib.scalafmt._
import mill.scalajslib._, mill.scalajslib.api._
import coursier.Repositories

import $ivy.`com.lihaoyi::mill-contrib-docker:$MILL_VERSION`
import contrib.docker.DockerModule
import $ivy.`com.goyeau::mill-scalafix::0.3.2`
import com.goyeau.mill.scalafix.ScalafixModule
import $ivy.`io.github.davidgregory084::mill-tpolecat::0.3.5`
import io.github.davidgregory084.TpolecatModule
import $ivy.`io.github.alexarchambault.mill::mill-native-image::0.1.26`
import io.github.alexarchambault.millnativeimage.NativeImage
import $ivy.`com.carlosedp::mill-docker-nativeimage::0.6.1`
import com.carlosedp.milldockernative.DockerNative
import $ivy.`com.carlosedp::mill-aliases::0.4.1`
import com.carlosedp.aliases._

object versions {
    val scala3     = "3.3.3"
    val scalajs    = "1.16.0"
    val zio        = "2.0.22"
    val ziometrics = "2.3.1"
    val ziologging = "2.2.3"
    val ziohttp    = "3.0.0-RC6"
    val sttp       = "3.9.6"
    val scalajsdom = "2.8.0"
    val scalatest  = "3.2.18"
    val graalvm    = "graalvm-java21:21.0.2"
}

trait Common extends ScalaModule with TpolecatModule with ScalafmtModule with ScalafixModule {
    override def scalaVersion = versions.scala3
    def sources = T.sources(
        millSourcePath / "src",
        millSourcePath / os.up / "shared" / "src",
    )
    def repositoriesTask = T.task {
        super.repositoriesTask() ++ Seq(Repositories.sonatype("snapshots"), Repositories.sonatypeS01("snapshots"))
    }
    def scalacOptions = T {
        super.scalacOptions().filterNot(Set("-Xfatal-warnings")) ++ // Disable fatal warnings to use the silencer -Wconf
            Seq(
                "-Wunused:all",
                "-Wvalue-discard",
                "-Wnonunit-statement",
                "-Wconf:msg=unused value of type org.scalatest.Assertion:s", // Disable ScalaTest Assertion warnings due -Wnonunit-statement
            )
    }
    def scalafixIvyDeps = super.scalacPluginIvyDeps() ++ Agg(ivy"com.github.xuwei-k::scalafix-rules:0.3.0")
}

// Shared config trait for Native Image and DockerNative build78
trait NativeImageConfig extends NativeImage {
    def nativeImageName         = "backend"
    def nativeImageMainClass    = "com.carlosedp.zioscalajs.backend.Main"
    def nativeImageGraalVmJvmId = T(versions.graalvm)
    // GraalVM initializes all classes at runtime, so lets ignore all configs from jars since some change this behavior
    def nativeImageOptions = super.nativeImageOptions() ++
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
    with NativeImageConfig { parent => // Uses config for Native image
    def nativeImageClassPath = runClasspath()
    def genNativeConfig      = T.input(T.env.get("NATIVECONFIG_GEN").contains("true"))
    def forkArgs = T {
        if (genNativeConfig())
            Seq(s"-agentlib:native-image-agent=config-merge-dir=backend/resources/META-INF/native-image")
        else Seq.empty
    }
    def ivyDeps = Agg(
        ivy"dev.zio::zio:${versions.zio}",
        ivy"dev.zio::zio-http:${versions.ziohttp}",
        ivy"dev.zio::zio-metrics-connectors:${versions.ziometrics}",
        ivy"dev.zio::zio-metrics-connectors-prometheus:${versions.ziometrics}",
        ivy"dev.zio::zio-logging:${versions.ziologging}",
    )

    object dockerNative extends DockerNativeConfig with NativeImageConfig {
        // Config for the Native binary (GraalVM) based Docker image
        def nativeImageClassPath = runClasspath()
        def baseImage            = "ubuntu:22.04"
        def tags                 = List("docker.io/carlosedp/zioscalajs-backend-native")
        def exposedPorts         = Seq(8080)
    }

    object docker extends DockerConfig {
        // Config for the JVM based Docker image
        def baseImage    = "eclipse-temurin:21-jre"
        def tags         = List("docker.io/carlosedp/zioscalajs-backend-jre")
        def exposedPorts = Seq(8080)
    }

    object test extends ScalaTests with TestModule.ZioTest with Common {
        def forkArgs = parent.forkArgs()
        def ivyDeps = Agg(
            ivy"dev.zio::zio-test:${versions.zio}",
            ivy"dev.zio::zio-test-sbt:${versions.zio}",
            ivy"dev.zio::zio-http-testkit:${versions.ziohttp}",
        )
    }
}

object frontend extends ScalaJSModule with Common {
    def scalaJSVersion = versions.scalajs
    def ivyDeps = Agg(
        ivy"org.scala-js::scalajs-dom::${versions.scalajsdom}",
        ivy"com.softwaremill.sttp.client3::core::${versions.sttp}",
    )
    def scalaJSUseMainModuleInitializer = true
    def moduleKind                      = ModuleKind.ESModule
    def moduleSplitStyle                = ModuleSplitStyle.SmallModulesFor(List("com.carlosedp.zioscalajs.frontend"))

    // These two tasks are used by Vite to get update path
    def fastLinkOut() = T.command(println(fastLinkJS().dest.path))
    def fullLinkOut() = T.command(println(fullLinkJS().dest.path))

    object test extends ScalaJSTests with Common with TestModule.ScalaTest {
        // Test dependencies
        def ivyDeps = Agg(
            ivy"org.scalatest::scalatest::${versions.scalatest}"
        )
        def moduleKind       = ModuleKind.NoModule
        def jsEnvConfig      = JsEnvConfig.JsDom()
        def moduleSplitStyle = ModuleSplitStyle.FewestModules

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
