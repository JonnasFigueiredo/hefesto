/**
 * Hefesto Bridge — extensão VS Code que sobe um servidor HTTP local em
 * 127.0.0.1:<port> e expõe os modelos de chat do Copilot via vscode.lm
 * pra qualquer cliente que rode na mesma máquina (no caso, o backend
 * Hefesto, que tem um adapter `copilot-bridge`).
 *
 * Endpoints:
 *   GET  /health   → { status, version, modelCount }
 *   GET  /models   → { models: [{id, family, name, ...}] }
 *   POST /chat     → { content, model, latencyMs }   (sync; coleta tudo)
 *
 * Segurança: bind exclusivo em 127.0.0.1, então só processos da própria
 * máquina enxergam. CORS aberto pra simplificar (não há credenciais
 * sensíveis trafegando — auth Copilot fica na sessão do VS Code).
 */
import * as http from 'http'
import * as vscode from 'vscode'

let server: http.Server | undefined
let statusBar: vscode.StatusBarItem
let extensionVersion = '0.1.0'

export function activate(context: vscode.ExtensionContext) {
    extensionVersion = context.extension.packageJSON.version ?? extensionVersion

    statusBar = vscode.window.createStatusBarItem(
        vscode.StatusBarAlignment.Right,
        100,
    )
    statusBar.command = 'hefestoBridge.showStatus'
    context.subscriptions.push(statusBar)

    context.subscriptions.push(
        vscode.commands.registerCommand('hefestoBridge.restart', () => {
            stopServer()
            startServer()
        }),
        vscode.commands.registerCommand('hefestoBridge.showStatus', async () => {
            const port = getPort()
            const enabled = isEnabled()
            const running = !!server
            const models = running ? await safeListModels() : []
            vscode.window.showInformationMessage(
                `Hefesto Bridge: ${running ? 'running' : 'stopped'} (port ${port}, enabled=${enabled}, models=${models.length})`,
            )
        }),
        vscode.commands.registerCommand('hefestoBridge.listModels', async () => {
            const models = await safeListModels()
            if (models.length === 0) {
                vscode.window.showWarningMessage(
                    'Nenhum modelo Copilot disponível. Verifique se você tem GitHub Copilot ativo.',
                )
                return
            }
            const items = models.map((m) => ({
                label: `${m.name} (${m.family})`,
                description: m.id,
                detail: `vendor=${m.vendor} maxInputTokens=${m.maxInputTokens}`,
            }))
            await vscode.window.showQuickPick(items, {
                title: 'Modelos Copilot disponíveis',
                placeHolder: 'Apenas visualização — copia o ID se quiser fixar em hefestoBridge.preferredFamily',
            })
        }),
    )

    // Reage a mudanças de config sem reiniciar VS Code.
    context.subscriptions.push(
        vscode.workspace.onDidChangeConfiguration((e) => {
            if (
                e.affectsConfiguration('hefestoBridge.port') ||
                e.affectsConfiguration('hefestoBridge.enabled')
            ) {
                stopServer()
                startServer()
            }
        }),
    )

    startServer()
}

export function deactivate() {
    stopServer()
}

// ---------------------------------------------------------------------------
// Server lifecycle
// ---------------------------------------------------------------------------

function startServer() {
    if (!isEnabled()) {
        updateStatusBar(false, 'disabled')
        return
    }
    const port = getPort()

    server = http.createServer(async (req, res) => {
        // CORS aberto — só host local enxerga, sem risco real.
        res.setHeader('Access-Control-Allow-Origin', '*')
        res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        res.setHeader('Access-Control-Allow-Headers', 'Content-Type')

        if (req.method === 'OPTIONS') {
            res.writeHead(204)
            res.end()
            return
        }

        try {
            const url = req.url ?? ''
            if (req.method === 'GET' && url === '/health') {
                await handleHealth(res)
            } else if (req.method === 'GET' && url === '/models') {
                await handleModels(res)
            } else if (req.method === 'POST' && url === '/chat') {
                const body = await readBody(req)
                await handleChat(res, parseJson(body))
            } else {
                res.writeHead(404, { 'Content-Type': 'application/json' })
                res.end(JSON.stringify({ error: 'not_found', path: url }))
            }
        } catch (err) {
            const message = err instanceof Error ? err.message : String(err)
            console.error('[hefesto-bridge] handler error:', err)
            if (!res.headersSent) {
                res.writeHead(500, { 'Content-Type': 'application/json' })
            }
            try {
                res.end(JSON.stringify({ error: 'unexpected', message }))
            } catch {
                /* response já fechado */
            }
        }
    })

    server.on('error', (err: NodeJS.ErrnoException) => {
        console.error('[hefesto-bridge] server error:', err)
        const msg =
            err.code === 'EADDRINUSE'
                ? `Porta ${port} já está em uso. Mude em Settings → Hefesto Bridge → Port.`
                : `Erro no servidor: ${err.message}`
        vscode.window.showWarningMessage(`Hefesto Bridge: ${msg}`)
        updateStatusBar(false, 'error')
        server = undefined
    })

    server.listen(port, '127.0.0.1', () => {
        console.log(`[hefesto-bridge] listening on http://127.0.0.1:${port}`)
        updateStatusBar(true, `port ${port}`)
    })
}

