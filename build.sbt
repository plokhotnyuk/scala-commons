import org.scalajs.jsenv.nodejs.NodeJSEnv

cancelable in Global := true

// We need to generate slightly different structure for IntelliJ in order to better support ScalaJS cross projects.
// idea.managed property is set by IntelliJ when running SBT (shell or import), idea.runid is set only for IntelliJ's
// SBT shell. In order for this technique to work, you MUST NOT set the "Use the sbt shell for build and import"
// option in IntelliJ's SBT settings.
val forIdeaImport = System.getProperty("idea.managed", "false").toBoolean && System.getProperty("idea.runid") == null

val silencerVersion = "1.6.0"
val collectionCompatVersion = "2.1.6"
val guavaVersion = "23.0"
val jsr305Version = "3.0.2"
val scalatestVersion = "3.2.0"
val scalacheckVersion = "1.14.3"
val scalatestplusScalacheckVersion = "3.2.0.0"
val jettyVersion = "9.4.29.v20200521"
val mongoVersion = "3.12.4"
val mongoReactiveVersion = "1.13.1"
val mongoScalaVersion = "2.9.0"
val kafkaVersion = "2.2.1"
val springVersion = "4.3.26.RELEASE"
val typesafeConfigVersion = "1.4.0"
val commonsIoVersion = "1.3.2"
val scalaLoggingVersion = "3.9.2"
val akkaVersion = "2.6.6"
val monixVersion = "3.2.1"
val mockitoVersion = "3.3.3"
val circeVersion = "0.13.0"
val upickleVersion = "1.0.0"
val scalajsBenchmarkVersion = "0.3.0"

useGpg := false // TODO: use sbt-ci-release
pgpPublicRing := file("./travis/local.pubring.asc")
pgpSecretRing := file("./travis/local.secring.asc")
pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray)

credentials in Global += Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  sys.env.getOrElse("SONATYPE_USERNAME", ""),
  sys.env.getOrElse("SONATYPE_PASSWORD", "")
)

version in ThisBuild :=
  sys.env.get("TRAVIS_TAG").filter(_.startsWith("v")).map(_.drop(1)).getOrElse("2.0.0-SNAPSHOT")

// for binary compatibility checking
val previousCompatibleVersions = Set("1.39.14")

val commonSettings = Seq(
  organization := "com.avsystem.commons",
  crossScalaVersions := Seq("2.12.11", "2.13.1"),
  scalaVersion := crossScalaVersions.value.last,
  compileOrder := CompileOrder.Mixed,
  scalacOptions ++= Seq(
    "-encoding", "utf-8",
    "-Yrangepos",
    "-explaintypes",
    "-feature",
    "-deprecation",
    "-unchecked",
    "-language:implicitConversions",
    "-language:existentials",
    "-language:dynamics",
    "-language:experimental.macros",
    "-language:higherKinds",
    "-Xfatal-warnings",
    "-Xlint:-missing-interpolator,-adapted-args,-unused,_",
  ),
  scalacOptions ++= {
    if (scalaBinaryVersion.value == "2.12") Seq(
      "-Ycache-plugin-class-loader:last-modified",
      "-Ycache-macro-class-loader:last-modified",
    ) else Seq.empty
  },
  sources in(Compile, doc) := Seq.empty, // relying on unidoc
  apiURL := Some(url("http://avsystem.github.io/scala-commons/api")),
  autoAPIMappings := true,

  publishTo := sonatypePublishToBundle.value,
  sonatypeProfileName := "com.avsystem",

  projectInfo := ModuleInfo(
    nameFormal = "AVSystem commons",
    description = "AVSystem commons library for Scala",
    homepage = Some(url("https://github.com/AVSystem/scala-commons")),
    startYear = Some(2015),
    licenses = Vector(
      "The MIT License" -> url("https://opensource.org/licenses/MIT"),
    ),
    organizationName = "AVSystem",
    organizationHomepage = Some(url("http://www.avsystem.com/")),
    scmInfo = Some(ScmInfo(
      browseUrl = url("https://github.com/AVSystem/scala-commons.git"),
      connection = "scm:git:git@github.com:AVSystem/scala-commons.git",
      devConnection = Some("scm:git:git@github.com:AVSystem/scala-commons.git"),
    )),
    developers = Vector(
      Developer("ghik", "Roman Janusz", "r.janusz@avsystem.com", url("https://github.com/ghik")),
    ),
  ),

  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },

  libraryDependencies ++= Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full,
    "org.scalatest" %%% "scalatest" % scalatestVersion % Test,
    "org.scalacheck" %%% "scalacheck" % scalacheckVersion % Test,
    "org.scalatestplus" %%% "scalacheck-1-14" % scalatestplusScalacheckVersion % Test,
    "org.mockito" % "mockito-core" % mockitoVersion % Test,
  ),
  ideBasePackages := Seq(organization.value),
  ideOutputDirectory in Compile := Some(target.value.getParentFile / "out/production"),
  ideOutputDirectory in Test := Some(target.value.getParentFile / "out/test"),
  fork in Test := true,
)

