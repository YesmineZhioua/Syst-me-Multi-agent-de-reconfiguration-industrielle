package festo.distributed;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import java.util.*;

/**
 * Générateur de Tâches - Injecte des tâches dans le système
 *  pour activer votre système!
 */
public class TaskGeneratorAgent extends Agent {

    private int taskCounter = 0;
    private List<String> machineIds;
    private List<String> siteIds;
    private Random random;

    // Statistiques
    private int tasksGenerated = 0;
    private int tasksAccepted = 0;
    private int tasksRefused = 0;

    protected void setup() {
        random = new Random();

        // Liste des machines et sites disponibles
        machineIds = Arrays.asList("M1", "M2", "M3", "M4");
        siteIds = Arrays.asList("A", "B");

        System.out.println("🎯 ===== GÉNÉRATEUR DE TÂCHES DÉMARRÉ =====");
        System.out.println("📦 Injection de tâches dans le système...");

        // Attendre que le système soit prêt apres 5s (waker c'est un réveil qui attend 5 secondes)
        addBehaviour(new jade.core.behaviours.WakerBehaviour(this, 5000) {
            protected void onWake() {
                System.out.println("✅ Système initialisé - Début génération de tâches");
                startTaskGeneration();
            }
        });

        //  RECEVOIR LES RÉPONSES DES MACHINES (BOUCLE INFINIE)

        addBehaviour(new jade.core.behaviours.CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    handleResponse(msg);
                } else {
                    block();
                }
            }
        });

        // Statistiques périodiques
        addBehaviour(new TickerBehaviour(this, 15000) {
            protected void onTick() {
                printStatistics();
            }
        });
    }

    private void startTaskGeneration() {
        // Génération continue de tâches
        addBehaviour(new TickerBehaviour(this, 3000) {
            protected void onTick() {
                generateTask();
            }
        });

        // Génération de rafales de tâches (stress test)  (pic de charge)
        addBehaviour(new TickerBehaviour(this, 20000) {
            protected void onTick() {
                generateTaskBurst();
            }
        });
    }


    //Génère une tâche simple
    private void generateTask() {
        // 1️. CRÉER UN ID UNIQUE
        taskCounter++;
        String taskId = "TASK_" + taskCounter;  // "TASK_1"
        // 2. PRIORITÉ ALÉATOIRE (1-10)
        int priority = random.nextInt(10) + 1;  // Ex: 7

        // 3. Choisir une machine au hasard
        String targetMachine = machineIds.get(random.nextInt(machineIds.size()));
        // machineIds = ["M1", "M2", "M3", "M4"]
        // random.nextInt(4) → 0, 1, 2 ou 3
        // Ex: 2 → "M3"

        // 4. ENVOYER LA TÂCHE
        sendTaskToMachine(taskId, targetMachine, priority);
        tasksGenerated++;
    }

    // Génère une rafale de tâches (simulation de pic de charge)
    private void generateTaskBurst() {
        System.out.println("💥 RAFALE DE TÂCHES - Simulation pic de charge");

        // 1. NOMBRE ALÉATOIRE DE TÂCHES (5-15)
        int burstSize = 5 + random.nextInt(10); // 5-15 tâches

        // 2️. CRÉER TOUTES LES TÂCHES D'UN COUP
        for (int i = 0; i < burstSize; i++) {
            taskCounter++;
            String taskId = "BURST_TASK_" + taskCounter;
            int priority = 7 + random.nextInt(3); // Priorité haute

            String targetMachine = machineIds.get(random.nextInt(machineIds.size()));
            sendTaskToMachine(taskId, targetMachine, priority);
            tasksGenerated++;
        }
    }
    //Génère une tâche complexe nécessitant négociation
    private void generateComplexTask() {
        taskCounter++;
        String taskId = "COMPLEX_TASK_" + taskCounter;

        System.out.println("🎯 Génération tâche complexe: " + taskId);

        // Envoyer un CFP (Call For Proposal) à toutes les machines
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

       //  négociation entre plusieurs machines, via le protocole CFP (Call For Proposal)
        for (String machineId : machineIds) {
            cfp.addReceiver(new AID(machineId, AID.ISLOCALNAME));
        }

        cfp.setContent("CFP:" + taskId + ":REQUIREMENTS:HIGH_CAPACITY");
        cfp.setConversationId("negotiation-" + taskId); // lier tous les messages qui font partie de la même discussio
        send(cfp);

        System.out.println("📢 CFP envoyé à " + machineIds.size() + " machines");
        tasksGenerated++;
    }

    /**
     * Génère une tâche inter-site
     */
    private void generateInterSiteTask() {
        taskCounter++;
        String taskId = "INTER_SITE_" + taskCounter;

        System.out.println("🌐 Génération tâche inter-site: " + taskId);

        // Envoyer au coordinateur de site
        String targetSite = siteIds.get(random.nextInt(siteIds.size()));

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("SiteCoordinator_" + targetSite, AID.ISLOCALNAME));
        msg.setContent("INTER_SITE_TASK:" + taskId + ":PRIORITY:8:URGENT");
        send(msg);

        tasksGenerated++;
    }

    /**
     * Envoie une tâche à une machine spécifique
     */
    private void sendTaskToMachine(String taskId, String machineId, int priority) {
        // 1️. CRÉER LE MESSAGE

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID(machineId, AID.ISLOCALNAME));
        // 2️. CONTENU
        msg.setContent("TASK:" + taskId + ":" + priority);
        // 3️. ID DE CONVERSATION (pour suivre)
        msg.setConversationId("task-" + taskId);
        // 4. ENVOYER
        send(msg);

        System.out.println("📤 Tâche envoyée: " + taskId +
                " → " + machineId +
                " (priorité: " + priority + ")");
    }

    /**
     * Gère les réponses des machines
     */
    private void handleResponse(ACLMessage msg) {
        String content = msg.getContent();
        String sender = msg.getSender().getLocalName();

        // 1️. SI ACCEPTÉE
        if (content.startsWith("ACCEPTED:")) {
            tasksAccepted++;
            String taskId = content.split(":")[1];
            System.out.println("✅ Tâche acceptée: " + taskId + " par " + sender);

            // 2️. SI REFUSÉE
        } else if (content.startsWith("REFUSED:")) {
            tasksRefused++;
            String taskId = content.split(":")[1];
            System.out.println("❌ Tâche refusée: " + taskId + " par " + sender);

            // Réessayer avec une autre machine
            retryTask(taskId);

            // 3️. SI PROPOSITION (NÉGOCIATION)
        } else if (msg.getPerformative() == ACLMessage.PROPOSE) {
            // Proposition reçue dans le cadre d'une négociation
            System.out.println("💡 Proposition reçue de " + sender + ": " + content);

            // Accepter la meilleure proposition (simplifié)
            ACLMessage accept = msg.createReply();
            accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            accept.setContent("ACCEPTED");
            send(accept);
        }
    }

    /**
     * Réessaie d'envoyer une tâche refusée
     */
    private void retryTask(String taskId) {
        System.out.println("🔄 Nouvelle tentative pour: " + taskId);

        // Copier la liste des machines
        List<String> availableMachines = new ArrayList<>(machineIds);

        if (!availableMachines.isEmpty()) {
            // Choisir une autre machine au hasard
            String newTarget = availableMachines.get(random.nextInt(availableMachines.size()));
            int priority = 8; // Priorité augmentée pour retry

            sendTaskToMachine(taskId, newTarget, priority);
        }
    }

    /**
     * Affiche les statistiques
     */
    private void printStatistics() {
        System.out.println("\n📊 ===== STATISTIQUES GÉNÉRATEUR =====");
        System.out.println("📦 Tâches générées: " + tasksGenerated);
        System.out.println("✅ Tâches acceptées: " + tasksAccepted);
        System.out.println("❌ Tâches refusées: " + tasksRefused);

        if (tasksGenerated > 0) {
            double acceptanceRate = (double) tasksAccepted / tasksGenerated * 100;
            System.out.println("📈 Taux d'acceptation: " +
                    String.format("%.1f", acceptanceRate) + "%");
        }

        System.out.println("=====================================\n");
    }

    protected void takeDown() {
        System.out.println("🛑 Générateur de tâches arrêté");
        printStatistics();
    }
}