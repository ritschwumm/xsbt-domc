sbtPlugin		:= true

name			:= "domc-sbt"

addSbtPlugin("de.djini" % "xsbt-util"	% "0.2.0")

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
	"-feature"
)
