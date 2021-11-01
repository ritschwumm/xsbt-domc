package xsbtDomc.compiler

import scala.collection.immutable.{ Seq => ISeq }

sealed trait PNode {
	def asPTag:Option[PTag]	=
		this match {
			case tag@PTag(_, _, _, _)	=> Some(tag)
			case _						=> None
		}
}

case class PTag(xid:Option[String], name:String, attributes:ISeq[(String,String)], children:ISeq[PNode])	extends PNode
case class PText(text:String)																				extends PNode
case class PComment(text:String)																			extends PNode
