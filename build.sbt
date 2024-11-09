scalaVersion := "3.5.0"
scalacOptions ++= Seq("-deprecation", "-feature", "-language:fewerBraces", "-Xfatal-warnings")
run / fork := true

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
  "org.scalameta" %% "munit" % "1.0.1" % Test
)

// scalafix
inThisBuild(
  List(
    scalaVersion := "3.5.0",
    semanticdbEnabled := true
  )
)
