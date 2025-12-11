package festo.agents.centralised;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import festo.utils.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RLRAControllerAgent extends Agent {

    // Constantes d'affichage
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";

    // Compteurs de performance
    private int messagesProcessed = 0;
    private int failuresHandled = 0;
    private int reconfigurationsSent = 0;
    private long startTime;

    protected void setup() {
        startTime = System.currentTimeMillis();
        printAgentHeader();

        Logger.log("Agent Contrôleur RLRA Centralisé démarré: " + getAID().getName());

        // Configuration initiale
        System.out.println(CYAN + "\n[CONFIGURATION] Initialisation du contrôleur central..." + RESET);
        printConfigurationPanel();

        // Comportement principal avec template pour filtrer les messages
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                // Utiliser un template pour filtrer les messages importants
                MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = receive(template);

                if (msg != null) {
                    processMessage(msg);
                } else {
                    block();
                }
            }
        });

        System.out.println(GREEN + "\n✓ Comportement principal activé - En attente de messages..." + RESET);
        printStatusBar();
    }

    private void printAgentHeader() {
        System.out.println(BLUE + "\n╔══════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BLUE + "║" + BOLD + PURPLE + "           CONTROLEUR RLRA CENTRALISÉ - AGENT JADE         " + BLUE + "║" + RESET);
        System.out.println(BLUE + "╠══════════════════════════════════════════════════════════════╣" + RESET);
        System.out.printf(BLUE + "║" + RESET + "  Nom: %-50s " + BLUE + "║\n", getAID().getName());
        System.out.printf(BLUE + "║" + RESET + "  Heure: %-47s " + BLUE + "║\n", getCurrentTime());
        System.out.println(BLUE + "╚══════════════════════════════════════════════════════════════╝" + RESET);
    }

    private void printConfigurationPanel() {
        System.out.println("\n" + CYAN + "[PANEL CONFIGURATION]" + RESET);
        System.out.println(BLUE + "┌────────────────────────────────────────────────────────────┐" + RESET);
        System.out.println(BLUE + "│" + RESET + "  " + BOLD + "RÔLES & RESPONSABILITÉS:" + RESET + "                                   " + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + RESET + "  • Supervision globale du système              " + BLUE + "             │" + RESET);
        System.out.println(BLUE + "│" + RESET + "  • Prise de décision centralisée               " + BLUE + "             │" + RESET);
        System.out.println(BLUE + "│" + RESET + "  • Gestion des reconfigurations                " + BLUE + "             │" + RESET);
        System.out.println(BLUE + "├────────────────────────────────────────────────────────────┤" + RESET);
        System.out.println(BLUE + "│" + RESET + "  " + BOLD + "MESSAGES TRACTÉS:" + RESET + "                                         " + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + RESET + "  ✓ FAILURE    - Pannes d'équipements           " + BLUE + "             │" + RESET);
        System.out.println(BLUE + "│" + RESET + "  ✓ HIGH_LOAD  - Surcharges machines           " + BLUE + "             │" + RESET);
        System.out.println(BLUE + "│" + RESET + "  ✓ REQUEST    - Demandes de service           " + BLUE + "             │" + RESET);
        System.out.println(BLUE + "└────────────────────────────────────────────────────────────┘" + RESET);
    }

    private void processMessage(ACLMessage msg) {
        messagesProcessed++;
        String sender = msg.getSender().getLocalName();
        String content = msg.getContent();

        printMessageHeader(sender, msg.getPerformative(), content);

        Logger.log("Message reçu de " + sender + ": " + content);

        if (content.startsWith("FAILURE")) {
            handleFailure(sender, content);
        } else if (content.startsWith("HIGH_LOAD")) {
            handleHighLoad(sender, content);
        } else if (content.startsWith("REQUEST")) {
            handleRequest(sender, content);
        } else {
            handleUnknownMessage(content);
        }

        updateStatusBar();
    }

    private void printMessageHeader(String sender, int performative, String content) {
        String perfText = getPerformativeText(performative);
        System.out.println("\n" + CYAN + "═".repeat(70) + RESET);
        System.out.println(BOLD + "📨 NOUVEAU MESSAGE REÇU" + RESET);
        System.out.println(CYAN + "─".repeat(70) + RESET);
        System.out.printf("  Expéditeur:  %s%s%s\n", YELLOW, sender, RESET);
        System.out.printf("  Type:        %s%s%s\n", BLUE, perfText, RESET);
        System.out.printf("  Contenu:     %s\n", content);
        System.out.println(CYAN + "─".repeat(70) + RESET);
    }

    private void handleFailure(String sender, String content) {
        failuresHandled++;
        System.out.println(RED + "\n🚨 DÉTECTION DE PANNE - PROCESSUS DE TRAITEMENT" + RESET);

        // Étape 1: Analyse de la panne
        System.out.println("\n" + BOLD + "ÉTAPE 1: ANALYSE DE LA PANNE" + RESET);
        System.out.println(BLUE + "┌────────────────────────────────────────────────────────────┐" + RESET);
        String[] parts = content.split(":");
        String machineId = parts[1];
        String errorType = parts.length > 2 ? parts[2] : "TYPE_INCONNU";
        System.out.printf(BLUE + "│" + RESET + "  Machine:    %s%-10s%s\n", RED, machineId, RESET);
        System.out.printf(BLUE + "│" + RESET + "  Type erreur: %s\n", errorType);
        System.out.printf(BLUE + "│" + RESET + "  Priorité:    %sHAUTE%s\n", RED, RESET);
        System.out.println(BLUE + "└────────────────────────────────────────────────────────────┘" + RESET);

        // Étape 2: Envoi accusé réception
        System.out.println("\n" + BOLD + "ÉTAPE 2: ACCUSÉ DE RÉCEPTION" + RESET);
        ACLMessage reply = new ACLMessage(ACLMessage.CONFIRM);
        reply.addReceiver(getAID());
        String ackMsg = "PANNE_TRAITEE:" + machineId + ":" + getCurrentTime();
        reply.setContent(ackMsg);
        send(reply);
        System.out.println("  " + GREEN + "✓ Accusé envoyé à " + sender + RESET);

        // Étape 3: Calcul du plan de reconfiguration
        System.out.println("\n" + BOLD + "ÉTAPE 3: CALCUL DU PLAN DE RECONFIGURATION" + RESET);
        System.out.println("  " + CYAN + "🔍 Recherche de solutions alternatives..." + RESET);
        System.out.println("  " + CYAN + "📊 Analyse de la charge des machines voisines..." + RESET);

        String plan = calculateReconfigurationPlan(machineId, errorType);

        // Étape 4: Envoi du plan
        sendReconfigurationPlan(plan);
        reconfigurationsSent++;

        printFailureSummary(machineId, errorType);
    }

    private void handleHighLoad(String sender, String content) {
        System.out.println(YELLOW + "\n⚠ ALERTE CHARGE ÉLEVÉE" + RESET);

        String[] parts = content.split(":");
        String machineId = parts[1];
        double load = Double.parseDouble(parts[2]);

        System.out.println("\n" + BOLD + "DIAGNOSTIC:" + RESET);
        System.out.println(BLUE + "┌────────────────────────────────────────────────────────────┐" + RESET);
        System.out.printf(BLUE + "│" + RESET + "  Machine:          %s\n", machineId);
        System.out.printf(BLUE + "│" + RESET + "  Charge actuelle:  %s%.1f%%%s\n",
                load > 90 ? RED : YELLOW, load, RESET);
        System.out.printf(BLUE + "│" + RESET + "  Seuil critique:   90%%\n");
        System.out.printf(BLUE + "│" + RESET + "  État:            %s\n",
                load > 90 ? RED + "CRITIQUE" + RESET : YELLOW + "SURVEILLANCE" + RESET);
        System.out.println(BLUE + "└────────────────────────────────────────────────────────────┘" + RESET);

        if (load > 90) {
            String plan = "REDUCE_LOAD:" + machineId + ":PRIORITY_HIGH:ACTION_IMMEDIATE";
            sendReconfigurationPlan(plan);
            System.out.println(YELLOW + "  → Plan de réduction de charge activé" + RESET);
        } else if (load > 75) {
            System.out.println(GREEN + "  → Charge élevée mais acceptable, monitoring continu" + RESET);
        } else {
            System.out.println(GREEN + "  → Charge normale, aucune action requise" + RESET);
        }
    }

    private void handleRequest(String sender, String content) {
        System.out.println(BLUE + "\n📋 DEMANDE DE SERVICE" + RESET);

        System.out.println("\n" + BOLD + "TRAITEMENT DE LA DEMANDE:" + RESET);
        System.out.println(BLUE + "┌────────────────────────────────────────────────────────────┐" + RESET);
        System.out.printf(BLUE + "│" + RESET + "  Expéditeur: %s\n", sender);
        System.out.printf(BLUE + "│" + RESET + "  Demande:    %s\n", content);
        System.out.println(BLUE + "│" + RESET + "  Statut:     " + GREEN + "EN TRAITEMENT" + RESET);
        System.out.println(BLUE + "└────────────────────────────────────────────────────────────┘" + RESET);

        Logger.log("Demande traitée: " + content);
    }

    private void handleUnknownMessage(String content) {
        System.out.println(YELLOW + "\n❓ MESSAGE NON RECONNU" + RESET);
        System.out.println("  Type de message non supporté: " + content.substring(0, Math.min(20, content.length())));
        System.out.println("  " + YELLOW + "⚠ Consigné pour analyse future" + RESET);
    }

    private String calculateReconfigurationPlan(String machineId, String errorType) {
        // Simulation d'un algorithme de décision
        System.out.println("\n" + CYAN + "[ALGORITHME DE DÉCISION]" + RESET);
        System.out.println(BLUE + "┌────────────────────────────────────────────────────────────┐" + RESET);
        System.out.println(BLUE + "│" + RESET + "  " + BOLD + "Critères d'analyse:" + RESET + "                                       " + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + RESET + "  • Disponibilité machines voisines            " + BLUE + "             │" + RESET);
        System.out.println(BLUE + "│" + RESET + "  • Charge de travail actuelle                  " + BLUE + "             │" + RESET);
        System.out.println(BLUE + "│" + RESET + "  • Priorité des opérations                     " + BLUE + "             │" + RESET);
        System.out.println(BLUE + "│" + RESET + "  • Coût de reconfiguration                     " + BLUE + "             │" + RESET);
        System.out.println(BLUE + "└────────────────────────────────────────────────────────────┘" + RESET);

        // Plan basé sur le type d'erreur
        String plan;
        switch(errorType.toUpperCase()) {
            case "MECHANICAL":
                plan = "BYPASS:" + machineId + ":ROUTE_TO_M2:PROTOCOL_SAFE";
                break;
            case "ELECTRICAL":
                plan = "SHUTDOWN:" + machineId + ":ISOLATE:ALERT_MAINTENANCE";
                break;
            case "SOFTWARE":
                plan = "RESTART:" + machineId + ":SAFE_MODE:DIAGNOSTIC";
                break;
            default:
                plan = "BYPASS:" + machineId + ":STANDARD_PROTOCOL";
        }

        return plan;
    }

    private void sendReconfigurationPlan(String plan) {
        System.out.println(PURPLE + "\n🚀 ENVOI DU PLAN DE RECONFIGURATION" + RESET);

        System.out.println("\n" + BOLD + "📋 PLAN DÉTAILLÉ:" + RESET);
        System.out.println(GREEN + "┌────────────────────────────────────────────────────────────┐" + RESET);
        System.out.printf(GREEN + "│" + RESET + "  ID Plan:    RLRA-RECONF-%d\n", System.currentTimeMillis() % 10000);
        System.out.printf(GREEN + "│" + RESET + "  Heure:      %s\n", getCurrentTime());
        System.out.printf(GREEN + "│" + RESET + "  Plan:       %s\n", plan);
        System.out.println(GREEN + "├────────────────────────────────────────────────────────────┤" + RESET);
        System.out.println(GREEN + "│" + RESET + "  " + BOLD + "Actions à exécuter:" + RESET + "                                     " + GREEN + "│" + RESET);

        String[] actions = plan.split(":");
        for (int i = 0; i < actions.length; i++) {
            System.out.printf(GREEN + "│" + RESET + "  %d. %s\n", i + 1, formatAction(actions[i]));
        }

        System.out.println(GREEN + "├────────────────────────────────────────────────────────────┤" + RESET);
        System.out.println(GREEN + "│" + RESET + "  " + BOLD + "Statut:       " + YELLOW + "EN COURS D'EXÉCUTION" + RESET + "               " + GREEN + "│" + RESET);
        System.out.println(GREEN + "│" + RESET + "  " + BOLD + "Priorité:     " + RED + "HAUTE" + RESET + "                                " + GREEN + "│" + RESET);
        System.out.println(GREEN + "└────────────────────────────────────────────────────────────┘" + RESET);

        Logger.log("Envoi du plan de reconfiguration: " + plan);
    }

    private void printFailureSummary(String machineId, String errorType) {
        System.out.println("\n" + CYAN + "═".repeat(70) + RESET);
        System.out.println(BOLD + "📊 RÉSUMÉ DE L'INTERVENTION" + RESET);
        System.out.println(CYAN + "─".repeat(70) + RESET);
        System.out.printf("  Machine affectée:  %s%s%s\n", RED, machineId, RESET);
        System.out.printf("  Type de panne:     %s\n", errorType);
        System.out.printf("  Heure détection:   %s\n", getCurrentTime());
        System.out.printf("  Temps réponse:     %d ms\n", System.currentTimeMillis() - startTime);
        System.out.printf("  Statut:            %sPANNE RÉSOLUE%s\n", GREEN, RESET);
        System.out.println(CYAN + "─".repeat(70) + RESET);
    }

    private void printStatusBar() {
        long uptime = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("\n" + CYAN + "═".repeat(70) + RESET);
        System.out.println(BOLD + "📈 STATISTIQUES DU CONTRÔLEUR" + RESET);
        System.out.println(CYAN + "─".repeat(70) + RESET);
        System.out.printf("  Messages traités:      %s%d%s\n", BLUE, messagesProcessed, RESET);
        System.out.printf("  Pannes gérées:         %s%d%s\n", BLUE, failuresHandled, RESET);
        System.out.printf("  Reconfigurations:      %s%d%s\n", BLUE, reconfigurationsSent, RESET);
        System.out.printf("  Temps de fonctionnement: %s%d secondes%s\n", GREEN, uptime, RESET);
        System.out.printf("  Statut système:        %s● OPÉRATIONNEL%s\n", GREEN, RESET);
        System.out.println(CYAN + "═".repeat(70) + RESET);
    }

    private void updateStatusBar() {
        // Mettre à jour périodiquement l'affichage des stats
        if (messagesProcessed % 5 == 0) {
            printStatusBar();
        }
    }

    // Méthodes utilitaires
    private String getPerformativeText(int performative) {
        switch(performative) {
            case ACLMessage.INFORM: return "INFORM";
            case ACLMessage.REQUEST: return "REQUEST";
            case ACLMessage.CONFIRM: return "CONFIRM";
            case ACLMessage.FAILURE: return "FAILURE";
            default: return "UNKNOWN";
        }
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private String formatAction(String action) {
        switch(action.toUpperCase()) {
            case "BYPASS": return "Contourner la machine défectueuse";
            case "SHUTDOWN": return "Arrêt sécurisé de l'équipement";
            case "RESTART": return "Redémarrage de la machine";
            case "REDUCE_LOAD": return "Réduction de la charge de travail";
            case "ROUTE_TO_M2": return "Redirection vers machine M2";
            default: return action;
        }
    }

    protected void takeDown() {
        System.out.println("\n" + RED + "═".repeat(70) + RESET);
        System.out.println(BOLD + "🛑 ARRÊT DU CONTRÔLEUR RLRA CENTRALISÉ" + RESET);
        System.out.println(RED + "─".repeat(70) + RESET);
        System.out.println("  Durée de fonctionnement: " + getFormattedUptime());
        System.out.println("  Messages traités au total: " + messagesProcessed);
        System.out.println("  Interventions réalisées: " + failuresHandled);
        System.out.println(RED + "═".repeat(70) + RESET);

        Logger.log("Agent Contrôleur Centralisé arrêté");
    }

    private String getFormattedUptime() {
        long uptime = (System.currentTimeMillis() - startTime) / 1000;
        long hours = uptime / 3600;
        long minutes = (uptime % 3600) / 60;
        long seconds = uptime % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}