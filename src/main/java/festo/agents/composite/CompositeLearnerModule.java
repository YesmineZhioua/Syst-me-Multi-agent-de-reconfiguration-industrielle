package festo.agents.composite;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import java.util.Queue;
import java.util.LinkedList;
import festo.models.ReconfigurationRequest;
import festo.models.CompositePlan;
import festo.utils.Logger;

public class CompositeLearnerModule {
    private Agent agent;
    private CompositeExecutorModule executorModule;
    private Queue<ReconfigurationRequest> requestQueue;
    private int requestsProcessed;

    public CompositeLearnerModule(Agent agent) {
        this.agent = agent;
        this.requestQueue = new LinkedList<>();
        this.requestsProcessed = 0;

        System.out.println("\n[LEARNER COMPOSITE] Module initialisé");
        System.out.println("└─ Agent: " + agent.getAID().getName());
    }

    public void setExecutorModule(CompositeExecutorModule executorModule) {
        this.executorModule = executorModule;
        System.out.println("[LEARNER] Executeur connecté");
    }

    public void addRequest(ReconfigurationRequest request) {
        requestQueue.add(request);

        System.out.println("\n[LEARNER] Demande reçue");
        System.out.println("├─ ID: " + request.getRequestId());
        System.out.println("├─ Type: " + request.getScenarioType());
        System.out.println("└─ File: " + requestQueue.size());

        Logger.log("[Composite] Demande ajoutée: " + request.getRequestId());
    }

    public TickerBehaviour getBehaviour() {
        return new TickerBehaviour(agent, 2000) {
            protected void onTick() {
                if (!requestQueue.isEmpty()) {
                    ReconfigurationRequest request = requestQueue.poll();
                    processRequest(request);
                }
            }
        };
    }

    private void processRequest(ReconfigurationRequest request) {
        requestsProcessed++;

        System.out.println("\n[LEARNER] Traitement demande");
        System.out.println("├─ ID: " + request.getRequestId());
        System.out.println("├─ Scénario: " + request.getScenarioType());
        System.out.println("└─ Déclencheur: " + request.getTrigger());

        Logger.log("[Composite] Traitement: " + request.getRequestId());

        CompositePlan plan = calculateCompositePlan(request);

        if (plan != null) {
            System.out.println("  ├─ Plan généré: " + plan.getPlanId());
            System.out.println("  ├─ Stratégie: " + plan.getStrategy());
            System.out.println("  └─ Actions: " + plan.getActions().size());

            if (executorModule != null) {
                executorModule.executePlan(plan);
                System.out.println("    └─ Transmis à l'exécuteur");
            } else {
                System.out.println("    └─ Erreur: Executeur non disponible");
            }
        }
    }

    private CompositePlan calculateCompositePlan(ReconfigurationRequest request) {
        String scenario = request.getScenarioType();
        String planId = "COMP_" + System.currentTimeMillis();

        System.out.println("\n  [CALCUL] Création plan composite");
        System.out.println("  ├─ ID Plan: " + planId);
        System.out.println("  ├─ Scénario: " + scenario);

        CompositePlan plan = new CompositePlan();
        plan.setPlanId(planId);

        if (scenario.equals("FAILURE")) {
            String[] parts = request.getTrigger().split(":");
            String machineId = parts.length > 1 ? parts[1] : "INCONNU";

            plan.setStrategy("CONTOURNEMENT_ANALYSE");

            System.out.println("  ├─ Machine: " + machineId);
            System.out.println("  ├─ Stratégie: Contournement avec analyse");

            plan.addAction("PHASE1:DIAGNOSTIC:" + machineId);
            plan.addAction("PHASE2:ANALYSE_IMPACT");
            plan.addAction("PHASE3:SELECTION_ROUTE");
            plan.addAction("PHASE4:IMPLEMENTATION");

            System.out.println("  └─ 4 phases créées");

        } else if (scenario.equals("HIGH_LOAD")) {
            plan.setStrategy("EQUILIBRAGE_CHARGE");

            System.out.println("  ├─ Stratégie: Équilibrage de charge");

            plan.addAction("PHASE1:ANALYSE_CHARGE");
            plan.addAction("PHASE2:REDISTRIBUTION");
            plan.addAction("PHASE3:OPTIMISATION");

            System.out.println("  └─ 3 phases créées");

        } else {
            plan.setStrategy("STRATEGIE_PAR_DEFAUT");
            plan.addAction("PHASE1:EVALUATION");
            plan.addAction("PHASE2:ADAPTATION");

            System.out.println("  └─ 2 phases par défaut");
        }

        return plan;
    }

    public void printStats() {
        System.out.println("\n[STATS LEARNER]");
        System.out.println("├─ Demandes traitées: " + requestsProcessed);
        System.out.println("├─ En attente: " + requestQueue.size());
        System.out.println("└─ Executeur: " + (executorModule != null ? "Connecté" : "Absent"));
    }
}