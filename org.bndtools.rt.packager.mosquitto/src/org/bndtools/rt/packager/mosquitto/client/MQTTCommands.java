package org.bndtools.rt.packager.mosquitto.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;

import org.apache.felix.service.command.Descriptor;
import org.bndtools.service.endpoint.Endpoint;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

@Component(
		provide = Object.class,
		immediate = true,
		properties = {
			"osgi.command.scope=mqtt",
			"osgi.command.function=publish"
		})
public class MQTTCommands {
	
	private final String clientId = Long.toString(System.currentTimeMillis());
	
	private URI boundUri;
	private MqttClient client;

	@Reference(target = "(uri=mqtt:*)")
	void bindEndpoint(Endpoint endpoint, Map<String, String> props) throws URISyntaxException {
		boundUri = new URI(props.get(Endpoint.URI));
	}
	
	@Activate
	void activate() throws Exception {
		try {
			URI tcpUri = new URI("tcp", null, boundUri.getHost(), boundUri.getPort(), null, null, null);
			client = new MqttClient(tcpUri.toString(), clientId);
			client.connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Deactivate
	void deactivate() throws Exception {
		client.disconnect();
	}

	@Descriptor("Publish a message to an MQTT topic")
	public void publish(@Descriptor("The MQTT topic name") String topicName, @Descriptor("The message") String message) throws Exception {
		MqttTopic topic = client.getTopic(topicName);
		topic.publish(message.getBytes(), 0, false);
		System.out.println("Message delivered");
	}
	
}
