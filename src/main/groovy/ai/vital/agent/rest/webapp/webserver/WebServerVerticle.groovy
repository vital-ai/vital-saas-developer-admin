package ai.vital.agent.rest.webapp.webserver

import ai.vital.agent.rest.webapp.VitalAgentRestWebappVerticle
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.StaticHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WebServerVerticle extends AbstractVerticle {

	// Note: depends on VitalAgentRestWebappVerticle initializing first for config
	
	private final static Logger log = LoggerFactory.getLogger(WebServerVerticle.class) 
	
	@Override
	public void start(Future<Void> startFuture) throws Exception {

		if(context == null) context = vertx.getOrCreateContext()
		
		Map<String, Object> webserverCfg = context.config().get("webserver")
		
		if(webserverCfg == null) {
			startFuture.fail("No webserver config part")
			return
		}
		
		def router = Router.router( vertx )

		router.route().handler(CorsHandler.create("*")
			.allowedMethod(io.vertx.core.http.HttpMethod.GET)
			.allowedMethod(io.vertx.core.http.HttpMethod.POST)
			.allowedMethod(io.vertx.core.http.HttpMethod.OPTIONS)
			//.allowCredentials(true)
			.allowedHeader("Access-Control-Allow-Headers")
			.allowedHeader("Authorization")
			.allowedHeader("Access-Control-Allow-Method")
			.allowedHeader("Access-Control-Allow-Origin")
			.allowedHeader("Access-Control-Allow-Credentials")
			.allowedHeader("Content-Type"));
		
		router.route().handler( { RoutingContext ctx ->
			ctx.response().putHeader("Cache-Control", "no-store, no-cache")
			ctx.next()
		})
		
		List<String> apiKeyList = []
		
		for(apiConfig in VitalAgentRestWebappVerticle.getAppConfig().apiKeyConfigList) {
			
			String apiKey = apiConfig.apiKey
			
			apiKeyList.add(apiKey)
			
		}
		
		router.route().handler {  RoutingContext ctx ->
			
			String authHeader = ctx.request().getHeader("Authorization")
			
			if (authHeader != null && authHeader.startsWith("Bearer ")) {
			
				String token = authHeader.substring(7)
			
				if (apiKeyList.contains(token)) {
					ctx.next()
				} else {
					ctx.response().setStatusCode(401).end("Unauthorized: Invalid API Key")
				}
			
			} else {
				ctx.response().setStatusCode(401).end("Unauthorized: Bearer token required")
			}
		}
	
	
	
		router.get('/status').handler(new StatusHandler())
		
		router.post('/vector').handler(new VectorHandler( vertx ))
		
		router.post('/tool').handler(new ToolHandler( vertx ))
		
		router.post('/kgupdate').handler(new KGUpdateHandler( vertx ))
		
		router.post('/kgquery').handler(new KGQueryHandler( vertx ))
		
		router.post('/binary').handler(new BinaryHandler( vertx ))
			
		StaticHandler staticHandler = StaticHandler.create()
		
		staticHandler.setCachingEnabled(false)
		
		staticHandler.setFilesReadOnly(false)

		router.route().handler(staticHandler)
		
		Map<String,Object> cfg = webserverCfg
		
		def server = vertx.createHttpServer(cfg)
		
		server.requestHandler(router.&accept)
		
		server.listen() { AsyncResult<HttpServer> res ->
		
			if(! res.succeeded() ) {
				
				log.error("Failed to start http server: ${res.cause().localizedMessage}", res.cause())
				
				startFuture.fail(res.cause())
				
				return 
			}
				
			log.info "WWW server started - host: ${cfg.host} port ${cfg.port}"
			
			startFuture.complete()
		}
	}
}
