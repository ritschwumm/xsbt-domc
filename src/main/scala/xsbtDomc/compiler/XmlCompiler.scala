package xsbtDomc.compiler

import java.io.File

import scala.collection.immutable.{ Seq => ISeq }
import scala.io.Source
import scala.xml._
import scala.xml.parsing.ConstructingParser

import xsbtUtil.implicits._
import xsbtUtil.data._
import xsbtUtil.data.Safe._

object XmlCompiler {
	//------------------------------------------------------------------------------
	//## constants
	
	val hash		= "xid"
	val toplevelId	= "$"
	val prefix		= "x"
	
	//------------------------------------------------------------------------------
	//## compiler
	
	def compileFile(file:File):Safe[String,String]   =
			loadXML(file) flatMap XmlCompiler.compile
			
	def compileString(string:String):Safe[String,String]   =
			parseXML(string) flatMap XmlCompiler.compile
	
	//------------------------------------------------------------------------------
	//## string to xml
	
	def loadXML(file:File):Safe[String,Node] =
			parseSource(
				Source fromFile file,
				e => s"loading xml file failed: ${file.getPath} cause: ${e.getMessage}"
			)
			
	def parseXML(string:String):Safe[String,Node]	=
			parseSource(
				Source fromString string,
				e => s"parsing xml string failed: ${e.getMessage}"
			)
			
	private def parseSource[T](source:Source, problem:Exception=>String):Safe[String,Node]	=
			try {
				win((ConstructingParser fromSource (source, true)).document.docElem)
			}
			catch { case e:Exception =>
				fail(problem(e).nes)
			}
			
	//------------------------------------------------------------------------------
	//## xml to ast
	
	private def parse(node:Node):Safe[String,PNode]	=
			node match {
				case elem:Elem	=>
					val (xids, attrs)	=
							elem.attributes
							.toVector
							.map		{ it:MetaData => (it.key, it.value.text) }
							.partition	{ _._1 == hash }
					for {
						xid	<-
								xids match {
									case ISeq()			=> win(None)
									case ISeq((_, x))	=> win(Some(x))
									case x				=> fail(s"expected zero or one xid, not ${x.size}".nes)
								}
						children	<-
								traverseISeq(parse)(elem.child.toVector)
					}
					yield PTag(
						xid			= xid,
						name		= elem.label,
						attributes	= attrs,
						children	= children
					)
					
				case Text(text)	=>
					win(PText(text))
				
				// TODO move into validation?
				case Comment(text)	=>
						 if (text contains "/*")	fail("comment must not contain /*".nes)
					else if (text contains "*/")	fail("comment must not contain */".nes)
					else if (text contains "\\")	fail("comment must not contain \\".nes)
					else							win(PComment(text))
					
				case _ =>
					fail(s"unexpected node: ${node}".nes)
			}
			
	private def gatherXids(pnode:PNode):ISeq[String]	=
			pnode match {
				case PTag(opt, _, _, children)	=> opt.toVector ++ (children flatMap gatherXids)
				case _							=> Vector.empty
			}
			
	//------------------------------------------------------------------------------
	//## validation
	
	// TODO validate toplevel xids to exist and be a js path
	
	private def validate(pnode:PNode):Safe[String,PTag]	=
			for {
				tag	<- pnode.asPTag toSafe "expected element at toplevel".nes
				_	<- preventDuplicates(gatherXids(tag))
			}
			yield tag
			
	private def preventDuplicates(ids:ISeq[String]):Safe[String,Unit]	=
			ids
			.groupBy	(identity)
			.collect	{ case (k, vs) if vs.size > 1	=> s"duplicate id: $k" }
			.toVector
			.preventing	(())
	
	//------------------------------------------------------------------------------
	//## plain ast to numbered ast
	
	private def number(pnode:PNode):INode	=
			numberImpl(pnode, 0)._1
		
	private def numberImpl(pnode:PNode, index:Int):(INode,Int)	=
			pnode match {
				case PTag(xid, name, attributes, children)	=>
					val (mapped, next)	=
							(children foldLeft (ISeq.empty[INode], index)) { (state:(ISeq[INode], Int), child:PNode) =>
								val (accu, current)	= state
								val (mapped, next)	= numberImpl(child, current)
								(accu :+ mapped, next)
							}
					(ITag(next, xid, name, attributes, mapped), next+1)
				case PText(text)	=> (IText(index, text),	index+1)
				case PComment(text)	=> (IComment(text),		index)
			}
			
	//------------------------------------------------------------------------------
	//## numbered ast to code
	
