package festo.agents.composite;

import jade.core.Agent;
import jade.core.behaviours.ParallelBehaviour;
import festo.utils.Logger;

public class RLRACompositeAgent extends Agent {

    private CompositeMonitorModule monitorModule;
    private CompositeLearnerModule learnerModule;
    private CompositeExecutorModule executorModule;

    protected void setup() {
        System.out.println("\n[AGENT COMPOSITE] Démarrage");
        System.out.println("├─ Nom: " + getAID().getName());
        System.out.println("└─ Architecture: Composite distribuée");

        Logger.log("Agent RLRA Composite démarré: " + getAID().getName());

        // Initialisation des modules
        System.out.println("\n[INITIALISATION] Création des modules");

        monitorModule = new CompositeMonitorModule(this);
        System.out.println("├─ Monitor: OK");

        learnerModule = new CompositeLearnerModule(this);
        System.out.println("├─ Learner: OK");

        executorModule = new CompositeExecutorModule(this);
        System.out.println("└─ Executor: OK");

        // Configuration des modules
        System.out.println("\n[CONFIGURATION] Connexion des modules");
        monitorModule.setLearnerModule(learnerModule);
        System.out.println("├─ Monitor → Learner");

        learnerModule.setExecutorModule(executorModule);
        System.out.println("└─ Learner → Executor");

        // Comportement parallèle
        System.out.println("\n[COMPORTEMENT] Activation parallèle");
        ParallelBehaviour parallelBehaviour = new ParallelBehaviour(
                this, ParallelBehaviour.WHEN_ANY);

        parallelBehaviour.addSubBehaviour(monitorModule.getBehaviour());
        System.out.println("├─ Monitor activé");

        parallelBehaviour.addSubBehaviour(learnerModule.getBehaviour());
        System.out.println("├─ Learner activé");

        parallelBehaviour.addSubBehaviour(executorModule.getBehaviour());
        System.out.println("└─ Executor activé");

        addBehaviour(parallelBehaviour);

        System.out.println("\n[SYSTÈME] Prêt à fonctionner");
        System.out.println("├─ Modules: 3 actifs");
        System.out.println("├─ Mode: Parallèle");
        System.out.println("└─ Statut: OPÉRATIONNEL");

        Logger.log("Modules RLRA Composite initialisés");
    }

    protected void takeDown() {
        System.out.println("\n[AGENT COMPOSITE] Arrêt");
        System.out.println("├─ Démontage modules...");

        if (monitorModule != null) {
            monitorModule.printStats();
        }

        if (learnerModule != null) {
            learnerModule.printStats();
        }

        if (executorModule != null) {
            executorModule.printFinalReport();
        }

        System.out.println("└─ Agent terminé");

        Logger.log("Agent RLRA Composite arrêté");
    }
}