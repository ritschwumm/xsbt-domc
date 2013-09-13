name			:= "domc"

organization	:= "de.djini"

version			:= "0.22.0"

scalaVersion	:= "2.10.2"

libraryDependencies	++= Seq(
	"de.djini"		%%	"scutil"			% "0.25.0"	% "compile",
	"de.djini"		%%	"scwebapp"			% "0.22.0"	% "compile",
	"org.scalaz"	%%	"scalaz-core"		% "7.0.2"	% "compile"		exclude("org.scala-lang", "scala-library"),
	"javax.servlet"	%	"javax.servlet-api"	% "3.0.1"	% "provided"
)

scalacOptions	++= Seq(
	"-deprecation",
	"-unchecked",
	// "-language:implicitConversions",
	// "-language:existentials",
	// "-language:higherKinds",
	// "-language:reflectiveCalls",
	// "-language:dynamics",
	"-language:postfixOps",
	// "-language:experimental.macros"
	"-feature"
)
