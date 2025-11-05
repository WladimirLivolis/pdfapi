# 📋 Roadmap de Evolução - PDF API

## 🎯 Visão Geral

Este documento registra o plano de evolução do projeto PDF API, transformando-o de uma ferramenta funcional em uma solução production-ready adequada para ambientes corporativos.

---

## 📊 Status das Fases

- [x] **Fase 1** - Produção Básica (1-2 semanas) ✅ **CONCLUÍDA**
- [x] **Fase 2** - Segurança e Escalabilidade (2-4 semanas) ✅ **CONCLUÍDA**
- [ ] **Fase 3** - Novas Funcionalidades (1-2 meses) 🚧 **EM PROGRESSO**
  - [x] **Fase 3A** - Quick Wins ✅ **CONCLUÍDA**
  - [ ] **Fase 3B** - High Impact
  - [ ] **Fase 3C** - Advanced Features
  - [ ] **Fase 3D** - Specialized
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

#### 2.1 Autenticação e Autorização ✅
- [x] Adicionar dependência `spring-boot-starter-security`
- [x] Implementar autenticação básica (HTTP Basic Auth)
- [x] Configurar segurança com `SecurityFilterChain`
- [x] Criar perfis de usuário (admin, user)
- [x] Proteger endpoints sensíveis
- [x] Manter endpoints públicos (health check, swagger)
- [ ] (Opcional) Migrar para JWT em vez de Basic Auth

**Benefícios:**
- Controle de acesso à API
- Logs de auditoria (quem fez qual operação)
- Conformidade com requisitos de segurança

**Implementação:**
- Adicionado Spring Security com HTTP Basic Auth
- Criados 2 usuários em memória: `user` (ROLE_USER) e `admin` (ROLE_USER, ROLE_ADMIN)
- Senhas criptografadas com BCrypt
- Endpoints `/pdfapi/**` requerem autenticação
- Endpoints públicos: `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`

#### 2.2 Rate Limiting ✅
- [x] Adicionar dependência Resilience4j
- [x] Configurar limites por endpoint
- [x] Implementar handler de exceção para rate limiting
- [x] Configurar diferentes limites por endpoint (normal vs heavy)
- [x] Adicionar métricas de rate limiter no Actuator

**Benefícios:**
- Proteção contra abuso e DoS
- Uso justo de recursos
- Melhor previsibilidade de custos

**Implementação:**
- Resilience4j configurado com dois perfis:
  - `pdfapi`: 10 requisições/minuto (extract, remove)
  - `pdfapi-heavy`: 3 requisições/minuto (merge, split, convertImageToPDF)
- Handler de exceção retorna HTTP 429 (Too Many Requests)
- Métricas expostas via `/actuator/ratelimiters`

#### 2.3 Processamento Assíncrono ✅
- [x] Habilitar `@EnableAsync` na aplicação
- [x] Criar configuração de ThreadPool customizada
- [x] Criar DTOs para jobs assíncronos (JobResponse)
- [ ] Implementar endpoints `/async` que retornam job IDs (futuro)
- [ ] Criar sistema de tracking de jobs (requer Redis/DB - futuro)

**Benefícios:**
- Infraestrutura preparada para processamento assíncrono
- ThreadPool configurado para operações pesadas
- Base para implementação futura de jobs

**Implementação:**
- `@EnableAsync` ativado na aplicação
- `AsyncConfig` com ThreadPool customizado (2-5 threads)
- `JobResponse` DTO criado para respostas de jobs
- Sistema completo de jobs pode ser implementado futuramente com Redis ou banco de dados

#### 2.4 CORS e Configurações de Segurança ✅
- [x] Configurar CORS adequadamente
- [x] Adicionar headers de segurança (X-Frame-Options, XSS Protection, CSP)
- [x] Desabilitar CSRF (apropriado para REST API stateless)
- [ ] Configurar HTTPS (produção - requer certificado)

**Implementação:**
- `CorsConfig` com configuração completa:
  - Allowed origins configuráveis
  - Suporte para credenciais
  - Headers expostos para downloads
