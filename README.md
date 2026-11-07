# TicketFlow

**Plateforme de billetterie microservices — Spring Boot 4 / Spring Cloud 2025.1 / Java 21**

Plateforme numérique de billetterie permettant de publier et découvrir des événements,
gérer les comptes clients, réserver des places, traiter un paiement selon le mode retenu,
émettre des billets et informer les utilisateurs. Le projet inclut également une expérience
conversationnelle intégrée aux services métier, accessible notamment depuis Telegram.

Le cahier des charges complet est disponible dans
[`docs/CAHIER_DES_CHARGES.md`](docs/CAHIER_DES_CHARGES.md) — il sert de source de vérité
pour toute décision d'architecture.

---

## Stack technique

| Composant | Technologie |
|---|---|
| Langage | Java 21 |
| Framework | Spring Boot 4.0.8 |
| Cloud | Spring Cloud 2025.1.2 |
| API Gateway | Spring Cloud Gateway Server WebFlux |
| Service registry | Netflix Eureka |
| Configuration | Spring Cloud Config (backend fichiers locaux en démo, Git en prod) |
| Communication synchrone | OpenFeign + Resilience4J |
| Communication asynchrone | Apache Kafka (events versionnés, DLT) |
| Persistance | PostgreSQL 16 + Flyway (migrations versionnées par service) |
| Authentification | Keycloak 25 (OIDC / JWT, 4 rôles) |
| Frontend | Angular 19 (standalone components) |
| Conteneurisation | Docker multi-étapes + Docker Compose |

---

## Architecture cible

```
                          ┌───────────────────┐
                          │   Angular Web     │
                          └─────────┬─────────┘
                                    │ HTTPS/JSON
                          ┌─────────▼─────────┐
                          │   API Gateway     │   ← validation JWT, CORS, rate limiting, X-Correlation-Id
                          └─────────┬─────────┘
                                    │
        ┌───────────┬───────────────┼───────────────┬──────────────┐
        ▼           ▼               ▼               ▼              ▼
  ┌──────────┐ ┌──────────┐ ┌──────────────┐ ┌────────────┐ ┌──────────────┐
  │ Customer │ │  Event   │ │   Booking    │ │  Payment   │ │    Ticket    │
  │ Service  │ │ Service  │ │   Service    │ │  Service   │ │   Service    │
  └────┬─────┘ └────┬─────┘ └──────┬───────┘ └─────┬──────┘ └──────┬───────┘
       │            │              │ Feign           │              │
       │            └──────────────┤─────────────────┘              │
       │                           │                                │
       ▼                           ▼                                ▼
  ┌──────────┐              ┌─────────────┐                  ┌──────────────┐
  │PostgreSQL│              │    Kafka    │                  │ Notification │
  │  (×7 BDD)│              │   (events)  │                  │   Service    │
  └──────────┘              └─────────────┘                  └──────────────┘

                          ┌──────────────────┐
                          │ Telegram Webhook │
                          └─────────┬─────────┘
                                    │
                          ┌─────────▼─────────┐    ┌──────────────────┐
                          │ AI Assistant Svc   │───▶│  MCP Tool Server │
                          │ (LLM + Telegram)  │    │   (allowlist)    │
                          └───────────────────┘    └────────┬─────────┘
                                                            │ Feign
                                                            ▼
                                                     Event / Booking
```

## Liste des modules

| # | Module | Port | Rôle | BDD |
|---|---|---|---|---|
| 1 | `config-server` | 8888 | Configuration centralisée | — |
| 2 | `discovery-server` | 8761 | Registre Eureka | — |
| 3 | `api-gateway` | 8080 | Point d'entrée unique | — |
| 4 | `customer-service` | 8081 | Comptes clients, profils, liaison Telegram | `ticketflow_customer` |
| 5 | `event-service` | 8082 | Catalogue d'événements, lieux, catégories | `ticketflow_event` |
| 6 | `booking-service` | 8083 | Réservations, inventaire atomique, TTL, saga | `ticketflow_booking` |
| 7 | `payment-service` | 8084 | Paiement simulé + adaptateur prestataire | `ticketflow_payment` |
| 8 | `ticket-service` | 8085 | Émission, consultation, vérification des billets | `ticketflow_ticket` |
| 9 | `notification-service` | 8086 | Consumer Kafka, adaptateurs e-mail/Telegram | `ticketflow_notification` |
| 10 | `ai-assistant-service` | 8087 | Bot Telegram + orchestrateur agent + LLM | `ticketflow_assistant` |
| 11 | `mcp-tool-server` | 8088 | Catalogue d'outils MCP allowlistés (stateless) | — |

---

## Structure du dépôt

