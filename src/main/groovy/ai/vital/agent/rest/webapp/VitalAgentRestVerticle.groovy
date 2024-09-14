package ai.vital.agent.rest.webapp

import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.model.VitalApp
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.lang.groovy.GroovyVerticle
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicInteger

// import ai.vital.vitalservice.query.VitalGraphQuery
// import org.apache.http.entity.mime.MultipartEntityBuilder

class VitalAgentRestVerticle extends GroovyVerticle {
	
	private final static Logger log = LoggerFactory.getLogger(VitalAgentRestVerticle.class)
	
	private static final AtomicInteger counter = new AtomicInteger()
	
	private int instanceId

	public static boolean initialized = false
	
	public static String appID = null
	
	public static VitalApp app = null

	static Vertx vertxInstance
			
	@Override
	public void start(Future<Void> startFuture)  throws Exception {
		
		log.info("starting...")
			
		if(initialized) {
			
			startFuture.complete(true)
			
			return		
		}
		
		instanceId = counter.incrementAndGet()
		
		vertxInstance = vertx
		
		Map<String, Object> mainConfig = vertxInstance.getOrCreateContext().config()
				
		////////////////////////////
		
		String _appID = mainConfig.get('appID')
		
		appID = _appID
		
		if(!_appID) throw new RuntimeException("No 'appID' param")
		
		log.info("appID: ${_appID}")
		
		app = VitalApp.withId( appID )
				
		initialize(startFuture)
			
		startFuture.complete(true)
	}
	
	public void initialize(Future<Void> startFuture) {
		
		VitalSigns vs = VitalSigns.get()
				
		BinaryImplHandler binaryImplHandler = new BinaryImplHandler(instanceId)
		
		vertxInstance.eventBus().consumer("binary") { Message msg ->
			
			binaryImplHandler.handle(msg)
		}
		


		

		
		
		
		log.info("initialized.")
			
		initialized = true
	
	}
	
	public void tearDown(Future<Void> stopFuture) {
		
		initialized = false
	}

	@Override
	public void stop(Future<Void> stopFuture) throws Exception {

		tearDown(stopFuture)
		
		stopFuture.complete(true)
	}
	

	
}
