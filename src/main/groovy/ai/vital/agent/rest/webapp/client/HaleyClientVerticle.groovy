package ai.vital.agent.rest.webapp.client

import ai.haley.api.HaleyAPI
import ai.haley.api.session.HaleySession
import ai.haley.api.session.HaleyStatus
import ai.vital.agent.rest.webapp.VitalAgentRestWebappVerticle
import ai.vital.domain.Account
import ai.vital.service.vertx3.binary.ResponseMessage
import ai.vital.service.vertx3.websocket.VitalServiceAsyncWebsocketClient
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalsigns.model.VitalApp
import com.vitalai.aimp.domain.AIMPMessage
import com.vitalai.aimp.domain.Channel
import com.vitalai.aimp.domain.UserCommandMessage
import io.vertx.core.Future
import io.vertx.lang.groovy.GroovyVerticle
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HaleyClientVerticle extends GroovyVerticle {

	// Note: depends on VitalAgentRestWebappVerticle initializing first for config
	
	private VitalServiceAsyncWebsocketClient websocketClient
	
	public HaleyAPI haleyAPI
	
	private HaleySession haleySession
	
	private final static Logger log = LoggerFactory.getLogger(HaleyClientVerticle.class)
	
	private Future<Void> startFuture
	
	private Account account
	
	static HaleyClientVerticle singleton
	
	private long lastMessageReceivedTimestamp
	
	// private Channel mailingChannel
	
	public Channel loginChannel = null
	
	HaleyClientVerticle() {
		
		singleton = this
		
		lastMessageReceivedTimestamp = System.currentTimeMillis()
	}
	
	public static HaleyClientVerticle get() {
		if(singleton == null) throw new Exception("HaleyClientVerticle not available");
		return singleton
	}
	
	@Override
	public void start(Future<Void> startFuture) throws Exception {

		this.startFuture = startFuture
		
		if(VitalAgentRestWebappVerticle.getAppConfig().disableSaasClient == true) {
			log.warn("saas client disabled- vitalservice not used");
			startFuture.complete()
			return
		}
		
		connect()
	}
	
	public void connect() {
		
		String endpointURL = VitalAgentRestWebappVerticle.getAppConfig().saasEventbusURL + '/websocket'
		
		VitalApp app = VitalApp.withId( VitalAgentRestWebappVerticle.getAppConfig().appID )
		
		websocketClient = new VitalServiceAsyncWebsocketClient(vertx, app, 'endpoint.', endpointURL, 100, 5000)
		
		log.info("connecting to endpoint... " + endpointURL)
		
		websocketClient.connect({ Throwable exception ->
			
			if(exception) {
				log.error(exception.getLocalizedMessage(), exception)
				startFuture.fail(exception)
				return
			}
			
			haleyAPI = new HaleyAPI(websocketClient)
			
			haleyAPI.addReconnectListener({HaleySession session ->
			
				log.info("websocket client reconnected")
			})
			
			log.info("Sessions: " + haleyAPI.getSessions().size())
			
			haleyAPI.openSession() { String errorMessage,  HaleySession session ->
				
				haleySession = session
				
				if(errorMessage) {
					log.error(errorMessage)
					startFuture.fail(errorMessage)
					return
				}
				
				log.info("Session opened ${session.toString()}")
				
				log.info("Sessions: " + haleyAPI.getSessions().size())
				
				onSessionReady()
				
			}
			
		}, { Integer attempts ->
		
			try {
				
				websocketClient.closeWebsocket() 
				
				websocketClient.close { ResponseMessage closeRes ->
				
				}
				
			} catch(Exception e) {}
			
			log.error("FAILED, attempts: ${attempts}")
			
			startFuture.fail("FAILED, attempts: ${attempts}")
		})
		
	}
	
	public void onSessionReady() {
		
		String username = VitalAgentRestWebappVerticle.getAppConfig().saasUsername
		String password = VitalAgentRestWebappVerticle.getAppConfig().saasPassword
		
		haleyAPI.authenticateSession(haleySession, username, password) { HaleyStatus status ->
			
			log.info("auth status: ${status}")

			if(!status.ok) {
				startFuture.fail(status.errorMessage)
				return
			}
			
			log.info("session: ${haleySession.toString()}")
			
			haleyAPI.listChannels(haleySession) { String error, List<Channel> channels ->
	
				if(error) {
					startFuture.fail("Error when listing channels")
					return
				}			
				
				log.info("channels count: ${channels.size()}")
				
				for(Channel ch in channels) {
					
					if(ch.name.toString() == username) {
						
						loginChannel = ch
						
					}
					
					
				}
				
				onChannelObtained()
			}
		}
	}
	
	void onChannelObtained() {
		
		HaleyStatus rs = haleyAPI.registerCallback(AIMPMessage.class, true, { ResultList msg ->
			
			// HaleyTextMessage m = msg.first()
			
			AIMPMessage m = msg.first()
			
			log.info("msg received: ${m.getClass().getCanonicalName()}")
			
			lastMessageReceivedTimestamp = System.currentTimeMillis()
			
		})
		
		log.info("Register callback status: ${rs}")
		
		if(!rs.isOk()) {
			startFuture.fail(rs.getErrorMessage())
			return
		}
		
		log.info("Register callback status: ${rs}")
		
		//randomized case, channel not checked
		onChannelReady()
	}
	
	void onChannelReady() {
		
		startFuture.complete()
	}

	@Override
	public void stop(Future<Void> stopFuture) throws Exception {

		if(websocketClient != null) {
			try {
				websocketClient.closeWebsocket()
			} catch(Exception e) {
				log.error(e.localizedMessage, e)
			}
			websocketClient = null
		}
		
		stopFuture.complete()
	}
	
	/**
	 * closure called with ok,msg pair
	 * @param closure
	 */
	public static void checkConnectivity(Closure closure) {
		
		if(singleton == null) {
			closure(false, "Singleton not available")
			return
		}
		
		singleton._checkConnectivity(closure)
		
	}
	
	public void _checkConnectivity(Closure closure) {
		
		// check timestamp of last received message over within last 5 minutes

		long diff = Math.round( ( System.currentTimeMillis() - lastMessageReceivedTimestamp) / 1000d );
				
		if(diff <= 30 ) {
			closure(true, "last haley message received " + diff + " second" + ( diff != 1 ? 's' : '') + " ago");
			return;
		}
		
		Long timeout = null;
		
		timeout = vertx.setTimer(5000) { Long _timerID ->
			timeout = null
			closure(false, 'session check request timed out (5000ms)');
		}
		
		UserCommandMessage msg = new UserCommandMessage().generateURI(VitalAgentRestWebappVerticle.app)
		msg.command = 'check-session'
	
		try {
			
		
			haleyAPI.sendMessageWithRequestCallback(haleySession, msg, [], { ResultList msgRL ->
			
			log.info("session check response", msgRL);
			
			//VirtualLoginResponseMessage
			if(timeout != null) {
				vertx.cancelTimer(timeout)
				timeout = null;
			} else {
				log.warn("Already timed out");
				return false;
			}
			
			lastMessageReceivedTimestamp = new Date().getTime();
			
			closure(true, "session check succeded");
			
			//no more messages expected
			return false;
			
		}) { HaleyStatus sendStatus ->
	
			if(!sendStatus.isOk()) {
				
				
				String error = "Error when sending check session request message: " + sendStatus.getErrorMessage()
				log.error(error)
				
				if(timeout != null) {
					
					vertx.cancelTimer(timeout)
					timeout = null
					closure(false, error)
					
				} else {
					//already timed out
				}
				
				
			} else {
				log.info("session check request sent successfully.");
			}
	
		}
	
		} catch(Exception e) {
		
			log.error(e.localizedMessage, e)
			
			if(timeout != null) {
				vertx.cancelTimer(timeout)
				timeout = null;
			}
		
			closure(false, e.localizedMessage)
			
		}
		
	}

	/*
	static void checkDomains(Closure callback) {
		
		if(singleton == null) {
			callback("Singleton not available")
			return
		}
		
		singleton._checkDomains(callback)
	}
	*/
	
	
	/*
	void _checkDomains(Closure callback) {
		
		boolean failIfListElementsDifferent = false
		
		HaleyAPIDomainsValidation.validateDomains(haleyAPI, failIfListElementsDifferent) { String err ->
			
			log.info("Domain validation status: ${err ? err : 'ok'}")
			
			callback(err)
			
		}
		
	}
	*/
	
	
	/*
	public void sendMailingListRequest(String action, String email, Closure closure) {
		
		//use simple command
		IntentMessage intentMsg = new IntentMessage().generateURI(HaleyGenericWebappVerticle.app)
		intentMsg.intent = 'mailing_' + action
		intentMsg.propertyValue = email
		intentMsg.channelURI = mailingChannel.URI
		
		Long timerID = null
		
		timerID = vertx.setTimer(10000) { Long _timerID ->
			
			if(timerID != null) {
				closure('mailing request timed out (10,000ms)')
			} else {
				//ok
			}
			
		}
		
		haleyAPI.sendMessageWithRequestCallback(haleySession, intentMsg, [], { ResultList messageRL ->
		
			println "mailing operation results received"
			
			AIMPMessage msg = messageRL.first()
			
			if(!(msg instanceof EntityMessage)) {
				log.warn("Not entity message, still waiting: " + msg.getClass().canonicalName + ' ' + msg.text)
				return true
			}
			
			EntityMessage entityMsg = msg
			
			String status = entityMsg.text
			
			log.info("mailing action ${action} status: " + status)
			
			
			if('ok'.equalsIgnoreCase(status)) {
				closure(null)
			} else {
				closure(status)
			}
			
			if(timerID != null) {
				vertx.cancelTimer(timerID)
			}
			
			//remove listener
			return false
				
		}) { HaleyStatus sendStatus ->
			
			log.info("send mailing request status: ${sendStatus}")
			
			if(!sendStatus.isOk()) {
			
				if(timerID != null) {
					vertx.cancelTimer(timerID)
				}
				
				closure('internal error when sending mailing list request')
				
			}
			
		}
	}
	
	*/
		
}
