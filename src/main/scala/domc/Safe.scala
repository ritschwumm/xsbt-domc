package domc

import scutil.lang.Nes

object Safe {
	def win[F,W](value:W):Safe[F,W]	=
			new Safe[F,W] {
				def cata[X](fail:Nes[F]=>X, win:W=>X):X	= win(value)
			}
			
	def fail[F,W](problems:Nes[F]):Safe[F,W]	=
			new Safe[F,W] {
				def cata[X](fail:Nes[F]=>X, win:W=>X):X	= fail(problems)
			}
			
	def fail1[F,W](problem:F):Safe[F,W]	=
			fail(Nes single problem)
		
	def optional[F,W](problems:Nes[F], value:Option[W]):Safe[F,W]	=
			value match {
				case Some(x)	=> win(x)
				case None		=> fail(problems)
			}
			
	def optional1[F,W](problem:F, value:Option[W]):Safe[F,W]	=
			optional(Nes single problem, value)
		
	def problematic[F,W](problems:Seq[F], value:W):Safe[F,W]	=
			Nes fromSeq problems map fail getOrElse win(value)
		
	def traverseIndexedSeq[F,S,T](func:S=>Safe[F,T]):IndexedSeq[S]=>Safe[F,IndexedSeq[T]]	= 
			ss	=> {
				(ss map func foldLeft win[F,IndexedSeq[T]](Vector.empty[T])) { (old, cur) =>
					old zip cur cata (
						(zipFail:Nes[F])		=> fail(zipFail),
						{ case (oldWin, curWin)	=> win(oldWin :+ curWin) }
					)
				}
			}
}

sealed trait Safe[+F,+W] {
	def cata[X](fail:Nes[F]=>X, win:W=>X):X
	
	def isWin:Boolean	= cata(_ => false, _ => true)
	def isFail:Boolean	= !isWin
	
	def zip[FF>:F,U](that:Safe[FF,U]):Safe[FF,(W,U)]	=
			this cata (
				thisProblems	=> {
					that cata (
						thatProblems	=> Safe fail (thisProblems ++ thatProblems),
						thatResult		=> Safe fail (thisProblems)
					)
				},
				thisResult	=> {
					that cata (
						thatProblems	=> Safe fail (thatProblems),
						thatResult		=> Safe win ((thisResult, thatResult))
					)
				}
			)
	
	def flatMap[FF>:F,U](func:W=>Safe[FF,U]):Safe[FF,U]	=
			cata(
				Safe.fail,
				func
			)
			
	def map[U](func:W=>U):Safe[F,U]	=
			cata(
				Safe.fail,
				func andThen Safe.win
			)
			
	def toOption:Option[W]	=
			cata(
				_ => None,
				Some.apply
			)
}
