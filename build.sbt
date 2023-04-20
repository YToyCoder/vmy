
ThisBuild / scalaVersion := "3.2.2"
//ThisBuild / javacOptions ++= Seq("-source", "17")
ThisBuild / compileOrder := CompileOrder.JavaThenScala
val vmyLanguage = (project in file("."))
  .settings{
    name := "vmyLanguage"
    scalaVersion := "3.2.2"
    javacOptions ++= Seq("-source", "17")
  }

