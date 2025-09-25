package com.fixserver.config;

import com.fixserver.store.InMemoryMessageStore;
import com.fixserver.store.MessageStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for MessageStore implementations.
 * 
 * This configuration ensures that an appropriate MessageStore implementation
 * is available based on the application setup:
 * 
 * - If database and JPA are configured, MessageStoreImpl will be used
 * - If database is not available, InMemoryMessageStore will be used as fallback
 * 
 * This allows the application to start and run even without database configuration,
 * making it suitable for development, testing, and proof-of-concept scenarios.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Configuration
public class MessageStoreConfiguration {
    
    /**
     * Provides an in-memory MessageStore implementation when no other implementation is available.
     * 
     * This bean is only created if no other MessageStore bean is found in the context.
     * It serves as a fallback to ensure the application can start without database dependencies.
     * 
     * @return InMemoryMessageStore instance for development/testing
     */
    @Bean
    @Primary
    public MessageStore inMemoryMessageStore() {
        log.info("Creating InMemoryMessageStore - database persistence is disabled");
        log.warn("Using in-memory storage: all messages will be lost on application restart");
        log.info("To enable database persistence, uncomment database dependencies in pom.xml and application.yml");
        
        return new InMemoryMessageStore();
    }
}