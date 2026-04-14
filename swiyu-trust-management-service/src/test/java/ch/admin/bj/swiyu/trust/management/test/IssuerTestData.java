package ch.admin.bj.swiyu.trust.management.test;

import ch.admin.bj.swiyu.trust.client.issuer.management.model.CredentialStatusTypeDto;
import ch.admin.bj.swiyu.trust.client.issuer.management.model.CredentialWithDeeplinkResponseDto;
import ch.admin.bj.swiyu.trust.client.issuer.management.model.StatusResponseDto;
import ch.admin.bj.swiyu.trust.client.issuer.management.model.UpdateStatusResponseDto;
import ch.admin.bj.swiyu.trust.client.issuer.oid4vci.model.OAuthTokenDto;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IssuerTestData {

    public static CredentialWithDeeplinkResponseDto credentialWithDeeplinkResponse() {
        var response = new CredentialWithDeeplinkResponseDto();
        response.setManagementId(UUID.fromString("6957baba-0525-4de8-bdeb-d71f674ba581"));
        response.setOfferDeeplink(
            "openid-credential-offer://?credential_offer=%7B%22grants%22%3A%7B%22urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Apre-authorized_code%22%3A%7B%22pre-authorized_code%22%3A%22i57baba-0525-4de8-bdeb-d71f674ba581%22%7D%7D%2C%22version%22%3A%221.0%22%2C%22credential_issuer%22%3A%22https%3A%2F%2Fissuer-agent-oid4vci-d.bit.admin.ch%22%2C%22credential_configuration_ids%22%3A%5B%22myIssuerMetadataCredentialSupportedId%22%5D%7D"
        );
        return response;
    }

    public static OAuthTokenDto oAuthToken() {
        var token = new OAuthTokenDto();
        token.setTokenType("Bearer");
        token.setAccessToken(
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ"
        );
        return token;
    }

    public static String credentialResponseAsString() {
        return """
        {
            "credential": "%s",
            "format": "application/json"
        }
        """.formatted(sdjwt());
    }

    public static String sdjwt() {
        return "eyJraWQiOiJkaWQ6ZXhhbXBsZTpsb2NhbGhvc3QlM0E4MDgwOmFiY2FiYyNzZGp3dCIsInR5cCI6InZjK3NkLWp3dCIsImFsZyI6IkVTMjU2In0.eyJfc2QiOlsiRDFLZGtGZm5yVVpFUTNVT1BYby1JbUhzdVYzRmcxTWJoQ2N1UXFhN3NXayIsIlBsNHlKSmYwcWdTZEJIbm1mUHdVU2llT3B4UVU0ay1zY2VNYVF3blAyV2MiLCJ5alRaVXA5azU0WTdlWkJTYTJ0OXIyaDVwNFZ2dWJZaTk2MU55NG5SS1M0Il0sInZjdCI6InVuaXZlcnNpdHlfZXhhbXBsZV9zZF9qd3QiLCJfc2RfYWxnIjoic2hhLTI1NiIsImlzcyI6ImRpZDpleGFtcGxlOmxvY2FsaG9zdCUzQTgwODA6YWJjYWJjIiwiY25mIjp7Imt0eSI6IkVDIiwidXNlIjoic2lnIiwiY3J2IjoiUC0yNTYiLCJraWQiOiJUZXN0LUtleSIsIngiOiJNMlVoc2RUY0pFOGVxOFlLVDVLbDJtX3F0MWZ5alAwOTF1c2hZdm80eGM0IiwieSI6InJQaEJRdmtnMFFYSHg1YVkxV1N5ZnpCa2dSMmJqcTVUazVJakhxZGtDOFUiLCJpYXQiOjE3MzY0NTA0NDd9LCJpYXQiOjE3MzY0NTA0NDgsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjAsInVyaSI6Imh0dHBzOi8vbG9jYWxob3N0OjgwODAvc3RhdHVzIn19fQ.Lhp1NUmPXhXKg6oZoIM8tmvXRCSJ_Zl9N3bSmV4Yq84WXz3-aB5vrDsfz120y1zLJ0CGK9pniIb1nFU366fNXQ~WyJzUWswNUs0OTR3bHZFc0VYQi1WZll3IiwiYXZlcmFnZV9ncmFkZSIsIjUuMzMiXQ~WyIyNEZadUtRYnlXX2dhcFNwT1hNUUNBIiwiZGVncmVlIiwiQmFjaGVsb3Igb2YgU2NpZW5jZSJd~WyJjUTRGSlNMNl8tNGdFQkFpdEdXa2lnIiwibmFtZSIsIkRhdGEgU2NpZW5jZSJd~";
    }

    public static ECKey jwtSigningKey() {
        try {
            return new ECKeyGenerator(Curve.P_256).keyID("testkey").generate();
        } catch (JOSEException e) {
            throw new AssertionError("failed to generate signing key", e);
        }
    }

    public static StatusResponseDto statusResponse_Issued() {
        var statusResponse = new StatusResponseDto();
        statusResponse.setStatus(CredentialStatusTypeDto.ISSUED);
        return statusResponse;
    }

    public static UpdateStatusResponseDto updateStatusResponse(UUID id, CredentialStatusTypeDto type) {
        var ret = new UpdateStatusResponseDto();
        ret.setStatus(type);
        ret.setId(id);
        return ret;
    }
}
