package domc

import java.io.File

import scala.collection.mutable
import scala.io.Source
import scala.xml._
import scala.xml.parsing.ConstructingParser

import scalaz.{ Node => ZNode, Source => ZSource, _ }
import Scalaz._

object DomTemplate {
	def compile(file:File):Safe[String]   =
			for {
				xml		<- loadXML(file)
				comp	<- DomTemplate compile xml
			} 
			yield comp
			
	def compile(string:String):Safe[String]   =
			for {
				xml		<- parseXML(string)
				comp	<- DomTemplate compile xml
			} 
			yield comp
	
	def loadXML(file:File):Safe[Node] =
			try {
				ConstructingParser.fromFile(file, true).document.docElem.success
			}
			catch {
				case e:Exception	=> s"loading xml failed: ${file.getPath} cause: ${e.getMessage}".fail.toValidationNel
			}
			
	def parseXML(string:String):Safe[Node]	=
			try {
				ConstructingParser.fromSource(Source fromString string, true).document.docElem.success
			}
			catch {
				case e:Exception	=> s"parsing xml failed: ${e.getMessage}".fail.toValidationNel
			}
			
	//------------------------------------------------------------------------------
			
	val hash		= "xid"
	val toplevel	= "$"
	val prefix		= "x"
	
	/** compiles a template element with name and hash attributes into either some error messages or a JS function */
	def compile(node:Node):Safe[String] =
			for {
				template	<- toplevelElem(node)
				name		<- attrText(template, hash)
				body		<- statements(template, hash)
				checked		<- check(body)
			} 
			yield {
				function(name, body)
			}
	
	private case class Compiled(code:String, varName:Option[String], refs:Seq[VarRef])
	private case class VarRef(xid:String, varName:String)
	
	/** compiles a node into either some error messages or a dom-constructing JS function */
	private def statements(elem:Elem, hash:String):Safe[Compiled]	= {
		// TODO use the State monad
		var	nextId		= 0
		def genId():Int	= { nextId += 1; nextId } 
		def freshName()	= prefix + genId()
		
		def compileNode(node:Node):Safe[Compiled] = node match {
			case x:Elem	=>
				// TODO i still want errors for the good compileds... 
				val subs:Safe[Vector[Compiled]]	= x.child.toVector traverse compileNode
				subs map { sub =>
					val	ownVarName	= freshName()
					val	ownCreate	= s"var ${ownVarName} = document.createElement(${escape(x.label)});"
					val ownAttrs	= x.attributes filter { _.key != hash } map { it:MetaData =>
						s"${ownVarName}.setAttribute(${escape(it.key)}, ${escape(it.value.text)});" 
					}
					val ownRef		= attrText(x, hash).toOption map { VarRef(_, ownVarName) } 
					
					val subCodes	= sub map { _.code }
					val subRefs		= sub flatMap { _.refs }
					val subVars		= sub flatMap { _.varName }
					val subAppends	= subVars map { subVarName =>
						s"${ownVarName}.appendChild(${subVarName});" 
					}
					
					val refs	= ownRef.toVector ++ subRefs
					val code	= (subCodes ++ Vector(ownCreate) ++ ownAttrs ++ subAppends) mkString "\n"
					
					Compiled(code, Some(ownVarName), refs)
				}
				
			case x:Text	=>
				val	varName	= freshName()
				val	create	= s"var ${varName} = document.createTextNode(${escape(x.text)});"
				Compiled(create, Some(varName), Nil).success
			
			case Comment(text)	=>
				val	comment	= s"/* ${text} */"	// TODO escape */
				Compiled(comment, None, Nil).success
				
			case _ => 
				s"unexpected node: ${node}".fail.toValidationNel 
		}
		
		compileNode(elem)
	}
	
	private def check(compiled:Compiled):Safe[Compiled] = {
		val hasVarName	= if (compiled.varName.isEmpty)	Some("toplevel node is not an element") else None 
		val hasDollar	= compiled.refs find { _.xid == toplevel } map { "forbidden id: " + _.xid }
		val duplicates	= compiled.refs collect { case VarRef(k,_) => k } groupBy identity collect { case (k,vs) if vs.size > 1 => "duplicate id: " + k }
		(hasVarName.toList ++ hasDollar.toList ++ duplicates.toList).toNel toFailure compiled
	}	
	
	private def function(name:String, compiled:Compiled):String	=
			s"""
			|function ${name}() {
			|	${compiled.code}
			|	return {
			|		${hash(outputRefs(compiled)) }
			|	};
			|}
			""".stripMargin
			
	// add toplevel var as prefix ($)
	private def outputRefs(compiled:Compiled):Seq[VarRef]	=
			toplevelRef(compiled).toVector ++ compiled.refs

	private def toplevelRef(compiled:Compiled):Option[VarRef]	=
			compiled.varName map { it => VarRef(toplevel, it) }
			
	private def hash(refs:Seq[VarRef]):String	=
			refs
			.map { case VarRef(xid, varName)	=> s"${escape(xid)}:	${varName}"	 }
			.mkString (",\n")
			
	//------------------------------------------------------------------------------
	
	private def toplevelElem(node:Node):Safe[Elem]	=
			node match {
				case x:Elem => x.success
				case _		=> "expected element at toplevel".fail.toValidationNel
			}
			
	private def attrText(elem:Elem, key:String):Safe[String]	=
			elem.attributes find { _.key == key } map { _.value.text } toSuccess NonEmptyList(s"missing attribute: ${key}")
	
	private def escape(s:String) = 
			s 
			.map {
				case '"' 	=> "\\\""
				case '\\'	=>	"\\\\"
				// this would be allowed but is ugly
				//case '/'	=> "\\/"
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
