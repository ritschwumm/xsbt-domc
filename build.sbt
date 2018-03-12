sbtPlugin		:= true

name			:= "xsbt-domc"
organization	:= "de.djini"
version			:= "2.1.0"

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
addSbtPlugin("de.djini" % "xsbt-util"	% "1.1.0")
addSbtPlugin("de.djini" % "xsbt-asset"	% "1.1.0")

resolvers		+= "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
libraryDependencies	++= Seq(
	"org.specs2"	%%	"specs2-core"		% "4.0.3"	% "test"
)
dependencyOverrides	++= Seq(
	"org.scalamacros"	%% "quasiquotes"	% "2.1.0"
)
