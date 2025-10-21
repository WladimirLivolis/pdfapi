# 📋 Roadmap de Evolução - PDF API

## 🎯 Visão Geral

Este documento registra o plano de evolução do projeto PDF API, transformando-o de uma ferramenta funcional em uma solução production-ready adequada para ambientes corporativos.

---

## 📊 Status das Fases

- [x] **Fase 1** - Produção Básica (1-2 semanas) ✅ **CONCLUÍDA**
- [ ] **Fase 2** - Segurança e Escalabilidade (2-4 semanas)
- [ ] **Fase 3** - Novas Funcionalidades (1-2 meses)
- [ ] **Fase 4** - Observabilidade e DevOps (Contínuo)

---

## 🔴 **FASE 1** - Produção Básica (1-2 semanas)

### Objetivo
Tornar a API robusta com respostas HTTP adequadas, validações e melhor experiência para o cliente.

### Tarefas

#### 1.1 Tratamento de Erros e Respostas HTTP ✅
- [x] Criar DTOs de resposta (`PdfOperationResponse`, `ErrorResponse`)
- [x] Implementar `@RestControllerAdvice` para tratamento global de exceções
- [x] Atualizar todos os métodos do controller para retornar `ResponseEntity`
- [x] Adicionar códigos HTTP apropriados (200, 400, 404, 500)
- [x] Incluir timestamps e mensagens descritivas nas respostas

**Benefícios:**
- APIs RESTful adequadas com códigos HTTP corretos
- Clientes recebem feedback claro sobre sucesso/falha
- Facilita debugging e monitoramento

**Implementação:**
- Criados DTOs: `PdfOperationResponse`, `ErrorResponse`, `PdfResult`
- Implementado `GlobalExceptionHandler` com handlers para `PdfErrorException`, `MethodArgumentNotValidException`, `MaxUploadSizeExceededException`, `IllegalArgumentException` e `Exception` genérica
- Todos os endpoints agora retornam `ResponseEntity<Resource>` ou `ResponseEntity<List<PdfOperationResponse>>`
- Códigos HTTP: 200 (sucesso), 400 (bad request), 413 (payload too large), 500 (erro interno)

#### 1.2 Validação de Entrada ✅
- [x] Adicionar dependência `spring-boot-starter-validation`
- [x] Criar DTOs de requisição com anotações de validação
- [x] Implementar validador customizado para arquivos PDF
- [x] Validar ranges de páginas (startPage <= endPage, valores positivos)
- [x] Validar tipos de arquivo (MIME type)
- [x] Validar tamanho máximo de arquivos por operação

**Benefícios:**
- Previne erros em runtime
- Melhora segurança (evita processamento de arquivos maliciosos)
- Mensagens de erro mais claras para o usuário

**Implementação:**
- Adicionada dependência `spring-boot-starter-validation`
- Criado `PdfFileValidator` com validações de:
  - Tipo MIME (PDF e imagens)
  - Assinatura de arquivo (magic bytes %PDF)
  - Tamanho máximo (100MB)
- Validações de ranges de páginas implementadas no `PdfService`
- Validações chamadas em todos os endpoints do controller

#### 1.3 Retornar PDFs Diretamente ✅
- [x] Modificar service para retornar `byte[]` em vez de salvar em disco
- [x] Atualizar controllers para retornar arquivo no response body
- [x] Configurar headers HTTP apropriados (Content-Type, Content-Disposition)
- [x] Implementar nomes de arquivo dinâmicos
- [x] Adicionar opção de download vs visualização inline

**Benefícios:**
- Elimina acúmulo de arquivos temporários
- Resposta imediata ao cliente
- Reduz uso de disco

**Implementação:**
- Todos os métodos do `PdfService` agora retornam `PdfResult` ou `List<PdfResult>`
- `PdfResult` contém: content (byte[]), suggestedFileName, sizeInBytes, pageCount
- Controllers retornam `ByteArrayResource` com headers apropriados
- Headers configurados: `Content-Type: application/pdf`, `Content-Disposition: attachment`
- Nomes de arquivo incluem timestamp para evitar conflitos

---

## 🟡 **FASE 2** - Segurança e Escalabilidade (2-4 semanas)

### Objetivo
Adicionar camadas de segurança e preparar a API para alto volume de requisições.

### Tarefas

