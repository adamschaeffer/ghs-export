package prob.ghs.soap;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.net.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class SoapClient {
	/**
	 * The web service is hosted at webadminisd.hosted.lac.com. Content type: text/xml; charset=utf-8.
	 * 
	 *  For more information, go to http://webadminisd.hosted.lac.com/iadwebsvc/service.asmx?op=GetUserProfile
	 */
	private static final String ServerURI = "http://webadminisd.hosted.lac.com/iadwebsvc/service.asmx?op=GetUserProfile";
	private static final String RequestStringTemplate = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
												"<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"+
												"<soap:Body>"+
													"<GetUserProfile xmlns=\"http://lacounty.gov/IADWebServices\">"+
														"<loginID>{0}</loginID>"+
													"</GetUserProfile>"+
												"</soap:Body>"+
												"</soap:Envelope>";
	
	public static Map<String,String> getNameFromID(String eNumber){
		Map<String,String> AdminName = null;
		
		try {
			AdminName = getName(eNumber);
		} catch (IOException e) {
			throw new RuntimeException("IO Error when getting Admin name: " + e.getMessage(),e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Error parsing XML response when getting Admin name: " + e.getMessage(),e);
		} catch (SAXException e) {
			throw new RuntimeException("Error when getting Admin name: " + e.getMessage(),e);
		}
		
		return AdminName;
	}
	private static Map<String,String> getName(String eNumber) throws IOException, ParserConfigurationException, SAXException{
		String RequestString = MessageFormat.format(RequestStringTemplate,eNumber);
		
		URL url = new URL(ServerURI);
		URLConnection conn = url.openConnection();
		HttpURLConnection httpConn = (HttpURLConnection) conn;
		
		byte[] RequestByteArray = RequestString.getBytes();
		
		httpConn.setRequestProperty("Content-Length",String.valueOf(RequestByteArray.length));
		httpConn.setRequestProperty("Content-Type","text/xml; charset=utf-8");
		httpConn.setRequestMethod("POST");
		httpConn.setDoOutput(true);
		httpConn.setDoInput(true);
		
		OutputStream out = httpConn.getOutputStream();
		out.write(RequestByteArray);
		out.close();
		
		InputStream in = httpConn.getInputStream();
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(in);
		
		Map<String,String> AdminName = new HashMap<String,String>();
		
		doc.getDocumentElement().normalize();
		NodeList nList = doc.getElementsByTagName("GetUserProfileResult");
		for (int temp = 0; temp < nList.getLength(); temp++) {
		    Node nNode = nList.item(temp);

		    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
		    	Element eElement = (Element) nNode;
		    	AdminName.put("firstName",eElement.getElementsByTagName("givenName").item(0).getTextContent());
		    	AdminName.put("lastName",eElement.getElementsByTagName("sn").item(0).getTextContent());
	    	}
		}
		
		return AdminName;
	}
}
