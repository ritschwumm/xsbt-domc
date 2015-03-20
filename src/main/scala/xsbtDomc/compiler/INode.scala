package xsbtDomc.compiler

import scala.collection.immutable.{ Seq => ISeq }

sealed trait INode {
	def asITag:Option[ITag]	=
			this match {
				case tag@ITag(_, _, _, _, _)	=> Some(tag)
				case _							=> None
			}
}

case class ITag(index:Int, xid:Option[String], name:String, attributes:ISeq[(String,String)], children:ISeq[INode])	extends INode
case class IText(index:Int, text:String)																			extends INode
case class IComment(text:String)																					extends INode
