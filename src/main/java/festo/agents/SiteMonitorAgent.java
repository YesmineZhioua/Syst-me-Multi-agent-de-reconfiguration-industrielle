package festo.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import festo.utils.Logger;

public class SiteMonitorAgent extends Agent {

    private String siteId;
    private String[] machines;
    private AID controllerAID;

    protected void setup() {
        // Récupérer les arguments
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            siteId = (String) args[0];
            machines = (String[]) args[1];
        }

        Logger.log("Monitor Agent démarré pour le site: " + siteId);

        // Chercher le contrôleur
        controllerAID = new AID("RLRA_Controller", AID.ISLOCALNAME);

        // Ajouter un comportement de surveillance
        addBehaviour(new TickerBehaviour(this, 5000) { // Toutes les 5 secondes
            protected void onTick() {
                simulateMonitoring();
            }
        });
    }

    private void simulateMonitoring() {
        // Simuler une surveillance aléatoire
        double random = Math.random();

        if (random < 0.2) { // 20% de chance de détecter une panne
            String randomMachine = machines[(int)(Math.random() * machines.length)];
            String errorCode = "ERR" + (int)(Math.random() * 100);

            sendFailureAlert(randomMachine, errorCode);
        }

        // Simuler une charge élevée
        if (random < 0.3) { // 30% de chance
            String randomMachine = machines[(int)(Math.random() * machines.length)];
            double load = 70 + (Math.random() * 30); // 70-100%

            if (load > 85) {
                sendLoadAlert(randomMachine, load);
            }
        }
    }

    private void sendFailureAlert(String machineId, String errorCode) {
        ACLMessage alert = new ACLMessage(ACLMessage.INFORM);
        alert.addReceiver(controllerAID);
        alert.setContent("FAILURE:" + machineId + ":" + errorCode);
        send(alert);

        Logger.log("Alerte panne envoyée: " + machineId + " - " + errorCode);
    }

    private void sendLoadAlert(String machineId, double load) {
        ACLMessage alert = new ACLMessage(ACLMessage.INFORM);
        alert.addReceiver(controllerAID);
        alert.setContent("HIGH_LOAD:" + machineId + ":" + load);
        send(alert);

        Logger.log("Alerte charge envoyée: " + machineId + " - " + load + "%");
    }

    protected void takeDown() {
        Logger.log("Monitor Agent " + siteId + " arrêté");
    }
}