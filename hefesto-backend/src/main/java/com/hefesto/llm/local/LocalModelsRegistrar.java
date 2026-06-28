package com.hefesto.llm.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * Lê {@code hefesto.local-models} do ambiente e registra um
 * {@link LlamaServerAdapter} para cada modelo ANTES do contexto instanciar o
 * {@code LlmAdapterRegistry}. Assim cada modelo local entra na injeção de
 * {@code List<LlmAdapter>} como um bean de primeira classe — sem hardcode e sem
 * alterar a assinatura do registry.
 *
 * <p>Usa um {@link BeanDefinitionRegistryPostProcessor} porque a quantidade de
 * adapters é dinâmica (vem da config), o que {@code @Bean} estático não cobre.</p>
 */
public class LocalModelsRegistrar
        implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(LocalModelsRegistrar.class);

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        List<LocalModelProperties> models = Binder.get(environment)
                .bind("hefesto.local-models", Bindable.listOf(LocalModelProperties.class))
                .orElse(List.of());

        if (models.isEmpty()) {
            log.info("Nenhum modelo local configurado (hefesto.local-models vazio).");
            return;
        }

        for (LocalModelProperties props : models) {
            if (props.id() == null || props.id().isBlank()) {
                log.warn("Ignorando modelo local sem 'id': {}", props);
                continue;
            }
            BeanDefinitionBuilder builder = BeanDefinitionBuilder
                    .genericBeanDefinition(LlamaServerAdapter.class)
                    .addConstructorArgValue(props)
                    .setScope(BeanDefinition.SCOPE_SINGLETON);
            String beanName = "llamaServerAdapter_" + props.id();
            registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
            log.info("Registrado adapter de modelo local: {} ({}) -> {}",
                    props.id(), props.resolvedModel(), props.baseUrl());
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // nada a fazer
    }
}
