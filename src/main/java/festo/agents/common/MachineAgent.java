package festo.agents.common;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import festo.utils.Logger;

public class MachineAgent extends Agent {

    private String machineId;
    private String machineType;
    private String site;
    private boolean operational;

    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 3) {
            machineId = (String) args[0];
            machineType = (String) args[1];
            site = (String) args[2];
        }

        operational = true;

        String agentName = getAID().getLocalName();
        if (agentName.contains("Central")) {
            Logger.log("Machine Agent Central démarré: " + machineId);
        } else if (agentName.contains("Composite")) {
            Logger.log("Machine Agent Composite démarré: " + machineId);
        } else {
            Logger.log("Machine Agent démarré: " + machineId);
        }

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    processCommand(msg);
                } else {
                    block();
                }
            }
        });

        addBehaviour(new TickerBehaviour(this, 3000) {
            protected void onTick() {
                if (operational) {
                    double random = Math.random();

                    if (random < 0.05) {
                        operational = false;
                        Logger.log("Machine " + machineId + " en panne!");
                    } else {
                        Logger.log("Machine " + machineId + " fonctionne normalement");
                    }
                }
            }
        });
    }

    private void processCommand(ACLMessage msg) {
        String content = msg.getContent();
        String sender = msg.getSender().getLocalName();

        Logger.log("Commande reçue de " + sender + ": " + content);

        if (content.contains("BYPASS")) {
            executeBypass(content);
        } else if (content.contains("OPTIMIZE")) {
            executeOptimization(content);
        } else if (content.contains("COMPOSITE")) {
            executeCompositeCommand(content);
        }

        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.CONFIRM);
        reply.setContent("Commande exécutée: " + content);
        send(reply);
    }

    private void executeBypass(String command) {
        Logger.log("Exécution bypass: " + command);
    }

    private void executeOptimization(String command) {
        Logger.log("Exécution optimisation: " + command);
    }

    private void executeCompositeCommand(String command) {
        Logger.log("Exécution commande composite: " + command);

        if (command.contains("RUN_DIAGNOSTIC")) {
            Logger.log("Diagnostic en cours pour " + machineId);
        } else if (command.contains("ACTIVATE_COMPOSITE_BYPASS")) {
            Logger.log("Bypass composite activé pour " + machineId);
        }
    }

    protected void takeDown() {
        Logger.log("Machine Agent " + machineId + " arrêté");
    }
}