val jvmCommonSettings = commonSettings ++ Seq(
  libraryDependencies ++= Seq(
    "org.apache.commons" % "commons-io" % commonsIoVersion % Test,
  ),
  mimaPreviousArtifacts := previousCompatibleVersions.map { previousVersion =>
    organization.value % s"${name.value}_${scalaBinaryVersion.value}" % previousVersion
  },
  Test / jsEnv := new NodeJSEnv(NodeJSEnv.Config().withEnv(Map(
    "RESOURCES_DIR" -> (Test / resourceDirectory).value.absolutePath)
  )),
)

val jsCommonSettings = commonSettings ++ Seq(
  scalacOptions += {
    val localDir = (baseDirectory in ThisBuild).value.toURI.toString
    val githubDir = "https://raw.githubusercontent.com/AVSystem/scala-commons"
    s"-P:scalajs:mapSourceURI:$localDir->$githubDir/v${version.value}/"
  },
  jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv(),
  fork in Test := false,
)

val noPublishSettings = Seq(
  skip in publish := true,
  mimaPreviousArtifacts := Set.empty,
)

val aggregateProjectSettings =
  commonSettings ++ noPublishSettings ++ Seq(
    ideSkipProject := true,
    ideExcludedDirectories := Seq(baseDirectory.value)
  )

val CompileAndTest = "compile->compile;test->test"
val OptionalCompileAndTest = "optional->compile;test->test"

lazy val commons = project.in(file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .aggregate(
    `commons-jvm`,
    `commons-js`,
  )
  .settings(
    commonSettings,
    noPublishSettings,
    name := "commons",
    ideExcludedDirectories := Seq(baseDirectory.value / ".bloop"),
    scalacOptions in ScalaUnidoc in unidoc += "-Ymacro-expand:none",
    unidocProjectFilter in ScalaUnidoc in unidoc :=
      inAnyProject -- inProjects(
        `commons-analyzer`,
        `commons-macros`,
        `commons-core-js`,
        `commons-benchmark`,
//        `commons-benchmark-js`,
        `commons-comprof`,
      ),
  )

lazy val `commons-jvm` = project.in(file(".jvm"))
  .aggregate(
    `commons-analyzer`,
    `commons-macros`,
    `commons-core`,
    `commons-jetty`,
    `commons-mongo`,
    `commons-hocon`,
    `commons-spring`,
    `commons-redis`,
    `commons-benchmark`,
  )
  .settings(aggregateProjectSettings)

lazy val `commons-js` = project.in(file(".js"))
  .aggregate(
    `commons-core-js`,
//    `commons-benchmark-js`,
  )
  .settings(aggregateProjectSettings)

lazy val `commons-analyzer` = project
  .dependsOn(`commons-core` % Test)
  .settings(
    jvmCommonSettings,
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  )

def mkSourceDirs(base: File, scalaBinary: String, conf: String): Seq[File] = Seq(
  base / "src" / conf / "scala",
  base / "src" / conf / s"scala-$scalaBinary",
  base / "src" / conf / "java"
)

def sourceDirsSettings(baseMapper: File => File) = Seq(
  unmanagedSourceDirectories in Compile ++=
    mkSourceDirs(baseMapper(baseDirectory.value), scalaBinaryVersion.value, "main"),
  unmanagedSourceDirectories in Test ++=
    mkSourceDirs(baseMapper(baseDirectory.value), scalaBinaryVersion.value, "test"),
)

def sameNameAs(proj: Project) =
  if (forIdeaImport) Seq.empty
  else Seq(name := (name in proj).value)

lazy val `commons-macros` = project.settings(
  jvmCommonSettings,
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
)

lazy val `commons-core` = project
  .dependsOn(`commons-macros`)
  .settings(
    jvmCommonSettings,
    sourceDirsSettings(_ / "jvm"),
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-collection-compat" % collectionCompatVersion,
      "com.google.code.findbugs" % "jsr305" % jsr305Version % Optional,
      "com.google.guava" % "guava" % guavaVersion % Optional,
    ),
  )

lazy val `commons-core-js` = project.in(`commons-core`.base / "js")
  .enablePlugins(ScalaJSPlugin)
  .configure(p => if (forIdeaImport) p.dependsOn(`commons-core`) else p)
  .dependsOn(`commons-macros`)
  .settings(
    jsCommonSettings,
    sameNameAs(`commons-core`),
    sourceDirsSettings(_.getParentFile),
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %%% "scala-collection-compat" % collectionCompatVersion,
    )
  )

