// package com.hotelbooking.chassis.logging.kafka;

// import org.apache.kafka.clients.producer.ProducerInterceptor;
// import org.apache.kafka.clients.producer.ProducerRecord;
// import org.apache.kafka.clients.producer.RecordMetadata;
// import org.slf4j.MDC;

// import java.nio.charset.StandardCharsets;
// import java.util.Map;

// public class MdcKafkaProducerInterceptor<K, V> implements ProducerInterceptor<K, V> {

//     @Override
//     public ProducerRecord<K, V> onSend(ProducerRecord<K, V> record) {
//         addHeaderIfPresent(record, KafkaLoggingUtils.HEADER_TRACE_ID,  MDC.get("traceId"));
//         addHeaderIfPresent(record, KafkaLoggingUtils.HEADER_TENANT_ID, MDC.get("tenantId"));
//         addHeaderIfPresent(record, KafkaLoggingUtils.HEADER_USER_ID,   MDC.get("userId"));
//         return record;
//     }

//     private void addHeaderIfPresent(ProducerRecord<K, V> record,
//                                     String headerName, String value) {
//         if (value != null && !value.isBlank()) {
//             record.headers().add(headerName, value.getBytes(StandardCharsets.UTF_8));
//         }
//     }

//     @Override
//     public void onAcknowledgement(RecordMetadata metadata, Exception exception) {}

//     @Override
//     public void close() {}

//     @Override
//     public void configure(Map<String, ?> configs) {}
// }