function stopServer() {
    if (server) {
        server.close()
        server = undefined
        updateStatusBar(false, 'stopped')
    }
}

// ---------------------------------------------------------------------------
// Handlers
// ---------------------------------------------------------------------------

async function handleHealth(res: http.ServerResponse) {
    const models = await safeListModels()
    res.writeHead(200, { 'Content-Type': 'application/json' })
    res.end(
        JSON.stringify({
            status: 'ok',
            version: extensionVersion,
            modelCount: models.length,
        }),
    )
}

async function handleModels(res: http.ServerResponse) {
    const models = await safeListModels()
    res.writeHead(200, { 'Content-Type': 'application/json' })
    res.end(
        JSON.stringify({
            models: models.map((m) => ({
                id: m.id,
                vendor: m.vendor,
                family: m.family,
                name: m.name,
                version: m.version,
                maxInputTokens: m.maxInputTokens,
            })),
        }),
    )
}

interface ChatRequestBody {
    prompt?: string
    model?: string
    family?: string
}

async function handleChat(res: http.ServerResponse, body: ChatRequestBody) {
    const prompt = body.prompt
    if (!prompt || typeof prompt !== 'string') {
        res.writeHead(400, { 'Content-Type': 'application/json' })
        res.end(JSON.stringify({ error: 'missing_prompt' }))
        return
    }

    const model = await selectModel(body.model, body.family)
    if (!model) {
        res.writeHead(503, { 'Content-Type': 'application/json' })
        res.end(
            JSON.stringify({
                error: 'no_model_available',
                message:
                    'Nenhum modelo Copilot disponível. Faça login no Copilot dentro do VS Code primeiro.',
            }),
        )
        return
    }

    const start = Date.now()
    const cts = new vscode.CancellationTokenSource()
    res.on('close', () => cts.cancel())

    try {
        const messages = [vscode.LanguageModelChatMessage.User(prompt)]
        const response = await model.sendRequest(messages, {}, cts.token)

        let content = ''
        for await (const chunk of response.text) {
            content += chunk
        }

        res.writeHead(200, { 'Content-Type': 'application/json' })
        res.end(
            JSON.stringify({
                content,
                model: model.id,
                family: model.family,
                latencyMs: Date.now() - start,
            }),
        )
    } catch (err) {
        const message = err instanceof Error ? err.message : String(err)
        console.error('[hefesto-bridge] sendRequest failed:', err)
        res.writeHead(502, { 'Content-Type': 'application/json' })
        res.end(JSON.stringify({ error: 'lm_request_failed', message }))
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

async function safeListModels(): Promise<readonly vscode.LanguageModelChat[]> {
    try {
        return await vscode.lm.selectChatModels({ vendor: 'copilot' })
    } catch (err) {
        console.warn('[hefesto-bridge] selectChatModels failed:', err)
        return []
    }
}

async function selectModel(
    preferredId?: string,
    preferredFamily?: string,
): Promise<vscode.LanguageModelChat | undefined> {
    const models = await safeListModels()
    if (models.length === 0) return undefined

    if (preferredId) {
        const found = models.find((m) => m.id === preferredId)
        if (found) return found
    }

    const fam = preferredFamily ?? getPreferredFamily()
    if (fam) {
        const found = models.find((m) => m.family === fam)
        if (found) return found
    }

    return models[0]
}

async function readBody(req: http.IncomingMessage): Promise<string> {
    return new Promise((resolve, reject) => {
        const chunks: Buffer[] = []
        req.on('data', (c: Buffer) => chunks.push(c))
        req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')))
        req.on('error', reject)
    })
}

function parseJson(s: string): any {
    if (!s || !s.trim()) return {}
    try {
        return JSON.parse(s)
    } catch {
        return {}
    }
}

function isEnabled(): boolean {
    return vscode.workspace
        .getConfiguration('hefestoBridge')
        .get<boolean>('enabled', true)
}

function getPort(): number {
    return vscode.workspace
        .getConfiguration('hefestoBridge')
        .get<number>('port', 35421)
}

function getPreferredFamily(): string {
    return vscode.workspace
        .getConfiguration('hefestoBridge')
        .get<string>('preferredFamily', '')
}

function updateStatusBar(running: boolean, detail: string) {
    if (!statusBar) return
    if (running) {
        statusBar.text = '$(broadcast) Hefesto'
        statusBar.tooltip = `Hefesto Bridge: ${detail}`
        statusBar.backgroundColor = undefined
    } else {
        statusBar.text = '$(circle-slash) Hefesto'
        statusBar.tooltip = `Hefesto Bridge: ${detail}`
        statusBar.backgroundColor = new vscode.ThemeColor(
            'statusBarItem.warningBackground',
        )
    }
    statusBar.show()
}
