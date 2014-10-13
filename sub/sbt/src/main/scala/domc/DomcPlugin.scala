import sbt._
import Keys.{ TaskStreams }
import Project.Initialize

import xsbtUtil._
import domc._

object DomcPlugin extends Plugin {
	val domcFilter		= GlobFilter("*.dom") && NotDirectoryFilter
	
	val domc			= taskKey[Seq[PathMapping]]("build output files")
	val domcTargetDir	= settingKey[File]("directory for output files")
	
	val domcSources		= taskKey[Traversable[PathMapping]]("input files")
	val domcSourceDir	= settingKey[File]("directory with input files")
	
	val domcProcessor	= taskKey[Seq[PathMapping]=>Seq[PathMapping]]("compiler")
	
	lazy val domcSettings:Seq[Def.Setting[_]]	=
			Vector(
				domcSourceDir	:= (Keys.sourceDirectory in Compile).value	/ "domc",
				domcSources		:=
						sourcesTask(
							sourceDir	= domcSourceDir.value
						), 
				domcTargetDir	:= Keys.target.value						/ "domc",
				domcProcessor	:=
						processorTask(
							streams		= Keys.streams.value,
							targetDir	= domcTargetDir.value
						),
				domc			:=
						domcTask(
							streams		= Keys.streams.value,
							sources		= domcSources.value,
							processor	= domcProcessor.value
						),
				Keys.watchSources	:= Keys.watchSources.value ++ (domcSources.value map PathMapping.getFile)
			)
			
	private def sourcesTask(sourceDir:File):Traversable[PathMapping]	= 
			selectSubpaths(sourceDir, domcFilter)
		
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
				
				def errorExit(errors:Nes[String]):Nothing	= {
					errors foreach { streams.log error _ }
					throw FailureException
				}
				
				Safe traverseISeq (treatFile _).tupled apply inputs.toVector cata (errorExit, identity)
			}
	
	private def domcTask(
		streams:TaskStreams, 
		sources:Traversable[PathMapping], 
		processor:Seq[PathMapping]=>Seq[PathMapping]
	):Seq[PathMapping]	= {
		processor(sources.toVector)
	}
}
