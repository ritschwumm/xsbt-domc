sbtPlugin		:= true

name			:= "xsbt-domc"
organization	:= "de.djini"
version			:= "2.5.0"

scalacOptions	++= Seq(
	"-feature",
	"-deprecation",
	"-unchecked",
	"-Xfatal-warnings",
)

conflictManager	:= ConflictManager.strict withOrganization "^(?!(org\\.scala-lang|org\\.scala-js|org\\.scala-sbt)(\\..*)?)$"
addSbtPlugin("de.djini" % "xsbt-util"	% "1.5.0")
addSbtPlugin("de.djini" % "xsbt-asset"	% "1.5.0")

libraryDependencies	++= Seq(
	"org.specs2"	%%	"specs2-core"		% "4.10.5"	% "test"
)
dependencyOverrides	++= Seq(
	"org.scalamacros"	%% "quasiquotes"	% "2.1.0"
)
