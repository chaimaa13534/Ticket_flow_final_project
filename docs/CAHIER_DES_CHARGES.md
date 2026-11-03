# TICKETFLOW — CAHIER DES CHARGES
### Fonctionnel, technique et opérationnel
---

> Document source de vérité pour toute l implémentation. Toute décision d architecture doit s y conformer ou être explicitement documentée comme dérogation motivée.

---

## 1. Objet, vision et objectifs
TicketFlow est une plateforme numérique de billetterie qui permet de publier et découvrir des événements, de
gérer les comptes clients, de réserver des places, de traiter un paiement selon le mode retenu, d’émettre des
billets et d’informer les utilisateurs. Le projet ajoute une expérience conversationnelle intégrée à ces mêmes
services métier, accessible notamment depuis Telegram.
Le présent cahier des charges définit une cible cohérente à l’échelle du produit complet. Il ne limite pas la portée
aux composants, aux étapes ou aux fonctionnalités actuellement présents dans un dépôt. L’architecture distribuée
organise les responsabilités par domaine et maintient les services métier comme sources d’autorité.
Résultats attendus
•   Une architecture de microservices Spring Boot/Spring Cloud avec API Gateway, découverte Eureka,
    configuration centralisée, resillience aux pannes, OpenFeign et Kafka.
•   Un assistant conversationnel relié à TicketFlow par des outils strictement contrôlés, avec Telegram comme
    canal externe et un fournisseur LLM cloud interchangeable.
•   Une expérience Web responsive pour les visiteurs, les clients et les responsables de catalogue.
•   Une livraison reproductible sous Docker, testable sans secrets ni fournisseur commercial obligatoire.
•   Une séparation nette entre les données et politiques métier, l’orchestration agentique et les fournisseurs
    externes.
2. Périmètre produit et acteurs
2.1 Inclus dans la cible
•   Catalogue public d’événements, recherche, filtres, détails, localisation, dates, catégories et informations
    tarifaires.
•   Administration du catalogue, gestion des lieux, capacités, publication et fermeture d’événements.
•   Comptes et profils clients, authentification, autorisation par rôle et gestion de session.
•   Réservation, contrôle des disponibilités, expiration des réservations temporaires, confirmation, annulation
    selon politique, suivi d’état.
•   Notifications transactionnelles et informatives, avec gestion des échecs et des doublons.
•   Application Web Angular pour les parcours clients et les fonctions administratives.
•   Assistant agentique dans TicketFlow, connecté aux API via un serveur d’outils MCP et accessible depuis
    Telegram.
•   Services transverses : API Gateway, Eureka, Config Server, Kafka, OpenFeign, sécurité, observabilité et
    Docker.
•   Optionnel : Paiement simulé pour les environnements de démonstration et architecture d’adaptateur vers un
    prestataire externe; émission et consultation de billets.
2.2 Hors périmètre de cette spécification
•   Revente de billets entre particuliers, enchères ou places de marché secondaires.
•   Gestion complète d’une billetterie physique ou d’un matériel de contrôle d’accès, sauf extension ultérieure
    explicitement cadrée.
•   Stockage ou traitement direct des numéros de carte bancaire par TicketFlow.
•   Entraînement d’un modèle sur les conversations des utilisateurs.
•   Dépendance à un fournisseur précis de LLM, de paiement, d’e-mail ou d’hébergement avant arbitrage.
2.3 Acteurs et responsabilités
Acteur                      Besoins et droits principaux
                            Consulter et rechercher les événements publics, afficher les détails et commencer un
Visiteur
                            parcours de création de compte.
                            Gérer son profil, réserver, payer, consulter ses commandes et billets, demander une
Client authentifié
                            annulation selon les règles applicables.
Organisateur /
                            Créer et administrer les événements qui lui sont attribués, gérer tarifs et capacités, publier
gestionnaire
                            ou clôturer selon ses droits.
d’événements
Administrateur
                            Administrer les rôles, paramètres opérationnels et référentiels transverses
plateforme
                            Poser des questions publiques; accéder aux informations personnelles uniquement après
Utilisateur Telegram
                            liaison vérifiée du compte et contrôle d’accès.
                            Telegram, fournisseur LLM, paiement et e-mail exécutent uniquement les fonctions
Fournisseurs externes
                            prévues par leurs adaptateurs et contrats.
