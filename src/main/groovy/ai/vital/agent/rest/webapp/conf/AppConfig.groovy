package ai.vital.agent.rest.webapp.conf

class AppConfig {
	
	String appID
	
	Boolean disableSaasClient
	
	String saasEventbusURL
	String saasUsername
	String saasPassword
	

	String weaviateServer
	
	Integer weaviatePort
	
	Integer weaviateVectorPort
	

	String virtuosoServer
	
	String virtuosoUsername
	
	String virtuosoPassword
	
	String virtuosoGraphName
	
	S3Config s3Config = new S3Config()

	List<APIKeyConfig> apiKeyConfigList = []
	

		
}


