package festo.distributed;


import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import festo.utils.Logger;
import java.util.*;

/**
 * Superviseur Global - Niveau 3
 * Vision globale du système et résolution des conflits inter-sites
 */
public class GlobalSupervisorAgent extends Agent {
    // Annuaire de TOUS les sites + status
   private Map<String, SiteStatus> sites;

    // File des conflits entre sites
    private Queue<InterSiteConflict> interSiteConflicts;

    // Ressources globales disponibles
// "ENERGY" → 10000 unités
// "TRANSPORT" → 100 camions
    private Map<String, GlobalResource> globalResources;

    // Historique de toutes les décisions prises
    private List<GlobalDecision> decisionHistory;
    // Métriques globales
    private double globalLoadAverage; // Charge moyenne de TOUT le système
    private int totalSystemFailures; // Nombre total de pannes dans TOUT le système
    private int conflictsResolved; // Nombre de conflits résolus

    private Map<String, Integer> sitePerformanceScores; // Score de performance de chaque site

    protected void setup() {
        sites = new HashMap<>();
        interSiteConflicts = new LinkedList<>();
        globalResources = new HashMap<>();
        decisionHistory = new ArrayList<>();
        sitePerformanceScores = new HashMap<>();

        totalSystemFailures = 0;
        conflictsResolved = 0;

        Logger.log("🌐 ===== SUPERVISEUR GLOBAL DÉMARRÉ =====");
        Logger.log("🌐 Vision globale du système activée");

        // Initialiser les ressources globales
        initializeGlobalResources();

        // Comportement 1 : Recevoir les messages (BOUCLE INFINIE)

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    handleMessage(msg);
                } else {
                    block();
                }
            }
        });

        // Comportement 2 : Résoudre conflits inter-sites (toutes les 4 secondes)
        addBehaviour(new TickerBehaviour(this, 4000) {
            protected void onTick() {
                resolveInterSiteConflicts();
            }
        });

        // Comportement 3 : Analyser et optimiser (toutes les 8 secondes)
        addBehaviour(new TickerBehaviour(this, 8000) {
            protected void onTick() {
                analyzeGlobalSystem();
                optimizeGlobalResources();
            }
        });

        // Comportement 4 : Planification stratégique (toutes les 15 secondes)
        addBehaviour(new TickerBehaviour(this, 15000) {
            protected void onTick() {
                performStrategicPlanning();
            }
        });

        // Comportement 5 : Générer rapport (toutes les 10 secondes)
        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {
                generateGlobalReport();
            }
        });
    }

    private void initializeGlobalResources() {
        // 1️. ÉNERGIE : 10000 unités disponibles
        globalResources.put("ENERGY", new GlobalResource("ENERGY", 10000, 10000));
        // 2️. TRANSPORT : 100 camions disponibles
        globalResources.put("TRANSPORT", new GlobalResource("TRANSPORT", 100, 100));
        // 3️. CAPACITÉ DE TRAITEMENT : 500 unités
        globalResources.put("PROCESSING", new GlobalResource("PROCESSING", 500, 500));

        Logger.log("📦 Ressources globales initialisées");
    }

    private void handleMessage(ACLMessage msg) {
        String content = msg.getContent();
        String sender = msg.getSender().getLocalName();

        if (content.startsWith("REGISTER_SITE:")) {
            handleSiteRegistration(msg);  // Un site s'enregistre
        } else if (content.startsWith("SITE_STATUS:")) {
            handleSiteStatusUpdate(msg); // Rapport d'un site
        } else if (content.startsWith("ESCALATION:")) {
            handleEscalation(msg); // Problème remonté par un site
        } else if (content.startsWith("INTER_SITE_RESOURCE:")) {
            handleInterSiteResourceRequest(msg); // Demande de ressource
        } else if (content.startsWith("CONFLICT_INTER_SITE:")) {
            handleInterSiteConflict(msg); // Conflit entre sites

        }
    }

    /**
     * Enregistrement des sites
     */
    private void handleSiteRegistration(ACLMessage msg) {
        String[] parts = msg.getContent().split(":");
        // "REGISTER_SITE:Paris" → ["REGISTER_SITE", "....."]
        String siteId = parts[1];

        // 2️. CRÉER UNE FICHE POUR LE SITE
        SiteStatus status = new SiteStatus(siteId);
        sites.put(siteId, status);
        // 3. DONNER UN SCORE INITIAL : 100/100
        sitePerformanceScores.put(siteId, 100);

        Logger.log("✅ Site enregistré: " + siteId);

        // Confirmer l'enregistrement
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.CONFIRM);
        reply.setContent("REGISTERED:" + siteId + ":SUPERVISOR_ACKNOWLEDGED");
        send(reply);
    }

    // Mise à jour du statut des sites
    private void handleSiteStatusUpdate(ACLMessage msg) {
        // Format: SITE_STATUS:SITE_ID:LOAD:X:MACHINES:Y:OPERATIONAL:Z:FAILURES:F:TASKS:T
        String[] parts = msg.getContent().split(":");

        if (parts.length >= 12) {
            String siteId = parts[1];
            double load = Double.parseDouble(parts[3].replace(",", "."));

            int totalMachines = Integer.parseInt(parts[5]);
            int operationalMachines = Integer.parseInt(parts[7]);
            int failures = Integer.parseInt(parts[9]);
            int tasks = Integer.parseInt(parts[11]);

             // les infos sur site
            SiteStatus status = sites.get(siteId);
            if (status != null) {
                status.load = load;
                status.totalMachines = totalMachines;
                status.operationalMachines = operationalMachines;
                status.failures = failures;
                status.tasksCompleted = tasks;
                status.lastUpdate = System.currentTimeMillis();

                // Mettre à jour le score de performance
                updateSitePerformanceScore(siteId, status);
            }
        }
    }

    // Calculer un score de performance pour chaque site (comme une note sur 100).
    private void updateSitePerformanceScore(String siteId, SiteStatus status) {
        // Calculer un score basé sur plusieurs facteurs
        // 1️. SCORE DE CHARGE (optimal = 60%)
        double loadScore = 100 - Math.abs(status.load - 60); // Optimal à 60%
        // 2️. SCORE DE DISPONIBILITÉ (machines qui marchent)
        double uptimeScore = (double) status.operationalMachines / status.totalMachines * 100;
        // 3️. SCORE DE PANNES (moins de pannes = mieux)
        double failureScore = Math.max(0, 100 - (status.failures * 20));
        // 4️. SCORE FINAL: 40% charge + 40% disponibilité + 20% pannes
        int finalScore = (int) ((loadScore * 0.4 + uptimeScore * 0.4 + failureScore * 0.2));
        sitePerformanceScores.put(siteId, finalScore);
    }

    // Gestion des escalades
    private void handleEscalation(ACLMessage msg) {
        String[] parts = msg.getContent().split(":");
        // "ESCALATION:Paris:FAILURE:M2:CRITICAL"

        String siteId = parts[1];
        String issue = parts[2]; // "FAILURE"
        String machineId = parts[3];
        String severity = parts[4]; // "CRITICAL"

        Logger.log("⚠️ ESCALATION reçue de " + siteId + ": " + issue +
                " (" + machineId + " - " + severity + ")");

        totalSystemFailures++;  // Compter les pannes globales

        // 2. Analyser et prendre une décision globale
        GlobalDecision decision = analyzeAndDecide(siteId, issue, machineId, severity);
        // 3️. EXÉCUTER LA DÉCISION
        executeGlobalDecision(decision);
    }

    // ANALYSER ET DÉCIDER
    private GlobalDecision analyzeAndDecide(String siteId, String issue,
                                            String machineId, String severity) {
        Logger.log("🧠 Analyse globale pour: " + issue);
        // Créer un objet décision
        GlobalDecision decision = new GlobalDecision();
        decision.siteId = siteId;
        decision.issue = issue;
        decision.machineId = machineId;
        decision.timestamp = System.currentTimeMillis();

        // ROUTER SELON LE TYPE DE PROBLÈME
        switch (issue) {
            case "FAILURE":
                decision = handleGlobalFailure(siteId, machineId, severity);
                break;
            case "NO_RESOURCES":
                decision = handleGlobalResourceShortage(siteId, machineId);
                break;
            case "SITE_OVERLOAD":
                decision = handleGlobalOverload(siteId);
                break;
            default:
                decision.action = "MONITOR";
                decision.description = "Continuer la surveillance";
        }

        decisionHistory.add(decision); // // Sauvegarder dans l'historique
        return decision;
    }

    private GlobalDecision handleGlobalFailure(String siteId, String machineId, String severity) {
        GlobalDecision decision = new GlobalDecision();
        decision.action = "REALLOCATE"; // Action par défaut

        //1. Trouver le meilleur site pour réallouer les tâches
        String bestAlternativeSite = findBestAlternativeSite(siteId);
        // Cherche un autre site qui a de la capacité
        if (bestAlternativeSite != null) {
            decision.targetSite = bestAlternativeSite;
            decision.description = "Réallocation inter-site: " + siteId + " → " + bestAlternativeSite;
            Logger.log("🔄 Décision: Réallouer vers " + bestAlternativeSite);
        } else {
            decision.action = "DEGRADED_MODE";
            decision.description = "Activation mode dégradé - Aucune alternative disponible";
            Logger.warn("⚠️ Mode dégradé activé - Système sous contrainte");
        }

        return decision;
    }

    //MANQUE DE RESSOURCES
    private GlobalDecision handleGlobalResourceShortage(String siteId, String machineId) {
        GlobalDecision decision = new GlobalDecision();
        decision.action = "RESOURCE_TRANSFER";

        // Trouver un site avec des ressources excédentaires
        String donorSite = findResourceDonorSite(siteId);  // Cherche un site avec charge < 50%

        if (donorSite != null) {
            //  SITE DONNEUR TROUVÉ
            decision.targetSite = donorSite;
            decision.description = "Transfert de ressources: " + donorSite + " → " + siteId;
            Logger.log("📦 Décision: Transfert de ressources depuis " + donorSite);

            // Orchestrer le transfert
            orchestrateResourceTransfer(donorSite, siteId, "PROCESSING", 50);
        } else {
            //  PAS DE DONNEUR
            decision.action = "OPTIMIZE_GLOBALLY";
            decision.description = "Optimisation globale des ressources existantes";
            Logger.log("🔧 Décision: Optimisation globale nécessaire");
        }

        return decision;
    }

    // SURCHARGE GLOBALE
    private GlobalDecision handleGlobalOverload(String siteId) {
        GlobalDecision decision = new GlobalDecision();
        decision.action = "LOAD_DISTRIBUTION";

        // 1️. TROUVER DES SITES DISPONIBLES
        List<String> availableSites = findAvailableSites(siteId);  // Cherche des sites avec charge < 70%

        if (availableSites.size() >= 2) {
            //  PLUSIEURS SITES DISPONIBLES
            decision.targetSites = availableSites;
            decision.description = "Distribution de charge sur " + availableSites.size() + " sites";
            Logger.log("⚖️ Décision: Distribution multi-sites");

            // Orchestrer la distribution
            distributeLoadAcrossSites(siteId, availableSites);
        } else {
            decision.action = "THROTTLE";
            decision.description = "Limitation temporaire des nouvelles tâches";
            Logger.log("🚦 Décision: Throttling activé");
        }

        return decision;
    }
    // EXÉCUTER LA DÉCISION
    private void executeGlobalDecision(GlobalDecision decision) {
        Logger.log("⚡ Exécution décision globale: " + decision.action);

        // Envoyer les directives aux coordinateurs de site concernés
        ACLMessage directive = new ACLMessage(ACLMessage.REQUEST);
        // 1️. SI SITE CIBLE SPÉCIFIQUE
        if (decision.targetSite != null) {
            // Envoyer au site source
            AID siteCoordinator = new AID("SiteCoordinator_" + decision.siteId, AID.ISLOCALNAME);
            directive.addReceiver(siteCoordinator);

            if (decision.targetSite != null && !decision.targetSite.equals(decision.siteId)) {
                // Envoyer au site cible
                AID targetCoordinator = new AID("SiteCoordinator_" + decision.targetSite, AID.ISLOCALNAME);
                directive.addReceiver(targetCoordinator);
            }
        } else {
            //2. sinon  Directive pour tous les sites
            for (String siteId : sites.keySet()) {
                AID coordinator = new AID("SiteCoordinator_" + siteId, AID.ISLOCALNAME);
                directive.addReceiver(coordinator);
            }
        }

        directive.setContent("SUPERVISOR_DIRECTIVE:" + decision.action + ":" +
                decision.machineId + ":" + decision.description);
        send(directive);

        conflictsResolved++; // Compter les conflits résolus
    }

    /**
     * Gestion des ressources inter-sites
     */
    private void handleInterSiteResourceRequest(ACLMessage msg) {
        // Format: INTER_SITE_RESOURCE:SITE_ID:MACHINE_ID:RESOURCE_TYPE:AMOUNT
        String[] parts = msg.getContent().split(":");
        String requestingSite = parts[1];
        String machineId = parts[2];
        String resourceType = parts[3];
        int amount = Integer.parseInt(parts[4]);

        Logger.log("📦 Demande ressource inter-site: " + resourceType + " x" + amount +
                " pour " + requestingSite);

        // Vérifier les ressources globales
        GlobalResource resource = globalResources.get(resourceType);

        if (resource != null && resource.available >= amount) {
            // Allouer depuis les ressources globales
            resource.available -= amount;

            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent("RESOURCE_GRANTED:" + resourceType + ":" + amount +
                    ":SOURCE:GLOBAL_POOL");
            send(reply);

            Logger.log("✅ Ressource allouée depuis le pool global");
        } else {
            // Chercher un site donneur
            String donorSite = findResourceDonorSite(requestingSite);

            if (donorSite != null) {
                orchestrateResourceTransfer(donorSite, requestingSite, resourceType, amount);
            } else {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("RESOURCE_UNAVAILABLE:" + resourceType);
                send(reply);

                Logger.warn("❌ Ressource non disponible dans le système");
            }
        }
    }

    private void orchestrateResourceTransfer(String fromSite, String toSite,
                                             String resourceType, int amount) {
        Logger.log("🔄 Orchestration transfert: " + fromSite + " → " + toSite);

        // Demander au site source
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        AID sourceCoordinator = new AID("SiteCoordinator_" + fromSite, AID.ISLOCALNAME);
        request.addReceiver(sourceCoordinator);
        request.setContent("TRANSFER_RESOURCE:" + resourceType + ":" + amount +
                ":TO:" + toSite);
        send(request);

        // Informer le site destination
        ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
        AID destCoordinator = new AID("SiteCoordinator_" + toSite, AID.ISLOCALNAME);
        inform.addReceiver(destCoordinator);
        inform.setContent("INCOMING_RESOURCE:" + resourceType + ":" + amount +
                ":FROM:" + fromSite);
        send(inform);
    }

    /**
     * Résolution des conflits inter-sites
     */
    private void resolveInterSiteConflicts() {
        if (interSiteConflicts.isEmpty()) {
            return;
        }

        Logger.log("🔍 Résolution de " + interSiteConflicts.size() + " conflits inter-sites");

        while (!interSiteConflicts.isEmpty()) {
            InterSiteConflict conflict = interSiteConflicts.poll();
            resolveConflict(conflict);
        }
    }

    private void handleInterSiteConflict(ACLMessage msg) {
        String[] parts = msg.getContent().split(":");
        InterSiteConflict conflict = new InterSiteConflict();
        conflict.type = parts[1];
        conflict.site1 = parts[2];
        conflict.site2 = parts[3];
        conflict.resource = parts.length > 4 ? parts[4] : "";

        interSiteConflicts.add(conflict);
        Logger.log("⚠️ Nouveau conflit inter-site: " + conflict.type +
                " entre " + conflict.site1 + " et " + conflict.site2);
    }

    private void resolveConflict(InterSiteConflict conflict) {
        Logger.log("⚖️ Résolution conflit: " + conflict.type);

        switch (conflict.type) {
            case "RESOURCE_CONTENTION":
                resolveResourceContention(conflict);
                break;
            case "PRIORITY_CONFLICT":
                resolvePriorityConflict(conflict);
                break;
            case "LOAD_IMBALANCE":
                resolveLoadImbalance(conflict);
                break;
        }

        conflictsResolved++;
    }

    private void resolveResourceContention(InterSiteConflict conflict) {
        // Politique: priorité au site avec le meilleur score de performance
        int score1 = sitePerformanceScores.getOrDefault(conflict.site1, 50);
        int score2 = sitePerformanceScores.getOrDefault(conflict.site2, 50);

        String prioritySite = score1 >= score2 ? conflict.site1 : conflict.site2;
        String otherSite = prioritySite.equals(conflict.site1) ? conflict.site2 : conflict.site1;

        Logger.log("🎯 Priorité donnée à " + prioritySite + " (score: " +
                Math.max(score1, score2) + ")");

        // Envoyer directives
        sendResourcePriorityDirective(prioritySite, otherSite, conflict.resource);
    }

    private void resolvePriorityConflict(InterSiteConflict conflict) {
        Logger.log("⭐ Résolution conflit de priorité entre " +
                conflict.site1 + " et " + conflict.site2);
        // Implémenter la logique de résolution
    }

    private void resolveLoadImbalance(InterSiteConflict conflict) {
        Logger.log("⚖️ Équilibrage de charge entre sites");
        distributeLoadAcrossSites(conflict.site1, Arrays.asList(conflict.site2));
    }

    private void sendResourcePriorityDirective(String prioritySite, String otherSite, String resource) {
        ACLMessage directive1 = new ACLMessage(ACLMessage.INFORM);
        directive1.addReceiver(new AID("SiteCoordinator_" + prioritySite, AID.ISLOCALNAME));
        directive1.setContent("RESOURCE_PRIORITY:HIGH:" + resource);
        send(directive1);

        ACLMessage directive2 = new ACLMessage(ACLMessage.INFORM);
        directive2.addReceiver(new AID("SiteCoordinator_" + otherSite, AID.ISLOCALNAME));
        directive2.setContent("RESOURCE_PRIORITY:LOW:" + resource + ":DEFER");
        send(directive2);
    }

    /**
     * Analyse et optimisation globale
     */
    private void analyzeGlobalSystem() {
        Logger.log("🔍 ===== ANALYSE GLOBALE DU SYSTÈME =====");

        // Calculer les métriques globales
        double totalLoad = 0;
        int totalMachines = 0;
        int totalOperational = 0;
        int totalFailures = 0;

        // Parcourir TOUS les sites
        for (SiteStatus status : sites.values()) {
            totalLoad += status.load * status.totalMachines;
            totalMachines += status.totalMachines;
            totalOperational += status.operationalMachines;
            totalFailures += status.failures;
        }

        // 2️. CALCULER LA CHARGE MOYENNE GLOBALE
        globalLoadAverage = totalMachines > 0 ? totalLoad / totalMachines : 0;

        Logger.log("📊 Charge globale moyenne: " + String.format("%.1f", globalLoadAverage) + "%");
        Logger.log("🖥️ Machines: " + totalOperational + "/" + totalMachines + " opérationnelles");
        Logger.log("⚠️ Pannes actives: " + totalFailures);
        Logger.log("✅ Conflits résolus: " + conflictsResolved);

        // Identifier les problèmes systémiques
        identifySystemicIssues();
    }

    private void identifySystemicIssues() {
        // 1️. SURCHARGE SYSTÈME ?
        if (globalLoadAverage > 85) {
            Logger.warn("⚠️ ALERTE: Surcharge système globale!");
            initiateGlobalLoadReduction();
        }

        // 2️. TROP DE PANNES ?
        if (totalSystemFailures > 5) {
            Logger.warn("⚠️ ALERTE: Taux de pannes élevé!");
            initiatePreventiveMaintenance();
        }

        // Vérifier l'équilibre entre sites
        checkInterSiteBalance();
    }

    //VÉRIFIER L'ÉQUILIBRE

    private void checkInterSiteBalance() {
        if (sites.size() < 2) return;

        double maxLoad = Double.MIN_VALUE;
        double minLoad = Double.MAX_VALUE;

        // Trouver la charge MAX et MIN
        for (SiteStatus status : sites.values()) {
            maxLoad = Math.max(maxLoad, status.load);
            minLoad = Math.min(minLoad, status.load);
        }

        double imbalance = maxLoad - minLoad;
        if (imbalance > 40) {
            Logger.warn("⚖️ Déséquilibre inter-sites détecté: " +
                    String.format("%.1f", imbalance) + "%");
            initiateGlobalRebalancing();
        }
    }

    private void optimizeGlobalResources() {
        Logger.log("🔧 Optimisation globale des ressources...");

        // Redistribuer les ressources globales
        generateGlobalReport();

        // Optimiser les flux inter-sites
        optimizeInterSiteFlows();
    }

    private void redistributeGlobalResources() {
        for (GlobalResource resource : globalResources.values()) {
            double utilizationRate = (double) (resource.total - resource.available) / resource.total * 100;

            if (utilizationRate > 90) {
                Logger.warn("📦 Ressource " + resource.type + " presque épuisée: " +
                        String.format("%.1f", utilizationRate) + "%");
            }
        }
    }

    private void optimizeInterSiteFlows() {
        // Analyser et optimiser les flux de transport entre sites
        Logger.log("🚛 Optimisation des flux inter-sites...");
    }

    /**
     * Planification stratégique
     */
    private void performStrategicPlanning() {
        Logger.log("🎯 ===== PLANIFICATION STRATÉGIQUE =====");
        // Prédire les besoins futurs
        predictFutureNeeds();
        // Planifier les reconfigurations
        planReconfigurations();
        // Optimiser la topologie du système
        optimizeSystemTopology();
    }

    //- PRÉDIRE L'AVENIR
    private void predictFutureNeeds() {
        // Analyser les tendances
        Logger.log("🔮 Prédiction des besoins basée sur l'historique");

        // Si la charge augmente constamment, prévoir l'activation de ressources
        if (globalLoadAverage > 70) {
            Logger.log("📈 Tendance à la hausse - Préparation de ressources additionnelles");
        }
    }

    private void planReconfigurations() {
        // Planifier des reconfigurations proactives
        Logger.log("🔄 Planification de reconfigurations proactives");
    }

    private void optimizeSystemTopology() {
        // Optimiser la structure globale du système
        Logger.log("🗺️ Analyse de la topologie du système");
    }

    /**
     * Fonctions utilitaires
     */
    private String findBestAlternativeSite(String excludeSite) {
        String bestSite = null;
        int bestScore = -1;

        for (Map.Entry<String, SiteStatus> entry : sites.entrySet()) {
            if (!entry.getKey().equals(excludeSite)) {
                SiteStatus status = entry.getValue();
                if (status.load < 70 && status.operationalMachines > 0) {
                    int score = sitePerformanceScores.getOrDefault(entry.getKey(), 0);
                    if (score > bestScore) {
                        bestScore = score;
                        bestSite = entry.getKey();
                    }
                }
            }
        }

        return bestSite;
    }

    private String findResourceDonorSite(String excludeSite) {
        // Trouver un site avec des ressources disponibles
        for (Map.Entry<String, SiteStatus> entry : sites.entrySet()) {
            if (!entry.getKey().equals(excludeSite)) {
                SiteStatus status = entry.getValue();
                if (status.load < 50) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private List<String> findAvailableSites(String excludeSite) {
        List<String> available = new ArrayList<>();
        for (Map.Entry<String, SiteStatus> entry : sites.entrySet()) {
            if (!entry.getKey().equals(excludeSite)) {
                SiteStatus status = entry.getValue();
                if (status.load < 70 && status.operationalMachines > 0) {
                    available.add(entry.getKey());
                }
            }
        }
        return available;
    }

    private void distributeLoadAcrossSites(String overloadedSite, List<String> targetSites) {
        Logger.log("🌐 Distribution de charge: " + overloadedSite + " → " + targetSites.size() + " sites");

        for (String targetSite : targetSites) {
            ACLMessage directive = new ACLMessage(ACLMessage.REQUEST);
            directive.addReceiver(new AID("SiteCoordinator_" + targetSite, AID.ISLOCALNAME));
            directive.setContent("ACCEPT_LOAD:FROM:" + overloadedSite + ":PRIORITY:HIGH");
            send(directive);
        }
    }

    private void initiateGlobalLoadReduction() {
        Logger.log("🚦 Initiation réduction charge globale");

        for (String siteId : sites.keySet()) {
            ACLMessage directive = new ACLMessage(ACLMessage.REQUEST);
            directive.addReceiver(new AID("SiteCoordinator_" + siteId, AID.ISLOCALNAME));
            directive.setContent("SUPERVISOR_DIRECTIVE:REDUCE_LOAD:THROTTLE_NEW_TASKS");
            send(directive);
        }
    }

    private void initiatePreventiveMaintenance() {
        Logger.log("🔧 Initiation maintenance préventive");
    }

    private void initiateGlobalRebalancing() {
        Logger.log("⚖️ Initiation rééquilibrage global");

        // Trouver le site le plus chargé et le moins chargé
        String mostLoaded = null;
        String leastLoaded = null;
        double maxLoad = Double.MIN_VALUE;
        double minLoad = Double.MAX_VALUE;

        for (Map.Entry<String, SiteStatus> entry : sites.entrySet()) {
            double load = entry.getValue().load;
            if (load > maxLoad) {
                maxLoad = load;
                mostLoaded = entry.getKey();
            }
            if (load < minLoad) {
                minLoad = load;
                leastLoaded = entry.getKey();
            }
        }

        if (mostLoaded != null && leastLoaded != null) {
            distributeLoadAcrossSites(mostLoaded, Arrays.asList(leastLoaded));
        }
    }

    /**
     * Reporting
     */
    private void generateGlobalReport() {
        Logger.log("📄 ===== RAPPORT GLOBAL =====");
        Logger.log("🌐 Sites actifs: " + sites.size());
        Logger.log("📊 Charge moyenne: " + String.format("%.1f", globalLoadAverage) + "%");
        Logger.log("✅ Conflits résolus: " + conflictsResolved);
        Logger.log("⚠️ Pannes totales: " + totalSystemFailures);

        Logger.log("🏆 Scores de performance par site:");
        for (Map.Entry<String, Integer> entry : sitePerformanceScores.entrySet()) {
            Logger.log("   " + entry.getKey() + ": " + entry.getValue() + "/100");
        }

        Logger.log("============================");
    }




    protected void takeDown() {
        Logger.log("🛑 ===== SUPERVISEUR GLOBAL ARRÊTÉ =====");
    }










    // Classes internes
    private static class SiteStatus {
        String siteId;
        double load;
        int totalMachines;
        int operationalMachines;
        int failures;
        int tasksCompleted;
        long lastUpdate;

        SiteStatus(String siteId) {
            this.siteId = siteId;
            this.load = 0;
            this.totalMachines = 0;
            this.operationalMachines = 0;
            this.failures = 0;
            this.tasksCompleted = 0;
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    private static class GlobalDecision {
        String siteId;
        String issue;
        String machineId;
        String action;
        String targetSite;
        List<String> targetSites;
        String description;
        long timestamp;
    }

    private static class InterSiteConflict {
        String type;
        String site1;
        String site2;
        String resource;
    }

    private static class GlobalResource {
        String type;
        int total;
        int available;

        GlobalResource(String type, int total, int available) {
            this.type = type;
            this.total = total;
            this.available = available;
        }
    }
}