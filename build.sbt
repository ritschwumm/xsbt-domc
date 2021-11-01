Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / versionScheme := Some("early-semver")

sbtPlugin		:= true

name			:= "xsbt-domc"
organization	:= "de.djini"
version			:= "2.6.0"

scalacOptions	++= Seq(
	"-feature",
	"-deprecation",
	"-unchecked",
	"-Xfatal-warnings",
)

conflictManager	:= ConflictManager.strict withOrganization "^(?!(org\\.scala-lang|org\\.scala-js|org\\.scala-sbt)(\\..*)?)$"
addSbtPlugin("de.djini" % "xsbt-util"	% "1.6.0")
addSbtPlugin("de.djini" % "xsbt-asset"	% "1.6.0")

libraryDependencies	++= Seq(
	"io.monix"			%%	"minitest"		% "2.9.6"	% "test"
)
dependencyOverrides	++= Seq(
	"org.scalamacros"	%% "quasiquotes"	% "2.1.0"
)

testFrameworks	+= new TestFramework("minitest.runner.Framework")
