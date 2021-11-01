package xsbtDomc.compiler

import minitest._

object XmlCompilerTest extends SimpleTestSuite {
	test("XmlCompilerTest should compile xml") {
		val xml	= """<xml xid="a.b.c"/>"""
		val js	= XmlCompiler compileString xml

		// println(js cata (_.toISeq mkString "\n", identity))

		assert(js.isWin)
	}
}
