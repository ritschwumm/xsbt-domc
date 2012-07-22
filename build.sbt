name			:= "domc"

organization	:= "de.djini"

version			:= "0.1.0"

scalaVersion	:= "2.9.2"

libraryDependencies	++= Seq(
	"de.djini"		%%	"scutil"		% "0.8.0"	% "compile",
	"de.djini"		%%	"scwebapp"		% "0.5.0"	% "compile",
	"org.scalaz"	%%	"scalaz-core"	% "6.0.4"	% "compile",
	"javax.servlet"	%	"servlet-api"	% "2.5"		% "provided"
)

scalacOptions	++= Seq("-deprecation", "-unchecked")
