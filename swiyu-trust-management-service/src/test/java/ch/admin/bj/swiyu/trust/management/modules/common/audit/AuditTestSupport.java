package ch.admin.bj.swiyu.trust.management.modules.common.audit;

import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditEventDataKey.BUSINESS_PARTNER_ID;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bit.jeap.audit.record.create.AuditObjectDataJSON;
import ch.admin.bit.jeap.audit.record.create.AuditObjectDataValue;
import ch.admin.bit.jeap.audit.record.create.CreateAuditRecordCommand;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessage;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessageRepository;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public final class AuditTestSupport {

    private AuditTestSupport() {}

    public static void clearDeferredMessages(DeferredMessageRepository repository) {
        var messages = repository.findAll();
        repository.deleteAllById(messages.stream().map(DeferredMessage::getId).collect(Collectors.toSet()));
    }

    public static void assertAuditedObject(
        CreateAuditRecordCommand message,
        String objectId,
        String objectType,
        String version
    ) {
        assertThat(message.getPayload().getAuditedData().getId()).isEqualTo(objectId);
        assertThat(message.getPayload().getAuditedData().getType()).isEqualTo(objectType);
        assertThat(message.getPayload().getAuditedData().getVersion()).isEqualTo(version);
    }

    public static void assertBusinessPartnerId(CreateAuditRecordCommand message, String partnerId) {
        assertThat(getEventDataByKey(message, BUSINESS_PARTNER_ID.getKey())).isEqualTo(partnerId);
    }

    public static void assertJsonObjectData(CreateAuditRecordCommand message, String name, String expectedJson) {
        var objectData = getAuditObjectDataJSON(message, name);
        var actualJson = StandardCharsets.UTF_8.decode(objectData.getJsonAsUTF8()).toString();
        assertThat(actualJson).isEqualTo(expectedJson);
    }

    public static void assertValueObjectData(CreateAuditRecordCommand message, String name, String expectedValue) {
        assertThat(getAuditObjectDataValue(message, name).getValue()).isEqualTo(expectedValue);
    }

    private static String getEventDataByKey(CreateAuditRecordCommand message, String key) {
        return message
            .getPayload()
            .getEvent()
            .getEventData()
            .stream()
            .filter(d -> d.getKey().equals(key))
            .findFirst()
            .orElseThrow()
            .getValue();
    }

    private static AuditObjectDataJSON getAuditObjectDataJSON(CreateAuditRecordCommand message, String name) {
        return message
            .getPayload()
            .getAuditedData()
            .getObjectData()
            .stream()
            .filter(AuditObjectDataJSON.class::isInstance)
            .map(AuditObjectDataJSON.class::cast)
            .filter(o -> o.getName().equals(name))
            .findFirst()
            .orElseThrow();
    }

    private static AuditObjectDataValue getAuditObjectDataValue(CreateAuditRecordCommand message, String name) {
        return message
            .getPayload()
            .getAuditedData()
            .getObjectData()
            .stream()
            .filter(AuditObjectDataValue.class::isInstance)
            .map(AuditObjectDataValue.class::cast)
            .filter(o -> o.getName().equals(name))
            .findFirst()
            .orElseThrow();
    }
}
