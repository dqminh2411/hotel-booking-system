package com.hotelbooking.userservice.schedule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hotelbooking.userservice.entity.TenantSubscription;
import com.hotelbooking.userservice.entity.TenantSubscriptionPlanStatus;
import com.hotelbooking.userservice.repository.TenantSubscriptionRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateExpireTenantSubscription {

    TenantSubscriptionRepository tenantSubscriptionRepository;

    @Scheduled(cron = "1 0 0 * * *")
    @Transactional
    public void updateExpireToday(){
        ZoneId id = ZoneId.of("Asia/Ho_Chi_Minh");

        LocalDate today = LocalDate.now(id);

        Instant start = today.atStartOfDay(id).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(id).minusNanos(1).toInstant();

        List<TenantSubscription> listTenantSubscriptions = tenantSubscriptionRepository.findByExpiresAtBetweenAndStatusAndIsDeletedFalse(start, end, TenantSubscriptionPlanStatus.ACTIVE);

        if(listTenantSubscriptions.isEmpty()){
            log.info("Không có gói nào hết hạn hôm nay");
            return;
        }

        listTenantSubscriptions.forEach(ts -> ts.setStatus(TenantSubscriptionPlanStatus.EXPIRED));

        tenantSubscriptionRepository.saveAll(listTenantSubscriptions);
        log.info("Cập nhật các gói thuê hết hạn hôm nay");
    }
}
