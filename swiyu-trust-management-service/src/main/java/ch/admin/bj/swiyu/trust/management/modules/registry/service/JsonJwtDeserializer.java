package ch.admin.bj.swiyu.trust.management.modules.registry.service;

import com.authlete.sd.Disclosure;
import com.authlete.sd.SDJWT;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
@RequiredArgsConstructor
public class JsonJwtDeserializer {

    private final ObjectMapper objectMapper;

    /**
     * Extracts the header and payload of an JWT as JSON Node
     *
     * @param serializedJwt the serialized JWT with header and payload
     * @return Returns the json node of the jwt
     */
    public JsonNode decodeJwt(String serializedJwt) {
        try {
            var splitEncodedJwt = serializedJwt.split("\\.");
            var decoder = Base64.getUrlDecoder();
            var serializedHeader = new String(decoder.decode(splitEncodedJwt[0]));
            var serializedPayload = new String(decoder.decode(splitEncodedJwt[1]));
            var payload = (ObjectNode) objectMapper.readTree(serializedPayload);
            var header = (ObjectNode) objectMapper.readTree(serializedHeader);
            return payload.setAll(header);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to decode encoded JWT payload", e);
        }
    }

    /**
     * Extracts the resolved payload of an SD-JWT as JSON Node
     *
     * @param serializedSdJwt the serialized SD-JWT with all disclosures etc.
     * @return Returns the json node of the sd jwt payload
     */
    public JsonNode decodeSdjwtPayload(String serializedSdJwt) {
        // Create JSON Mapper for further JSON manipulation
        ObjectMapper mapper = JsonMapper.builder()
            .enable(tools.jackson.core.json.JsonWriteFeature.ESCAPE_NON_ASCII)
            .build();

        // Parse into SD JWT library
        var token = SDJWT.parse(serializedSdJwt);

        // extract original payload of SD-JWT
        try {
            var rawPayload = new String(Base64.getDecoder().decode(token.getCredentialJwt().split("\\.")[1]));
            ObjectNode payloadJson = (ObjectNode) mapper.readTree(rawPayload);

            // expand original payload with disclosed values
            for (Disclosure d : token.getDisclosures()) {
                payloadJson.set(d.getClaimName(), mapper.readTree(d.getJson()).get(2));
            }

            return payloadJson;
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to decode encoded SD-JWT payload", e);
        }
    }
}
