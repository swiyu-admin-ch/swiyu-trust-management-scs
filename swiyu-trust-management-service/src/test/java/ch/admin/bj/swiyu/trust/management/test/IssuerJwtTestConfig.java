package ch.admin.bj.swiyu.trust.management.test;

import ch.admin.bj.swiyu.jwssignatureservice.config.JwsSignatureServiceConfiguration;
import ch.admin.bj.swiyu.jwssignatureservice.config.KeyManagementStrategyFactoryConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ JwsSignatureServiceConfiguration.class, KeyManagementStrategyFactoryConfiguration.class })
public class IssuerJwtTestConfig {}
