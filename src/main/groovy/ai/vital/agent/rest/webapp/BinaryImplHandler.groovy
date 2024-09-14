package ai.vital.agent.rest.webapp

import ai.vital.vitalsigns.model.VitalApp
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.eventbus.Message
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// import org.apache.http.entity.mime.MultipartEntityBuilder

class BinaryImplHandler {
	
	private final static Logger log = LoggerFactory.getLogger(BinaryImplHandler.class)
	
	static JsonSlurper parser = new JsonSlurper()
	
	static ExecutorService executorService = Executors.newFixedThreadPool(1)
	
	static VitalApp app = VitalApp.withId("chat-saas")
	
	Integer instanceId
		
	public BinaryImplHandler(Integer instanceId) {
			
		this.instanceId = instanceId
	}
		
	public void handle(Message message) {
		
		log.info("Instance: ${instanceId} Handling: " + message)
		
		String body = message.body()
		
		log.info("Instance: ${instanceId} Process Message: " + body)
		
		Map requestMap = parser.parseText(body as String)
		
		
		
		Map replyMap = [:]
		
		replyMap["status"] = "warn"
		
		replyMap["message"] = "An error occurred. Unknown criteria was supplied."
		
		String replyJSON = JsonOutput.toJson(replyMap)
		
		message.reply(replyJSON)
		
		return	
	}
	
	
}
