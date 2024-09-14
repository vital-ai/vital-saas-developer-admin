package ai.vital.agent.rest.webapp

import ai.haley.api.HaleyAPI
import ai.haley.api.session.HaleySession
import ai.haley.api.session.HaleyStatus
import ai.vital.agent.rest.webapp.client.HaleyClientVerticle
import ai.vital.query.querybuilder.VitalBuilder
import ai.vital.service.vertx3.binary.ResponseMessage
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.query.ResultElement
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalsigns.block.CompactStringSerializer
import ai.vital.vitalsigns.model.GraphMatch
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VitalSegment
import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.vitalsigns.model.property.StringProperty
import com.vitalai.aimp.domain.AIMPMessage
import com.vitalai.aimp.domain.Channel
import io.vertx.core.Vertx
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Map.Entry
import java.util.concurrent.CountDownLatch

class VertxUtils {
	
	private final static Logger log = LoggerFactory.getLogger(VertxUtils.class)
	
	public static String error_vital_service = 'error_vital_service'
	
	public static String error_not_logged_in = 'error_not_logged_in'
	
	public static String error_invalid_login_type = 'error_invalid_login_type'
	
	public final static String error_no_app_param = 'error_no_app_param'
	
	public final static String error_missing_param_root_key = 'error_missing_param_root_key'
	
	public final static String error_missing_param_service = 'error_missing_param_service'
	
	public final static String error_invalid_account_type = 'error_invalid_account_type'
	
	public final static String error_session_account_not_found = 'error_session_account_not_found'
	
	protected static VitalBuilder builder = new VitalBuilder()

	static public HaleyResponse sendCommandMessage(Vertx vertxInstance, AIMPMessage msg, List<GraphObject> payloadList, Long commandTimeout) {
		
		HaleyClientVerticle clientVerticle = HaleyClientVerticle.get()
		
		HaleyAPI haleyAPI = clientVerticle.haleyAPI
		
		HaleySession haleySession = clientVerticle.haleySession
		
		Channel loginChannel = clientVerticle.loginChannel
		
		msg.channelURI = loginChannel.URI
		
		HaleyResponse haleyResponse = new HaleyResponse()
		
		Long timeout
		
		timeout = vertxInstance.setTimer(5000) { Long _timerID ->
			
			timeout = null
			
			haleyResponse.errorCode = -1
			
			haleyResponse.statusMessage = "Timed out."
		}
		
		CountDownLatch sendLatch = new CountDownLatch(1)
		
		try {
			
			log.info("Sending Message: " + msg.toJSON())
			
			haleyAPI.sendMessageWithRequestCallback(haleySession, msg, payloadList, { ResultList msgRL ->
		
				// check response
				
				AIMPMessage responseMessage = null
				
				List<GraphObject> responsePayloadList = []
				
				if( msgRL.results.size() == 0 ) {
					
					// no response, error
					
					haleyResponse.errorCode = -1
					
					haleyResponse.statusMessage = "No response to command"
				}
				
				if( msgRL.results.size() == 1 ) {
				
					responseMessage = msgRL.results[0].graphObject
				}
				
				if( msgRL.results.size() > 1 ) {
					
					responseMessage = msgRL.results[0]
				
					for(int i = 1; i++; i < msgRL.results.size() ) {
						
						ResultElement re = msgRL.results[i]
						
						GraphObject g = re.graphObject
						
						responsePayloadList.add(g)
					}
				}
				
				
				log.info("Response Message: " + responseMessage?.toJSON())
				
				long lastMessageReceivedTimestamp = new Date().getTime()
				
				haleyResponse.responseMessage = responseMessage
				
				haleyResponse.payloadList = responsePayloadList
				
				sendLatch.countDown()
					
				if(timeout != null) {
					
					vertxInstance.cancelTimer(timeout)
					
					timeout = null
					
				} else {
					
					log.warn("Already timed out: " + msg.toJSON() )
						
					return false
				}
								
				return false
						
			}, { HaleyStatus sendStatus ->
					
				if(!sendStatus.isOk()) {
							
					String error = "Error when sending command: " + sendStatus.getErrorMessage()
							
					log.error(error)
							
					if(timeout != null) {
								
						vertxInstance.cancelTimer(timeout)
								
						timeout = null
											
					} else {
						
						// already timed out
									
						log.warn("Already timed out: " + msg.toJSON() )
					}
					
					haleyResponse.errorCode = -1
							
					haleyResponse.statusMessage = "error sending command"
								
					// there was a send error
					sendLatch.countDown()
										
				} else {
					log.info("command sent successfully.");
				}
					
			})
			
		} catch(Exception ex) {
			
			log.error(ex.localizedMessage, ex)
			
			if(timeout != null) {
				
				vertxInstance.cancelTimer(timeout)
				
				timeout = null
			}
			
			// there was an exception
			sendLatch.countDown()
		}
		
		try {
			
			sendLatch.await()
			
		} catch (InterruptedException e) {
			
			e.printStackTrace()
		
			haleyResponse.errorCode = -1
				
			haleyResponse.statusMessage = "command interrupted exception."
		}
		
		return haleyResponse
	}
	
