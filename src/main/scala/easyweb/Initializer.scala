
/*

======== auto-generated code ========
WARNING !!! Do not edit! 
Otherwise your changes may be lost the next time this file is generated.
  
Instead edit the file that generated this code. Refer below for details

Class that generated this code: org.sh.easyweb.AutoWebSession

Stacktrace of the call is given below:

org.sh.reflect.CodeGenUtil$:CodeGenUtil.scala:69
org.sh.easyweb.AutoWebSession:AutoWebSession.scala:22
org.sh.easyweb.AutoWebSession:AutoWebSession.scala:141
kiosk.KioskWeb$:KioskWeb.scala:43
kiosk.KioskWeb$delayedInit$body:KioskWeb.scala:20
scala.Function0:Function0.scala:39
scala.Function0:Function0.scala:39
scala.runtime.AbstractFunction0:AbstractFunction0.scala:17
scala.App:App.scala:80
scala.collection.immutable.List:List.scala:392

*/
package easyweb {
  import javax.servlet.http.HttpServlet
  import javax.servlet.http.{HttpServletRequest => HReq}
  import javax.servlet.http.{HttpServletResponse => HResp}
  import org.sh.easyweb.{HTMLClientCodeGenerator, Random}
  import org.sh.reflect.{DefaultTypeHandler, EasyProxy}

  class ShowHtmlServlet extends HttpServlet {
    val anyRefs = List(
      kiosk.Env,kiosk.Script,kiosk.ECC,kiosk.Box
    )
    val htmlGen = new HTMLClientCodeGenerator(anyRefs, """This is front-end for Kiosk, which is a library for interacting with the Ergo blockchain.
Kiosk is built on top of Ergo-AppKit and provides the ability to send transactions by spending
arbitrary boxes (provided their script allows them to be spent) and creating boxes with arbitrary guard scripts.
In order to create a script that hard-wires constants (such as a group element), first create a named constant
in the environment of the correct type. Then reference that name in the script code. To add that constant to a
register, use that name as part of the registerKeys array when creating a box.

This is a multi-tenant version of Kiosk, where each URL uniquely determines an environment. Variables and
boxes in one environment are independent of those in other environments. Thus, multiple users may use Kiosk without
mixing up their environments as long as they use different URLs.
When visiting the bare base URL, the system automatically redirects to a randomly generated new URL.

To get a new environment, simply use a new end-URL, either by visiting the bare base URL or manually creating one.
Note that environments are not saved to disk so the garbage collector may clear unused environments.""", None, false, false)
    val html = htmlGen.generateFilteredOut("", Nil)
    def isLocalHost(req:HReq) = req.getServerName == "localhost"
    override def doGet(hReq:HReq, hResp:HResp) = doPost(hReq, hResp)
    override def doPost(hReq:HReq, hResp:HResp) = {
      val urlPattern = hReq.getPathInfo
      val isNewPatternNeeded = urlPattern == null || urlPattern.replace("/", "") == ""
      val isHttpsNeeded = hReq.getScheme == "http" && !isLocalHost(hReq)
      val secret = if (isNewPatternNeeded) "/"+Random.randString else urlPattern

      if (isNewPatternNeeded || isHttpsNeeded) {
        hResp.sendRedirect(fullUrl(hReq, secret))
      } else {
        hResp.getWriter.print(html.replace("replaceWithActualSecret", secret))
      }
    }
    def getRedirectUrl(sessionUrl:String) = "/session"+sessionUrl
    def fullUrl(req:HReq, sessionUrl:String):String = {
      val relativeUrl = getRedirectUrl(sessionUrl)
      if (isLocalHost(req)) relativeUrl else {
        "https://"+req.getServerName+relativeUrl
      }
    }
  }

  class Initializer extends HttpServlet {

    val anyRefs = List(
      kiosk.Env,kiosk.Script,kiosk.ECC,kiosk.Box
    )
    anyRefs.foreach(EasyProxy.addProcessor("", _, DefaultTypeHandler, true))
    def getReq(hReq:HReq) = {}
    override def doGet(hReq:HReq, hResp:HResp) = doPost(hReq, hResp)
    override def doPost(hReq:HReq, hResp:HResp) = {}
  }
}