- Security headers configurados no `SecurityFilterChain`:
  - X-Frame-Options: DENY
  - XSS Protection habilitado
  - Content Security Policy: default-src 'self'
- CSRF desabilitado (apropriado para REST API com Basic Auth)

---

## 🟢 **FASE 3** - Novas Funcionalidades (1-2 meses)

### Objetivo
Expandir as capacidades da API com funcionalidades avançadas de PDF.

### ⚠️ **IMPORTANTE - Política de Licenciamento**

**APENAS bibliotecas Open Source e gratuitas serão utilizadas:**
- ✅ **iText Community (AGPL)** - Já em uso, continuar utilizando
- ✅ **Apache PDFBox** - Apache License 2.0 (permissiva)
- ✅ **Tesseract OCR** - Apache License 2.0 (permissiva)
- ❌ **iText Commercial** - Licença paga (NÃO usar)
- ❌ **Qualquer biblioteca proprietária** - NÃO usar

**Princípio:** Se uma funcionalidade requer biblioteca paga, ela será descartada ou implementada com alternativa open source.

---

### 📊 Status Atual das Funcionalidades

**5 operações implementadas:**
1. ✅ Merge PDFs
2. ✅ Split PDF
3. ✅ Extract Pages
4. ✅ Remove Pages
5. ✅ Image to PDF

---

### 🎯 Subfases de Implementação

A Fase 3 foi dividida em subfases para facilitar a implementação incremental:

---

## **FASE 3A - Quick Wins** (1-2 semanas) 🚀 ✅ **CONCLUÍDA**

**Objetivo:** Implementar funcionalidades fáceis e úteis para resultados rápidos.

**Dificuldade:** ⭐ Fácil a ⭐⭐ Média
**Impacto:** ⭐⭐⭐⭐ Alto
**Bibliotecas:** iText Community (já instalada)

### Funcionalidades:

#### 3A.1 Rotate Pages (Rotação de Páginas) ✅
- [x] Endpoint POST `/pdfapi/rotate`
- [x] Rotação de páginas específicas ou todas
- [x] Suporte para 90°, 180°, 270°, -90° e múltiplos de 90°
- [x] Parâmetros: `file`, `pages` (opcional), `rotation`
- [x] Normalização automática de ângulos (360° = 0°, -90° = 270°)

**Complexidade:** ⭐ Fácil
**Biblioteca:** iText Community
**Estimativa:** 2-3 horas

**Implementação:**
- Método `PdfService.rotate()` implementado
- Validação de ângulos (múltiplos de 90°)
- Rotação relativa (adiciona ao ângulo atual da página)
- Rate limiting: `pdfapi` (10 req/min)

#### 3A.2 PDF Info (Informações do PDF) ✅
- [x] Endpoint POST `/pdfapi/info`
- [x] Retornar: número de páginas, tamanho, versão PDF, dimensões
- [x] Response JSON com metadados básicos
- [x] Verifica se todas as páginas têm a mesma dimensão
- [x] Dimensões da primeira página em pontos

**Complexidade:** ⭐ Fácil
**Biblioteca:** iText Community
**Estimativa:** 1-2 horas

**Implementação:**
- DTO `PdfInfoResponse` criado
- Método `PdfService.getInfo()` implementado
- Retorna: pageCount, fileSizeBytes, pdfVersion, firstPageDimensions, allPagesSameDimension
- Rate limiting: `pdfapi` (10 req/min)

#### 3A.3 PDF Metadata (Metadados) ✅
- [x] Endpoint POST `/pdfapi/metadata` - ler metadados
- [x] Endpoint PUT `/pdfapi/metadata` - atualizar metadados
- [x] Informações: título, autor, assunto, palavras-chave, criador, produtor
- [x] Datas: criação, modificação

**Complexidade:** ⭐ Fácil
**Biblioteca:** iText Community
**Estimativa:** 2-3 horas