```
ticketflow/
├── pom.xml                          # Parent Maven (Spring Boot 4 + Spring Cloud BOM)
├── mvnw, mvnw.cmd, .mvn/           # Wrapper Maven (aucune installation Maven requise)
├── .env.example                     # Variables d'environnement (à copier en .env)
├── .gitignore
├── README.md
├── infra/
│   ├── docker-compose.yml           # Stack complète (Postgres + Kafka + Keycloak + 11 services)
│   └── init-scripts/
│       ├── postgres-init.sql        # Création des 7 bases de données
│       └── keycloak-realm.json      # Realm + 4 rôles + 3 utilisateurs de démo
├── config-repo/                     # YAML servis par le Config Server (10 fichiers)
├── docs/
│   └── CAHIER_DES_CHARGES.md        # Cahier des charges TicketFlow (source de vérité)
├── web-frontend/                    # Angular 19 standalone (squelette)
└── services/                        # 11 modules Maven
    ├── config-server/
    ├── discovery-server/
    ├── api-gateway/
    ├── customer-service/
    ├── event-service/
    ├── booking-service/
    ├── payment-service/
    ├── ticket-service/
    ├── notification-service/
    ├── ai-assistant-service/
    └── mcp-tool-server/
```

Chaque service suit la même arborescence interne :

```
services/<service>/
├── pom.xml
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/ticketflow/<service>/
    │   │   ├── <Service>Application.java
    │   │   ├── config/        → SecurityConfig, ClockConfig, filtres Gateway (CorrelationId…)
    │   │   ├── controllers/   → REST endpoints
    │   │   ├── services/      → logique métier
    │   │   ├── repositories/  → Spring Data JPA
    │   │   ├── entities/      → JPA + enums
    │   │   ├── dto/           → records (request/response)
    │   │   ├── exceptions/    → ApiExceptionHandler + erreurs métier
    │   │   ├── security/      → JwtAuthFilter, etc.
    │   │   ├── audit/         → AuditAspect (cross-cutting)
    │   │   ├── clients/       → Feign clients
    │   │   └── kafka/         → producers + consumers
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/  → scripts Flyway (à venir, étape 2)
    └── test/java/...
```

---

## Prérequis

- **JDK 21** (Adoptium Temurin recommandé)
- **Docker 24+** et **Docker Compose v2+**
- **Node 20+** et **npm 10+** (pour le frontend Angular uniquement)

> Maven n'a **pas** besoin d'être installé : le wrapper `./mvnw` télécharge
> automatiquement la bonne version (3.9.9) au premier appel.

---

## Lancement local (sans Docker)

Pour itérer rapidement sur un service sans rebuild Docker à chaque changement :

```bash
# 1) Démarrer l'infrastructure transverse (Postgres, Kafka, Keycloak)
cd infra
docker-compose up -d postgres kafka keycloak

# 2) Démarrer config-server et discovery-server en local
./mvnw -pl services/config-server spring-boot:run
# (dans un autre terminal)
./mvnw -pl services/discovery-server spring-boot:run

# 3) Démarrer les services métier dont tu as besoin
./mvnw -pl services/customer-service spring-boot:run
./mvnw -pl services/event-service spring-boot:run
# ...
```

Vérifications :
- Eureka : http://localhost:8761 — tes services doivent apparaître.
- Config Server : http://localhost:8888/customer-service/default — YAML servi.
- Keycloak : http://localhost:9090 — admin/admin (realm `ticketflow` importé automatiquement).
- PostgreSQL : `localhost:5432`, user `ticketflow`, password `ticketflow`.

---

## Lancement complet (Docker Compose)

```bash
# 1) Copier .env.example en .env et ajuster les valeurs réelles
cp .env.example .env
#   → Renseigner TELEGRAM_BOT_TOKEN, LLM_API_KEY

# 2) Construire et démarrer la stack complète
cd infra
docker-compose --env-file ../.env up --build

# 3) Vérifier que tout est UP
docker-compose ps
#   → Tous les services doivent afficher "healthy" ou "started"

# 4) Pour arrêter
docker-compose down

# 5) Pour tout réinitialiser (y compris les bases)
docker-compose down -v
```

---

## Utilisateurs de démo (Keycloak)

3 utilisateurs pré-configurés dans le realm `ticketflow` :

| Username | Password | Rôle(s) |
|---|---|---|
| `demo-client` | `demo1234` | `ROLE_CLIENT`, `ROLE_VISITOR` |
| `demo-organizer` | `demo1234` | `ROLE_ORGANIZER`, `ROLE_VISITOR` |
| `demo-admin` | `demo1234` | `ROLE_ADMIN`, `ROLE_VISITOR` |

Endpoint Keycloak pour obtenir un token :
```
POST http://localhost:9090/realms/ticketflow/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&client_id=ticketflow-web&username=demo-client&password=demo1234
```

---

## Étapes du projet

- **Étape 0** — Analyse et plan d'adaptation
- **Étape 1** — Initialisation du projet (ce dépôt)
- **Étape 2** — Bases de données : entités JPA, enums, migrations Flyway
- **Étape 3a** — API REST des services existants (Customer, Event, Booking)
- **Étape 3b** — API REST des nouveaux services (Payment, Ticket, Notification, AI, MCP)
- **Étape 4** — Sécurité : Keycloak, JWT, rôles, filtres Spring Security
- **Étape 5** — Communication inter-services : OpenFeign + Resilience4J + Kafka
- **Étape 6** — API Gateway : routes, validation JWT, CORS, rate limiting
- **Étape 7** — Docker : Dockerfiles, docker-compose, healthchecks
- **Étape 8** — Tests : unitaires, intégration, checklist démo
- **Étape 9** — Documentation finale : README, diagrammes, justifications
