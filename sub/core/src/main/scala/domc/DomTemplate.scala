package domc

import java.io.File

import scala.collection.immutable.{ Seq => ISeq }
import scala.io.Source
import scala.xml._
import scala.xml.parsing.ConstructingParser

import domc.Safe._

object DomTemplate {
	def compile(file:File):Safe[String,String]   =
			for {
				xml		<- loadXML(file)
				comp	<- DomTemplate compile xml
			} 
			yield comp
			
	def compile(string:String):Safe[String,String]   =
			for {
				xml		<- parseXML(string)
				comp	<- DomTemplate compile xml
			} 
			yield comp
	
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
			
	val hash		= "xid"
	val toplevel	= "$"
	val prefix		= "x"
	
	/** compiles a template element with name and hash attributes into either some error messages or a JS function */
	def compile(node:Node):Safe[String,String] =
			for {
				template	<- toplevelElem(node)
				name		<- attrText(template, hash)
				body		<- statements(template, hash)
			} 
			yield {
				function(name, body)
			}
	
	private case class Compiled(code:String, varName:Option[String], refs:ISeq[VarRef])
	private case class VarRef(xid:String, varName:String)
	
	/** compiles a node into either some error messages or a dom-constructing JS function */
	private def statements(elem:Elem, hash:String):Safe[String,Compiled]	= {
		// TODO use the State monad
		var	nextId	= 0
		def freshName():String	= {
			nextId += 1
			prefix + nextId.toString
		}
		
		def compileNode(node:Node):Safe[String,Compiled] = node match {
			case elem:Elem	=>
				for {
					subs		<- traverseISeq(compileNode)(elem.child.toVector)
					
					// own stuff
					ownVarName	= freshName()
					ownCreate	= s"var ${ownVarName} = document.createElement(${escape(elem.label)});"
					ownAttrs	= 
							elem.attributes
							.filter	{ _.key != hash }
							.map	{ it:MetaData => s"${ownVarName}.setAttribute(${escape(it.key)}, ${escape(it.value.text)});"  }
					ownAttr		= attrText(elem, hash).toOption
					_			<- 
							ownAttr 
							.exists			{ _ == toplevel }
							.safePrevent	(s"illegal ${hash} attribute ${toplevel}".nes)
					ownRef		= ownAttr map { VarRef(_, ownVarName) } 
						
					// child stuff
					subCodes	= subs map		{ _.code	}
					subRefs		= subs flatMap	{ _.refs	}
					subAppends	=
							subs
							.flatMap	{ _.varName	}
							.map		{ subVarName => s"${ownVarName}.appendChild(${subVarName});" }
						
					// fused
					refs		= ownRef.toVector ++ subRefs
					_			<- preventDuplicates(refs)
					code		= (subCodes ++ Vector(ownCreate) ++ ownAttrs ++ subAppends) mkString "\n"
				}
				yield Compiled(code, Some(ownVarName), refs)
				
			case Text(text)	=>
				val	varName	= freshName()
				val	create	= s"var ${varName} = document.createTextNode(${escape(text)});"
				win(Compiled(create, Some(varName), Nil))
			
			case Comment(text)	=>
					 if (text contains "/*")	fail("comment must not contain /*".nes)
				else if (text contains "*/")	fail("comment must not contain /*".nes)
				else if (text contains "\\")	fail("comment must not contain \\".nes)
				else {
					val	comment	= s"/* ${text} */"
					win(Compiled(comment, None, Nil))
				}
				
			case _ => 
				fail(s"unexpected node: ${node}".nes) 
		}
		
		compileNode(elem)
	}
	
	private def preventDuplicates(refs:ISeq[VarRef]):Safe[String,Unit]	=
			refs
			.collect	{ case VarRef(k,_) => k }
			.groupBy	(identity)
			.collect	{ case (k,vs) if vs.size > 1 => "duplicate id: " + k }
			.toVector
			.preventing	(())
	
	private def function(name:String, compiled:Compiled):String	=
			s"""
			|function ${name}() {
			|	${compiled.code}
			|	return {
			|		${hash(outputRefs(compiled)) }
			|	};
			|}
			""".stripMargin
			
	private def outputRefs(compiled:Compiled):ISeq[VarRef]	=
			toplevelRef(compiled).toVector ++ compiled.refs

	private def toplevelRef(compiled:Compiled):Option[VarRef]	=
			compiled.varName map { VarRef(toplevel, _) }
			
	private def hash(refs:ISeq[VarRef]):String	=
			refs
			.map		{ case VarRef(xid, varName)	=> s"${escape(xid)}:	${varName}"	 }
			.mkString	(",\n")
			
	//------------------------------------------------------------------------------
	
	private def toplevelElem(node:Node):Safe[String,Elem]	=
			node match {
				case x:Elem => win(x)
				case _		=> fail("expected element at toplevel".nes)
			}
			
	private def attrText(elem:Elem, key:String):Safe[String,String]	=
			elem.attributes 
			.find	{ _.key == key }
			.map	{ _.value.text }
			.toSafe	(s"missing attribute: ${key}".nes)
	
	private def escape(s:String) = 
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
