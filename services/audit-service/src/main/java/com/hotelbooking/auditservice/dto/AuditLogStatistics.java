package com.hotelbooking.auditservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogStatistics {
    private long totalEvents;
    private List<CategoryCount> byActionCategory;
    private List<SeverityCount> bySeverity;
    private List<DailyTrend> dailyTrend;
    private List<TopActor> topActors;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryCount {
        private String category;
        private long count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SeverityCount {
        private String severity;
        private long count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyTrend {
        private String date;
        private long count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopActor {
        private String actorId;
        private String actorEmail;
        private long count;
    }
}