	public static ResultList unpackGraphMatch(ResultList rl) {
		
		ResultList r = new ResultList();
				
		for(GraphObject g : rl) {
					
			if(g instanceof GraphMatch) {
						
				for(Entry<String, IProperty> p : g.getPropertiesMap().entrySet()) {
									
					IProperty unwrapped = p.getValue().unwrapped();
					if(unwrapped instanceof StringProperty) {
						GraphObject x = CompactStringSerializer.fromString((String) unwrapped.rawValue());
						if(x != null) r.getResults().add(new ResultElement(x, 1D));
					}
							
				}
					
			} else {
				throw new RuntimeException("Expected graph match objects only");
			}
					
		}
			
		return r;
	}
	
	public static List<VitalSegment> segmentsList(List<VitalSegment> segments) {
		
		Set<String> ids = new HashSet<String>()
		
		List<VitalSegment> out = []
		
		for(VitalSegment s : segments) {
			
			if(ids.add(s.segmentID.toString())) {
				out.add(s)
			}
			
		}
		
		return out	
	}
	
	public static boolean handleErrorResultList(Closure closure, ResponseMessage rm) {
		
		ResultList rl = new ResultList()
		
		if(rm.exceptionMessage) {
			rl.status = VitalStatus.withError(error_vital_service + " ${rm.exceptionType} - ${rm.exceptionMessage}")
			closure(rl)
			return true
		}
		
		rl = (ResultList) rm.response
		
		if(rl.status.status != VitalStatus.Status.ok) {
			
			closure(rl)
			
			return true
		}
		
		return false
		
	}
	
	
	public static String checkErrorResultListResponse(ResponseMessage rm) {
		
		if(rm.exceptionType) {
			return "${rm.exceptionType} - ${rm.exceptionMessage}"
		}
		
		ResultList rl = rm.response
		
		if(rl.status.status != VitalStatus.Status.ok) {
			String m = rl.status.message
			if(m == null || m.isEmpty()) m = '(unknown error)'
			return m
		}
		
		return null
		
	}
	
	public static String checkErrorStatusResponse(ResponseMessage rm) {
		
		if(rm.exceptionType) {
			return "${rm.exceptionType} - ${rm.exceptionMessage}"
		}
		
		VitalStatus status = rm.response
				
		if(status.status != VitalStatus.Status.ok) {
			String m = status.message
			if(m == null || m.isEmpty()) m = '(unknown error)'
			return m
		}
		
		return null
				
	}
	
	public static boolean handleErrorStatusResponse(Closure closure, ResponseMessage rm) {
		
		ResultList rl = new ResultList()
				
		if(rm.exceptionMessage) {
			rl.status = VitalStatus.withError(error_vital_service + " ${rm.exceptionType} - ${rm.exceptionMessage}")
			closure(rl)
			return true
		}
		
		VitalStatus status = rm.response
		
		if(status.status != VitalStatus.Status.ok) {
			rl.status = status
			closure(rl)
			return true
		}
		
		return false			
	}

	public static errorResultList(Closure closure, String error) {
		ResultList rl = new ResultList()
		rl.status = VitalStatus.withError(error)
		closure(rl)
	}
	
	public static String maskPassword(String n) {
		
		if(n == null) return 'null'
		
		String output = "";

		for( int i = 0 ; i < n.length(); i++) {
				
			if(n.length() < 12 || ( i >= 4 && i < (n.length() - 4) ) ) {
				output += '*'
			} else {
				output += n.substring(i, i+1)
			}
				
		}
		
		return output
		
	}
}
