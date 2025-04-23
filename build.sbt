import sbtassembly.AssemblyPlugin.autoImport._

lazy val common_project = Seq(

  run / fork := true,
  run / connectInput := true,
  Global / cancelable := true,

  libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11",
      "org.scalactic" %% "scalactic" % "3.2.0",
      "org.scalatest" %% "scalatest" % "3.2.0"
  )
)

lazy val scala_project = common_project ++ Seq(
  scalaVersion := "2.13.4",   // scalaのバージョンを指定
  scalacOptions := Seq("-feature", "-unchecked", "-deprecation"),

  // Scalaのプロジェクトのファイル構成を設定。
  // https://www.scala-sbt.org/1.x/docs/Multi-Project.html
  Compile / scalaSource := baseDirectory.value / "scala",
  Test / scalaSource := baseDirectory.value / "test"
)

lazy val java_project = scala_project ++ Seq(
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8"),
  Compile / javaSource := baseDirectory.value / "java"
)

lazy val root = (project in file(".")).settings(common_project)
lazy val support = (project in file ("src/support")).settings(java_project)
lazy val tetris = (project in file("src/tetris"))
  .settings(scala_project)
  .settings(assemblySettings)
  .settings(
    Compile / resourceDirectory := baseDirectory.value / "sound"
  )
  .dependsOn(support)

lazy val assemblySettings = Seq(
  assembly / assemblyMergeStrategy := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case _                             => MergeStrategy.first
  }
)

