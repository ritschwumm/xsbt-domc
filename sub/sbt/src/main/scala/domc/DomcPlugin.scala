import sbt._
import Keys.{ TaskStreams }
import Project.Initialize
import IO.utf8

import domc._

object DomcPlugin extends Plugin {
	val domcFilter	= GlobFilter("*.dom")
	
	/** build the js files */
	val domcBuild	= TaskKey[File]("domc")
	
	/** directory with input files */
	val domcSource	= SettingKey[File]("domc-source")
	
	/** directory for output files */
	val domcTarget	= SettingKey[File]("domc-target")
	
	lazy val domcSettings:Seq[Def.Setting[_]]	=
			Seq(
				domcBuild			<<= domcTask,
				domcSource			<<= (Keys.sourceDirectory in Compile)	{ _ / "domc"	},
				domcTarget			<<= Keys.target							{ _ / "domc"	},
				Keys.watchSources	<<= (Keys.watchSources, domcSource) map {
					(watchSources, domcSource) => { 
						val sourceFiles	= (domcSource ** domcFilter).get
						watchSources ++ sourceFiles
					}
				}
			)
	
	private def domcTask:Def.Initialize[Task[File]] = 
			(Keys.streams, domcSource, domcTarget) map domcTaskImpl
	
	private def domcTaskImpl(streams:TaskStreams, source:File, target:File):File	= {
		streams.log info s"compiling dom templates from ${source} to ${target}"
		IO delete target
		
		val errors:Seq[String]	=
				selectSubpaths(source, domcFilter).toVector flatMap { case (input, path) =>
					// TODO ugly
					val output		= target / (path + ".js")
					val compiled	= DomTemplate compile input
					compiled forEach { IO write (output, _, utf8) }
					compiled cata (
						_.toSeq,
						_ => Seq.empty
					)
				}
		// TODO ugly
		if (errors.nonEmpty) sys error (errors mkString "\n")
		target
	}
}
