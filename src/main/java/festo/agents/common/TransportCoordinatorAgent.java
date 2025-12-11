package festo.agents.common;

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

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.CONFIRM);
        reply.setContent("TRANSPORT_ACCEPTED");
        send(reply);
    }

    private void optimizeTransportFlow() {
        String agentName = getAID().getLocalName();

        if (agentName.contains("Composite")) {
            Logger.log("[Composite Transport] Optimisation avancée du flux...");
            Logger.log("[Composite Transport] Analyse multi-critères en cours");
            Logger.log("[Composite Transport] Utilisation: " + currentLoad + "/" + MAX_CAPACITY);
        } else {
            Logger.log("Optimisation du flux de transport...");

            if (currentLoad > MAX_CAPACITY * 0.8) {
                Logger.log("Charge élevée détectée - Activation mode rapide");
            } else if (currentLoad < MAX_CAPACITY * 0.3) {
                Logger.log("Charge faible - Économie d'énergie activée");
            }

            Logger.log("Utilisation transport: " + currentLoad + "/" + MAX_CAPACITY);
        }
    }

    protected void takeDown() {
        Logger.log("Transport Coordinator Agent arrêté");
    }
}