#### 2.1 Autenticação e Autorização
- [ ] Adicionar dependência `spring-boot-starter-security`
- [ ] Implementar autenticação básica (HTTP Basic Auth)
- [ ] Configurar segurança com `SecurityFilterChain`
- [ ] Criar perfis de usuário (admin, user)
- [ ] Proteger endpoints sensíveis
- [ ] Manter endpoints públicos (health check, swagger)
- [ ] (Opcional) Migrar para JWT em vez de Basic Auth

**Benefícios:**
- Controle de acesso à API
- Logs de auditoria (quem fez qual operação)
- Conformidade com requisitos de segurança

#### 2.2 Rate Limiting
- [ ] Adicionar dependência Bucket4j ou Resilience4j
- [ ] Configurar limites por IP/usuário
- [ ] Implementar fallback methods
- [ ] Adicionar headers de rate limit nas respostas
- [ ] Configurar diferentes limites por endpoint

**Benefícios:**
- Proteção contra abuso e DoS
- Uso justo de recursos
- Melhor previsibilidade de custos

#### 2.3 Processamento Assíncrono
- [ ] Habilitar `@EnableAsync` na aplicação
- [ ] Criar `AsyncPdfService` com métodos `@Async`
- [ ] Implementar endpoints `/async` que retornam job IDs
- [ ] Criar sistema de tracking de jobs (status, progresso)
- [ ] Adicionar endpoint para consultar status do job
- [ ] Implementar notificação via webhook (opcional)

**Benefícios:**
- Suporte para arquivos grandes sem timeout
- Melhor experiência do usuário
- Libera recursos do servidor mais rapidamente

#### 2.4 CORS e Configurações de Segurança
- [ ] Configurar CORS adequadamente
- [ ] Adicionar headers de segurança (X-Frame-Options, CSP, etc.)
- [ ] Configurar HTTPS (produção)
- [ ] Implementar proteção CSRF quando necessário

---

## 🟢 **FASE 3** - Novas Funcionalidades (1-2 meses)

### Objetivo
Expandir as capacidades da API com funcionalidades avançadas de PDF.

### Tarefas

#### 3.1 Adicionar Marca D'água (Watermark)
- [ ] Endpoint POST `/pdfapi/watermark`
- [ ] Suporte para texto e imagem como watermark
- [ ] Configuração de posição, opacidade, rotação
- [ ] Watermark em todas as páginas ou páginas específicas

#### 3.2 Compressão de PDFs
- [ ] Endpoint POST `/pdfapi/compress`
- [ ] Diferentes níveis de compressão (baixa, média, alta)
- [ ] Compressão de imagens embutidas
- [ ] Relatório de redução de tamanho

#### 3.3 Rotação de Páginas
- [ ] Endpoint POST `/pdfapi/rotate`
- [ ] Rotação de páginas específicas ou todas
- [ ] Suporte para 90°, 180°, 270°

#### 3.4 Criptografia e Senha
- [ ] Endpoint POST `/pdfapi/encrypt`
- [ ] Adicionar senha de abertura
- [ ] Adicionar senha de permissões
- [ ] Configurar permissões (impressão, cópia, edição)

#### 3.5 Conversão PDF para Imagens
- [ ] Endpoint POST `/pdfapi/toImages`
- [ ] Suporte para PNG, JPG
- [ ] Configuração de DPI/qualidade
- [ ] Retornar ZIP com todas as imagens

#### 3.6 OCR em PDFs Escaneados
- [ ] Integração com Tesseract OCR
- [ ] Endpoint POST `/pdfapi/ocr`
- [ ] Suporte para múltiplos idiomas
- [ ] Retornar PDF pesquisável

#### 3.7 Metadados de PDF
- [ ] Endpoint GET `/pdfapi/metadata` - ler metadados
- [ ] Endpoint POST `/pdfapi/metadata` - atualizar metadados
- [ ] Informações: autor, título, data de criação, etc.

---

## 🔵 **FASE 4** - Observabilidade e DevOps (Contínuo)

### Objetivo
Garantir visibilidade, monitoramento e automação de deploy.

### Tarefas

#### 4.1 Métricas e Monitoramento
- [ ] Configurar Micrometer para métricas customizadas
- [ ] Criar métricas de negócio (PDFs processados, tempo de processamento)
- [ ] Configurar Actuator endpoints
- [ ] Integração com Prometheus
- [ ] Dashboard Grafana com principais métricas

#### 4.2 Logging Estruturado
- [ ] Migrar para logging estruturado (JSON)
- [ ] Adicionar correlation IDs para rastreamento
- [ ] Configurar diferentes níveis de log por ambiente
- [ ] Integração com ELK Stack (Elasticsearch, Logstash, Kibana)

