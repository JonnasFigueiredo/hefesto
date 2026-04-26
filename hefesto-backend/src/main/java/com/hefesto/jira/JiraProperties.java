package com.hefesto.jira;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binding tipado das propriedades de Jira definidas em application.yml /
 * application-local.yml. Valores padrão vazios — Spring inicializa o adapter
 * mas {@link JiraService} verifica antes de chamar a API.
 *
 * @param url    URL completa da instância Atlassian (ex: https://acme.atlassian.net)
 * @param email  e-mail da conta Atlassian usada como user em basic auth.
 * @param token  API token gerado em id.atlassian.com/manage-profile/security/api-tokens.
 */
@ConfigurationProperties(prefix = "jira")
public record JiraProperties(
    String url,
    String email,
    String token
) {

    public boolean isConfigured() {
        return url != null && !url.isBlank()
            && email != null && !email.isBlank()
            && token != null && !token.isBlank();
    }
}