	/** compiles a template element with name and hash attributes into either some error messages or a JS function */
	def compile(node:Node):Safe[String,String] =
			for {
				parsed		<- parse(node)
				validated	<- validate(parsed)
				path		<- validated.xid toSafe s"missing attribute ${hash} on toplevel node".nes
				numbered	= number(validated)
				toplevel	<- numbered.asITag toSafe "expected tag at toplevel".nes
			}
			yield {
				val statements	= generateCode(numbered)
				val hash		= Vector((toplevelId, mkVarName(toplevel.index))) ++ generateHash(numbered)
				
				val hashExpr		= JS hashExpr hash
				val functionExpr	=
						JS functionExpr (
							paramRefs	= ISeq.empty,
							bodyStmts	= statements,
							resultExpr	= hashExpr
						)
				val templateStmts	=
						JS makeNamespaceStmts (path, functionExpr)
					
				templateStmts + "\n"
			}
			
	private def generateHash(inode:INode):ISeq[(String,String)]	=
			generateHashImpl(true)(inode)
			
	private def generateHashImpl(root:Boolean)(inode:INode):ISeq[(String,String)]	=
			inode match {
				case ITag(index, xid, _, _, children)	=>
						(	if (root)	Vector.empty
							else		xid.toVector map { xid => (xid, mkVarName(index)) }
						)	++
						(children flatMap generateHashImpl(false))
				case IText(index, text)		=> ISeq.empty
				case IComment(text)			=> ISeq.empty
			}
			
	private def generateCode(inode:INode):ISeq[String]	=
			inode match {
				case ITag(index, xid, name, attributes, children)	=>
					val childStmts		= children flatMap generateCode
					
					// own stuff
					val varName			= mkVarName(index)
					val createStmt		= JS makeVarStmt (varName, JS createElementExpr name)
					val setAttrStmts	= attributes map { case (k, v)	=> JS setAttributeStmt (varName, k, v) }
						
					// TODO extract?
					val childIndizes	=
							children collect {
								case ITag(index, _, _, _, _)	=> index
								case IText(index, _)			=> index
							}
					val childVarNames		=
							childIndizes map mkVarName
					val appendChildStmts	=
							childVarNames map { subVarName => JS appendChildStmt (varName, subVarName) }
				
					childStmts ++ Vector(createStmt) ++ setAttrStmts ++ appendChildStmts
					
				case IText(index, text)	=>
					val	code	= JS makeVarStmt (mkVarName(index), JS createTextNodeExpr text)
					Vector(code)
					
				case IComment(text)	=>
					val code	= JS comment text
					Vector(code)
			}
			
	private def mkVarName(index:Int):String	=
			s"${prefix}${index}"
			
	//------------------------------------------------------------------------------
	// TODO separate references, statements and expressions by type
	
	private object JS {
		def comment(text:String):String	=
				s"/* ${text} */"
			
		// TODO cleanup
		def makeNamespaceStmts(name:String, valueExpr:String):String	=
				(name split "\\.").inits.toVector.init.zipWithIndex.reverse.zipWithIndex
				.map { case ((parts, e), a) =>
					val prefix	= if (a == 0) "var " else ""
					val path	= parts mkString "."
					val expr	= if (e == 0) valueExpr else path + " || {}"
					s"$prefix$path = $expr;"
				}
				.mkString ("\n")
			
		def makeVarStmt(name:String, valueExpr:String):String	=
				s"var ${name} = ${valueExpr};"
			
		def returnStmt(valueExpr:String):String	=
				s"return ${valueExpr};"
			
		// TODO cleanup
		def functionExpr(paramRefs:ISeq[String], bodyStmts:ISeq[String], resultExpr:String):String	=
				s"""
				|function(${paramRefs mkString ", "}) {
				|	${ bodyStmts mkString "\n" replaceAll ("\n", "\n\t") }
				|	${ returnStmt(resultExpr) }
				|}
				"""
				.stripMargin
				.replaceAll ("^\\s+", "")
				.replaceAll ("\\s+$", "")
		
		def createElementExpr(name:String):String	=
				s"document.createElement(${stringExpr(name)})"
		
		def createTextNodeExpr(text:String):String	=
				s"document.createTextNode(${stringExpr(text)})"
			
		def appendChildStmt(target:String, child:String):String	=
				s"${target}.appendChild(${child});"
			
		// TODO Id->Id->Id, but could be String/String too
		def setAttributeStmt(target:String, key:String, value:String):String	=
				s"${target}.setAttribute(${stringExpr(key)}, ${stringExpr(value)});"
	
		// TODO Seq[Id->Expr]
		def hashExpr(hash:ISeq[(String,String)]):String	=
				hash
				.map		{ case (id, expr)	=> s"\t${stringExpr(id)}:	${expr}"	 }
				.mkString	("{", ",\n", "}")
				
		def stringExpr(s:String):String =
				s
				.map {
					case '"' 	=> "\\\""
					case '\\'	=>	"\\\\"
					// this would be legal, but it's ugly
					// case '/'	=> "\\/"
					// these are optional
					case '\b'	=> "\\b"
					case '\f'	=> "\\f"
					case '\n'	=> "\\n"
					case '\r'	=> "\\r"
					case '\t'	=> "\\t"
					case c
					if c < 32	=> "\\u%04x".format(c.toInt)
					case c 		=> c.toString
				}
				.mkString("\"", "", "\"")
	}
}