#### 4.3 Tracing Distribuído
- [ ] Adicionar Spring Cloud Sleuth
- [ ] Integração com Zipkin ou Jaeger
- [ ] Rastreamento de requisições end-to-end

#### 4.4 Containerização
- [ ] Criar Dockerfile otimizado (multi-stage build)
- [ ] Criar docker-compose.yml para desenvolvimento local
- [ ] Otimizar imagem Docker (Alpine, JRE em vez de JDK)
- [ ] Configurar health checks no container

#### 4.5 CI/CD Pipeline
- [ ] Configurar GitHub Actions / GitLab CI
- [ ] Pipeline de build automático
- [ ] Execução de testes em cada commit
- [ ] Análise de código estático (SonarQube)
- [ ] Deploy automático em ambientes de staging
- [ ] Deploy manual/aprovado para produção

#### 4.6 Testes Adicionais
- [ ] Testes de integração com TestContainers
- [ ] Testes de carga com Gatling ou JMeter
- [ ] Testes de segurança com OWASP ZAP
- [ ] Mutation testing com PITest
- [ ] Aumentar cobertura de testes para >80%

#### 4.7 Documentação
- [ ] Enriquecer anotações OpenAPI/Swagger
- [ ] Adicionar exemplos de requisição/resposta
- [ ] Criar collection Postman/Insomnia
- [ ] Documentar processo de deploy
- [ ] Criar guia de contribuição (CONTRIBUTING.md)
- [ ] Adicionar exemplos de código no README

---

## 📈 Métricas de Sucesso

### Fase 1 ✅
- [x] 100% dos endpoints retornam respostas HTTP adequadas
- [x] 0 exceções não tratadas em produção
- [x] Validação em todos os inputs públicos

### Fase 2
- [ ] API requer autenticação
- [ ] Rate limiting implementado e testado
- [ ] Processamento assíncrono disponível para arquivos >10MB

### Fase 3
- [ ] Mínimo 8 operações de PDF disponíveis
- [ ] Testes de integração para todas as novas funcionalidades

### Fase 4
- [ ] Tempo de deploy <5 minutos
- [ ] Monitoramento ativo em produção
- [ ] Cobertura de testes >80%

---

## 🗓️ Histórico de Progresso

### 2025-10-21

**Sessão 1:**
- ✅ Projeto analisado completamente
- ✅ Roadmap criado e salvo
- ✅ **Fase 1 CONCLUÍDA** (todas as tarefas implementadas e testadas)
  - ✅ DTOs de resposta criados (PdfOperationResponse, ErrorResponse, PdfResult)
  - ✅ GlobalExceptionHandler implementado com 5 handlers diferentes
  - ✅ PdfController atualizado para retornar ResponseEntity
  - ✅ PdfService refatorado para retornar bytes em vez de salvar em disco
  - ✅ Validador customizado de arquivos PDF criado (PdfFileValidator)
  - ✅ Validação de entrada implementada em todos os endpoints
  - ✅ Todos os 11 testes unitários passando (5 controller + 6 service)
  - ✅ Build compilando sem erros

**Arquivos Criados:**
- `src/main/java/com/pdf/pdfapi/dto/PdfOperationResponse.java`
- `src/main/java/com/pdf/pdfapi/dto/ErrorResponse.java`
- `src/main/java/com/pdf/pdfapi/dto/PdfResult.java`
- `src/main/java/com/pdf/pdfapi/exception/GlobalExceptionHandler.java`
- `src/main/java/com/pdf/pdfapi/validator/PdfFileValidator.java`
- `ROADMAP.md`

**Arquivos Modificados:**
- `pom.xml` (adicionada dependência spring-boot-starter-validation)
- `src/main/java/com/pdf/pdfapi/controller/PdfController.java`
- `src/main/java/com/pdf/pdfapi/service/PdfService.java`
- `src/test/java/com/pdf/pdfapi/controller/PdfControllerTest.java`
- `src/test/java/com/pdf/pdfapi/service/PdfServiceTest.java`

---

## 📝 Notas

- Este roadmap é um documento vivo e deve ser atualizado conforme o projeto evolui
- Prioridades podem ser ajustadas baseadas em necessidades do negócio
- Cada fase pode ser dividida em sprints menores
- Mantenha este documento atualizado ao final de cada sessão de desenvolvimento

---

**Última atualização:** 2025-10-21
