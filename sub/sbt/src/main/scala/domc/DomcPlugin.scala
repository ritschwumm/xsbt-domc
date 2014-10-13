package xsbtDomc

import sbt._
import Keys.TaskStreams

import xsbtUtil.types._
import xsbtUtil.{ util => xu }

import domc._

object Import {
	val domcFilter		= GlobFilter("*.dom") && xu.filter.NotDirectoryFilter
	
	val domc			= taskKey[Seq[PathMapping]]("build output files")
	val domcTargetDir	= settingKey[File]("directory for output files")
	
	val domcSources		= taskKey[Traversable[PathMapping]]("input files")
	val domcSourceDir	= settingKey[File]("directory with input files")
	
	val domcProcessor	= taskKey[Seq[PathMapping]=>Seq[PathMapping]]("compiler")
	
}

object DomcPlugin extends AutoPlugin {
	//------------------------------------------------------------------------------
	//## exports
	
	override val requires:Plugins		= empty
	
	override val trigger:PluginTrigger	= noTrigger
	
	lazy val autoImport	= Import
	import autoImport._
	
	override lazy val projectSettings:Seq[Def.Setting[_]]	=
			Vector(
				domcSourceDir		:= (Keys.sourceDirectory in Compile).value	/ "domc",
				domcSources			:= selectSubpaths(domcSourceDir.value, domcFilter),
				domcTargetDir		:= Keys.target.value						/ "domc",
				domcProcessor		:=
						processorTask(
							streams		= Keys.streams.value,
							targetDir	= domcTargetDir.value
						),
				domc				:= domcProcessor.value apply domcSources.value.toVector,
				Keys.watchSources	:= Keys.watchSources.value ++ (domcSources.value map xu.pathMapping.getFile)
			)
			
	//------------------------------------------------------------------------------
	//## tasks
		
	private def processorTask(streams:TaskStreams, targetDir:File):Seq[PathMapping]=>Seq[PathMapping]	=
			inputs => {
				streams.log info s"compiling dom templates to ${targetDir}"
				IO delete targetDir
				
				def treatFile(inFile:File, path:String):Safe[String,PathMapping]	=
						if (domcFilter accept inFile)	compileFile(inFile, path)
						else							Safe win ((inFile, path))
					
				def compileFile(inFile:File, path:String):Safe[String,PathMapping]	= {
					val targetPath	= path + ".js"
					val targetFile	= targetDir / targetPath
					val compiled	= DomTemplate compile inFile
					compiled forEach	{ IO write (targetFile, _, IO.utf8) }
					compiled map		{ _ => (targetFile, targetPath) }
				}
				
				def errorExit(errors:Nes[String]):Nothing	=
						xu.fail logging (streams, (errors.toISeq):_*)
				
				Safe traverseISeq (treatFile _).tupled apply inputs.toVector cata (errorExit, identity)
			}
}