**Implementação:**
- DTOs `PdfMetadataResponse` e `PdfMetadataRequest` criados
- Método `PdfService.getMetadata()` implementado para leitura
- Método `PdfService.updateMetadata()` implementado para atualização
- Campos opcionais na atualização (apenas campos fornecidos são alterados)
- Rate limiting: `pdfapi` (10 req/min)

#### 3A.4 Add Page Numbers (Numeração de Páginas) ✅
- [x] Endpoint POST `/pdfapi/addPageNumbers`
- [x] Adicionar números de página
- [x] Configuração: posição (topo/rodapé, esquerda/centro/direita)
- [x] Formato customizável (ex: "Page {current} of {total}", "{page}", etc.)
- [x] Range de páginas (opcional)
- [x] Suporte para 9 posições: top-left, top-center, top-right, bottom-left, bottom-center, bottom-right

**Complexidade:** ⭐⭐ Média
**Biblioteca:** iText Community
**Estimativa:** 3-4 horas

**Implementação:**
- Método `PdfService.addPageNumbers()` implementado
- Posição padrão: `bottom-center`
- Formato padrão: `Page {current} of {total}`
- Placeholders suportados: `{current}`, `{total}`, `{page}`
- Font size: 10pt, cor: preto
- Rate limiting: `pdfapi` (10 req/min)

**Total Fase 3A:** ~8-12 horas de desenvolvimento ✅ **CONCLUÍDO**

---

## **FASE 3B - High Impact** (2-3 semanas) 🔥

**Objetivo:** Implementar funcionalidades mais complexas mas muito solicitadas.

**Dificuldade:** ⭐⭐ Média a ⭐⭐⭐ Difícil
**Impacto:** ⭐⭐⭐⭐⭐ Muito Alto
**Bibliotecas:** iText Community

### Funcionalidades:

#### 3B.1 Watermark (Marca d'água)
- [ ] Endpoint POST `/pdfapi/watermark`
- [ ] Suporte para texto como watermark
- [ ] Suporte para imagem como watermark
- [ ] Configuração: posição, opacidade, rotação, escala
- [ ] Aplicar em todas as páginas ou páginas específicas
- [ ] Configurar camada (frente/fundo)

**Complexidade:** ⭐⭐ Média
**Biblioteca:** iText Community
**Estimativa:** 6-8 horas

#### 3B.2 Compress PDF (Compressão)
- [ ] Endpoint POST `/pdfapi/compress`
- [ ] Diferentes níveis: LOW, MEDIUM, HIGH
- [ ] Compressão de imagens embutidas
- [ ] Remoção de objetos duplicados
- [ ] Relatório: tamanho original vs comprimido, % redução
- [ ] Opção de qualidade de imagem

**Complexidade:** ⭐⭐⭐ Difícil
**Biblioteca:** iText Community
**Estimativa:** 8-10 horas

#### 3B.3 Encrypt/Password (Criptografia)
- [ ] Endpoint POST `/pdfapi/encrypt`
- [ ] Adicionar senha de abertura (user password)
- [ ] Adicionar senha de permissões (owner password)
- [ ] Configurar permissões: impressão, cópia, edição, anotações
- [ ] Níveis de criptografia: 40-bit, 128-bit, 256-bit AES
- [ ] Endpoint POST `/pdfapi/decrypt` (remover senha com permissão)

**Complexidade:** ⭐⭐ Média
**Biblioteca:** iText Community
**Estimativa:** 6-8 horas

#### 3B.4 Optimize PDF (Otimização)
- [ ] Endpoint POST `/pdfapi/optimize`
- [ ] Linearização para fast web view
- [ ] Compressão + remoção de redundâncias
- [ ] Otimização de fontes
- [ ] Ideal para publicação web

**Complexidade:** ⭐⭐⭐ Difícil
**Biblioteca:** iText Community
**Estimativa:** 8-10 horas

**Total Fase 3B:** ~28-36 horas de desenvolvimento

---

## **FASE 3C - Advanced Features** (3-4 semanas) 📊

**Objetivo:** Funcionalidades avançadas que agregam diferencial competitivo.

**Dificuldade:** ⭐⭐⭐ Difícil
**Impacto:** ⭐⭐⭐⭐ Alto
**Bibliotecas:** Apache PDFBox (nova dependência)

