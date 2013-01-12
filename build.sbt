name			:= "domc"

organization	:= "de.djini"

version			:= "0.6.0"

scalaVersion	:= "2.9.2"

libraryDependencies	++= Seq(
	"de.djini"		%%	"scutil"			% "0.13.0"	% "compile",
	"de.djini"		%%	"scwebapp"			% "0.10.0"	% "compile",
	"org.scalaz"	%%	"scalaz-core"		% "6.0.4"	% "compile",
	"javax.servlet"	%	"javax.servlet-api"	% "3.0.1"	% "provided"
)

scalacOptions	++= Seq("-deprecation", "-unchecked")