3. Parcours métier de bout en bout
3.1 Découverte
Le visiteur ouvre le catalogue Angular, recherche par mots-clés, date, catégorie, lieu et fourchette de prix, puis
consulte une fiche d’événement à partir des informations publiées.
3.2 Création de compte et accès
Le client s’inscrit ou se connecte via le fournisseur d’identité. Les écrans privés exigent une session valide; les
rôles d’organisateur et d’administration sont contrôlés côté serveur.
3.3 Réservation
Le client choisit une quantité et, le cas échéant, une catégorie de billet. Le service de réservation vérifie
l’éligibilité et la capacité de façon atomique, calcule un prix figé et place la demande dans l’état d’attente de
paiement pendant une durée limitée.
3.4 Paiement et confirmation
Le paiement simulé valide les scénarios de réussite, d’échec et d’annulation. Dans un environnement relié à un
prestataire, le service paiement traite les retours par webhook vérifié. Une réservation n’est confirmée qu’après
un état de paiement admissible.
3.5 Émission et accès au billet
À la confirmation, un billet unique est émis et rattaché à la commande. Le client peut le consulter dans son
espace et recevoir une notification. Le code de billet ne doit pas exposer de données personnelles.
3.6 Administration d’un événement
Un utilisateur autorisé crée ou modifie un brouillon, renseigne dates, lieu, capacité et tarifs, vérifie les données
puis publie. Toute modification incompatible avec des ventes en cours suit une règle métier explicite et laisse
une trace d’audit.
3.7 Assistance conversationnelle
Le client interroge l’assistant depuis Telegram. L’agent comprend l’intention, obtient des données par les outils
MCP autorisés, synthétise une réponse dans la langue de l’échange et signale clairement toute indisponibilité.
Les demandes personnelles déclenchent le contrôle d’identité avant l’accès.
3.8 Notification et suivi
Les événements métier publiés sur Kafka alimentent les notifications. Un échec de canal n’annule ni une
réservation ni un paiement; les tentatives sont contrôlées et observables.
4. Exigences fonctionnelles
Les exigences ci-dessous décrivent le périmètre fonctionnel du produit. Les priorités indiquent l’ordre de
livraison recommandé et ne restreignent pas la cible globale.
ID        Fonction              Exigence                                                                    Priorité
                                Le système affiche une liste paginée des événements publiés avec titre,
       Catalogue
FR-001                          résumé, date, lieu, catégorie, tarif affichable et état de disponibilité P1
       public
                                pertinent.
          Recherche        et Le visiteur peut combiner recherche textuelle, période, ville/lieu, catégorie
FR-002                                                                                                      P1
          filtres             et critères de prix; les filtres sont validés et paginés.
                                La fiche présente description, organisateur public, horaires, lieu,
       Fiche
FR-003                          modalités, catégories de billets et disponibilité; aucun brouillon n’est P1
       événement
                                exposé publiquement.
          Gestion               Les rôles autorisés créent, modifient, publient, ferment ou archivent un
FR-004                                                                                                   P1
          événementielle        événement selon son état et les réservations associées.
                                Le client peut s’inscrire, s’authentifier, consulter/modifier les champs de
       Comptes             et
FR-005                          profil autorisés et demander la fermeture de compte selon les règles de P1
       profil
                                conservation.
ID       Fonction            Exigence                                                                    Priorité
                             Les fonctions client, organisateur, support et administration sont protégées
         Rôles          et
FR-006                       par des autorisations vérifiées côté API, sans faire confiance au seul P1
         permissions
                             contrôle Angular.
                             Le client choisit événement, catégorie et quantité; le système vérifie
FR-007 Réservation           disponibilité, limite par commande et règles de vente avant de créer la P1
                             réservation.
                             La réservation de capacité est atomique face à la concurrence; le stock ne
         Gestion
FR-008                       peut pas devenir négatif et les réservations temporaires expirées restituent P1
         d’inventaire
                             la capacité.
                             Un adaptateur de paiement permet le mode simulé et l’intégration
FR-009 Paiement              ultérieure d’un prestataire; les callbacks sont authentifiés, idempotents et P2
                             corrélés à la commande.
                             La confirmation survient une seule fois après validation de paiement; le
FR-010 Confirmation                                                                                   P1
                             client reçoit un état explicite et un identifiant de commande stable.
                             Le système émet un ou plusieurs billets uniques associés aux lignes
FR-011 Billets
                             confirmées, avec code de contrôle non prédictible et vérification de statut. P1
                      Le système applique les règles d’annulation et de remboursement
       Annulation  et
FR-012                configurées, expose le résultat et interdit les transitions incompatibles avec P2
       remboursement
                      l’état de commande.
                             Le client consulte ses réservations, états de paiement, billets, dates et
