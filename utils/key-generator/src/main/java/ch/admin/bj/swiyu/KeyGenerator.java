package ch.admin.bj.swiyu;

import static com.nimbusds.jose.jwk.JWK.parseFromPEMEncodedObjects;

import com.nimbusds.jose.jwk.ECKey;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Date;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

/**
 * Generates a new EC key pair and saves the private key to a PEM file and the public key as JWK to a JSON file.
 */
public class KeyGenerator {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {
        var keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
        keyPairGenerator.initialize(256);
        var keyPair = keyPairGenerator.generateKeyPair();

        var pem = toPem(keyPair.getPrivate());
        var jwk = new ECKey.Builder(parseFromPEMEncodedObjects(pem).toECKey())
            .keyID("trust-management-" + new Date().getTime())
            .build();

        // Create tmp directory if it doesn't exist
        Files.createDirectories(Paths.get(".keys"));

        // Save the private key to a PEM file
        try (var pemWriter = new FileWriter(".keys/JWT_SIGNING_KEY.pem")) {
            pemWriter.write(pem);
        }

        // Save the public key to a JSON file
        String publicKeyJson;
        try (var jsonWriter = new FileWriter(".keys/JWT_PUBLIC_KEY.json")) {
            publicKeyJson = jwk.toPublicJWK().toJSONString();
            jsonWriter.write(publicKeyJson);
        }
        // Create the JWKS_ALLOWLIST.json file with the template
        var allowList = "{\"keys\":[%s]}".formatted(publicKeyJson);
        try (var jwksWriter = new FileWriter(".keys/JWKS_ALLOWLIST.json")) {
            jwksWriter.write(allowList);
        }
    }

    public static String toPem(PrivateKey key) throws IOException {
        var stringWriter = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
            pemWriter.writeObject(key);
        }
        return stringWriter.toString();
    }
}
