package festo.agents.centralised;

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
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            siteId = (String) args[0];
            machines = (String[]) args[1];
        }

        Logger.log("Monitor Agent Centralisé démarré pour le site: " + siteId);

// ✅ CORRECTION : Adapter selon l'architecture
        String agentName = getAID().getLocalName();
        if (agentName.contains("Composite")) {
            controllerAID = new AID("RLRA_Controller_Composite", AID.ISLOCALNAME);
        } else {
            controllerAID = new AID("RLRA_Controller_Central", AID.ISLOCALNAME);
        }
        addBehaviour(new TickerBehaviour(this, 5000) {
            protected void onTick() {
                simulateMonitoring();
            }
        });
    }

    private void simulateMonitoring() {
        for (String machineId : machines) {
            double random = Math.random();

            if (random < 0.1) { // 10% chance de panne
                String errorCode = "ERR" + (int)(Math.random() * 100);
                sendFailureAlert(machineId, errorCode);
            }

            if (random < 0.3) { // 30% chance de charge élevée
                double load = 70 + (Math.random() * 30);
                if (load > 85) {
                    sendLoadAlert(machineId, load);
                }
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
        Logger.log("Monitor Agent Centralisé " + siteId + " arrêté");
    }
}