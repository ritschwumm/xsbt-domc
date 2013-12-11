package domc.servlet

import java.io._

import javax.servlet.http._

import scutil.lang._
import scutil.implicits._
import scutil.io.Charsets._
import scutil.log._

import scwebapp._
import scwebapp.implicits._
import scwebapp.instances._
import scwebapp.method._
import scwebapp.status._

import domc._

/**
mount this with an url-pattern of *.dom
expects UTF-8 encoded URLs
only reacts to GET requests
*/
final class DomServlet extends HttpServlet with Logging {
	private val encoding				= utf_8
    private val text_javascript_charset	= text_javascript addParameter ("charset", encoding.name)
	
    override def init() {}
	override def destroy() {}
    
    override def service(request:HttpServletRequest, response:HttpServletResponse) {
		// TODO no!
		response.noCache()	
		
		val action:Tried[HttpResponder,HttpResponder]	=
				for {
					_		<- 
							request.method == GET trueWin {
								ERROR("not a get request", request.method)
								SetStatus(METHOD_NOT_ALLOWED)
							}
					path	<- 
							request.fullPathUTF8.guardNonEmpty toWin {
								ERROR("missing path")
								SetStatus(NOT_FOUND)
							}
					_		<-
							path splitAroundChar '/' contains ".." falseWin {
								ERROR("invalid path", path)
								SetStatus(NOT_FOUND)
							}
					url		<- 
							getServletContext resourceOption path toWin {
								ERROR("resource not found", path)
								SetStatus(NOT_FOUND)
							}
					text	=
							(url withReader encoding) { _.readFully }
					code	<-
							DomTemplate compile text cata (
								err => {
									ERROR("cannot compile DOM", path)
									ERROR((err.toSeq):_*)
									Fail(SetStatus(INTERNAL_SERVER_ERROR))
								},
								Win.apply
							)
				}
				yield {
					// DEBUG("compiled DOM for path", path)
					SetStatus(OK)							~>
					SetContentType(text_javascript_charset)	~>
					SendString(code)
				}
				
		action.merge apply response
	}
}
