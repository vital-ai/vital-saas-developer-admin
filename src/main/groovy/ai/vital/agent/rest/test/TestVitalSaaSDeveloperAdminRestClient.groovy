package ai.vital.agent.rest.test

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

class TestVitalSaaSDeveloperAdminRestClient extends groovy.lang.Script {

	static JsonSlurper parser = new JsonSlurper()
	
	static void main(args) {
		
		TestVitalSaaSDeveloperAdminRestClient script = new TestVitalSaaSDeveloperAdminRestClient()
		
		script.runScript(args)		
	}
	
	@Override
	public Object run() { }
	
	public void runScript(args) {
		
		println "Test Vital SaaS Developer Admin Rest Client"
		
		String apiKey = "key1"
		
		CloseableHttpClient httpclient = null
				
		Map parameterMap = [
			
			toolType: "WEATHER_INFO",
			
			requestMap: [
			
				location: "Brooklyn, NY",
				latitude: 40.6501,
				longitude: -73.9496	
			]
		]
		

		
		String jsonParameters = JsonOutput.toJson(parameterMap)
			
		httpclient = HttpClients.createDefault()
					
		HttpPost httppost = new HttpPost ( "http://localhost:8044/tool" )
			
		// HttpPost httppost = new HttpPost ( "http://localhost:8044/vector" )
		
		StringEntity entity = new StringEntity(jsonParameters, "utf-8")
			
		httppost.setEntity(entity)
			
		httppost.setHeader("Authorization", "Bearer ${apiKey}")
			
		httppost.setHeader("Content-type", "application/json")
			
		CloseableHttpResponse httpResponse = httpclient.execute(httppost)
		
		Integer statusCode = httpResponse.getStatusLine().getStatusCode()
		
		println "Status Code: " + statusCode
		
		String statusLine = httpResponse.getStatusLine()
		
		println "Status: " + statusLine
		
		String json_string = EntityUtils.toString( httpResponse.getEntity() )
		
		httpResponse.close()
					
		def pretty = JsonOutput.prettyPrint(json_string)
		
		Map result = parser.parse(json_string.toCharArray())
					
		println "Result:\n" + pretty
		
	}
	
}
