organization	:= "de.djini"

name			:= "domc"

version			:= "1.1.0"

organization	in ThisBuild	:= organization.value

version			in ThisBuild	:= version.value

scalaVersion	in ThisBuild	:= "2.10.4"

lazy val `domc`	=
		project 
		.in			(file("."))
		.aggregate	(`domc-core`, `domc-sbt`)
		.settings	(publishArtifact := false)

lazy val `domc-core`	= 
		project 
		.in			(file("sub/core"))

lazy val `domc-sbt`	=
		project
		.in			(file("sub/sbt"))
		.dependsOn	(`domc-core`)
