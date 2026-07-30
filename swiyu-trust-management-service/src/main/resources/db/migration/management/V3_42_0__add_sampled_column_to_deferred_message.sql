-- jeap-messaging-outbox 17.1.0 added a "sampled" field to OutboxTraceContext (see V2_0_6 for the original trace columns)
ALTER TABLE deferred_message
    ADD COLUMN sampled BOOLEAN;
