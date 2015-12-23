sbtPlugin		:= true

name			:= "xsbt-domc"
organization	:= "de.djini"
version			:= "1.10.0"

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
addSbtPlugin("de.djini" % "xsbt-util"	% "0.6.0")
addSbtPlugin("de.djini" % "xsbt-webapp"	% "1.8.0")

resolvers		+= "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
libraryDependencies	++= Seq(
	"org.specs2"	%%	"specs2-core"	% "3.6.5"	% "test"
)
dependencyOverrides	++= Set(
	"org.scalamacros"	%% "quasiquotes"	% "2.0.1"
)
