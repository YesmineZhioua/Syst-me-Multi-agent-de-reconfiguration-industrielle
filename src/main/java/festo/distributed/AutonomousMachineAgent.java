package festo.distributed;


import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import festo.utils.Logger;
import jade.lang.acl.MessageTemplate;

import java.util.*;

/**
 * Agent Machine Autonome - Niveau 1
 * Prend ses propres décisions locales et négocie avec les autres machines
 */
public class AutonomousMachineAgent extends Agent {

    private String machineId;
    private String machineType;
    private String site;
    private boolean operational;
    private double load;
    private int capacity;
    private int cycleTime;
    private double energyConsumption;

    // Pour la prise de décision autonome
    private Queue<String> localQueue;
    private Map<String, Integer> resourceAllocation;
    private AID siteCoordinatorAID;
    private List<String> neighborMachines;

    // Métriques de performance
    private int tasksCompleted;
    private int tasksFailed;
    private double averageProcessingTime;
    /**
     * Méthode simple pour tester la distribution
     * - Chaque machine connaît ses voisins
     * - Communication directe machine à machine
     */

    private void establishPeerToPeerNetwork() {
        addBehaviour(new TickerBehaviour(this, 3000) {
            protected void onTick() {
                String loadStr = String.format(Locale.US, "%.1f", load);

                for (String neighbor : neighborMachines) {
                    ACLMessage ping = new ACLMessage(ACLMessage.INFORM);
                    ping.addReceiver(new AID(neighbor, AID.ISLOCALNAME));
                    ping.setContent("PING:" + machineId +
                            ":LOAD:" + loadStr +
                            ":QUEUE:" + localQueue.size());
                    send(ping);
                }

                ACLMessage monitorMsg = new ACLMessage(ACLMessage.INFORM);
                monitorMsg.addReceiver(new AID("SystemMonitor", AID.ISLOCALNAME));
                monitorMsg.setContent("PING:" + machineId +
                        ":LOAD:" + loadStr +
                        ":QUEUE:" + localQueue.size() +
                        ":SITE:" + site);
                send(monitorMsg);
            }
        });
        //  CHANGE : Ne pas bloquer, traiter TOUS les messages
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    if (msg.getContent() != null && msg.getContent().startsWith("PING:")) {
                        // Répondre au PING
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.INFORM);
                        String loadStrForReply = String.format(Locale.US, "%.1f", load);
                        reply.setContent("PONG:" + machineId +
                                ":LOAD:" + loadStrForReply +
                                ":STATUS:" + operational +
                                ":CAPACITY:" + (capacity - localQueue.size()) +
                                ":SITE:" + site);
                        send(reply);
                    } else {
                        //  AJOUTE : Traiter les autres messages ici aussi
                        handleMessage(msg);
                    }
                } else {
                    block();
                }
            }
        });
    }

    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 6) {
            machineId = (String) args[0];
            machineType = (String) args[1];
            site = (String) args[2];
            capacity = (Integer) args[3];
            cycleTime = (Integer) args[4];
            energyConsumption = (Double) args[5];
        }

        operational = true;
        load = 0.0;
        localQueue = new LinkedList<>();
        resourceAllocation = new HashMap<>();
        neighborMachines = new ArrayList<>();
        tasksCompleted = 0;
        tasksFailed = 0;
        // Initialiser la liste des machines voisines
        neighborMachines.add("M1");
        neighborMachines.add("M2");
        neighborMachines.add("M3");
        neighborMachines.add("M4");
        neighborMachines.remove(machineId);

        // Trouver le coordinateur du site
        siteCoordinatorAID = new AID("SiteCoordinator_" + site, AID.ISLOCALNAME);

        // Trouver le coordinateur du site
        siteCoordinatorAID = new AID("SiteCoordinator_" + site, AID.ISLOCALNAME);

        Logger.log("🤖 Machine Autonome démarrée: " + machineId +
                " (Site: " + site + ", Capacité: " + capacity + ")");

        // S'enregistrer auprès du coordinateur de site
        registerWithCoordinator();

        // Comportement pour recevoir les demandes et négociations
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    handleMessage(msg);
                } else {
                    block();
                }
            }
        });

        // Prise de décision autonome périodique
        addBehaviour(new TickerBehaviour(this, 2000) {
            protected void onTick() {
                makeAutonomousDecision();
            }
        });

        // Surveillance et auto-diagnostic
        addBehaviour(new TickerBehaviour(this, 3000) {
            protected void onTick() {
                performSelfMonitoring();
            }
        });

        // Optimisation locale
        addBehaviour(new TickerBehaviour(this, 5000) {
            protected void onTick() {
                optimizeLocalPerformance();
            }
        });

        establishPeerToPeerNetwork();

        Logger.log("🔗 " + machineId + " - Réseau P2P activé");

    }

    private void registerWithCoordinator() {
        ACLMessage register = new ACLMessage(ACLMessage.SUBSCRIBE);
        register.addReceiver(siteCoordinatorAID);
        register.setContent("REGISTER:" + machineId + ":" + machineType +
                ":" + capacity + ":" + operational);
        send(register);
        Logger.log("📝 " + machineId + " enregistré auprès du coordinateur");
    }

    /**
     * Décision collective : trouver la meilleure machine pour une tâche
     */
    private void collaborativeTaskAssignment(String taskId, int priority) {
        // Envoyer une consultation aux voisins
        ACLMessage consultation = new ACLMessage(ACLMessage.CFP);
        for (String neighbor : neighborMachines) {
            consultation.addReceiver(new AID(neighbor, AID.ISLOCALNAME));
        }
        consultation.setContent("WHO_CAN_HANDLE:" + taskId +
                ":PRIORITY:" + priority +
                ":REQUIRED_TIME:" + cycleTime);
        send(consultation);

        Logger.log("🗳️ " + machineId + " consulte les voisins pour " + taskId);
    }

    private void handleMessage(ACLMessage msg) {
        String content = msg.getContent();
        String sender = msg.getSender().getLocalName();
        int performative = msg.getPerformative();

        switch (performative) {
            case ACLMessage.REQUEST:
                handleTaskRequest(msg);
                break;
            case ACLMessage.PROPOSE:
                handleNegotiationProposal(msg);
                break;
            case ACLMessage.ACCEPT_PROPOSAL:
                handleProposalAcceptance(msg);
                break;
            case ACLMessage.REJECT_PROPOSAL:
                handleProposalRejection(msg);
                break;
            case ACLMessage.INFORM:
                handleInformation(msg);
                break;
            case ACLMessage.CFP:
                handleCallForProposal(msg);
                break;
            default:
                Logger.log("📨 " + machineId + " - Message de " + sender + ": " + content);
        }
    }

    /**
     * Prise de décision autonome
     */
    private void makeAutonomousDecision() {
        if (!operational) {
            attemptSelfRecovery();
            return;
        }

        // Décision 1: Gestion de la file d'attente locale
        if (!localQueue.isEmpty() && load < 80) {
            processNextTask();
        }

        // Décision 2: Redistribution si surcharge
        if (load > 85) {
            requestLoadBalancing();
        }

        // Décision 3: Offrir de l'aide si sous-utilisé
        if (load < 30 && operational) {
            offerHelpToNeighbors();
        }

        // Décision 4: Ajustement des paramètres
        if (load > 70) {
            adjustOperationalParameters();
        }
    }


    private void handleTaskRequest(ACLMessage msg) {
        System.out.println("🎯 " + machineId + " reçoit demande de tâche: " + msg.getContent());

        String[] parts = msg.getContent().split(":");
        if (parts.length >= 3) {
            String taskId = parts[1];
            int priority = Integer.parseInt(parts[2]);

            // Décision autonome: accepter ou refuser
            boolean canAccept = evaluateTaskAcceptance(taskId, priority);

            ACLMessage reply = msg.createReply();
            if (canAccept) {
                localQueue.add(taskId);
                reply.setPerformative(ACLMessage.AGREE);
                reply.setContent("ACCEPTED:" + taskId + ":ETA:" + estimateCompletionTime());
                Logger.log("✅ " + machineId + " accepte tâche " + taskId);

                //  AJOUTE CETTE LIGNE - Traiter immédiatement
                processNextTask();

            } else {
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("REFUSED:" + taskId + ":LOAD:" + load);
                Logger.log("❌ " + machineId + " refuse tâche " + taskId + " (charge: " + load + "%)");
            }
            send(reply);
        }
    }

    private boolean evaluateTaskAcceptance(String taskId, int priority) {
        System.out.println("🤔 " + machineId + " évalue tâche " + taskId +
                " (load=" + load + ", queue=" + localQueue.size() +
                ", capacity=" + capacity + ", priority=" + priority + ")");

        // Logique de décision autonome
        if (!operational) return false;
        if (load > 90) return false;  // ← OK, charge = 0
        if (localQueue.size() >= capacity) {  // ← capacity = 10 pour M1
            // Accepter seulement si priorité haute
            return priority > 7;
        }
        return true;
      }

    /**
     * Négociation avec d'autres machines
     */
    private void handleCallForProposal(ACLMessage msg) {
        String[] parts = msg.getContent().split(":");
        if (parts.length >= 3) {
            String taskId = parts[1];
            String requirements = parts[2];

            // Calculer ma proposition
            double myScore = calculateProposalScore(taskId, requirements);

            if (myScore > 0.5) { // Seuil de participation
                ACLMessage proposal = msg.createReply();
                proposal.setPerformative(ACLMessage.PROPOSE);
                proposal.setContent("PROPOSAL:" + taskId + ":SCORE:" + myScore +
                        ":ETA:" + estimateCompletionTime() +
                        ":ENERGY:" + energyConsumption);
                send(proposal);
                Logger.log("💡 " + machineId + " propose pour " + taskId +
                        " (score: " + String.format("%.2f", myScore) + ")");
            }
        }
    }

    private double calculateProposalScore(String taskId, String requirements) {
        // Score basé sur: disponibilité, capacité, efficacité énergétique
        double availabilityScore = (100 - load) / 100.0;
        double capacityScore = (capacity - localQueue.size()) / (double) capacity;
        double energyScore = 1.0 - (energyConsumption / 200.0);

        return (availabilityScore * 0.5 + capacityScore * 0.3 + energyScore * 0.2);
    }

    private void handleNegotiationProposal(ACLMessage msg) {
        // Réception d'une proposition d'une autre machine
        String content = msg.getContent();
        Logger.log("🤝 " + machineId + " reçoit proposition: " + content);

        // Évaluer la proposition
        boolean accept = evaluateProposal(content);

        ACLMessage reply = msg.createReply();
        if (accept) {
            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            reply.setContent("ACCEPTED:" + content);
        } else {
            reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
            reply.setContent("REJECTED:" + content);
        }
        send(reply);
    }

    private boolean evaluateProposal(String proposal) {
        // Logique d'évaluation des propositions
        return Math.random() > 0.3; // Simplification
    }

    private void handleProposalAcceptance(ACLMessage msg) {
        Logger.log("✅ " + machineId + " - Proposition acceptée: " + msg.getContent());
        // Continuer avec la tâche négociée
    }

    private void handleProposalRejection(ACLMessage msg) {
        Logger.log("❌ " + machineId + " - Proposition rejetée: " + msg.getContent());
        // Trouver une alternative
    }

    private void handleInformation(ACLMessage msg) {
        String content = msg.getContent();

        if (content.startsWith("NEIGHBOR:")) {
            // Mise à jour de la liste des voisins
            String neighbor = content.split(":")[1];
            if (!neighborMachines.contains(neighbor)) {
                neighborMachines.add(neighbor);
                Logger.log("👥 " + machineId + " - Nouveau voisin: " + neighbor);
            }
        } else if (content.startsWith("STATUS_UPDATE:")) {
            // Mise à jour de statut d'un voisin
            Logger.log("📊 " + machineId + " - Mise à jour voisin: " + content);
        }
    }

    /**
     * Traitement des tâches
     */
    private void processNextTask() {
        if (!localQueue.isEmpty()) {
            String taskId = localQueue.poll();
            load = Math.min(100, load + (100.0 / capacity));

            System.out.println("⚡ " + machineId + " charge AVANT: " + load +
                    ", APRÈS: " + load + " (incrément: " + (100.0/capacity) + ")");
            Logger.log("⚙️ " + machineId + " traite " + taskId +
                    " (charge: " + String.format("%.1f", load) + "%)");
            // Simuler le traitement
            addBehaviour(new jade.core.behaviours.WakerBehaviour(this, (int)(cycleTime * 1000)) {
                protected void onWake() {
                    completeTask(taskId);
                }
            });
        }
    }
    private void completeTask(String taskId) {
        load = Math.max(0, load - (100.0 / capacity));
        tasksCompleted++;

        Logger.log("✨ " + machineId + " termine " + taskId);

        // Notifier le coordinateur
        ACLMessage notification = new ACLMessage(ACLMessage.INFORM);
        notification.addReceiver(siteCoordinatorAID);
        notification.setContent("TASK_COMPLETED:" + taskId + ":" + machineId);
        send(notification);
    }

    /**
     * Équilibrage de charge autonome
     */
    private void requestLoadBalancing() {
        Logger.log("⚖️ " + machineId + " demande équilibrage (charge: " +
                String.format("%.1f", load) + "%)");

        // Demander au coordinateur de site
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(siteCoordinatorAID);
        request.setContent("LOAD_BALANCING:" + machineId + ":" + load + ":" + localQueue.size());
        send(request);
    }

    private void offerHelpToNeighbors() {
        // Proposer de l'aide aux machines voisines
        for (String neighbor : neighborMachines) {
            ACLMessage offer = new ACLMessage(ACLMessage.PROPOSE);
            offer.addReceiver(new AID(neighbor, AID.ISLOCALNAME));
            offer.setContent("HELP_OFFER:" + machineId + ":CAPACITY:" +
                    (capacity - localQueue.size()));
            send(offer);
        }
    }

    /**
     * Auto-surveillance et diagnostic
     */
    private void performSelfMonitoring() {
        if (operational) {
            // Simuler des pannes aléatoires
            if (Math.random() < 0.03) { // 3% de chance
                operational = false;
                Logger.log("🔴 " + machineId + " PANNE détectée!");
                notifyFailure();
            }

            // Surveiller les performances
            if (load > 95) {
                Logger.warn(machineId + " en surcharge critique!");
                notifyOverload();
            }
        }
    }

    private void notifyFailure() {
        ACLMessage alert = new ACLMessage(ACLMessage.INFORM);
        alert.addReceiver(siteCoordinatorAID);
        alert.setContent("FAILURE:" + machineId + ":SELF_DETECTED:CRITICAL");
        send(alert);
    }

    private void notifyOverload() {
        ACLMessage alert = new ACLMessage(ACLMessage.INFORM);
        alert.addReceiver(siteCoordinatorAID);
        alert.setContent("OVERLOAD:" + machineId + ":" + load);
        send(alert);
    }

    private void attemptSelfRecovery() {
        // Tentative de récupération automatique
        if (Math.random() < 0.15) { // 15% de chance
            operational = true;
            Logger.log("🔧 " + machineId + " s'est auto-réparé!");

            ACLMessage recovery = new ACLMessage(ACLMessage.INFORM);
            recovery.addReceiver(siteCoordinatorAID);
            recovery.setContent("RECOVERY:" + machineId + ":SELF_REPAIRED");
            send(recovery);
        }
    }

    /**
     * Optimisation locale
     */
    private void optimizeLocalPerformance() {
        // Ajuster les paramètres pour optimiser
        if (load > 50 && load < 80) {
            // Zone optimale - maintenir
            Logger.log("📈 " + machineId + " en zone optimale");
        } else if (load < 30) {
            // Sous-utilisé - mode économie
            Logger.log("💤 " + machineId + " en mode économie d'énergie");
        }

        // Calculer les métriques
        double efficiency = tasksCompleted / (double)(tasksCompleted + tasksFailed + 1) * 100;
        Logger.log("📊 " + machineId + " - Efficacité: " +
                String.format("%.1f", efficiency) + "%");
    }

    private void adjustOperationalParameters() {
        // Ajustement dynamique des paramètres
        Logger.log("🔧 " + machineId + " ajuste ses paramètres (charge élevée)");
    }

    private int estimateCompletionTime() {
        return cycleTime * (localQueue.size() + 1);
    }

    protected void takeDown() {
        Logger.log("🛑 Machine Autonome " + machineId + " arrêtée");
    }
}