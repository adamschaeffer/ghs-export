package prob.pix;

/* pxSocket - Class to send PIX messages to a server thread in the Cloverleaf engine. 

   Written June 2015 by T Gucwa
   Modified May 2016 by A Schaeffer
   		- Added SetTimeout function
   		- Added support for the socket failing to receive anything from Cloverleaf.
   		- Added 3-argument constructor

   Originally written to replace, in part, the pclibc C program running in the UWIN environment of a Windows server to send PIMS data to Cloverleaf (UWIN will no longer be used by DA).

The constructor for this class opens the socket to a TCP/IP server at a parameter host name and port number.  It provides public method sendMsg() to send the message to the server, public method recvAck() to receive acknowledgement that the message was received by the server, and public methid close() to close the socket connection.

sendMsg() sends the character data in the UTF codeset via the writeUTF() method of class DataOutputStream.  Note that, internally, writeUTF() prefixes each write with a two-byte exclusive binary message length.  That binary length is not visible in this Java code, but must be considered when configuring the Cloverleaf server thread.

recvAck() uses two Java networking methods to receive the acknowledgment:  readShort() receives the two-byte binary length of the message; readByte() receives the one-byte length of the PIX standard acknowledgment.  By PIX standard, the standard acknowledgment character has been the ASCII ACK character, hexadecimal x'06'.  For future consideration, that standard might be expanded or changed for readability purpose.  Currently, recvAck() considers any other response a non-acknowledgement. 

*/

import java.io.*;
import java.net.*;
import java.util.logging.*;

public class pxSocket{

  Logger pxLog = Logger.getLogger(pxClient.class.getName()); 

  Socket sock;

  DataOutputStream out;

  DataInputStream  in;

  public pxSocket(String host, int port, int timeout){
	  this(host,port);
	  setTimeout(timeout);
  }
  public pxSocket(String host, int port) {
    try {
      //1. creating a socket to connect to the server
      sock = new Socket(host, port);
      pxLog.fine("Connected to host " + host + "on port " + port + ".");

      //2. get Input and Output streams
      out = new DataOutputStream(sock.getOutputStream());

      in  = new DataInputStream(sock.getInputStream());

    } catch(UnknownHostException unknownHost){
      pxLog.severe("Trying to connect to an unknown host.");
      throw new RuntimeException("Trying to connect to an unknown host.");
    } catch(ConnectException connectException){
      pxLog.severe("Host found but not listening on specified port.");
      throw new RuntimeException("Host found but not listening on specified port.");
    } catch(IOException ioException){
      ioException.printStackTrace();
      throw new RuntimeException("Random IOException: " + ioException.getMessage());
    } 
  }
  
  public void setTimeout(int timeout){
	  try {
		sock.setSoTimeout(5000);
	} catch (SocketException e) {
		throw new RuntimeException("Error: Cannot set timeout for the socket.");
	}
  }

  public void sendMsg(String msg) {

    pxLog.fine("Sending message:\n" + msg);
    try {
      out.writeUTF(msg);
      out.flush();
      pxLog.fine("Message sent.");
    }
    catch(IOException ioException) {
      ioException.printStackTrace();
    }
  }

  public boolean recvAck() {

    pxLog.finest("Awaiting acknowledement...");
    try {
      int  len   = in.readShort();  
      if (len == 1) { 
        byte ack   = in.readByte();  
        if (ack == 0x06) {  
          pxLog.fine("Acknowledgement received: hex 0x" + String.format("%02x", ack));
        } else {
          pxLog.fine("Something received, but not an acknowledgement: <" + ack + String.format("> hex: %02x", ack));
          return false;
        }
      }
    } catch(java.net.SocketTimeoutException e){
    	pxLog.severe("Timed out waiting for acknowledgement.");
    	return false;
    } catch(IOException ioException){
      ioException.printStackTrace();
      return false;
    }
    return true;
  }

  public void close() {

    pxLog.finest("About to close socket.");

    try {
      in.close();
      out.close();
      sock.close();
    }
    catch(IOException ioException){
      ioException.printStackTrace();
    }
  }

}
