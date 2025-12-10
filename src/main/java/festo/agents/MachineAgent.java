package festo.agents;

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
        // Récupérer les arguments
        Object[] args = getArguments();
        if (args != null && args.length >= 3) {
            machineId = (String) args[0];
            machineType = (String) args[1];
            site = (String) args[2];
        }

        operational = true;

        Logger.log("Machine Agent démarré: " + machineId +
                " (" + machineType + " - Site " + site + ")");

        // Ajouter un comportement pour recevoir les commandes
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

        // Simuler le fonctionnement
        simulateOperation();
    }

    private void simulateOperation() {
        addBehaviour(new TickerBehaviour(this, 3000) {
            protected void onTick() {
                if (operational) {
                    double random = Math.random();

                    if (random < 0.05) { // 5% de chance de panne
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

        if (content.contains("RECONFIGURE")) {
            executeReconfiguration(content);
        } else if (content.contains("STOP")) {
            stopMachine();
        } else if (content.contains("START")) {
            startMachine();
        }

        // Envoyer confirmation
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.CONFIRM);
        reply.setContent("Commande exécutée: " + content);
        send(reply);
    }

    private void executeReconfiguration(String command) {
        Logger.log("Exécution reconfiguration: " + command);

        if (command.contains("BYPASS")) {
            Logger.log("Mode bypass activé pour " + machineId);
        } else if (command.contains("CHANGE_PARAMS")) {
            Logger.log("Paramètres modifiés pour " + machineId);
        }
    }

    private void stopMachine() {
        operational = false;
        Logger.log("Machine " + machineId + " arrêtée");
    }

    private void startMachine() {
        operational = true;
        Logger.log("Machine " + machineId + " démarrée");
    }

    protected void takeDown() {
        Logger.log("Machine Agent " + machineId + " arrêté");
    }
}