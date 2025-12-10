package festo.agents;

        import jade.core.Agent;
        import jade.core.behaviours.CyclicBehaviour;
        import jade.core.behaviours.TickerBehaviour;
        import jade.lang.acl.ACLMessage;
        import festo.utils.Logger;

public class TransportCoordinatorAgent extends Agent {

    private boolean transportActive;
    private int currentLoad;
    private final int MAX_CAPACITY = 20;

    protected void setup() {
        Logger.log("Transport Coordinator Agent démarré: " + getAID().getName());

        transportActive = true;
        currentLoad = 0;

        // Comportement pour recevoir les demandes de transport
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    processTransportRequest(msg);
                } else {
                    block();
                }
            }
        });

        // Surveillance du système de transport
        addBehaviour(new TickerBehaviour(this, 3000) {
            protected void onTick() {
                simulateTransportOperation();
            }
        });

        // Optimisation du flux
        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {
                optimizeTransportFlow();
            }
        });
    }

    private void processTransportRequest(ACLMessage msg) {
        String content = msg.getContent();
        String sender = msg.getSender().getLocalName();

        Logger.log("Demande transport reçue de " + sender + ": " + content);

        if (content.startsWith("TRANSPORT_REQUEST")) {
            // Format: TRANSPORT_REQUEST:FROM:TO:PRODUCT_ID:PRIORITY
            String[] parts = content.split(":");
            if (parts.length >= 5) {
                String from = parts[1];
                String to = parts[2];
                String productId = parts[3];
                String priority = parts[4];

                boolean accepted = handleTransportRequest(from, to, productId, priority);

                // Répondre à la demande
                ACLMessage reply = msg.createReply();
                if (accepted) {
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.setContent("TRANSPORT_ACCEPTED:" + productId + ":ESTIMATED_TIME:5s");
                    Logger.log("Transport accepté pour " + productId + " de " + from + " à " + to);
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("TRANSPORT_REFUSED:" + productId + ":CAPACITY_FULL");
                    Logger.log("Transport refusé pour " + productId + " (capacité pleine)");
                }
                send(reply);
            }
        }
        else if (content.startsWith("TRANSPORT_STATUS")) {
            // Mise à jour du statut
            updateTransportStatus(content);
        }
    }

    private boolean handleTransportRequest(String from, String to, String productId, String priority) {
        // Vérifier la capacité
        if (currentLoad >= MAX_CAPACITY) {
            return false;
        }

        // Simuler le transport
        currentLoad++;

        // Logique de routage
        String route = calculateOptimalRoute(from, to);
        Logger.log("Produit " + productId + " routé via: " + route);

        // Simuler le temps de transport
        int transportTime = calculateTransportTime(from, to);
        Logger.log("Temps estimé: " + transportTime + " secondes");

        return true;
    }

    private String calculateOptimalRoute(String from, String to) {
        // Logique simple de routage
        if (from.equals("SiteA") && to.equals("SiteB")) {
            return "M2->T1->M3";
        } else if (from.equals("SiteB") && to.equals("SiteA")) {
            return "M4->T1->M1";
        } else {
            return "DIRECT";
        }
    }

    private int calculateTransportTime(String from, String to) {
        // Temps de transport basique
        if (from.equals("SiteA") && to.equals("SiteB")) {
            return 5; // 5 secondes
        } else if (from.equals("SiteB") && to.equals("SiteA")) {
            return 5; // 5 secondes
        } else {
            return 3; // 3 secondes
        }
    }

    private void updateTransportStatus(String content) {
        // Format: TRANSPORT_STATUS:LOAD:CAPACITY:STATE
        String[] parts = content.split(":");
        if (parts.length >= 4) {
            currentLoad = Integer.parseInt(parts[1]);
            String state = parts[3];

            Logger.log("Statut transport mis à jour - Charge: " + currentLoad + "/" + MAX_CAPACITY);
        }
    }

    private void simulateTransportOperation() {
        if (transportActive) {
            // Simuler le mouvement
            if (currentLoad > 0) {
                currentLoad--;
                Logger.log("Transport en cours - Charge restante: " + currentLoad);

                // Simuler des pannes occasionnelles
                if (Math.random() < 0.02) { // 2% de chance
                    transportActive = false;
                    Logger.log("SYSTÈME TRANSPORT EN PANNE!");

                    // Notifier le contrôleur
                    notifyTransportFailure();
                }
            }
        } else {
            // Tenter de réparer
            if (Math.random() < 0.1) { // 10% de chance de réparation
                transportActive = true;
                Logger.log("Système transport réparé et opérationnel");
            }
        }
    }

    private void notifyTransportFailure() {
        // Envoyer alerte au contrôleur RLRA
        ACLMessage alert = new ACLMessage(ACLMessage.INFORM);
        alert.addReceiver(new jade.core.AID("RLRA_Controller", jade.core.AID.ISLOCALNAME));
        alert.setContent("TRANSPORT_FAILURE:T1:SYSTEM_DOWN");
        send(alert);
    }

    private void optimizeTransportFlow() {
        Logger.log("Optimisation du flux de transport...");

        // Logique d'optimisation simple
        if (currentLoad > MAX_CAPACITY * 0.8) {
            Logger.log("Charge élevée détectée - Activation mode rapide");
            // Activer des stratégies pour augmenter la capacité
        } else if (currentLoad < MAX_CAPACITY * 0.3) {
            Logger.log("Charge faible - Économie d'énergie activée");
            // Activer des modes économes en énergie
        }

        // Calculer les métriques
        double utilization = (double) currentLoad / MAX_CAPACITY * 100;
        Logger.log("Utilisation transport: " + String.format("%.1f", utilization) + "%");
    }

    protected void takeDown() {
        Logger.log("Transport Coordinator Agent arrêté");
    }
}