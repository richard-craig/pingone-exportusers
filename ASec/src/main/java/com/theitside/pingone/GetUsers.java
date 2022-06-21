package com.theitside.pingone;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.github.opendevl.JFlatMine;

public class GetUsers {

	private static final boolean DEBUG 					= false;	
	private static final String pingOneBaseURL 			= "https://api.pingone.eu/v1/environments/";
	private static final String pingOneFunctionNoLimit 	= "/users?limit=";

	public static void main(String[] args) {
		
		if( args.length != 4 ) {
			System.out.println("Usage: java -jar GetUsers.jar <fileName> <pingEnvironmentID> <limit> <authToken>"
				+ "\r\n e.g. java -jar GetUsers.jar users.csv 65cf3695-3caf-4591-aa32-33e24d36a6ef 100 eyJhbGciOiJSUzI1NiIsImtpZCI6ImRlZmF1bHQifQ");
			System.exit(1);
		}
					
		String fileName = args[0];
		String pingOneEnvironment = args[1];		
		String pingOneFunction = pingOneFunctionNoLimit + args[2];		
		String pingOneToken = args[3];
	
		try {
						
				JSONParser parse = new JSONParser();
				JSONObject jobj = (JSONObject) parse.parse(getDataFromUrl(pingOneBaseURL + pingOneEnvironment + pingOneFunction, pingOneToken));
								
				JFlatMine flatMe = new JFlatMine(((JSONObject)jobj.get("_embedded")).get("users").toString() );				
				List<Object[]> json2csv = flatMe.json2Sheet().getJsonAsSheet();				
				flatMe.write2csv(fileName);
								
				//See if we have a next?
				while ( jobj.get("_links") != null 
						&& ( (JSONObject) jobj.get("_links") ).get("next")  != null    
						&& ( (JSONObject)( (JSONObject) jobj.get("_links")).get("next") ).get("href") != null ) {
					
					System.out.println("Processing next batch of user objects...");
					
					String next = ((JSONObject)((JSONObject)jobj.get("_links")).get("next")).get("href").toString();
					
					jobj = (JSONObject) parse.parse(getDataFromUrl(next, pingOneToken));					
					flatMe = new JFlatMine(((JSONObject)jobj.get("_embedded")).get("users").toString() );				
					json2csv = flatMe.json2Sheet(false).getJsonAsSheet();				
					flatMe.write2csv(fileName);																		
				} 				
				System.out.println("DONE!");													
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String getDataFromUrl(String targetURL, String token) {		
		StringBuffer response = new StringBuffer();
		HttpURLConnection conn = null;
		try {
			URL url = new URL(targetURL);
			//Parse URL into HttpURLConnection in order to open the connection in order to get the JSON data
			conn = (HttpURLConnection) url.openConnection();
			//Set the request to GET or POST as per the requirements
			conn.setRequestMethod("GET");
			//Set the bearer token
			conn.setRequestProperty("Authorization", "Bearer " + token);
			//Use the connect method to create the connection bridge
			conn.connect();
			//Get the response status of the Rest API
			int responsecode = conn.getResponseCode();
			System.out.println("Response code is: " + responsecode);			
			//Iterating condition to if response code is not 200 then throw a runtime exception
			//else continue the actual process of getting the JSON data
			if(responsecode != 200)
				throw new RuntimeException("HttpResponseCode: " + responsecode);
			else {				
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String inputLine;				
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}			
				if( DEBUG ) System.out.println("Response:" + response);									
				in.close();				
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if( conn != null )
				conn.disconnect();
		}
		return response.toString();		
	}

}