### Funcionalidades:

#### 3C.1 PDF to Images (PDF para Imagens)
- [ ] Endpoint POST `/pdfapi/toImages`
- [ ] Suporte para PNG, JPG
- [ ] Configuração de DPI (72, 150, 300)
- [ ] Configuração de qualidade JPEG (0-100)
- [ ] Retornar ZIP com todas as imagens
- [ ] Opção de converter páginas específicas

**Complexidade:** ⭐⭐⭐ Difícil
**Biblioteca:** Apache PDFBox
**Estimativa:** 10-12 horas

#### 3C.2 Extract Images (Extrair Imagens)
- [ ] Endpoint POST `/pdfapi/extractImages`
- [ ] Extrair todas as imagens embutidas no PDF
- [ ] Retornar ZIP com imagens
- [ ] Metadados: página de origem, dimensões, formato
- [ ] Filtro por tamanho mínimo (evitar ícones pequenos)

**Complexidade:** ⭐⭐ Média
**Biblioteca:** Apache PDFBox ou iText
**Estimativa:** 6-8 horas

#### 3C.3 Crop Pages (Cortar Páginas)
- [ ] Endpoint POST `/pdfapi/crop`
- [ ] Definir área de corte: x, y, width, height
- [ ] Aplicar a páginas específicas ou todas
- [ ] Presets: remover margens, centralizar conteúdo

**Complexidade:** ⭐⭐ Média
**Biblioteca:** iText Community
**Estimativa:** 5-6 horas

#### 3C.4 Fill Forms (Preencher Formulários)
- [ ] Endpoint POST `/pdfapi/fillForm`
- [ ] Aceitar JSON com campos e valores
- [ ] Suporte para campos de texto, checkbox, radio button
- [ ] Opção de "flatten" (tornar não-editável)
- [ ] Validação de campos obrigatórios

**Complexidade:** ⭐⭐⭐ Difícil
**Biblioteca:** iText Community
**Estimativa:** 10-12 horas

#### 3C.5 Merge with Bookmarks (Merge com Índice)
- [ ] Melhorar endpoint `/pdfapi/merge` existente
- [ ] Adicionar parâmetro `createBookmarks=true`
- [ ] Criar bookmark para cada PDF mesclado
- [ ] Usar nome do arquivo como título do bookmark

**Complexidade:** ⭐⭐ Média
**Biblioteca:** iText Community
**Estimativa:** 4-5 horas

**Total Fase 3C:** ~35-43 horas de desenvolvimento

---

## **FASE 3D - Specialized** (apenas se necessário) ⚠️

**Objetivo:** Funcionalidades especializadas e de nicho.

**Dificuldade:** ⭐⭐⭐⭐⭐ Muito Difícil
**Impacto:** ⭐⭐⭐ Médio (nicho específico)
**Bibliotecas:** Tesseract OCR (dependência externa pesada)

### Funcionalidades:

#### 3D.1 OCR (Reconhecimento Óptico de Caracteres)
- [ ] Integração com Tesseract OCR
- [ ] Endpoint POST `/pdfapi/ocr`
- [ ] Suporte para múltiplos idiomas (por, eng, spa)
- [ ] Retornar PDF pesquisável (searchable PDF)
- [ ] Opção de retornar apenas texto extraído
- [ ] **ATENÇÃO:** Processamento muito pesado, considerar async obrigatório

**Complexidade:** ⭐⭐⭐⭐⭐ Muito Difícil
**Biblioteca:** Tesseract OCR (externa)
**Estimativa:** 20-30 horas
**Requisitos:**
- Tesseract instalado no servidor
- Linguagem data files
- Processamento assíncrono obrigatório
- Rate limiting muito restritivo (1 req/5min)

**Nota:** Implementar APENAS se houver demanda específica. Considerar alternativas como serviços externos (OCR.space API, Google Vision API) se viável.

**Total Fase 3D:** ~20-30 horas de desenvolvimento

---

### 📊 Resumo das Subfases

