package prob.pix;

/* pxClient - Client program to send PIX messages to a Cloveleaf server thread via the Berkeley Sockets TCP/IP protocol.

   Written June 2015 by T Gucwa

   Originally written to replace the pclibc C program running in the UWIN environment of a Windows server to send PIMS data to Cloverleaf (UWIN will no longer be iinstalled or used by DA). 
   
Uses the pxQueue class to retrieve the queued messages from an application.  Uses the pxSocket class to open a socket connection, send the messages, and receive acknowledgments.
                                pxClient
                              |         |
                           pxQueue   pxSocket

Obtains the destination host name and port number from environmental variables.
Also obtains the PIX Message path from an environmental variable.

Logs significant events and daignostic information via the standard Java Logging API.  Customaization of logging, including the turning on of debug logging (ie, fine, finer, and finest log levels) can be controlled via a configuration file as specified in the java run command.
*/ 

import java.io.*;
import java.util.logging.*;

public class pxClient {

public pxClient() {}

static Logger pxLog = Logger.getLogger(pxClient.class.getName());

static pxSocket sock;

static String msgPathName;
                        /* Default directory for received PIX messages. */

	@SuppressWarnings("unused")
	public static void main(String args[]) {
	
	  int  msgCnt   = 0;
	
	  StringBuffer msg;   /* The PIX Message */ 
	
	   pxLog.info("======     PIX Client started     ======");
	
	   String hostName = GetEnvVar("HOST");
	   //String hostName = "10.48.156.80";
	   if (hostName == null) {
	     pxLog.severe("PIX Client cannot proceed without server host name. PIX Client terminating.");
	     throw new RuntimeException("PIX Client cannot proceed without server host name. PIX Client terminating.");
	   }
	
	   String strPort  = GetEnvVar("PORT");
	   //String strPort = "22501";
	   if (strPort == null) {
	     pxLog.severe("PIX Client cannot proceed without server port number. PIX Client terminating.");
	     throw new RuntimeException("PIX Client cannot proceed without server port number. PIX Client terminating.");
	   }
	
	  String msgPathName = GetEnvVar("PIXMSGPATH");
	  //msgPathName = "C:\\PIX\\";
	  if (msgPathName == null) {
	    pxLog.severe("PIX Client cannot proceed without a path name for the PIX messages. PIX Client terminating.");
	    throw new RuntimeException("PIX Client cannot proceed without a path name for the PIX messages. PIX Client terminating.");
	  }

	  try {
	    pxQueue q = new pxQueue(msgPathName);

	    if (q != null) {
	      pxLog.finest("PIX inbound fileset instantiated at " + msgPathName + ".");
	      if (q.found) { 
	        msg = q.getMsg();
	        if (msg != null) {
	          pxLog.finest("First IB message retrieved:\n" + msg.toString());
	          int intPort = Integer.parseInt(strPort);
	          sock = new pxSocket(hostName, intPort);
	          pxLog.fine("Socket connection established.");
	        }
	        while (msg != null) {
	          pxLog.fine("Calling sendMsg().");
	          sock.sendMsg(msg.toString());
	          pxLog.fine("Returned from sendMsg().  Now to recvAck().");
	          pxLog.info("Message sent. Waiting for reply.");
	          if (!sock.recvAck()) {
	            pxLog.severe("No Acknowlegment received from server. Terminating.");
	            throw new RuntimeException("No Acknowlegment received from server. Terminating.");
	          } else {
	            ++msgCnt;
	          }
	          msg = q.getMsg();
	          if (msg != null) {
	            pxLog.finest("Next IB message retrieved:\n" + msg.toString());
	          }
	        }
	      } else {
	        pxLog.info("No PIX message files in " + msgPathName + ".");
	      }
	    } else {
	      pxLog.severe("PIX inbound fileset in " + msgPathName + " failed to instantiate.");
	      pxLog.severe("!!!!! PIX Client ending abnormally after delivering " + msgCnt + " messages !!!!!");
	      throw new RuntimeException("!!!!! PIX Client ending abnormally after delivering " + msgCnt + " messages !!!!!");
	    }
	    pxLog.info("PIX Client delivered " + msgCnt + " messages.");
	    pxLog.info("====== PIX Client ending normally after delivering " + msgCnt + " messages ======");
	    throw new RuntimeException("====== PIX Client ending normally after delivering " + msgCnt + " messages ======");
	
	  } catch (FileNotFoundException fnf) {
	     pxLog.info("PIX Client found no queued messages. Terminating.");
	     throw new RuntimeException("PIX Client found no queued messages. Terminating.");
	  }
	}

	private static String GetEnvVar(String envVarName) {
	
	  String envVar = System.getenv(envVarName);
	  if (envVar != null) {
	    pxLog.info("Environmental variable " + envVarName + "=" + envVar + ".");
	    return(envVar);
	  } else {
	    pxLog.info("Environmental variable " + envVarName + " not defined.");
	    return(null);
	  }
	} 
}
