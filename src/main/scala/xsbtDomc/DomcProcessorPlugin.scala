package xsbtDomc

import sbt._
import Keys.TaskStreams

import xsbtUtil.types._
import xsbtUtil.data._
import xsbtUtil.{ util => xu }

import xsbtAsset.AssetPlugin
import xsbtAsset.Import.AssetProcessor

import xsbtDomc.compiler._

object Import {
	val domcFilter		= GlobFilter("*.dom") && xu.filter.NotDirectoryFilter
	val domcProcessor	= taskKey[AssetProcessor]("processor for xsbt-asset")
	val domcFileSuffix	= settingKey[String]("suffix for file names")
	val domcBuildDir	= settingKey[File]("directory for output files")
}

object DomcProcessorPlugin extends AutoPlugin {
	//------------------------------------------------------------------------------
	//## exports

	override val requires:Plugins		= AssetPlugin

	override val trigger:PluginTrigger	= allRequirements

	lazy val autoImport	= Import
	import autoImport._

	override lazy val projectSettings:Seq[Def.Setting[_]]	=
		Vector(
			domcProcessor		:=
					processorTask(
						streams		= Keys.streams.value,
						fileSuffix	= domcFileSuffix.value,
						buildDir	= domcBuildDir.value
					),
			domcFileSuffix		:= ".js",
			domcBuildDir		:= Keys.target.value / "domc"
		)

	//------------------------------------------------------------------------------
	//## tasks

	def processorTask(streams:TaskStreams, fileSuffix:String, buildDir:File):AssetProcessor	=
		inputs => {
			streams.log info s"compiling dom templates to ${buildDir}"
			IO delete buildDir

			def treatFile(inFile:File, path:String):Safe[String,PathMapping]	=
					if (domcFilter accept inFile)	compileFile(inFile, path)
					else							Safe win ((inFile, path))

			def compileFile(inFile:File, path:String):Safe[String,PathMapping]	= {
				val targetPath	= path + fileSuffix
				val targetFile	= buildDir / targetPath
				val compiled	= XmlCompiler compileFile inFile
				compiled foreach	{ IO write (targetFile, _, IO.utf8) }
				compiled map		{ _ => (targetFile, targetPath) }
			}

			def errorExit(errors:Nes[String]):Nothing	=
					xu.fail logging (streams, (errors.toISeq):_*)

			Safe traverseISeq (treatFile _).tupled apply inputs.toVector cata (errorExit, identity)
		}
}
