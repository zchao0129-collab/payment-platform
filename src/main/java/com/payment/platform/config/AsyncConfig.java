package com.payment.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * <p>
 * 支付成功后的商户回调推送（开放API订单等）走独立线程池，避免阻塞支付宝/微信的异步通知响应。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /** 商户回调推送线程池 */
    @Bean("notifyCallbackExecutor")
    public ThreadPoolTaskExecutor notifyCallbackExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notify-cb-");
        // 队列满时由调用线程执行，避免丢回调（仅极端情况下发生）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
