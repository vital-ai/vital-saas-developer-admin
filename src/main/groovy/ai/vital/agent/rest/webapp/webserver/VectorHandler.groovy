package ai.vital.agent.rest.webapp.webserver

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.ext.web.RoutingContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class VectorHandler implements Handler<RoutingContext> {
	
	private final static Logger log = LoggerFactory.getLogger(VectorHandler.class)
	
	static JsonSlurper jsonParser = new JsonSlurper()
		
	static Vertx vertxInstance
		
	public VectorHandler(Vertx vertxInstance) {
			
		this.vertxInstance = vertxInstance
	}
		
	@Override
	public void handle(RoutingContext ctx) {
			
		ctx.request().bodyHandler({ body ->
				
			log.info("VectorHandler: " + body)
				
			try {
						
				Map jsonMap = jsonParser.parseText(body as String)
								
				boolean ok = true
				
				String requestJSON = JsonOutput.toJson(jsonMap)
	
				DeliveryOptions options = new DeliveryOptions()
	
				options.setSendTimeout(600000)
	
				vertxInstance.eventBus().send("vector", requestJSON, options) { Future<Message> response ->
	
					if(response.succeeded()) {
	
						String resultJSON = response.result().body()
	
						Map resultMap = jsonParser.parseText(resultJSON)
		
						def sendResponse = { ->
	
							ctx.response().end(JsonOutput.toJson(resultMap), 'UTF-8')
						}
	
						sendResponse()
	
						return
	
					}
					else {
	
						String message = 'error in vector request'
	
						def sendResponse = { ->
							Map r = [ok: false, message: message]
							ctx.response().end(JsonOutput.toJson(r), 'UTF-8')
						}
	
						sendResponse()
					}
				}
				
			} catch(Exception ex) {
				
				log.error("JSON Parse Error: " + ex.localizedMessage)
			
				boolean ok = false
				
				String message = 'VectorHandle Error: ' + ex.localizedMessage
				
				def sendResponse = { ->
				Map r = [ok: ok, message: message]
					ctx.response().end(JsonOutput.toJson(r), 'UTF-8')
				}
				
				sendResponse()
			}
		})
	}
}