FR-013 Espace client                                                                                   P1
                             consignes; les données d’un autre compte ne sont jamais accessibles.
                             Le client peut recevoir des confirmations, rappels et changements
FR-014 Notifications         importants via les canaux configurés; les préférences et désabonnements P1
                             sont respectés.
         Assistant           L’assistant répond aux questions de découverte et de détail d’événements
FR-015                                                                                                P1
         événementiel        à partir de données publiques renvoyées par les API métier.
                             Après liaison du compte Telegram et authentification, l’assistant peut
         Assistant
FR-016                       consulter les seules informations de réservation du client lié, selon les P2
         personnel
                             scopes autorisés.
                             La liaison est initiée depuis une session TicketFlow authentifiée, via code
         Liaison
FR-017                       unique à durée courte, à usage unique et révocable; Telegram seul ne P2
         Telegram
                             prouve jamais l’identité métier.
         Limites d’action L’agent ne confirme pas de réservation, paiement, annulation ou
FR-018                    changement au seul motif d’une demande en langage naturel; toute future P1
         agentique
                          commande agentique exige un flux métier contrôlé et une confirmation
ID       Fonction         Exigence                                                                      Priorité
                          explicite distincte.
                          Le bot reçoit les webhooks validés, déduplique les mises à jour, gère les
       Échanges
FR-019                    messages trop longs ou non pris en charge et envoie une réponse ou un         P1
       Telegram
                          repli contrôlé.
                          Les opérations privilégiées sont journalisées avec acteur, action,
         Administration
FR-020                    ressource, horodatage et corrélation, sans enregistrer de secrets ni de       P1
         et audit
                          contenu superflu.
                          Les opérateurs disposent d’indicateurs de santé, disponibilité des
FR-021 Santé et support   dépendances et erreurs; les endpoints de diagnostic ne révèlent pas de        P1
                          données sensibles.
                          Les parcours principaux sont utilisables sur mobile et bureau, avec textes
       Langue et
FR-022                    localisables, navigation clavier et exigences d’accessibilité définies pour   P2
       accessibilité
                          les interfaces.
                          Les gestionnaires autorisés consultent des synthèses de ventes,
FR-023 Rapports métier    réservations et capacité selon leur périmètre; les chiffres dérivent des      P2
                          services sources et sont horodatés.
                          Si une mémoire conversationnelle temporaire est activée, le client peut
       Réinitialisation
FR-024                    réinitialiser ou supprimer son contexte selon la politique de conservation    P2
       du contexte
                          communiquée.
5. Architecture cible complète
L’architecture sépare les canaux, le routage, les domaines métier, l’orchestration agentique et les services transverses. Les composants métier conservent leur autonomie; les appels
synchrones passent par des contrats REST et les événements de domaine alimentent les traitements asynchrones.
         Figure 1 - Vue logique cible de TicketFlow. Les flèches pleines représentent les échanges métier; les pointillés, les fonctions transverses ou d’exploitation.
6.1 Composants et responsabilités
Composant              Responsabilité cible                                              Technologies / frontière
                       Interface visiteur, espace client et console de gestion selon
Angular Web            les rôles; consomme uniquement des API publiques exposées         HTTPS/JSON, OIDC
                       par le Gateway.
                       Point d’entrée externe; routes explicites, validation du jeton,
API Gateway            limitation de débit, CORS, corrélation et filtrage des routes     Spring Cloud Gateway
                       internes.
                       Registre de services et résolution des instances pour les
Discovery Server                                                                         Eureka
                       appels internes; services enregistrés et contrôlés.
                       Configuration non secrète, profils d’environnement, valeurs
Config Server          de délais et de fonctionnalités; aucune clé privée ou jeton en    Spring Cloud Config
                       clair.
                       Comptes métier, profil et préférences; applique les règles        Spring Boot, API REST, base
Customer Service
                       d’accès et de confidentialité.                                    propriétaire
                       Événements, lieux, catégories, calendrier, tarifs publiables,     Spring Boot, API REST, base
Event Service
                       statut et capacité de catalogue.                                  propriétaire
                       Commandes/réservations, vérification d’inventaire, durée de       Spring Boot, OpenFeign et
Booking Service
                       retenue, transitions et idempotence.                              événements Kafka
                       Simulation des paiements et façade vers un prestataire futur;     Adaptateur fournisseur, webhooks
Payment Service
                       validation des callbacks et rapprochement.                        sécurisés
                       Émission, consultation et contrôle des billets uniques après
