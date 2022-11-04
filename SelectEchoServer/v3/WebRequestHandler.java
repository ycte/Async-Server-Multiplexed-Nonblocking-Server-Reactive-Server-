/**
 ** XMU CNNS Class Demo Basic Web Server
 **/
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;

class WebRequestHandler {

    static boolean _DEBUG = true;
    static int     reqCount = 0;

    String WWW_ROOT;
//    Socket connSocket;
    ByteBuffer inFromClient;
	String outToClient = "";
//    DataOutputStream outToClient;

    String urlName;
    String fileName;
    File fileInfo;

	Map<String, String> fileCache = new HashMap<>();

    public WebRequestHandler(ByteBuffer inBuffer,
			     String WWW_ROOT, Map<String, String> fileCache) throws Exception
    {
        reqCount ++;
		this.fileCache = fileCache;
	    this.WWW_ROOT = WWW_ROOT;
	    inFromClient =
	      inBuffer;
    }

    public String processRequest()
    {

	try {
	    mapURL2File();

	    if ( fileInfo != null ) // found the file and knows its info
	    {
		    outputResponseHeader();
		    outputResponseBody();
	    } // dod not handle error
		return outToClient;
//	    connSocket.close();
	} catch (Exception e) {
	    outputError(400, "Server error");
		return outToClient;
	}



    } // end of processARequest

    private void mapURL2File() throws Exception 
    {
				
	    String requestMessageLine = new String(inFromClient.array());
	    DEBUG("Request " + reqCount + ": " + requestMessageLine);
		String userAgent;
		String[] userAgentArray, sentenceFromClient;
//		while ((sentenceFromClient = requestMessageLine.readLine()) != null) {
////			DEBUG(sentenceFromClient);
//			if(sentenceFromClient.charAt(0) == 'U') {
//				userAgentArray = sentenceFromClient.split("\\s");
//				userAgent = userAgentArray[1];
////				DEBUG(userAgent);
//				break;
//			}
//		}
		// String requestMessageLine1 = inFromClient.readLine();
	    // DEBUG(requestMessageLine1);
	    // process the request
	    String[] request = requestMessageLine.split("\\s");
		
	    if (request.length < 2 || !request[0].equals("GET"))
	    {
		    outputError(500, "Bad request");
		    return;
	    }

	    // parse URL to retrieve file name
	    urlName = request[1];
	    DEBUG(urlName);
		if (urlName.equals("/"))
			urlName = "index.html";
	    else if ( urlName.startsWith("/") == true )
	       urlName  = urlName.substring(1);

        // debugging
        // if (_DEBUG) {
        //    String line = inFromClient.readLine();
        //    while ( !line.equals("") ) {
        //       DEBUG( "Header: " + line );
        //       line = inFromClient.readLine();
        //    }
        // }

	    // map to file name
	    fileName = WWW_ROOT + urlName;
	    DEBUG("Map to File name: " + fileName);
	    fileInfo = new File( fileName );
	    if ( !fileInfo.isFile() ) 
	    {
			DEBUG("file not found");
		    outputError(404,  "Not Found");
		    fileInfo = null;
	    }

    } // end mapURL2file


    private void outputResponseHeader() throws Exception 
    {
	    String strTemp = "HTTP/1.0 200 Document Follows\r\n"
				+ "Set-Cookie: MyCool433Seq12345\r\n";

//	    outToClient.writeBytes("Set-Cookie: MyCool433Seq12345\r\n");

		// Time in the format of Java currentTimeMill
		// rfc1123-date = wkday "," SP date1 SP time SP "GMT"
		// date1        = 2DIGIT SP month SP 4DIGIT
		SimpleDateFormat formatter= new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
		formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date date = new Date(System.currentTimeMillis());
//        System.out.println("Start time:" + formatter.format(date));
		Date date1 = formatter.parse("wed, 26 oct 2022 15:37:15 CST");
//        System.out.println("Start time:" + formatter.format(date1));
		strTemp += "Date: "+formatter.format(date);

		String htName =  InetAddress.getLocalHost().getHostName();
		strTemp += "Server: "+htName;

	    if (urlName.endsWith(".jpg"))
	        strTemp += "Content-Type: image/jpeg\r\n";
	    else if (urlName.endsWith(".gif"))
	        strTemp += "Content-Type: image/gif\r\n";
	    else if (urlName.endsWith(".html") || urlName.endsWith(".htm"))
	        strTemp += "Content-Type: text/html\r\n";
	    else
	        strTemp += "Content-Type: text/plain\r\n";
		DEBUG("Header");
		outToClient += strTemp;
    }

    private void outputResponseBody() throws Exception 
    {

	    int numOfBytes = (int) fileInfo.length();
		String strTemp =
	    	"Content-Length: " + numOfBytes + "\r\n";
	    strTemp += "\r\n";

		if(fileCache.containsKey(fileName)) {
			byte[] fileInBytes = new byte[numOfBytes];
			fileInBytes = fileCache.get(fileName).getBytes();
			strTemp += new String(fileInBytes);
//			outToClient.write(fileInBytes, 0, numOfBytes);
			DEBUG("fileCache:" + fileName);
		} else {
			// send file content
			FileInputStream fileStream  = new FileInputStream (fileName);

			byte[] fileInBytes = new byte[numOfBytes];
			fileStream.read(fileInBytes);
			// DEBUG(fileInBytes.toString());
			strTemp += new String(fileInBytes);
//			outToClient.write(fileInBytes, 0, numOfBytes);
			if(true) {
				fileCache.put(fileName, new String(fileInBytes));
			}
//			DEBUG(fileCache.get(fileName));
		}
		DEBUG("Body");
		outToClient += strTemp;
    }

    void outputError(int errCode, String errMsg)
    {
//	    try {
//	        outToClient.writeBytes("HTTP/1.0 " + errCode + " " + errMsg + "\r\n");
//	    } catch (Exception e) {}
//		StringBuffer strBufTemp = new StringBuffer(4096);
//		strBufTemp.append("HTTP/1.0 " + errCode + " " + errMsg + "\r\n");
//		for (int i = 0; i < strBufTemp.length(); i++) {
//			char ch = strBufTemp.charAt(i);
//
////			ch = Character.toUpperCase(ch);
//
//			outToClient.put((byte) ch);
//		}
		outToClient += "HTTP/1.0 " + errCode + " " + errMsg + "\r\n";
    }

    static void DEBUG(String s) 
    {
       if (_DEBUG)
          System.out.println( s );
    }
}
