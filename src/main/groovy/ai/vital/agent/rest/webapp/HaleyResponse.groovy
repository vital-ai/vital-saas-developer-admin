package ai.vital.agent.rest.webapp

import ai.vital.vitalsigns.model.GraphObject
import com.vitalai.aimp.domain.AIMPMessage

class HaleyResponse {
	
	// 0 is Ok
	Integer errorCode = 0
	
	String statusMessage
	
	AIMPMessage responseMessage
	
	List<GraphObject> payloadList = []
	
}
