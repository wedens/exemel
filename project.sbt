import ReleaseTransformations._

lazy val buildSettings = Seq(
  organization := "org.wedens",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.5", scalaVersion.value)
)

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yinline-warnings",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture"
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 11)) => Seq("-Ywarn-unused-import")
    case _             => Seq.empty
  }),
  scalacOptions in (Compile, console) ~= (_ filterNot (_ == "-Ywarn-unused-import"))
)

lazy val exemel = project.in(file("."))
  .settings(buildSettings ++ commonSettings ++ noPublishSettings)
  .aggregate(core)

lazy val core = project
  .settings(name := "exemel-core")
  .settings(buildSettings ++ commonSettings ++ publishSettings ++ releaseSettings)
  .settings(
    resolvers ++= Seq(
      "scalaz.bintray" at "http://dl.bintray.com/scalaz/releases"
    )
  )
  .settings(
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-core" % "7.1.2"
    )
  )

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val releaseSettings = Seq(
  releaseCrossBuild := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val publishSettings = Seq(
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  publishArtifact in Test := false,
  pomExtra := (
    <developers>
      <developer>
        <id>wedens</id>
        <name>wedens</name>
        <url>http://github.com/wedens/</url>
      </developer>
    </developers>
  ),
  scmInfo := Some(ScmInfo(
    url("https://github.com/wedens/exemel"),
    "git@github.com:wedens/exemel.git"
  ))
)
