package festo.agents.composite;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import java.util.Queue;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import festo.models.ReconfigurationPlan;
import festo.utils.Logger;

public class CompositeExecutorModule {

    private Agent agent;
    private Queue<ReconfigurationPlan> executionQueue;
    private int totalPlansExecuted;
    private int totalActionsExecuted;

    public CompositeExecutorModule(Agent agent) {
        this.agent = agent;
        this.executionQueue = new LinkedList<>();
        this.totalPlansExecuted = 0;
        this.totalActionsExecuted = 0;

        System.out.println("\n[EXÉCUTEUR COMPOSITE] Module initialisé");
        System.out.println("└─ Agent: " + agent.getAID().getName());
    }

    public void executePlan(ReconfigurationPlan plan) {
        executionQueue.add(plan);

        System.out.println("\n[EXÉCUTEUR] Plan reçu");
        System.out.println("├─ ID: " + plan.getPlanId());
        System.out.println("├─ Actions: " + plan.getActions().size());
        System.out.println("└─ File d'attente: " + executionQueue.size());

        Logger.log("[Composite] Plan ajouté: " + plan.getPlanId());
    }

    public TickerBehaviour getBehaviour() {
        return new TickerBehaviour(agent, 3000) {
            protected void onTick() {
                System.out.println("\n[CYCLE] Vérification file...");

                if (!executionQueue.isEmpty()) {
                    ReconfigurationPlan plan = executionQueue.poll();
                    executeCompositePlan(plan);
                } else {
                    System.out.println("└─ Aucun plan en attente");
                }
            }
        };
    }

    private void executeCompositePlan(ReconfigurationPlan plan) {
        totalPlansExecuted++;

        System.out.println("\n[EXÉCUTION] Démarrage");
        System.out.println("├─ Plan: " + plan.getPlanId());
        System.out.println("└─ Phases: " + plan.getActions().size());

        Logger.log("[Composite] Début exécution: " + plan.getPlanId());

        // Exécution des phases
        List<String> actions = plan.getActions();
        for (int i = 0; i < actions.size(); i++) {
            String action = actions.get(i);
            int phase = i + 1;

            System.out.println("\n  [PHASE " + phase + "/" + actions.size() + "]");
            System.out.println("  ├─ Action: " + action);

            executeAction(action, plan.getPlanId(), phase);
            totalActionsExecuted++;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("  └─ Interruption");
                break;
            }
        }

        System.out.println("\n[EXÉCUTION] Terminée");
        System.out.println("├─ Plan: " + plan.getPlanId());
        System.out.println("└─ Durée: " + actions.size() + " secondes");

        Logger.log("[Composite] Plan terminé: " + plan.getPlanId());

        // Afficher stats occasionnellement
        if (totalPlansExecuted % 3 == 0) {
            printStats();
        }
    }

    private void executeAction(String action, String planId, int phase) {
        System.out.println("  ├─ Analyse...");

        if (action.contains("DIAGNOSTIC")) {
            String[] parts = action.split(":");
            if (parts.length >= 3) {
                String machineId = parts[2];
                sendCommand(machineId, "RUN_DIAGNOSTIC", planId);
                System.out.println("  ├─ Diagnostic: " + machineId);
            }
        }
        else if (action.contains("BYPASS")) {
            String machineId = "M2";
            sendCommand(machineId, "ACTIVATE_BYPASS", planId);
            System.out.println("  ├─ Bypass: " + machineId);
        }
        else if (action.contains("OPTIMIZE")) {
            broadcastCommand("OPTIMIZE_PARAMETERS", planId);
            System.out.println("  ├─ Optimisation: Toutes machines");
        }
        else if (action.contains("REDISTRIBUTE")) {
            System.out.println("  ├─ Redistribution charge");
            redistributeLoad(planId);
        }

        System.out.println("  └─ Exécuté");
    }

    private void redistributeLoad(String planId) {
        String[] machines = {"M1", "M2", "M3", "M4"};
        for (String machine : machines) {
            sendCommand(machine, "ADJUST_LOAD", planId);
        }
        System.out.println("    ├─ Machines: " + String.join(", ", machines));
    }

    private void sendCommand(String machineId, String command, String planId) {
        try {
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            String agentName = "Machine_" + machineId + "_Composite";
            msg.addReceiver(new jade.core.AID(agentName, jade.core.AID.ISLOCALNAME));

            msg.setContent("COMPOSITE:" + command + ":PLAN=" + planId);
            msg.setOntology("Festo-Ontology");

            agent.send(msg);

            System.out.println("    ├─ Envoyé à: " + machineId);

        } catch (Exception e) {
            System.out.println("    ├─ Erreur: " + machineId + " - " + e.getMessage());
        }
    }

    private void broadcastCommand(String command, String planId) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);

        String[] machines = {"M1", "M2", "M3", "M4", "T1"};
        for (String machine : machines) {
            msg.addReceiver(new jade.core.AID("Machine_" + machine + "_Composite",
                    jade.core.AID.ISLOCALNAME));
        }

        msg.setContent("COMPOSITE:" + command + ":PLAN=" + planId);
        msg.setOntology("Festo-Ontology");
        agent.send(msg);

        System.out.println("    ├─ Broadcast: " + machines.length + " machines");
    }

    private void printStats() {
        System.out.println("\n[STATISTIQUES]");
        System.out.println("├─ Plans exécutés: " + totalPlansExecuted);
        System.out.println("├─ Actions: " + totalActionsExecuted);
        System.out.println("└─ File: " + executionQueue.size());
    }

    public void printFinalReport() {
        System.out.println("\n[RAPPORT FINAL - EXÉCUTEUR]");
        System.out.println("├─ Agent: " + agent.getAID().getName());
        System.out.println("├─ Total plans: " + totalPlansExecuted);
        System.out.println("├─ Total actions: " + totalActionsExecuted);
        System.out.println("└─ Terminé");
    }
}