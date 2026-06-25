package ch.admin.bj.swiyu.trust.management.modules.common.did;

import ch.admin.eid.didresolver.DidKt;
import ch.admin.eid.didresolver.DidResolveException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DidUtil {

    public static String getDidFromKeyId(String keyId) {
        try {
            return DidKt.getDidFromAbsoluteKid(keyId).asString();
        } catch (DidResolveException e) {
            throw new IllegalArgumentException("Cannot extract DID from kid: " + keyId, e);
        }
    }
}
