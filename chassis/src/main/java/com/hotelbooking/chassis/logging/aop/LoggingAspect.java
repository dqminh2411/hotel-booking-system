// package com.hotelbooking.chassis.logging.aop;

// import com.hotelbooking.chassis.logging.util.SensitiveDataMasker;
// import lombok.extern.slf4j.Slf4j;
// import net.logstash.logback.argument.StructuredArgument;
// import net.logstash.logback.argument.StructuredArguments;
// import org.aspectj.lang.ProceedingJoinPoint;
// import org.aspectj.lang.annotation.Around;
// import org.aspectj.lang.annotation.Aspect;
// import org.aspectj.lang.reflect.MethodSignature;
// import org.slf4j.MDC;

// import java.lang.reflect.Parameter;
// import java.util.ArrayList;
// import java.util.List;

// import io.opentelemetry.api.GlobalOpenTelemetry;
// import io.opentelemetry.api.trace.Span;
// import io.opentelemetry.api.trace.SpanKind;
// import io.opentelemetry.api.trace.StatusCode;
// import io.opentelemetry.api.trace.Tracer;
// import io.opentelemetry.context.Scope;


// @Slf4j
// @Aspect
// public class LoggingAspect {

//     private final SensitiveDataMasker masker;

//     public LoggingAspect(SensitiveDataMasker masker) {
//         this.masker = masker;
//     }

//     /**
//      * Lazy lookup — lấy Tracer mỗi lần gọi thay vì cache lúc khởi tạo.
//      * Vì LoggingAspect bean được tạo TRƯỚC khi TracingAutoConfiguration đăng ký SDK
//      * vào GlobalOpenTelemetry, nếu cache sẵn sẽ luôn nhận no-op Tracer.
//      */
//     private Tracer getTracer() {
//         return GlobalOpenTelemetry.getTracer("com.hotelbooking.chassis", "1.1.0");
//     }

//     /**
//      * Bắt tất cả method được annotate @Loggable ở bất kỳ class nào.
//      */
//     @Around("@annotation(loggable)")
//     public Object logBusinessEvent(ProceedingJoinPoint pjp, Loggable loggable) throws Throwable {
//         MethodSignature sig = (MethodSignature) pjp.getSignature();
//         List<StructuredArgument> baseArgs = buildBaseArgs(loggable, sig, pjp.getArgs());

//         Span span = getTracer().spanBuilder(loggable.event())
//             .setSpanKind(SpanKind.INTERNAL)
//             .startSpan();

//         span.setAttribute("business.event", loggable.event());
//         span.setAttribute("business.message", loggable.message());
//         span.setAttribute("service.layer", "business");

//         Parameter[] params = sig.getMethod().getParameters();
//         Object[] args = pjp.getArgs();
//         for (int i = 0; i < params.length; i++) {
//             LogParam logParam = params[i].getAnnotation(LogParam.class);
//             if (logParam == null || args[i] == null) continue;
//             if (!logParam.sensitive()) {
//                 span.setAttribute(logParam.value(), String.valueOf(args[i]));
//             }
//         }

//         try (Scope scope = span.makeCurrent()) {
//             // Log khi bắt đầu method
//             if (loggable.logOnEntry()) {
//                 List<StructuredArgument> entryArgs = new ArrayList<>(baseArgs);
//                 entryArgs.add(StructuredArguments.kv("phase", "START"));
//                 log.info(loggable.message() + " started", entryArgs.toArray());
//             }

//             long startTime = System.currentTimeMillis();

//             try {

//                 // thực thi hàm và đo thời gian thực thi
//                 Object result = pjp.proceed();
//                 long duration = System.currentTimeMillis() - startTime;

//                 span.setAttribute("duration_ms", duration);
//                 span.setStatus(StatusCode.OK);

//                 List<StructuredArgument> successArgs = new ArrayList<>(baseArgs);
//                 successArgs.add(StructuredArguments.kv("phase", "SUCCESS"));
//                 successArgs.add(StructuredArguments.kv("duration_ms", duration));

//                 if (loggable.logReturnValue() && result != null) {
//                     successArgs.add(StructuredArguments.kv("result", masker.maskObject(result)));
//                 }

//                 // Log khi hoàn tất thành công
//                 switch (loggable.successLevel()) {
//                     case DEBUG -> log.debug(loggable.message() + " completed", successArgs.toArray());
//                     case WARN  -> log.warn(loggable.message() + " completed", successArgs.toArray());
//                     default    -> log.info(loggable.message() + " completed", successArgs.toArray());
//                 }

//                 return result;

//             } catch (Throwable ex) {
                
//                 long duration = System.currentTimeMillis() - startTime;

//                 span.setStatus(StatusCode.ERROR, ex.getMessage());
//                 span.recordException(ex);
//                 span.setAttribute("duration_ms", duration);
//                 span.setAttribute("exception.type", ex.getClass().getSimpleName());

//                 List<StructuredArgument> errorArgs = new ArrayList<>(baseArgs);
//                 errorArgs.add(StructuredArguments.kv("phase", "ERROR"));
//                 errorArgs.add(StructuredArguments.kv("duration_ms", duration));
//                 errorArgs.add(StructuredArguments.kv("exception.type", ex.getClass().getSimpleName()));
//                 errorArgs.add(StructuredArguments.kv("exception.message", ex.getMessage()));

//                 // log khi gặp lỗi
//                 log.error(loggable.message() + " failed", errorArgs.toArray(), ex);
//                 throw ex;
//             }
//         } finally {
//             span.end();
//         }
//     }

//     /**
//      * Xây dựng danh sách argument cơ bản: event, message, clientIp,
//      * và các @LogParam từ method signature.
//      */
//     private List<StructuredArgument> buildBaseArgs(Loggable loggable,
//                                                    MethodSignature sig,
//                                                    Object[] args) {
//         List<StructuredArgument> result = new ArrayList<>();
//         result.add(StructuredArguments.kv("event", loggable.event()));

//         // Thêm clientIp từ MDC (đã được MdcFilter set từ HTTP request)
//         String clientIp = MDC.get("clientIp");
//         if (clientIp != null) {
//             result.add(StructuredArguments.kv("clientIp", clientIp));
//         }

//         // Đọc @LogParam từ từng parameter của method được annotate @Loggable 
//         Parameter[] params = sig.getMethod().getParameters();
//         for (int i = 0; i < params.length; i++) {
//             LogParam logParam = params[i].getAnnotation(LogParam.class);
//             if (logParam == null || args[i] == null) continue;

//             String fieldName = logParam.value();
//             String fieldValue = logParam.sensitive()
//                 ? "***"
//                 : masker.maskString(String.valueOf(args[i]));

//             result.add(StructuredArguments.kv(fieldName, fieldValue));
//         }

//         return result;
//     }
// }
