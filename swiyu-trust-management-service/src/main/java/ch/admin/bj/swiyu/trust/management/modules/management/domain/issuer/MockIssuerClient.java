package ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer;

import ch.admin.bj.swiyu.trust.client.issuer.management.model.CreateCredentialRequestDto;
import ch.admin.bj.swiyu.trust.client.issuer.management.model.CredentialStatusTypeDto;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mock implementation of the IssuerClient for local happy path testing.
 */
@Slf4j
@AllArgsConstructor
public class MockIssuerClient implements IssuerClient {

    private final IssuerProperties issuerProperties;
    private final Map<UUID, CredentialStatusTypeDto> db = inMemoryMap();

    @Override
    public TrustStatementIssuanceResult issueTrustStatement(CreateCredentialRequestDto credentialOfferRequest) {
        log.debug("Mocking issuance of trust statement with credential offer request: {}", credentialOfferRequest);
        var credentialId = UUID.randomUUID();
        db.put(credentialId, CredentialStatusTypeDto.ISSUED);
        return new TrustStatementIssuanceResult(
            credentialId,
            "eyJraWQiOiJkaWQ6ZXhhbXBsZTpsb2NhbGhvc3QlM0E4MDgwOmFiY2FiYyNzZGp3dCIsInR5cCI6InZjK3NkLWp3dCIsImFsZyI6IkVTMjU2In0.eyJfc2QiOlsiRDFLZGtGZm5yVVpFUTNVT1BYby1JbUhzdVYzRmcxTWJoQ2N1UXFhN3NXayIsIlBsNHlKSmYwcWdTZEJIbm1mUHdVU2llT3B4UVU0ay1zY2VNYVF3blAyV2MiLCJ5alRaVXA5azU0WTdlWkJTYTJ0OXIyaDVwNFZ2dWJZaTk2MU55NG5SS1M0Il0sInZjdCI6InVuaXZlcnNpdHlfZXhhbXBsZV9zZF9qd3QiLCJfc2RfYWxnIjoic2hhLTI1NiIsImlzcyI6ImRpZDpleGFtcGxlOmxvY2FsaG9zdCUzQTgwODA6YWJjYWJjIiwiY25mIjp7Imt0eSI6IkVDIiwidXNlIjoic2lnIiwiY3J2IjoiUC0yNTYiLCJraWQiOiJUZXN0LUtleSIsIngiOiJNMlVoc2RUY0pFOGVxOFlLVDVLbDJtX3F0MWZ5alAwOTF1c2hZdm80eGM0IiwieSI6InJQaEJRdmtnMFFYSHg1YVkxV1N5ZnpCa2dSMmJqcTVUazVJakhxZGtDOFUiLCJpYXQiOjE3MzY0NTA0NDd9LCJpYXQiOjE3MzY0NTA0NDgsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjAsInVyaSI6Imh0dHBzOi8vbG9jYWxob3N0OjgwODAvc3RhdHVzIn19fQ.Lhp1NUmPXhXKg6oZoIM8tmvXRCSJ_Zl9N3bSmV4Yq84WXz3-aB5vrDsfz120y1zLJ0CGK9pniIb1nFU366fNXQ~WyJzUWswNUs0OTR3bHZFc0VYQi1WZll3IiwiYXZlcmFnZV9ncmFkZSIsIjUuMzMiXQ~WyIyNEZadUtRYnlXX2dhcFNwT1hNUUNBIiwiZGVncmVlIiwiQmFjaGVsb3Igb2YgU2NpZW5jZSJd~WyJjUTRGSlNMNl8tNGdFQkFpdEdXa2lnIiwibmFtZSIsIkRhdGEgU2NpZW5jZSJd~"
        );
    }

    @Override
    public void updateCredentialStatus(UUID credentialId, CredentialStatusTypeDto credentialStatus) {
        log.debug("Mocking update of credential status for credential with id {}", credentialId);
        if (db.containsKey(credentialId)) {
            db.put(credentialId, credentialStatus);
        } else {
            throw new IllegalArgumentException("Credential with id " + credentialId + " not found");
        }
    }

    @Override
    public CredentialStatusTypeDto getCredentialStatus(UUID trustIssuerManagementId) {
        log.debug("Mocking retrieval of credential status for management ID {}", trustIssuerManagementId);
        if (!db.containsKey(trustIssuerManagementId)) {
            throw new IllegalArgumentException("Credential with id " + trustIssuerManagementId + " not found");
        }
        return db.get(trustIssuerManagementId);
    }

    @Override
    public String getStatusListUri() {
        return issuerProperties.statusListUri();
    }

    private static LinkedHashMap<UUID, CredentialStatusTypeDto> inMemoryMap() {
        return new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, CredentialStatusTypeDto> eldest) {
                return size() > 100; // Limit the size of the map to 100 entries
            }
        };
    }
}
