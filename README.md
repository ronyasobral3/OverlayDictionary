# OverlayDictionary

Aplicativo Android que exibe um botão flutuante persistente sobre qualquer app, permitindo traduzir textos de inglês para português sem sair do contexto atual.

## Funcionalidades

- **Overlay flutuante** — barra lateral arrastável, sempre visível sobre outros apps
- **Modal de tradução** — painel inferior com campo de entrada, resultado e botão de fechar; pode ser arrastado verticalmente segurando o cabeçalho
- **Tradução EN → PT** — integração com a [MyMemory API](https://api.mymemory.translated.net/)
- **Cache LRU** — traduções recentes são reutilizadas sem nova chamada à API (até 50 entradas)
- **Detecção de clipboard** — ao copiar um texto, o modal abre automaticamente com o texto pré-preenchido e a tradução disparada
- **Desligar overlay** — botão no modal encerra o serviço sem precisar abrir o app

## Tecnologias

| Camada | Tecnologia |
|--------|-----------|
| UI (Activity) | Jetpack Compose + Material 3 |
| UI (Overlay) | Android Views + `WindowManager` |
| Async | Coroutines (`Dispatchers.IO`) |
| Rede | `HttpURLConnection` |
| Build | Gradle 8.9 (Kotlin DSL), AGP 8.7.3 |

**Requisitos:** Android 8.0+ (API 26), permissão `SYSTEM_ALERT_WINDOW`

## Como usar

1. Abra o app e conceda a permissão de sobreposição
2. A barra lateral aparecerá na lateral da tela
3. Toque na barra para abrir o modal de tradução
4. Digite ou copie um texto em inglês — a tradução é exibida automaticamente

## Build

```bash
./gradlew assembleDebug
```