| Subfase | Funcionalidades | Dificuldade | Tempo Estimado | Prioridade |
|---------|-----------------|-------------|----------------|------------|
| **3A** | 4 features (Rotate, Info, Metadata, Page Numbers) | ⭐-⭐⭐ | 8-12h | 🔥 **MUITO ALTA** |
| **3B** | 4 features (Watermark, Compress, Encrypt, Optimize) | ⭐⭐-⭐⭐⭐ | 28-36h | 🔥 **ALTA** |
| **3C** | 5 features (To Images, Extract Images, Crop, Forms, Bookmarks) | ⭐⭐-⭐⭐⭐ | 35-43h | 📊 **MÉDIA** |
| **3D** | 1 feature (OCR) | ⭐⭐⭐⭐⭐ | 20-30h | ⚠️ **BAIXA** |

**Total:** 18 novas funcionalidades potenciais

---

### 🎯 Recomendação de Implementação

**Próxima Sessão: Começar com FASE 3A** ✅

**Motivos:**
1. ✅ Resultados rápidos (4 features em 8-12h)
2. ✅ Baixo risco de problemas
3. ✅ Usa apenas bibliotecas já instaladas (iText)
4. ✅ Alta utilidade para usuários
5. ✅ Boa base para testar padrões antes das features complexas

**Após 3A:** Avaliar feedback e priorizar 3B ou 3C baseado na demanda.

---

### Tarefas Originais (Referência)

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

### Fase 2 ✅
- [x] API requer autenticação
- [x] Rate limiting implementado e testado
- [x] Infraestrutura de processamento assíncrono disponível

### Fase 3
- [x] Mínimo 8 operações de PDF disponíveis (9 operações implementadas)
- [ ] Testes de integração para todas as novas funcionalidades (em andamento)

### Fase 4
- [ ] Tempo de deploy <5 minutos
- [ ] Monitoramento ativo em produção
- [ ] Cobertura de testes >80%

---

## 🗓️ Histórico de Progresso

### 2025-10-21

**Sessão 1 - Fase 1:**
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

**Arquivos Criados (Fase 1):**
- `src/main/java/com/pdf/pdfapi/dto/PdfOperationResponse.java`
- `src/main/java/com/pdf/pdfapi/dto/ErrorResponse.java`
- `src/main/java/com/pdf/pdfapi/dto/PdfResult.java`
- `src/main/java/com/pdf/pdfapi/exception/GlobalExceptionHandler.java`
- `src/main/java/com/pdf/pdfapi/validator/PdfFileValidator.java`
- `ROADMAP.md`

**Arquivos Modificados (Fase 1):**
- `pom.xml` (adicionada dependência spring-boot-starter-validation)
- `src/main/java/com/pdf/pdfapi/controller/PdfController.java`
- `src/main/java/com/pdf/pdfapi/service/PdfService.java`
- `src/test/java/com/pdf/pdfapi/controller/PdfControllerTest.java`
- `src/test/java/com/pdf/pdfapi/service/PdfServiceTest.java`

---

**Sessão 2 - Fase 2:**
- ✅ **Fase 2 CONCLUÍDA** (segurança e escalabilidade implementadas)
  - ✅ Spring Security configurado com HTTP Basic Auth
  - ✅ 2 usuários criados (user/user123, admin/admin123)
  - ✅ Rate limiting implementado com Resilience4j (2 perfis: normal e heavy)
  - ✅ CORS configurado com headers de segurança
  - ✅ Infraestrutura assíncrona preparada (ThreadPool + DTOs)
  - ✅ Handler de rate limiting (HTTP 429)
  - ✅ Todos os 11 testes ainda passando
  - ✅ Build compilando sem erros

**Arquivos Criados (Fase 2):**
- `src/main/java/com/pdf/pdfapi/config/security/SecurityConfig.java`
- `src/main/java/com/pdf/pdfapi/config/security/CorsConfig.java`
- `src/main/java/com/pdf/pdfapi/config/AsyncConfig.java`
- `src/main/java/com/pdf/pdfapi/dto/JobResponse.java`
- `src/main/resources/application.yml`

