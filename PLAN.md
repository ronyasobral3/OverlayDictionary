Desenvolva um aplicativo Android com funcionalidade de tradução rápida acessível globalmente através de overlay.

Objetivo

Criar um app que permita ao usuário traduzir textos sem sair do aplicativo atual, utilizando um elemento flutuante persistente na tela.

Requisitos funcionais
Serviço em background
Implementar um Foreground Service responsável por manter o overlay ativo.
O serviço deve iniciar automaticamente após a permissão de overlay ser concedida.
Overlay flutuante (barra lateral)
Criar uma view pequena fixa na lateral da tela.
Deve permanecer acima de qualquer app (TYPE_APPLICATION_OVERLAY).
Deve ser arrastável verticalmente.
Deve ser discreta e não bloquear interação com o app subjacente.
Interação com a barra
Ao tocar ou arrastar a barra, abrir um modal.
Modal de tradução
Deve aparecer na parte inferior da tela.
Altura máxima de 50% da tela.
Não deve ocupar tela cheia.
Deve permitir interação (input de texto).
Conteúdo do modal
Campo de entrada de texto
Botão de ação (“Traduzir”)
Área de exibição do resultado
Botão para fechar o modal
Tradução
Integrar com uma API de tradução (ex: Google Translate, DeepL ou similar).
Implementar chamada assíncrona (coroutines ou equivalente).
Tratar erros de rede.
Requisitos técnicos
Linguagem: Kotlin
Utilizar WindowManager para renderizar overlays
Permissão obrigatória: SYSTEM_ALERT_WINDOW
Garantir compatibilidade com Android 8+ (API 26+)
Evitar bloqueio de UI thread
Gerenciar corretamente o ciclo de vida do Service
Requisitos de UX
O overlay não deve ser intrusivo
Deve existir forma clara de fechar o modal
O usuário deve conseguir continuar usando o app original sem interrupção
O desempenho deve ser leve (baixo consumo de memória)
Extras (opcional, mas desejável)
Detectar texto copiado automaticamente
Implementar AccessibilityService para capturar texto de outros apps
Cache de traduções recentes
Detecção automática de idioma
Suporte offline
Resultado esperado

Um app funcional onde:

Um botão flutuante fica sempre visível
Ao interagir, abre um painel inferior
O usuário consegue traduzir texto rapidamente sem sair do contexto atual