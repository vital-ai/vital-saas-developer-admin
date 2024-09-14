package ai.vital.agent.rest.webapp

import ai.vital.agent.rest.webapp.conf.*
import ai.vital.vitalsigns.model.VitalApp
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class VitalAgentRestWebappVerticle extends AbstractVerticle {

	public static boolean initialized = false
	
	private final static Logger log = LoggerFactory.getLogger(VitalAgentRestWebappVerticle.class)
	
	static Vertx vertxInstance
	
	public static VitalApp app
	
	private static AppConfig appConfig = null
		
	static public AppConfig getAppConfig() {
		
		return appConfig
	}
	
	@Override
	public void start(Future<Void> startedResult) {
		
		if(initialized) {
			
			startedResult.complete(true)
			
			return
		}
		
		vertxInstance = vertx
		
		synchronized (VitalAgentRestWebappVerticle.class) {
			
			if(initialized) return
			
			initialized = true
		}
		
		if(context == null) context = vertx.getOrCreateContext()
			
		Boolean disableSaasClient = false
			
		String saasEventbusURL
			
		String saasUsername
			
		String saasPassword
		
		String appID
				
		Map config = context.config()
		
		appConfig = new AppConfig()
		
		disableSaasClient = config.get('disableSaasClient') != null && config.get('disableSaasClient') == true
		
		log.info("disableSaasClient: ${disableSaasClient}")
		
		appConfig.disableSaasClient = disableSaasClient
		
		saasEventbusURL = config.get('saasEventbusURL')
		
		if(!saasEventbusURL) throw new RuntimeException("No 'saasEventbusURL' param")
		
		log.info("saasEventbusURL: ${saasEventbusURL}")
		
		appConfig.saasEventbusURL = saasEventbusURL
		
		saasUsername = config.get('saasUsername')
		
		if(!saasUsername) throw new RuntimeException("No saasUsername")
		
		log.info("saasUsername: ${saasUsername}")
		
		appConfig.saasUsername = saasUsername
		
		saasPassword = config.get('saasPassword')
		
		if(!saasPassword) throw new RuntimeException("No saasPassword")
		
		log.info("saasPassword ${VertxUtils.maskPassword(saasPassword)}")
		
		appConfig.saasPassword = saasPassword
		
		appID = config.get('appID')
		
		if(!appID) throw new RuntimeException("No appID string param")
		
		log.info("appID: ${appID}")
		
		app = VitalApp.withId(appID)
		
		appConfig.appID = appID
		

		

		
		
		
		
		// s3
		
		appConfig.s3Config = new S3Config()
			
		Map s3Map = config.get("s3")
		
		if(!s3Map) throw new RuntimeException("No s3 map in config.")
			
		
		String s3_aws_zone = s3Map.get('aws_zone')
		
		log.info("s3_aws_zone: ${s3_aws_zone}")
		
		if(!s3_aws_zone) throw new RuntimeException("No s3_aws_zone")
			
		appConfig.s3Config.aws_zone = s3_aws_zone
		
		
		String s3_organization_id = s3Map.get('organization_id')
		
		log.info("s3_organization_id: ${s3_organization_id}")
		
		if(!s3_organization_id) throw new RuntimeException("No s3_organization_id")
			
		appConfig.s3Config.organization_id = s3_organization_id
		
		
		String s3_app_id = s3Map.get('app_id')
		
		log.info("s3_app_id: ${s3_app_id}")
		
		if(!s3_app_id) throw new RuntimeException("No s3_app_id")
			
		appConfig.s3Config.app_id = s3_app_id
		
		
		String s3_access_key = s3Map.get('access_key')
		
		log.info("s3_access_key: ${VertxUtils.maskPassword(s3_access_key)}")
		
		if(!s3_access_key) throw new RuntimeException("No s3_access_key")
			
		appConfig.s3Config.access_key = s3_access_key
		

		String s3_secret_key = s3Map.get('secret_key')
		
		log.info("s3_secret_key: ${VertxUtils.maskPassword(s3_secret_key)}")
				
		if(!s3_secret_key) throw new RuntimeException("No s3_secret_key")
			
		appConfig.s3Config.secret_key = s3_secret_key
		

		String s3_private_bucket_name = s3Map.get('private_bucket_name')
		
		log.info("s3_private_bucket_name: ${s3_private_bucket_name}")
		
		if(!s3_private_bucket_name) throw new RuntimeException("No s3_private_bucket_name")
			
		appConfig.s3Config.private_bucket_name = s3_private_bucket_name
		
		
		String s3_public_bucket_name = s3Map.get('public_bucket_name')
		
		log.info("s3_public_bucket_name: ${s3_public_bucket_name}")
		
		if(!s3_public_bucket_name) throw new RuntimeException("No s3_public_bucket_name")
			
		appConfig.s3Config.public_bucket_name = s3_public_bucket_name
		

		appConfig.apiKeyConfigList = []
		
		List<String> apiKeyList = config.get('auth_key_list')

		for(apiKey in apiKeyList) {
			
			APIKeyConfig keyConfig = new APIKeyConfig()
			
			keyConfig.apiKey = apiKey
			
			appConfig.apiKeyConfigList.add(keyConfig)
			
		}
		
		startedResult.complete()				
	}

}