Ticket Service                                                                           API REST / événements métier
                       confirmation.
                       Envoi d’e-mails et messages via adaptateurs; reprises,
Notification Service                                                                     Kafka + adaptateurs de canal
                       préférences et déduplication.
                       Adaptateur Telegram, orchestration de l’agent, gestion de
                                                                                         Spring Boot ou runtime compatible,
AI Assistant Service   contexte à durée limitée, client MCP, adaptateur LLM et
                                                                                         APIs internes
                       politiques de réponse.
                       Catalogue versionné d’outils, validation des entrées,
                                                                                         MCP Streamable HTTP, réseau
MCP Tool Server        authentification de service, autorisation et appels API métier
                                                                                         privé
                       minimaux.
                       Transport asynchrone des événements métier entre domaines;
                                                                                         Topics séparés par
Kafka                  événements versionnés, consommateurs idempotents et file
                                                                                         domaine/événement
                       d’échec.
                       Authentification des clients et opérateurs, émission et
OIDC / Keycloak                                                                          OIDC/JWT
                       validation de jetons, rôles et scopes.
                       Construction d’images reproductibles et lancement local
Docker / Compose       coordonné des services, dépendances et jeux de                    Dockerfiles, Compose, profils
                       démonstration.
6.2 Flux principaux
•   Web : Angular → API Gateway → service métier concerné; les clients n’appellent ni Eureka ni les services
    internes directement.
•   Communication synchrone : OpenFeign invoque les contrats REST internes lorsque la réponse immédiate
    est nécessaire (par exemple contrôle de capacité ou validation du client); délais, retries et circuit breakers
    sont explicites.
•   Communication événementielle : les services publient des événements métier sur Kafka pour les
    notifications et suites de traitements; Kafka n’est pas un substitut aux validations synchrones indispensables.
•   Telegram : Telegram Bot API → webhook HTTPS dédié via Gateway → AI Assistant Service; le service
    valide la signature/token Telegram, déduplique l’update, puis répond via l’API Bot en sortie.
•   Agent : AI Assistant Service → LLM cloud et, si nécessaire, MCP Tool Server. L’appel outil est proposé
    par le modèle mais évalué et exécuté par l’application selon une allowlist.
•   Données : le MCP Tool Server appelle les API métier internes; le LLM ne reçoit aucun accès réseau direct
    aux services, aux bases, à Kafka ou à Eureka.
7. Données, API et intégrations
7.1 Domaines de données
Domaine           Entités essentielles                                   Responsable de la donnée
Client            Customer, profil, préférences, liaison de canal        Customer Service
                  Event, Venue, Category, TicketTier, calendrier, prix
Catalogue                                                                Event Service
                  public
                  Booking, BookingLine, réservation temporaire,
Réservation                                                              Booking Service
                  snapshot prix, idempotency key
                  PaymentAttempt, statut, référence externe,
Paiement                                                                 Payment Service
                  remboursement
Billetterie       Ticket, code vérifiable, statut d’usage                Ticket Service
Notification      NotificationRequest, canal, tentatives, résultat       Notification Service
                  Lien Telegram, identifiant opaque de conversation,     AI Assistant Service; contenu brut non
Agent
                  TTL de session, audit technique des outils             persistant par défaut
Chaque service possède ses données et migrations. Les jointures directes entre bases de services et les écritures
croisées sont interdites; les vues de synthèse passent par API ou par projection d’événements explicitement
conçue.
7.2 Conventions API
•   API REST/JSON versionnées sous un préfixe stable, documentées par OpenAPI; schémas DTO séparés des
    entités de persistance.
•   Pagination cohérente, filtres autorisés, tri borné, limites de taille et erreurs métier structurées.
•   Authentification OIDC/JWT; scopes/roles vérifiés par endpoint
•   Codes HTTP cohérents : 2xx pour succès, 4xx pour entrée/droit/état invalide, 5xx pour défaillance; détails
    d’erreur sans stack trace ni secret.
•   OpenFeign consomme uniquement les URL/résolutions internes déclarées; configuration de timeouts, circuit
    breakers et reprise limitée.
API représentative                 Opération                   Contrôles à garantir
                                   Rechercher le               Publication, filtre, pagination bornée, aucune donnée
GET /api/v1/events
                                   catalogue public            privée.
                                   Consulter un                404 ou réponse publique si non publié; pas de fuite de
GET /api/v1/events/{id}
                                   événement public            brouillon.
                                                               JWT client, idempotence, disponibilité atomique,
