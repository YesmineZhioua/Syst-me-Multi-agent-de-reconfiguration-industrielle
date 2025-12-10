package festo.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import festo.utils.Logger;

public class RLRAControllerAgent extends Agent {

    protected void setup() {
        Logger.log("Agent Contrôleur RLRA démarré: " + getAID().getName());

        // Ajouter un comportement pour recevoir les messages
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    processMessage(msg);
                } else {
                    block();
                }
            }
        });
    }

    private void processMessage(ACLMessage msg) {
        String sender = msg.getSender().getLocalName();
        String content = msg.getContent();

        Logger.log("Message reçu de " + sender + ": " + content);

        if (content.startsWith("FAILURE")) {
            handleFailure(sender, content);
        } else if (content.startsWith("REQUEST")) {
            handleRequest(sender, content);
        }
    }

    private void handleFailure(String sender, String content) {
        Logger.log("Traitement d'une panne: " + content);

        // Envoyer un accusé de réception
        ACLMessage reply = new ACLMessage(ACLMessage.CONFIRM);
        reply.addReceiver(getAID()); // À l'expéditeur
        reply.setContent("Panne reçue et en traitement: " + content);
        send(reply);

        // Simuler un traitement
        Logger.log("Calcul du plan de reconfiguration...");

        // Plan de reconfiguration simple
        String plan = "BYPASS:" + content.split(":")[1];

        // Envoyer le plan aux machines concernées
        sendReconfigurationPlan(plan);
    }

    private void sendReconfigurationPlan(String plan) {
        Logger.log("Envoi du plan de reconfiguration: " + plan);

        // Pour l'exemple, on l'affiche juste
        System.out.println("=== PLAN DE RECONFIGURATION ===");
        System.out.println("Plan: " + plan);
        System.out.println("Actions:");
        System.out.println("1. Mettre la machine en mode bypass");
        System.out.println("2. Rediriger le flux");
        System.out.println("3. Ajuster les paramètres");
        System.out.println("================================");
    }

    private void handleRequest(String sender, String content) {
        Logger.log("Traitement d'une demande: " + content);
    }

    protected void takeDown() {
        Logger.log("Agent Contrôleur arrêté");
    }
}