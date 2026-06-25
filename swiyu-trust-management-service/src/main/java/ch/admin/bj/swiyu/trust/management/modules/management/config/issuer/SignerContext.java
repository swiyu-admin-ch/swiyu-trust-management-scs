package ch.admin.bj.swiyu.trust.management.modules.management.config.issuer;

import com.nimbusds.jose.JWSSigner;

/**
 * Provides context information for a given signer.
 *
 * @param did The DID which identifies this signers public identity
 * @param kid The KeyId which identifies this signers public key
 */
public record SignerContext(String did, String kid, JWSSigner signer) {}