lazy val `commons-jetty` = project
  .dependsOn(`commons-core` % CompileAndTest)
  .settings(
    jvmCommonSettings,
    libraryDependencies ++= Seq(
      "org.eclipse.jetty" % "jetty-client" % jettyVersion,
      "org.eclipse.jetty" % "jetty-server" % jettyVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,

      "org.eclipse.jetty" % "jetty-servlet" % jettyVersion % Test,
      "org.slf4j" % "slf4j-simple" % "1.7.29" % Test,
    ),
  )

lazy val `commons-benchmark` = project
  .dependsOn(`commons-redis`, `commons-mongo`)
  .enablePlugins(JmhPlugin)
  .settings(
    jvmCommonSettings,
    noPublishSettings,
    sourceDirsSettings(_ / "jvm"),
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-jawn" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "com.lihaoyi" %% "upickle" % upickleVersion,
    ),
    ideExcludedDirectories := (managedSourceDirectories in Jmh).value,
  )

//https://github.com/japgolly/scalajs-benchmark/issues/57
//
//lazy val `commons-benchmark-js` = project.in(`commons-benchmark`.base / "js")
//  .enablePlugins(ScalaJSPlugin)
//  .configure(p => if (forIdeaImport) p.dependsOn(`commons-benchmark`) else p)
//  .dependsOn(`commons-core-js`)
//  .settings(
//    jsCommonSettings,
//    noPublishSettings,
//    sameNameAs(`commons-benchmark`),
//    sourceDirsSettings(_.getParentFile),
//    libraryDependencies ++= Seq(
//      "io.circe" %%% "circe-core" % circeVersion,
//      "io.circe" %%% "circe-generic" % circeVersion,
//      "io.circe" %%% "circe-parser" % circeVersion,
//      "com.lihaoyi" %%% "upickle" % upickleVersion,
//      "com.github.japgolly.scalajs-benchmark" %%% "benchmark" % scalajsBenchmarkVersion,
//    ),
//    scalaJSUseMainModuleInitializer := true,
//    test := {},
//    testOnly := {},
//    testQuick := {},
//  )

lazy val `commons-mongo` = project
  .dependsOn(`commons-core` % CompileAndTest)
  .settings(
    jvmCommonSettings,
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % guavaVersion,
      "io.monix" %% "monix" % monixVersion,
      "org.mongodb" % "mongodb-driver-core" % mongoVersion,
      "org.mongodb" % "mongodb-driver" % mongoVersion % Optional,
      "org.mongodb" % "mongodb-driver-async" % mongoVersion % Optional,
      "org.mongodb" % "mongodb-driver-reactivestreams" % mongoReactiveVersion % Optional,
      "org.mongodb.scala" %% "mongo-scala-driver" % mongoScalaVersion % Optional,
    ),
  )

lazy val `commons-redis` = project
  .dependsOn(`commons-core` % CompileAndTest)
  .settings(
    jvmCommonSettings,
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % guavaVersion,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
    ),
    parallelExecution in Test := false,
  )

lazy val `commons-hocon` = project
  .dependsOn(`commons-core` % CompileAndTest)
  .settings(
    jvmCommonSettings,
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % typesafeConfigVersion,
    ),
  )

lazy val `commons-spring` = project
  .dependsOn(`commons-hocon` % CompileAndTest)
  .settings(
    jvmCommonSettings,
    libraryDependencies ++= Seq(
      "org.springframework" % "spring-context" % springVersion,
    ),
  )

lazy val `commons-comprof` = project
  .dependsOn(`commons-core`)
  .settings(
    jvmCommonSettings,
    noPublishSettings,
    ideSkipProject := true,
    addCompilerPlugin("ch.epfl.scala" %% "scalac-profiling" % "1.0.0"),
    scalacOptions ++= Seq(
      s"-P:scalac-profiling:sourceroot:${baseDirectory.value}",
      "-P:scalac-profiling:generate-macro-flamegraph",
      "-P:scalac-profiling:no-profiledb",
      "-Xmacro-settings:statsEnabled",
      "-Ystatistics:typer",
    ),
    sourceGenerators in Compile += Def.task {
      val originalSrc = (sourceDirectory in `commons-core`).value /
        "test/scala/com/avsystem/commons/rest/RestTestApi.scala"
      val originalContent = IO.read(originalSrc)
      (0 until 100).map { i =>
        val pkg = f"oa$i%02d"
        val newContent = originalContent.replaceAllLiterally("package rest", s"package rest\npackage $pkg")
        val newFile = (sourceManaged in Compile).value / pkg / "RestTestApi.scala"
        IO.write(newFile, newContent)
        newFile
      }
    }.taskValue
  )
