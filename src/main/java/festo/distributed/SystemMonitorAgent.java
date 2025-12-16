package festo.distributed;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import java.util.*;


//affiche l'état de toutes les machines en temps
public class SystemMonitorAgent extends Agent {

    private Map<String, MachineStatus> statusMap = new HashMap<>();
    private long lastUpdate = 0;  // Dernier affichage

    // Classe interne pour stocker les statuts
    static class MachineStatus {
        String machineId;
        double load = 0;
        int queueSize = 0;
        String site = "?";
        boolean operational = true;
        long timestamp = 0;

        @Override
        public String toString() {
            return String.format("%s(Site:%s, Load:%.1f%%, Queue:%d, %s)",
                    machineId, site, load, queueSize,
                    operational ? "✅" : "❌");
        }
    }

    protected void setup() {
        System.out.println("\n📊 ===== MONITEUR SYSTÈME DÉMARRÉ =====");
        System.out.println("👁️  Surveille l'état des machines distribuées");

        // 1️. RECEVOIR LES MESSAGES (BOUCLE INFINIE)
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    updateStatus(msg);

                    // Afficher le dashboard toutes les 5 secondes
                    long now = System.currentTimeMillis();
                    if (now - lastUpdate > 5000) {
                        displayDashboard();
                        lastUpdate = now;
                    }
                } else {
                    block();
                }
            }
        });

        // 2. S'abonner aux mises à jour
        subscribeToUpdates();

        // 3. Dashboard périodique
        addBehaviour(new jade.core.behaviours.TickerBehaviour(this, 5000) {
            protected void onTick() {
                cleanupOldEntries();
                displayDashboard();
            }
        });
    }

    /**
     * S'abonne aux mises à jour des machines
     */
    private void subscribeToUpdates() {
        // Envoyer une demande d'abonnement à toutes les machines
        ACLMessage subscribe = new ACLMessage(ACLMessage.SUBSCRIBE);
        subscribe.setContent("MONITOR_SUBSCRIBE");

        // Machines connues (vous pouvez les découvrir dynamiquement)
        String[] machines = {"M1", "M2", "M3", "M4"};
        for (String machine : machines) {
            subscribe.addReceiver(new jade.core.AID(machine, jade.core.AID.ISLOCALNAME));
        }

        send(subscribe);
        System.out.println("📝 Abonnement envoyé aux machines");
    }

    /**
     * Met à jour les statuts à partir d'un message
     */
    private void updateStatus(ACLMessage msg) {
        String sender = msg.getSender().getLocalName();
        String content = msg.getContent();

        if (content == null) return;

        // 1. RÉCUPÉRER OU CRÉER LE STATUT
        MachineStatus status = statusMap.get(sender);
        if (status == null) {
            status = new MachineStatus();
            status.machineId = sender;
            statusMap.put(sender, status);
        }

        status.timestamp = System.currentTimeMillis();

        // 2. PARSER LE MESSAGE
        if (content.startsWith("PING:") || content.startsWith("PONG:")) {
            try {
                String[] parts = content.split(":");
                // ["PING", "M1", "LOAD", "45.5", "QUEUE", "3", "SITE", "A"]
                for (int i = 0; i < parts.length; i++) {
                    switch (parts[i]) {
                        case "LOAD":
                            if (i + 1 < parts.length) {
                                // Convertir avec point décimal
                                String loadStr = parts[i + 1];
                                status.load = Double.parseDouble(loadStr.replace(",", "."));
                                System.out.println("🔍 " + sender + " load: " + loadStr + " → " + status.load);
                            }
                            break;
                        case "QUEUE":
                            if (i + 1 < parts.length) {
                                status.queueSize = Integer.parseInt(parts[i + 1]);
                            }
                            break;
                        case "SITE":
                            if (i + 1 < parts.length) {
                                status.site = parts[i + 1];
                            }
                            break;
                        case "STATUS":
                            if (i + 1 < parts.length) {
                                status.operational = Boolean.parseBoolean(parts[i + 1]);
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Erreur parsing [" + sender + "]: " + content + " - " + e.getMessage());
            }
        }

    }
    /**
     * Message : "PING:M1:LOAD:45.5:QUEUE:3:SITE:A"
     *
     * Après parsing :
     * status.machineId = "M1"
     * status.load = 45.5
     * status.queueSize = 3
     * status.site = "A"
     * status.operational = true*/



    /**
     * Nettoie les entrées trop anciennes
     */
    private void cleanupOldEntries() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, MachineStatus>> it = statusMap.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, MachineStatus> entry = it.next();
            if (now - entry.getValue().timestamp > 15000) { // 15 secondes
                System.out.println("🧹 Nettoyage: " + entry.getKey() + " (inactif)");
                it.remove();
            }
        }
    }

    /**
     * Affiche le tableau de bord
     */
    private void displayDashboard() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 TABLEAU DE BORD DISTRIBUÉ - " +
                new Date().toString().substring(11, 19));
        System.out.println("=".repeat(60));
        System.out.printf("%-8s | %-6s | %-5s | %-8s | %-12s | %s%n",
                "Machine", "Site", "Charge", "File", "État", "Dernière MAJ");
        System.out.println("-".repeat(60));

        List<String> machineIds = new ArrayList<>(statusMap.keySet());
        Collections.sort(machineIds);

        for (String machineId : machineIds) {
            MachineStatus s = statusMap.get(machineId);

            // Calculer le temps depuis dernière mise à jour
            long secondsAgo = (System.currentTimeMillis() - s.timestamp) / 1000;
            String timeAgo = secondsAgo + "s";
            if (secondsAgo > 60) {
                timeAgo = (secondsAgo / 60) + "m";
            }

            String statusIcon = s.operational ? "✅" : "🔴";
            String loadColor = s.load > 80 ? "🟠" : s.load > 60 ? "🟡" : "🟢";

            System.out.printf("%-8s | %-6s | %s%5.1f%% | %-8d | %-12s | %-8s%n",
                    machineId,
                    s.site,
                    loadColor,
                    s.load,
                    s.queueSize,
                    statusIcon + (s.operational ? " Actif" : " Panne"),
                    timeAgo + " ago");
        }

        // Statistiques résumées
        if (!statusMap.isEmpty()) {
            System.out.println("-".repeat(60));
            double avgLoad = statusMap.values().stream()
                    .mapToDouble(s -> s.load)
                    .average()
                    .orElse(0);

            long activeMachines = statusMap.values().stream()
                    .filter(s -> s.operational)
                    .count();

            System.out.printf("📈 Machines actives: %d/%d | Charge moyenne: %.1f%%%n",
                    activeMachines, statusMap.size(), avgLoad);
        }

        System.out.println("=".repeat(60));
    }

    protected void takeDown() {
        System.out.println("\n🛑 Moniteur système arrêté");
        displayDashboard();
    }
}