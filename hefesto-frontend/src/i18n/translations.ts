/**
 * Dicionário de tradução flat. Chave usa convenção dot.notation por
 * domínio (chat.*, jira.*, etc.) pra organização.
 *
 * Default: pt-BR. Adicionar locales novos basta estender o tipo Locale e
 * fornecer todas as chaves.
 */

export type Locale = 'pt-BR' | 'en-US'

export const LOCALES: Locale[] = ['pt-BR', 'en-US']

export const DEFAULT_LOCALE: Locale = 'pt-BR'

export type Dict = Record<string, string>

export const translations: Record<Locale, Dict> = {
  // ---------------------------------------------------------------------------
  // Português Brasil
  // ---------------------------------------------------------------------------
  'pt-BR': {
    // Nav / shell
    'nav.chat': 'CHAT',
    'nav.jira': 'JIRA',
    'nav.analytics': 'ANALYTICS',
    'nav.settings': 'CONFIGURAÇÕES',
    'nav.home': 'INÍCIO',

    // System status (sidebar footer)
    'status.backend': 'BACKEND',
    'status.adapter': 'ADAPTER',
    'status.version': 'VERSÃO',
    'status.online': 'ONLINE',
    'status.offline': 'OFFLINE',
    'status.connecting': 'CONECTANDO',
    'status.lat': 'LAT',

    // Chat page
    'chat.title': 'CHAT',
    'chat.session': 'SESSÃO',
    'chat.sessions': 'SESSÕES',
    'chat.newSession': 'NOVA',
    'chat.noActiveSessions': '// NENHUMA SESSÃO ATIVA',
    'chat.untitled': '// sem título',
    'chat.noSessionSelected': '// NENHUMA SESSÃO SELECIONADA',
    'chat.connected': 'CONECTADO',
    'chat.connecting': 'CONECTANDO',
    'chat.offline': 'OFFLINE',
    'chat.clear': 'LIMPAR',
    'chat.clearAria': 'limpar sessão',
    'chat.error': 'ERRO',
    'chat.dismiss': 'FECHAR',
    'chat.deleteAria': 'remover conversa',

    // Composer
    'composer.transmit': 'ENVIAR',
    'composer.sending': 'ENVIANDO',
    'composer.stop': 'PARAR',
    'composer.ready': '// PRONTO',
    'composer.live': '// AO VIVO',
    'composer.charsSuffix': 'caracteres',
    'composer.placeholderIdle': '// DIGITE SUA MENSAGEM...',
    'composer.placeholderSending': '// AGUARDANDO RESPOSTA...',
    'composer.placeholderStreaming': '// STREAMING EM ANDAMENTO — PARAR PARA INTERROMPER',

    // Message bubbles
    'message.user': 'VOCÊ',
    'message.streaming': 'STREAMING',
    'message.aborted': 'ABORTADO',
    'message.systemContext': '// SISTEMA',
    'message.jiraContext': '// CONTEXTO JIRA INJETADO',

    // Empty state / loaders
    'empty.readyToChat': 'PRONTO PRO CHAT',
    'empty.typeToBegin': '// DIGITE UMA MENSAGEM PARA INICIAR A TRANSMISSÃO',
    'thinking.processing': '// PROCESSANDO',
    'matrix.decrypting': 'DECIFRANDO TRANSMISSÃO',
    'matrix.channelOpen': '// CANAL ABERTO',
    'matrix.blocks': 'BLOCOS',

    // Adapter selector
    'adapter.loading': '// CARREGANDO ADAPTERS',
    'adapter.unavailable': '// ADAPTERS INDISPONÍVEIS',
    'adapter.selectTitle': '// SELECIONE O ADAPTER LLM',
    'adapter.unavailableSuffix': '// INDISPONÍVEL',

    // Agent selector
    'agent.loading': '// CARREGANDO AGENTES',
    'agent.placeholder': 'AGENTE',
    'agent.titleTooltip': 'Agente: {name}',
    'agent.selectFallback': 'Selecionar agente',
    'agent.modalTitle': 'Escolha um agente',
    'agent.modalSubtitle': 'Define a persona e o comportamento das respostas',
    'agent.availableSuffix': 'disponíveis',
    'agent.active': 'ATIVO',
    'agent.personaActive': '// PERSONA ATIVA',

    // Attachment picker / list
    'attach.attach': 'ANEXAR',
    'attach.uploading': 'ENVIANDO',
    'attach.dropToAttach': '// SOLTE PARA ANEXAR',
    'attach.dismiss': 'FECHAR',
    'attach.errorNoSession': 'Crie uma sessão antes de anexar arquivos.',
    'attach.errorUpload': 'falha ao enviar arquivo',
    'attach.tooltip': 'Anexar arquivos (.txt, .md, .json, .yml). Ou arraste e solte na tela.',
    'attach.itemTooltip': 'Clique no X para desanexar; Shift+clique para deletar do servidor',

    // Context bar (Jira chip)
    'jira.attachByKey': 'JIRA',
    'jira.attachByKeyTooltip': 'Anexar issue do Jira pela KEY',
    'jira.openTooltip': 'Abrir {key} no Jira',
    'jira.detachAria': 'desanexar issue do Jira',

    // Jira page
    'jira.title': 'JIRA',
    'jira.connected': 'CONECTADO',
    'jira.checking': 'VERIFICANDO...',
    'jira.notConfigured': 'NÃO CONFIGURADO',
    'jira.notConfiguredFrame': '// JIRA NÃO CONFIGURADO',
    'jira.notConfiguredBody':
      'Configure as credenciais do Jira em application-local.yml ou via variáveis de ambiente (JIRA_URL, JIRA_EMAIL, JIRA_TOKEN) e reinicie o backend.',
    'jira.goToSettings': '// IR PARA CONFIGURAÇÕES →',
    'jira.frameQuery': '// CONSULTA',
    'jira.frameIssue': '// HISTÓRIA',
    'jira.emptyTitle': '// SELECIONE UMA HISTÓRIA PARA INSPECIONAR',
    'jira.emptyHint':
      'Use a barra de busca ou clique em uma das sugestões rápidas (MEUS, EM PROGRESSO, etc.)',
    'jira.detailError': '// ERRO // {message}',
    // Criar história
    'jira.createButton': '+ NOVA HISTÓRIA',
    'jira.createTitle': '// NOVA HISTÓRIA',
    'jira.fieldProject': 'PROJETO',
    'jira.fieldType': 'TIPO',
    'jira.fieldSummary': 'TÍTULO',
    'jira.fieldSummaryPlaceholder': 'Resumo curto da história',
    'jira.fieldDescription': 'DESCRIÇÃO',
    'jira.fieldDescriptionPlaceholder': 'Contexto, regras de negócio, comportamento esperado...',
    'jira.fieldCriteria': 'CRITÉRIOS DE ACEITE',
    'jira.fieldCriteriaHint': '(um por linha)',
    'jira.createSubmit': 'CRIAR',
    'jira.createSubmitting': 'CRIANDO...',
    'jira.createCancel': 'CANCELAR',
    'jira.createError': 'Falha ao criar história',
    'jira.aiTitle': '✨ ASSISTÊNCIA DE IA',
    'jira.aiContextPlaceholder':
      'Cole requisitos, contexto de negócio, descrição de telas/designs, padrões... A IA gera um rascunho da história.',
    'jira.aiHint': 'Gera título, descrição e critérios — revise antes de criar',
    'jira.aiGenerate': '✨ GERAR RASCUNHO',
    'jira.aiGenerating': 'GERANDO...',
    'jira.aiError': 'Falha ao gerar rascunho',

    // JQL search
    'jql.execute': 'EXECUTAR',
    'jql.executing': 'EXECUTANDO...',
    'jql.shortcuts': '// ATALHOS:',
    'jql.shortcutMine': 'MEUS',
    'jql.shortcutInProgress': 'EM PROGRESSO',
    'jql.shortcutOpen': 'ABERTOS',
    'jql.shortcutLast7d': '7 DIAS',
    'jql.placeholder': '// JQL: project = HEF AND status = "In Progress"',

    // Issue list
    'issues.errorLoad': '// ERRO // {message}',
    'issues.errorFallback': 'falha ao carregar histórias',
    'issues.empty': '// NENHUM RESULTADO',
    'issues.countSingular': '// {count} HISTÓRIA',
    'issues.countPlural': '// {count} HISTÓRIAS',
    'issues.hasMorePages': '// HÁ MAIS PÁGINAS',
    'issues.unassigned': '— SEM RESPONSÁVEL',
    'issues.statusUnknown': 'DESCONHECIDO',

    // Issue detail
    'issue.tabDescription': 'DESCRIÇÃO',
    'issue.tabAcceptance': 'ACEITE',
    'issue.tabComments': 'COMENTÁRIOS',
    'issue.tabMeta': 'META',
    'issue.sendToChat': 'ENVIAR PRO CHAT',
    'issue.openInJira': 'ABRIR NO JIRA',
    'issue.noDescription': '// SEM DESCRIÇÃO',
    'issue.noAcceptance': '// NENHUM CRITÉRIO DE ACEITE DETECTADO',
    'issue.acceptanceHint':
      '(o backend procura por seções "critérios de aceite" / "acceptance criteria" na descrição)',
    'issue.noComments': '// SEM COMENTÁRIOS',
    'issue.metaKey': 'KEY',
    'issue.metaType': 'TIPO',
    'issue.metaPriority': 'PRIORIDADE',
    'issue.metaStatus': 'STATUS',
    'issue.metaAssignee': 'RESPONSÁVEL',
    'issue.metaReporter': 'RELATOR',
    'issue.metaSprint': 'SPRINT',
    'issue.metaLabels': 'LABELS',
    'issue.metaCreated': 'CRIADO EM',
    'issue.metaUpdated': 'ATUALIZADO EM',

    // Settings page
    'settings.title': 'CONFIGURAÇÕES',
    'settings.badgeInternal': 'USO INTERNO',
    'settings.system': '// SISTEMA',
    'settings.statusBackend': 'STATUS DO BACKEND',
    'settings.service': 'SERVIÇO',
    'settings.version': 'VERSÃO',
    'settings.adapters': '// ADAPTERS LLM',
    'settings.adaptersHint': '// CONFIGURADO VIA application-local.yml',
    'settings.jira': '// CONEXÃO JIRA',
    'settings.jiraUrl': 'URL DO JIRA',
    'settings.email': 'EMAIL',
    'settings.token': 'API TOKEN',
    'settings.testConnection': 'TESTAR CONEXÃO',
    'settings.testHint': '// EDITE application-local.yml E REINICIE O BACKEND',
    'settings.appearance': '// APARÊNCIA',
    'settings.appearanceHint': '// SCANLINE / INTENSIDADE DE GLOW / CRT — EM BREVE',

    // Analytics
    'analytics.title': 'ANALYTICS',
    'analytics.kpiTotalEvents': 'EVENTOS TOTAIS',
    'analytics.kpiCompletedMessages': 'MENSAGENS CONCLUÍDAS',
    'analytics.kpiAttachmentUploads': 'UPLOADS DE ANEXO',
    'analytics.kpiErrorRate': 'TAXA DE ERRO (CHAT)',
    'analytics.avgLatency': '// LATÊNCIA MÉDIA',
    'analytics.avgLatencyHint': '// chat.complete (ida + volta do LLM)',
    'analytics.noData': '// SEM DADOS AINDA',
    'analytics.eventsByType': '// EVENTOS POR TIPO',
    'analytics.activity14days': '// ATIVIDADE NOS ÚLTIMOS 14 DIAS',
    'analytics.recentEvents': '// EVENTOS RECENTES',
    'analytics.noEventsYet': '// SEM EVENTOS REGISTRADOS AINDA',
    'analytics.no14dData': '// SEM DADOS NOS ÚLTIMOS 14 DIAS',
    'analytics.noEvents': '// NENHUM EVENTO REGISTRADO',
    'analytics.tableTimestamp': 'TIMESTAMP',
    'analytics.tableType': 'TIPO',
    'analytics.tableConv': 'CONV',
    'analytics.tableDuration': 'DURAÇÃO',
    'analytics.tablePayload': 'PAYLOAD',

    // Test cases panel
    'tc.panelTitle': 'CASOS DE TESTE EXTRAÍDOS',
    'tc.totalSuffix': 'total',
    'tc.generateReport': 'GERAR RELATÓRIO',
    'tc.generateReportTooltip':
      'Gera relatório HTML pronto pra imprimir ou salvar como PDF (Ctrl+P na nova aba)',

    // Test case card
    'tc.statusPending': 'PENDENTE',
    'tc.statusPassed': 'PASSOU',
    'tc.statusFailed': 'FALHOU',
    'tc.statusBlocked': 'BLOQUEADO',
    'tc.sectionPreconditions': '// PRÉ-CONDIÇÕES',
    'tc.sectionSteps': '// PASSOS ({count})',
    'tc.sectionExpected': '// RESULTADO ESPERADO',
    'tc.sectionExecution': '// EXECUÇÃO',
    'tc.sectionEvidence': '// EVIDÊNCIAS',
    'tc.errorPrefix': '// ERRO ::',
    'tc.errorFallback': 'falha na requisição',
    'tc.untitled': 'Caso sem título',
    'tc.noEvidence':
      'NENHUMA EVIDÊNCIA — ANEXE PRINTS, LOGS OU RESPOSTAS DE API NO BOTÃO ACIMA.',

    // Evidence picker / list
    'evidence.attach': 'EVIDÊNCIA',
    'evidence.uploading': 'ENVIANDO',
    'evidence.dropHere': 'SOLTE AQUI',
    'evidence.tooltip':
      'Anexar evidência (screenshot, log, etc.). Ou arraste e solte aqui.',
    'evidence.removeAria': 'remover',

    // Language toggle
    'lang.label': 'Idioma',
    'lang.brazilianPortuguese': 'Português (Brasil)',
    'lang.usEnglish': 'English (United States)',
  },

  // ---------------------------------------------------------------------------
  // English (US)
  // ---------------------------------------------------------------------------
  'en-US': {
    // Nav / shell
    'nav.chat': 'CHAT',
    'nav.jira': 'JIRA',
    'nav.analytics': 'ANALYTICS',
    'nav.settings': 'SETTINGS',
    'nav.home': 'HOME',

    // System status
    'status.backend': 'BACKEND',
    'status.adapter': 'ADAPTER',
    'status.version': 'VERSION',
    'status.online': 'ONLINE',
    'status.offline': 'OFFLINE',
    'status.connecting': 'CONNECTING',
    'status.lat': 'LAT',

    // Chat page
    'chat.title': 'CHAT',
    'chat.session': 'SESSION',
    'chat.sessions': 'SESSIONS',
    'chat.newSession': 'NEW',
    'chat.noActiveSessions': '// NO ACTIVE SESSIONS',
    'chat.untitled': '// untitled',
    'chat.noSessionSelected': '// NO SESSION SELECTED',
    'chat.connected': 'CONNECTED',
    'chat.connecting': 'CONNECTING',
    'chat.offline': 'OFFLINE',
    'chat.clear': 'CLEAR',
    'chat.clearAria': 'clear session',
    'chat.error': 'ERROR',
    'chat.dismiss': 'DISMISS',
    'chat.deleteAria': 'delete conversation',

    // Composer
    'composer.transmit': 'TRANSMIT',
    'composer.sending': 'SENDING',
    'composer.stop': 'STOP',
    'composer.ready': '// READY',
    'composer.live': '// LIVE',
    'composer.charsSuffix': 'chars',
    'composer.placeholderIdle': '// TRANSMIT MESSAGE...',
    'composer.placeholderSending': '// AWAITING RESPONSE...',
    'composer.placeholderStreaming': '// STREAMING IN PROGRESS — STOP TO INTERRUPT',

    // Message bubbles
    'message.user': 'USER',
    'message.streaming': 'STREAMING',
    'message.aborted': 'ABORTED',
    'message.systemContext': '// SYSTEM',
    'message.jiraContext': '// JIRA CONTEXT INJECTED',

    // Empty state / loaders
    'empty.readyToChat': 'READY TO CHAT',
    'empty.typeToBegin': '// TYPE A MESSAGE TO BEGIN TRANSMISSION',
    'thinking.processing': '// PROCESSING',
    'matrix.decrypting': 'DECRYPTING TRANSMISSION',
    'matrix.channelOpen': '// CHANNEL OPEN',
    'matrix.blocks': 'BLOCKS',

    // Adapter selector
    'adapter.loading': '// LOADING ADAPTERS',
    'adapter.unavailable': '// ADAPTERS UNAVAILABLE',
    'adapter.selectTitle': '// SELECT LLM ADAPTER',
    'adapter.unavailableSuffix': '// UNAVAILABLE',

    // Agent selector
    'agent.loading': '// LOADING AGENTS',
    'agent.placeholder': 'AGENT',
    'agent.titleTooltip': 'Agent: {name}',
    'agent.selectFallback': 'Select agent',
    'agent.modalTitle': 'Choose an agent',
    'agent.modalSubtitle': 'Defines the persona and response behavior',
    'agent.availableSuffix': 'available',
    'agent.active': 'ACTIVE',
    'agent.personaActive': '// PERSONA ACTIVE',

    // Attachment picker / list
    'attach.attach': 'ATTACH',
    'attach.uploading': 'UPLOADING',
    'attach.dropToAttach': '// DROP TO ATTACH',
    'attach.dismiss': 'DISMISS',
    'attach.errorNoSession': 'Create a session before attaching files.',
    'attach.errorUpload': 'upload failed',
    'attach.tooltip': 'Attach files (.txt, .md, .json, .yml). Or drag and drop on screen.',
    'attach.itemTooltip': 'Click X to detach; Shift+click to delete from server',

    // Context bar (Jira chip)
    'jira.attachByKey': 'JIRA',
    'jira.attachByKeyTooltip': 'Attach Jira issue by KEY',
    'jira.openTooltip': 'Open {key} in Jira',
    'jira.detachAria': 'detach Jira issue',

    // Jira page
    'jira.title': 'JIRA',
    'jira.connected': 'CONNECTED',
    'jira.checking': 'CHECKING...',
    'jira.notConfigured': 'NOT CONFIGURED',
    'jira.notConfiguredFrame': '// JIRA NOT CONFIGURED',
    'jira.notConfiguredBody':
      'Configure Jira credentials in application-local.yml or via environment variables (JIRA_URL, JIRA_EMAIL, JIRA_TOKEN) and restart the backend.',
    'jira.goToSettings': '// GO TO SETTINGS →',
    'jira.frameQuery': '// QUERY',
    'jira.frameIssue': '// ISSUE',
    'jira.emptyTitle': '// SELECT AN ISSUE TO INSPECT',
    'jira.emptyHint':
      'Use the search bar or click one of the quick suggestions (MINE, IN PROGRESS, etc.)',
    'jira.detailError': '// ERROR // {message}',
    // Create story
    'jira.createButton': '+ NEW STORY',
    'jira.createTitle': '// NEW STORY',
    'jira.fieldProject': 'PROJECT',
    'jira.fieldType': 'TYPE',
    'jira.fieldSummary': 'SUMMARY',
    'jira.fieldSummaryPlaceholder': 'Short story summary',
    'jira.fieldDescription': 'DESCRIPTION',
    'jira.fieldDescriptionPlaceholder': 'Context, business rules, expected behavior...',
    'jira.fieldCriteria': 'ACCEPTANCE CRITERIA',
    'jira.fieldCriteriaHint': '(one per line)',
    'jira.createSubmit': 'CREATE',
    'jira.createSubmitting': 'CREATING...',
    'jira.createCancel': 'CANCEL',
    'jira.createError': 'Failed to create story',
    'jira.aiTitle': '✨ AI ASSIST',
    'jira.aiContextPlaceholder':
      'Paste requirements, business context, screen/design notes, patterns... AI drafts the story.',
    'jira.aiHint': 'Generates title, description and criteria — review before creating',
    'jira.aiGenerate': '✨ GENERATE DRAFT',
    'jira.aiGenerating': 'GENERATING...',
    'jira.aiError': 'Failed to generate draft',

    // JQL search
    'jql.execute': 'EXECUTE',
    'jql.executing': 'EXECUTING...',
    'jql.shortcuts': '// SHORTCUTS:',
    'jql.shortcutMine': 'MINE',
    'jql.shortcutInProgress': 'IN PROGRESS',
    'jql.shortcutOpen': 'OPEN',
    'jql.shortcutLast7d': '7 DAYS',
    'jql.placeholder': '// JQL: project = HEF AND status = "In Progress"',

    // Issue list
    'issues.errorLoad': '// ERROR // {message}',
    'issues.errorFallback': 'failed to load issues',
    'issues.empty': '// NO ISSUES MATCHED',
    'issues.countSingular': '// {count} ISSUE',
    'issues.countPlural': '// {count} ISSUES',
    'issues.hasMorePages': '// HAS MORE PAGES',
    'issues.unassigned': '— UNASSIGNED',
    'issues.statusUnknown': 'UNKNOWN',

    // Issue detail
    'issue.tabDescription': 'DESCRIPTION',
    'issue.tabAcceptance': 'ACCEPTANCE',
    'issue.tabComments': 'COMMENTS',
    'issue.tabMeta': 'META',
    'issue.sendToChat': 'SEND TO CHAT',
    'issue.openInJira': 'OPEN IN JIRA',
    'issue.noDescription': '// NO DESCRIPTION',
    'issue.noAcceptance': '// NO ACCEPTANCE CRITERIA DETECTED',
    'issue.acceptanceHint':
      '(the backend looks for "acceptance criteria" / "critérios de aceite" sections in the description)',
    'issue.noComments': '// NO COMMENTS',
    'issue.metaKey': 'KEY',
    'issue.metaType': 'TYPE',
    'issue.metaPriority': 'PRIORITY',
    'issue.metaStatus': 'STATUS',
    'issue.metaAssignee': 'ASSIGNEE',
    'issue.metaReporter': 'REPORTER',
    'issue.metaSprint': 'SPRINT',
    'issue.metaLabels': 'LABELS',
    'issue.metaCreated': 'CREATED',
    'issue.metaUpdated': 'UPDATED',

    // Settings page
    'settings.title': 'SETTINGS',
    'settings.badgeInternal': 'INTERNAL USE',
    'settings.system': '// SYSTEM',
    'settings.statusBackend': 'BACKEND STATUS',
    'settings.service': 'SERVICE',
    'settings.version': 'VERSION',
    'settings.adapters': '// LLM ADAPTERS',
    'settings.adaptersHint': '// CONFIGURED VIA application-local.yml',
    'settings.jira': '// JIRA CONNECTION',
    'settings.jiraUrl': 'JIRA URL',
    'settings.email': 'EMAIL',
    'settings.token': 'API TOKEN',
    'settings.testConnection': 'TEST CONNECTION',
    'settings.testHint': '// EDIT application-local.yml AND RESTART THE BACKEND',
    'settings.appearance': '// APPEARANCE',
    'settings.appearanceHint': '// SCANLINE / GLOW INTENSITY / CRT — COMING SOON',

    // Analytics
    'analytics.title': 'ANALYTICS',
    'analytics.kpiTotalEvents': 'TOTAL EVENTS',
    'analytics.kpiCompletedMessages': 'COMPLETED MESSAGES',
    'analytics.kpiAttachmentUploads': 'ATTACHMENT UPLOADS',
    'analytics.kpiErrorRate': 'ERROR RATE (CHAT)',
    'analytics.avgLatency': '// AVERAGE LATENCY',
    'analytics.avgLatencyHint': '// chat.complete (LLM round-trip)',
    'analytics.noData': '// NO DATA YET',
    'analytics.eventsByType': '// EVENTS BY TYPE',
    'analytics.activity14days': '// ACTIVITY IN THE LAST 14 DAYS',
    'analytics.recentEvents': '// RECENT EVENTS',
    'analytics.noEventsYet': '// NO EVENTS RECORDED YET',
    'analytics.no14dData': '// NO DATA IN THE LAST 14 DAYS',
    'analytics.noEvents': '// NO EVENT RECORDED',
    'analytics.tableTimestamp': 'TIMESTAMP',
    'analytics.tableType': 'TYPE',
    'analytics.tableConv': 'CONV',
    'analytics.tableDuration': 'DURATION',
    'analytics.tablePayload': 'PAYLOAD',

    // Test cases panel
    'tc.panelTitle': 'EXTRACTED TEST CASES',
    'tc.totalSuffix': 'total',
    'tc.generateReport': 'GENERATE REPORT',
    'tc.generateReportTooltip':
      'Generates HTML report ready to print or save as PDF (Ctrl+P in the new tab)',

    // Test case card
    'tc.statusPending': 'PENDING',
    'tc.statusPassed': 'PASSED',
    'tc.statusFailed': 'FAILED',
    'tc.statusBlocked': 'BLOCKED',
    'tc.sectionPreconditions': '// PRECONDITIONS',
    'tc.sectionSteps': '// STEPS ({count})',
    'tc.sectionExpected': '// EXPECTED RESULT',
    'tc.sectionExecution': '// EXECUTION',
    'tc.sectionEvidence': '// EVIDENCE',
    'tc.errorPrefix': '// ERROR ::',
    'tc.errorFallback': 'request failed',
    'tc.untitled': 'Untitled case',
    'tc.noEvidence':
      'NO EVIDENCE — ATTACH SCREENSHOTS, LOGS OR API RESPONSES VIA THE BUTTON ABOVE.',

    // Evidence picker / list
    'evidence.attach': 'EVIDENCE',
    'evidence.uploading': 'UPLOADING',
    'evidence.dropHere': 'DROP HERE',
    'evidence.tooltip':
      'Attach evidence (screenshot, log, etc.). Or drag and drop here.',
    'evidence.removeAria': 'remove',

    // Language toggle
    'lang.label': 'Language',
    'lang.brazilianPortuguese': 'Portuguese (Brazil)',
    'lang.usEnglish': 'English (United States)',
  },
}

/**
 * Substitui placeholders {chave} no template pelos valores fornecidos.
 */
export function interpolate(template: string, vars?: Record<string, string | number>): string {
  if (!vars) return template
  return template.replace(/\{(\w+)\}/g, (_, k) =>
    vars[k] !== undefined ? String(vars[k]) : `{${k}}`,
  )
}