POST /api/v1/bookings              Créer une réservation
                                                               limite de quantité et total figé.
                                                               Identité issue du jeton; jamais de customerId fourni
GET /api/v1/me/bookings            Lister mes réservations
                                                               comme autorité.
                               Simuler un paiement
POST                                                           Accessible seulement selon profil de démo; interdit
                               en environnement
/api/v1/payments/{id}/simulate                                 en production réelle.
                               autorisé
                                                               Validation du secret Telegram, anti-rejeu, limite de
POST /webhooks/telegram            Recevoir un update bot
                                                               taille et déduplication.
                                   Interroger événements
                                                               Client MCP authentifié; outils allowlistés; schéma
MCP tools                          / réservations
                                                               strict; résultat structuré minimal.
                                   autorisées
7.3 Kafka et événements de domaine
•   Événements indicatifs : EventPublished, EventClosed, BookingCreated, BookingExpired,
    BookingConfirmed, PaymentSucceeded, PaymentFailed, TicketIssued, NotificationRequested.
•   Chaque message possède un identifiant unique, un type/version de schéma, un horodatage UTC, un
    identifiant d’agrégat et une corrélation; les données personnelles sont minimisées.
7.4 Outils MCP agentiques
Outil logique             Entrées et sortie                               Autorisation et règle
search_public_events      Filtres facultatifs; liste bornée de résumés    Accessible au visiteur; résultats limités aux
Outil logique             Entrées et sortie                            Autorisation et règle
                          publics et dates/prix renvoyés par Event     événements publiés.
                          Service.
                          Identifiant d’événement; détail public       Même règle de publication; aucune donnée
get_public_event
                          structuré.                                   interne ni brouillon.
                          Contexte authentifié dérivé du jeton lié;    Seulement après liaison Telegram vérifiée;
get_my_bookings
                          résumé des propres réservations.             aucun customerId contrôlé par le modèle.
                          Référence de réservation validée et          Vérification propriétaire côté API à chaque
get_booking_status
                          contrôlée par le service propriétaire.       appel; filtrage des champs personnels.
8. Exigences techniques
8.1 Stack et organisation logicielle
•   Backend : Java 21 et Spring Boot/Spring Cloud; versions alignées par BOM et qualifiées en CI. Chaque
    service a une responsabilité de domaine, configuration d’exécution et tests propres.
•   Gateway : Spring Cloud Gateway, ne pas exposer automatiquement tout service découvert.
•   Communication interne : OpenFeign pour les appels synchrones; résilience (timeouts, circuit breaker,
    reprises bornées) et contrat REST documenté.
•   Asynchronisme : Kafka pour événements de domaine et intégrations différées.
•   Frontend : Angular avec composants accessibles, séparation des vues publiques et privées, gestion explicite
    des états de chargement/erreur, appels centralisés vers le Gateway.
•   Déploiement local : Dockerfiles multi-étapes si pertinent, Docker Compose, health checks, volumes non
    sensibles, réseau interne et profils permettant de simuler les fournisseurs externes.
•   Stockage : base relationnelle par domaine stricte avec propriété claire, transactions/migrations versionnées et
    sauvegarde configurée. PostgreSQL est la cible de référence recommandée; le choix précis et le mode de
    persistance local sont à confirmer.
8.2 Configuration, secrets et environnements
•   Config Server distribue des paramètres non sensibles (URLs internes, délais, limites, profils, indicateurs de
    fonctionnalité) par environnement.
•   Les clés d’API LLM, jetons Telegram, identifiants de paiement, mots de passe et certificats sont fournis au
    runtime depuis un gestionnaire de secrets ou le mécanisme de secrets de la plateforme. [Optionel, sinon
    utiliser le ficher .env]
9. Livraison, déploiement et exploitation
9.1 Livrables du projet
•   Applications et services : code, contrats API, modèles de données et configuration d’exécution.
•   Frontend Angular : parcours visiteur, client et administrateur/organisateur selon le périmètre retenu.
•   Microservices : Gateway, découverte, configuration, domaines métier, notification et services agentiques.
•   Infrastructure : Dockerfiles, Docker Compose, configuration Kafka, bases, health checks, secrets
    d’exemple fictifs et scripts de démonstration.
•   Documentation : architecture, diagrammes, contrats, instructions d’installation, configuration, opérations,
    sécurité, limites et perspectives.
•   Démonstration reproductible : scénarios catalogue/réservation/paiement simulé/billet et interaction
    Telegram avec données synthétiques.
