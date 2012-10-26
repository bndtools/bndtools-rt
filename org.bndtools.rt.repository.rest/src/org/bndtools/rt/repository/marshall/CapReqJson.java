package org.bndtools.rt.repository.marshall;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.resource.Requirement;

import aQute.lib.io.IO;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class CapReqJson {
	
	public static void writeRequirementArray(Collection<? extends Requirement> requirements, JsonGenerator generator) throws IOException {
		generator.writeStartArray();
		for (Requirement req : requirements) {
			CapReqJson.writeRequirement(req, generator);
		}
		generator.writeEndArray();
	}
	
	public static void writeRequirement(Requirement requirement, JsonGenerator generator) throws IOException {
		generator.writeStartObject();
		generator.writeStringField("ns", requirement.getNamespace());
		
		Map<String, Object> attrs = requirement.getAttributes();
		if (!attrs.isEmpty()) {
			generator.writeArrayFieldStart("attrs");
			for (Entry<String, Object> entry : attrs.entrySet()) {
				writeAttribute(entry.getKey(), entry.getValue(), generator);
			}
			generator.writeEndArray();
		}
		
		Map<String, String> dirs = requirement.getDirectives();
		if (!dirs.isEmpty()) {
			generator.writeArrayFieldStart("dirs");
			for (Entry<String, String> entry : dirs.entrySet()) {
				writeDirective(entry.getKey(), entry.getValue(), generator);
			}
			generator.writeEndArray();
		}
		
		generator.writeEndObject();
	}
	
	private static void writeAttribute(String key, Object value, JsonGenerator generator) throws IOException {
		throw new UnsupportedOperationException("TODO");
	}
	
	private static void writeDirective(String key, String value, JsonGenerator generator) throws IOException {
		generator.writeStartObject();
		generator.writeStringField("name", key);
		generator.writeStringField("value", value);
		generator.writeEndObject();
	}
	

	public static List<Requirement> parseRequirements(JsonFactory jf, InputStream stream) throws IOException {
		List<Requirement> result;
		
		String collect = IO.collect(stream);
		JsonNode rootNode = new ObjectMapper(jf).readTree(collect);
		if (rootNode.isArray()) {
			result = new LinkedList<Requirement>();
			for (JsonNode reqNode : rootNode) {
				result.add(parseCapReq(reqNode).buildSyntheticRequirement());
			}
		} else if (rootNode.isObject()) {
			result = Collections.singletonList(parseCapReq(rootNode).buildRequirement());
		} else {
			throw new IllegalAccessError("JSON data is neither an Array nor an Object");
		}
		
		return result;
	}

	private static CapReqBuilder parseCapReq(JsonNode reqNode) throws IllegalArgumentException {
		String namespace = getRequiredField(reqNode, "ns").asText();
		CapReqBuilder builder = new CapReqBuilder(namespace);
		
		JsonNode dirsNode = reqNode.get("dirs");
		if (dirsNode != null) {
			if (dirsNode.isArray()) {
				for (JsonNode dirNode : dirsNode) {
					toDirective(builder, dirNode);
				}
			} else if (dirsNode.isObject()) {
				toDirective(builder, dirsNode);
			} else {
				throw new IllegalAccessError("Value of 'dirs' node is neither an Array or an Object.");
			}
		}
		
		JsonNode attrsNode = reqNode.get("attrs");
		if (attrsNode != null) {
			if (attrsNode.isArray()) {
				for (JsonNode attrNode : attrsNode) {
					toAttribute(builder, attrNode);
				}
			} else if (attrsNode.isObject()) {
				toAttribute(builder, attrsNode);
			} else {
				throw new IllegalAccessError("Value of 'attrs' node is neither an Array or an Object.");
			}
		}
		
		return builder;
	}

	private static void toDirective(CapReqBuilder builder, JsonNode dirNode) {
		String name = getRequiredValueField(dirNode, "name").asText();
		String value = getRequiredValueField(dirNode, "value").asText();
		
		builder.addDirective(name, value);
	}

	private static void toAttribute(CapReqBuilder builder, JsonNode attrNode) throws IllegalArgumentException {
		String name = getRequiredValueField(attrNode, "name").asText();
		Object value;
		JsonNode valueNode = getRequiredValueField(attrNode, "value");
		
		JsonNode typeNode = attrNode.get("type");
		AttributeType type = AttributeType.DEFAULT;
		if (typeNode != null) {
			String typeName = typeNode.asText();
			type = AttributeType.parseTypeName(typeName);
		}

		if (valueNode.isFloatingPointNumber()) {
			if (typeNode == null || type.equals(AttributeType.DOUBLE))
				value = valueNode.asDouble();
			else
				throw new IllegalArgumentException(String.format("JSON type for value is floating point, does not match declared type '%s'", type));
		} else if (valueNode.isIntegralNumber()) {
			if (typeNode == null || type.equals(AttributeType.LONG))
				value = valueNode.asLong();
			else if (type.equals(AttributeType.DOUBLE))
				value = valueNode.asDouble();
			else
				throw new IllegalArgumentException(String.format("JSON type for value is integral, does not match declared type '%s'", type));
		} else if (valueNode.isTextual()) {
			String valueStr = valueNode.asText();
			value = type.parseString(valueStr);
		} else {
			throw new IllegalArgumentException("JSON value node is not a recognised type");
		}

		builder.addAttribute(name, value);
	}

	private static JsonNode getRequiredField(JsonNode node, String fieldName) throws IllegalArgumentException {
		JsonNode child = node.get(fieldName);
		if (child == null)
			throw new IllegalArgumentException(String.format("Missing required field '%s'", fieldName));
		return child;
	}
	
	private static JsonNode getRequiredValueField(JsonNode node, String fieldName) {
		JsonNode valueNode = getRequiredField(node, fieldName);
		if (!valueNode.isValueNode())
			throw new IllegalArgumentException(String.format("Field '%s' is required to be a value field.", fieldName));
		return valueNode;
	}
}
