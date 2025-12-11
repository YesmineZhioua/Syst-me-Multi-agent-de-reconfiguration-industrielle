package festo.agents.common;

import jade.core.Agent;
import festo.utils.Logger;

public class ProductAgent extends Agent {

    protected void setup() {
        String agentName = getAID().getLocalName();

        if (agentName.contains("Composite")) {
            Logger.log("Product Agent Composite démarré");
        } else {
            Logger.log("Product Agent Central démarré");
        }
    }

    protected void takeDown() {
        Logger.log("Product Agent arrêté");
    }
}