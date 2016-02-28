sbtPlugin		:= true

name			:= "xsbt-domc"
organization	:= "de.djini"
version			:= "1.14.0"

scalacOptions	++= Seq(
	"-deprecation",
	"-unchecked",
	// "-language:implicitConversions",
	// "-language:existentials",
	// "-language:higherKinds",
	// "-language:reflectiveCalls",
	// "-language:dynamics",
	// "-language:postfixOps",
	// "-language:experimental.macros"
	"-feature",
	"-Xfatal-warnings"
)

conflictManager	:= ConflictManager.strict
addSbtPlugin("de.djini" % "xsbt-util"	% "0.7.0")
addSbtPlugin("de.djini" % "xsbt-asset"	% "0.3.0")

resolvers		+= "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
libraryDependencies	++= Seq(
	"org.specs2"		%%	"specs2-core"	% "3.7.1"	% "test"
)
dependencyOverrides	++= Set(
	"org.scalamacros"	%% "quasiquotes"	% "2.0.1"
)
