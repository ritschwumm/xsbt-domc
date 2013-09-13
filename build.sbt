name			:= "domc"

organization	:= "de.djini"

version			:= "0.21.0"

scalaVersion	:= "2.10.2"

libraryDependencies	++= Seq(
	"de.djini"		%%	"scutil"			% "0.24.0"	% "compile",
	"de.djini"		%%	"scwebapp"			% "0.21.0"	% "compile",
	"org.scalaz"	%%	"scalaz-core"		% "7.0.2"	% "compile",
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
