import sbt._
import Keys.{ TaskStreams }
import Project.Initialize
import IO.utf8

import scala.collection.immutable.{ Seq => ISeq }

import domc._

object DomcPlugin extends Plugin {
	val domcFilter	= GlobFilter("*.dom") && -DirectoryFilter
	
	val domc		= taskKey[File]("build the js files")
	val domcSource	= settingKey[File]("directory with input files")
	val domcTarget	= settingKey[File]("directory for output files")
	
	lazy val domcSettings:ISeq[Def.Setting[_]]	=
			ISeq(
				domcSource	:= (Keys.sourceDirectory in Compile).value	/ "domc",
				domcTarget	:= Keys.target.value						/ "domc",
				domc		:=
						domcTaskImpl(
							streams	= Keys.streams.value,
							source	= domcSource.value,
							target	= domcTarget.value
						),
				Keys.watchSources	:= Keys.watchSources.value ++ (domcSource.value ** domcFilter).get
			)
	
	private def domcTaskImpl(streams:TaskStreams, source:File, target:File):File	= {
		streams.log info s"compiling dom templates from ${source} to ${target}"
		IO delete target
		
		val errors:ISeq[String]	=
				selectSubpaths(source, domcFilter).toVector flatMap { case (input, path) =>
					// TODO ugly
					val output		= target / (path + ".js")
					val compiled	= DomTemplate compile input
					compiled forEach { IO write (output, _, utf8) }
					compiled cata (
						_.toISeq,
						_ => ISeq.empty
					)
				}
				
		errors foreach { streams.log error _ }
		// TODO ugly
		if (errors.nonEmpty) sys error "domc failed"
		
		target
	}
}
