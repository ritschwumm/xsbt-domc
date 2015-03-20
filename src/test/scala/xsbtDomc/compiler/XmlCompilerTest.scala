package xsbtDomc.compiler

import org.specs2.mutable._

class XmlCompilerTest extends Specification {
	"XmlCompilerTest" should {
		"compile xml" in {
			val xml	= """<xml xid="a.b.c"/>"""
			val js	= XmlCompiler compileString xml
			
			// println(js cata (_.toISeq mkString "\n", identity))
			
			js.isWin mustEqual true
		}
	}
}