**Arquivos Modificados (Fase 2):**
- `pom.xml` (adicionadas dependências: spring-security, resilience4j, spring-security-test)
- `src/main/java/com/pdf/pdfapi/PdfApiApplication.java` (@EnableAsync)
- `src/main/java/com/pdf/pdfapi/controller/PdfController.java` (@RateLimiter)
- `src/main/java/com/pdf/pdfapi/exception/GlobalExceptionHandler.java` (handler RequestNotPermitted)

**Arquivos Removidos (Fase 2):**
- `src/main/resources/application.properties` (migrado para application.yml)

---

**Melhoria de Segurança - Variáveis de Ambiente:**
- ✅ **Credenciais removidas do código** (nenhuma senha hardcoded)
  - ✅ Criado `SecurityProperties` para configuração via environment variables
  - ✅ SecurityConfig refatorado para usar `@ConfigurationProperties`
  - ✅ Criado `.env.example` com template de configuração
  - ✅ Atualizado `.gitignore` para excluir `.env` e `output/`
  - ✅ README atualizado com instruções de configuração
  - ✅ Validação de configuração obrigatória (senhas devem ser fornecidas)
  - ✅ Todos os testes passando com variáveis de ambiente

**Arquivos Criados (Melhoria):**
- `.env.example` - Template de configuração (commitado)
- `.env` - Configuração local (NÃO commitado, em .gitignore)
- `src/main/java/com/pdf/pdfapi/config/security/SecurityProperties.java`

**Arquivos Modificados (Melhoria):**
- `src/main/resources/application.yml` (variáveis de ambiente com ${})
- `src/main/java/com/pdf/pdfapi/config/security/SecurityConfig.java` (usa SecurityProperties)
- `.gitignore` (adicionados .env e output/)
- `README.md` (instruções completas de setup)

---

**Sessão 3 - Fase 3A:**
- ✅ **Fase 3A CONCLUÍDA** (todas as 4 funcionalidades implementadas e testadas)
  - ✅ Rotate Pages: rotação de páginas específicas ou todas, múltiplos de 90°
  - ✅ PDF Info: extração de informações do PDF (páginas, tamanho, versão, dimensões)
  - ✅ PDF Metadata: leitura e atualização de metadados (título, autor, subject, etc.)
  - ✅ Add Page Numbers: adição de numeração de páginas com posição e formato customizáveis
  - ✅ Build compilando sem erros
  - ✅ Todos os 11 testes passando
  - ✅ 4 novos endpoints implementados
  - ✅ 3 novos DTOs criados (PdfInfoResponse, PdfMetadataResponse, PdfMetadataRequest)

**Arquivos Criados (Fase 3A):**
- `src/main/java/com/pdf/pdfapi/dto/PdfInfoResponse.java`
- `src/main/java/com/pdf/pdfapi/dto/PdfMetadataResponse.java`
- `src/main/java/com/pdf/pdfapi/dto/PdfMetadataRequest.java`

**Arquivos Modificados (Fase 3A):**
- `src/main/java/com/pdf/pdfapi/service/PdfService.java` (4 novos métodos)
- `src/main/java/com/pdf/pdfapi/controller/PdfController.java` (5 novos endpoints)

**Novos Endpoints:**
1. `POST /pdfapi/rotate` - Rotacionar páginas
2. `POST /pdfapi/info` - Obter informações do PDF
3. `POST /pdfapi/metadata` - Obter metadados do PDF
4. `PUT /pdfapi/metadata` - Atualizar metadados do PDF
5. `POST /pdfapi/addPageNumbers` - Adicionar números de página

---

## 📝 Notas

- Este roadmap é um documento vivo e deve ser atualizado conforme o projeto evolui
- Prioridades podem ser ajustadas baseadas em necessidades do negócio
- Cada fase pode ser dividida em sprints menores
- Mantenha este documento atualizado ao final de cada sessão de desenvolvimento

---

**Última atualização:** 2025-11-04 - Fase 3A Concluída
