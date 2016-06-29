package prob.pix;
/* pxQueue - Class to read PIX messages from a local filesystem directory for sending on to Cloverleaf.

   Written June 2015 by T Gucwa

   Originally written to replace, in part, the pclibc C program running in the UWIN environment of a Windows server to send PIMS data to Cloverleaf 
   (UWIN will no longer be used by DA). Currently, this class assumes the PIX messages are contained in files within a single directory, all at the same level.  
   Each file contains one or more PIX messages.  Each message begins with a 4-character exclusive message length (that is, the length of the message does not 
   include the length of the message length). The next message in the file, if there is one, begins immediately with another 4-character length; no delimiting 
   characters separate the messages.

   The constructor for this class opens the directory and loads an array of file objects in lastModified() sequence.
   It uses private methood dirListByAscendingDate(File folder) to perform the sort by lastModified() sequence.
   This class also contains a public method to retrieve the next message in sequence --getMsg()--.  getMsg() invokes a private method to reads that retrives 
   length and the content of that next message ---readMsg(). 

Future enhancements to this class may provide support for other ways of implementing message queues.
*/

import java.io.*;
import java.util.logging.*;
import java.util.Arrays;
import java.util.Comparator;

public class pxQueue {

  static Logger pxLog = Logger.getLogger(pxClient.class.getName());

  boolean found = false;

  FileReader fr;
  BufferedReader br;

  File d = null;
  File f = null;
  File[] paths;
  private int fileIndex = 0;
  private int fileOffset  = 0;

  public pxQueue(String pxMsgPath) throws FileNotFoundException {

    try {      
       File d = new File(pxMsgPath);
       if (!d.exists()) {
          pxLog.severe("PIX message path directory " + pxMsgPath + " does not exist."); 
          pxLog.severe("PIX Client terminating."); 
          System.exit(-1);
       } else {
         if (!d.isDirectory()) {
            pxLog.severe("PIX Message is not a directory. Terminating"); 
            pxLog.severe("PIX Client terminating."); 
            System.exit(-1);
         }
       }

       pxLog.fine("PIX message path directory " + pxMsgPath + " opened."); 

       // returns pathnames for files and directory
//     paths = d.listFiles();
       paths = dirListByAscendingDate(d);
       fileIndex = 0;

       if (paths.length > 0) {
         pxLog.fine("PIX message files found: " + Arrays.asList(paths) + ".");
         found = true;
         fr = new FileReader(paths[0]);
         br = new BufferedReader(fr);
         pxLog.finest("PIX FileReader: " + fr + "BufferedReader: " + br + "."); 
       }
     } catch(Exception e){
         e.printStackTrace();
     }
  }

  public StringBuffer getMsg() {

    pxLog.finest("Attempt getMsg(). FileReader: " + fr + ", BufferedReader: " + br + "."); 
    if (br == null) {
       pxLog.fine("Null BufferedReader, so return from next()."); 
       return null;
    } 

    StringBuffer sb = readMsg();
    if (sb == null) {
    	try {
    		br.close(); 
    		fr.close(); 
    	} catch (Exception e) {
    		pxLog.severe("Exception while closing message file reader."); 
    		System.exit(-1);
    	}
    	if (!paths[fileIndex].delete()) {
    		throw new IllegalArgumentException("Delete failed on PIX message file " + paths[fileIndex]);
    	} 
    	else {
    		pxLog.info("PIX message file "+ paths[fileIndex] + " deleted."); 
    		if (++fileIndex >= paths.length) {
    			pxLog.info("All " + paths.length + " message files processed/deleted."); 
    			found = false;
    			return null;
    		} 
    		else {
    			pxLog.fine("Go on to next PIX message file "+ paths[fileIndex] + "."); 
    			try {      
    				fr = new FileReader(paths[fileIndex]);
    				br = new BufferedReader(fr);
    			} catch(Exception e){
    				e.printStackTrace();
    			}
    			pxLog.finest("PIX FileReader: " + fr + "BufferedReader: " + br + "."); 
    		}
    	}
    	sb = readMsg();
	}
    return sb;
}

  public StringBuffer readMsg() {

     char[] chMsgLen = new char[4];
     char[] msgBuf   = new char[4096];
     int    readLen = 0;
     StringBuffer sb = new StringBuffer("");

     pxLog.finest("Attempt readMsg(). FileReader: " + fr + ", BufferedReader: " + br + "."); 
     if (br == null) {
       pxLog.finest("Null BufferedReader, so return from readMsg()."); 
       return null;
     } 

     try { 
       pxLog.finest("Attempt read of a msgLen at file offset " + fileOffset + ".");
       readLen = br.read(msgBuf, 0, 4); 
       if (readLen == 4) { 
         pxLog.finest("readLen=" + readLen + ". msg length found is " + String.valueOf(msgBuf) + "--" + msgBuf[0] + msgBuf[1] + msgBuf[2] + msgBuf[3]);

         System.arraycopy(msgBuf, 0, chMsgLen, 0, 4);
         pxLog.finest("chMsgLen=" + String.valueOf(chMsgLen));
         sb.append(chMsgLen);
         int msgLen = Integer.parseInt(new String(chMsgLen));

         fileOffset = fileOffset + readLen;
         pxLog.finest("msgLen=" + msgLen + " fileOffset to read="+ fileOffset + " max to read=" + msgLen);

         readLen = br.read(msgBuf, 4, msgLen);
         sb.append(msgBuf, 4, msgLen);
       } else {
          pxLog.finest("End of file. Missing or incomplete record length.  readLen=" + readLen + "."); 
          fileOffset = 0;
          br.close(); 
          fr.close(); 
          return null;
       }
       return sb;

     } catch(Exception e){
       // if any error occurs
       e.printStackTrace();
       try {      
         br.close(); 
       } catch(Exception io) {
         io.printStackTrace();
       }
       return null;
     }
  }

@SuppressWarnings({ "unchecked", "rawtypes" })

  public static File[] dirListByAscendingDate(File folder) {

    File files[] = folder.listFiles();

    pxLog.fine("Sort file names within directory ascending by timestamp."); 
    Arrays.sort(files, new Comparator() {
      public int compare(final Object o1, final Object o2) {
        return new Long(((File)o1).lastModified()).compareTo
             (new Long(((File) o2).lastModified()));
      }
    }); 
    return files;
  }  

}
