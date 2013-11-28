organization	:= "de.djini"

name			:= "domc"

version			:= "0.36.0"

organization	in ThisBuild	<<= organization

version			in ThisBuild	<<= version

scalaVersion	in ThisBuild	:= "2.10.3"

lazy val `domc`	=
		project 
		.in			(file("."))
		.aggregate	(`domc-core`, `domc-sbt`, `domc-servlet`)
		.settings	(publishArtifact := false)

lazy val `domc-core`	= 
		project 
		.in			(file("sub/core"))

lazy val `domc-sbt`	=
		project
		.in			(file("sub/sbt"))
		.dependsOn	(`domc-core`)

lazy val `domc-servlet`	=
		project
		.in			(file("sub/servlet"))
		.dependsOn	(`domc-core`)
