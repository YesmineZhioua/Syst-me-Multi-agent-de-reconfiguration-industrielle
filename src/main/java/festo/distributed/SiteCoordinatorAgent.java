package festo.distributed;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import java.util.*;

/**
 * Coordinateur de Site - Niveau 2
 * Gère les machines locales et communique avec le superviseur global
 */
public class SiteCoordinatorAgent extends Agent {

    private String siteId; // Nom du site
    private Map<String, MachineInfo> machines; // Liste de TOUTES les machines du site + infos
    private Queue<String> pendingTasks;  // Tâches en attente
    private AID globalSupervisorAID;  // Adresse du grand patron

    // Métriques du site
    private double siteLoadAverage; // Charge moyenne
    private int totalMachines;// Nombre total : 4
    private int operationalMachines; // Machines qui marchent
    private int siteFailures;// Nombre de pannes
    private int tasksCompleted;// Tâches terminées
    private int conflictsResolved; // Conflits résolus

    protected void setup() {

        // 1️. RÉCUPÉRER LE NOM DU SITE
        Object[] args = getArguments();
        if (args != null && args.length >= 1) {
            siteId = (String) args[0];
        }

        // 2. INITIALISER LES STRUCTURES
        machines = new HashMap<>();
        pendingTasks = new LinkedList<>();
        globalSupervisorAID = new AID("GlobalSupervisor", AID.ISLOCALNAME);
        totalMachines = 0;
        operationalMachines = 0;
        siteFailures = 0;
        tasksCompleted = 0;
        conflictsResolved = 0;

        System.out.println("🏢 Coordinateur Site " + siteId + " démarré");

        //3. S'enregistrer auprès du superviseur global
        registerWithSupervisor();

        // 4️.  LANCER LES COMPORTEMENTS AUTOMATIQUES
        // Comportement 1  pour recevoir les messages
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(); // Attendre un message
                if (msg != null) {
                    handleMessage(msg); // Traiter le message
                } else {
                    block();
                }
            }
        });

        // Comportement 2 : Surveiller le site toutes les 5 secondes
        addBehaviour(new TickerBehaviour(this, 5000) {
            protected void onTick() {

                monitorSite();
            }
        });

        // Comportement3 : Rapports au superviseur toutes les 7 secondes
        addBehaviour(new TickerBehaviour(this, 7000) {
            protected void onTick() {
                reportToSupervisor();
            }
        });

        //  Comportement 4 :Résoudre les conflits toutes les 6 secondes
        addBehaviour(new TickerBehaviour(this, 6000) {
            protected void onTick() {
                resolveLocalConflicts();
            }
        });
    }

    private void registerWithSupervisor() {
        //1. Créer un message
        ACLMessage register = new ACLMessage(ACLMessage.SUBSCRIBE);
        // 2️. Destinataire : GlobalSupervisor
        register.addReceiver(globalSupervisorAID);
        // 3️. Contenu : "Je suis le site ...."
        register.setContent("REGISTER_SITE:" + siteId);
        // 4️. ENVOYER
        send(register);
        System.out.println("📝 Site " + siteId + " enregistré auprès du superviseur");
    }



    private void handleMessage(ACLMessage msg) {
        String content = msg.getContent(); // Ex: "REGISTER:M1:Assemblage:10:true"
        int performative = msg.getPerformative(); // Type de message

        //route les messages selon leur contenu.
        if (content.startsWith("REGISTER:")) {
            handleMachineRegistration(msg);  // Une machine s'enregistre
        } else if (content.startsWith("TASK_COMPLETED:")) {
            handleTaskCompletion(msg); // Une tâche est terminée
        } else if (content.startsWith("FAILURE:")) {
            handleMachineFailure(msg); // Une machine est en panne
        } else if (content.startsWith("OVERLOAD:")) {
            handleMachineOverload(msg); // Une machine est surchargée
        } else if (content.startsWith("LOAD_BALANCING:")) {
            handleLoadBalancingRequest(msg); // Demande d'équilibrage
        } else if (content.startsWith("SUPERVISOR_DIRECTIVE:")) {
            handleSupervisorDirective(msg); // Ordre du patron
        } else if (content.startsWith("INTER_SITE_TASK:")) {
            handleInterSiteTask(msg);  // Tâche d'un autre site
        } else if (performative == ACLMessage.CONFIRM) {
            System.out.println("✅ Confirmation superviseur: " + content);
        }
    }

    /**
     * Enregistrement des machines
     */
    private void handleMachineRegistration(ACLMessage msg) {
        // 1. DÉCOUPER LE MESSAGE
        String[] parts = msg.getContent().split(":");
        // "REGISTER:M1:Assemblage:10:true"
        // → ["REGISTER", "M1", "Assemblage", "10", "true"]
        if (parts.length >= 5) {
            String machineId = parts[1]; // "M1"
            String machineType = parts[2];  // "Assemblage"
            int capacity = Integer.parseInt(parts[3]); // 10
            boolean operational = Boolean.parseBoolean(parts[4]);  // true


            MachineInfo info = new MachineInfo();
            info.machineId = machineId;
            info.machineType = machineType;
            info.capacity = capacity;
            info.operational = operational;
            info.load = 0.0;
            info.aid = msg.getSender();

            // 3️. AJOUTER À L'ANNUAIRE
            machines.put(machineId, info);
            totalMachines++;
            if (operational) {
                operationalMachines++;
            }

            System.out.println("✅ Machine enregistrée: " + machineId +
                    " (Type: " + machineType + ", Site: " + siteId + ")");

            // 4️. CONFIRMER À LA MACHINE
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.CONFIRM);
            reply.setContent("REGISTERED:" + machineId + ":SITE:" + siteId);
            send(reply);
        }
    }
    /**
     * 1. M1 envoie : "REGISTER:M1:Assemblage:10:true"
     * 2. Coordinateur reçoit le message
     * 3. Coordinateur crée une fiche pour M1 contenant les infos
     * 4. Coordinateur ajoute M1 à son annuaire
     * 5. Coordinateur répond à M1 : "REGISTERED:M1:SITE...."
     * **/



    /**
     * Gestion des tâches complétées
     */
    private void handleTaskCompletion(ACLMessage msg) {

        // 1️. DÉCOUPER LE MESSAGE
        String[] parts = msg.getContent().split(":");
        // "TASK_COMPLETED:T1:M1" → ["TASK_COMPLETED", "T1", "M1"]
        if (parts.length >= 3) {
            String taskId = parts[1]; // "T1"
            String machineId = parts[2];  // "M1"

            // 2️. INCRÉMENTER LES COMPTEURS
            tasksCompleted++;

            MachineInfo machine = machines.get(machineId);
            if (machine != null) {
                machine.tasksCompleted++; // Compteur de M? ++
            }

            System.out.println("✅ Site " + siteId + " - Tâche complétée: " +
                    taskId + " par " + machineId);
        }
    }

    /**
     * Gestion des pannes
     */
    private void handleMachineFailure(ACLMessage msg) {
        // 1️. DÉCOUPER
        String[] parts = msg.getContent().split(":");
        // "FAILURE:M2:SELF_DETECTED:CRITICAL" → ["FAILURE", "M2", ...]


        if (parts.length >= 2) {
            String machineId = parts[1]; // "M2"

            MachineInfo machine = machines.get(machineId);
            if (machine != null) {
                machine.operational = false; // M2 ne marche plus !!!!!!!
                operationalMachines--; // 3 → 2 machines opérationnelles
                siteFailures++; // 0 → 1 panne

                System.out.println("🔴 PANNE détectée - Site " + siteId +
                        ", Machine: " + machineId);

                // 3️. ESSAYER DE RÉSOUDRE LOCALEMENT
                boolean resolved = attemptLocalResolution(machineId);

                if (!resolved) {
                    // 4️. SI ÉCHEC →
                    // Escalader au superviseur

                    escalateToSupervisor("FAILURE", machineId, "CRITICAL");
                }
            }
        }
    }

    /**
     * Gestion des surcharges
     */
    private void handleMachineOverload(ACLMessage msg) {
        // 1️. DÉCOUPER

        String[] parts = msg.getContent().split(":");
        // "OVERLOAD:M1:95.0" → ["OVERLOAD", "M1", "95.0"]
        if (parts.length >= 3) {
            String machineId = parts[1]; // "M1"
            double load = Double.parseDouble(parts[2]);   // 95.0

            System.out.println("⚠️ Surcharge détectée - Site " + siteId +
                    ", Machine: " + machineId + " (" +
                    String.format("%.1f", load) + "%)");

            // 2️. ESSAYER D'ÉQUILIBRER LOCALEMENT
            boolean balanced = attemptLocalLoadBalancing(machineId);

            if (!balanced) {
                escalateToSupervisor("SITE_OVERLOAD", machineId, "HIGH");
            }
        }
    }

    /**
     * Demandes d'équilibrage de charge => redistribuer tâches
     */
    private void handleLoadBalancingRequest(ACLMessage msg) {
        // 1️. DÉCOUPER
        String[] parts = msg.getContent().split(":");
        // "LOAD_BALANCING:M1:90.0:5" → ["LOAD_BALANCING", "M1", "90.0", "5"]
        if (parts.length >= 4) {
            String machineId = parts[1]; // "M1"
            double load = Double.parseDouble(parts[2]); // 90.0%
            int queueSize = Integer.parseInt(parts[3]); // 5 tâches en attente

            System.out.println("⚖️ Demande équilibrage - Machine: " + machineId);

            // 2️. TROUVER UNE MACHINE LÉGÈRE
            // Cherche dans l'annuaire une machine avec charge < 70%
            String targetMachine = findLightlyLoadedMachine(machineId);

            if (targetMachine != null) {
                // 3️. REDISTRIBUER LA MOITIÉ DES TÂCHES
                redistributeTasks(machineId, targetMachine, queueSize / 2);
                // M1 → M4 : 2-3 tâches
                conflictsResolved++;
            } else {
                // 4️. AUCUNE MACHINE DISPONIBLE
                System.out.println("⚠️ Aucune machine disponible pour équilibrage local");
                escalateToSupervisor("NO_RESOURCES", machineId, "MEDIUM");
            }
        }
    }

    /**
     * Directives du superviseur => Réduisez la charge du site
     */
    private void handleSupervisorDirective(ACLMessage msg) {
        // 1️. DÉCOUPER
        String[] parts = msg.getContent().split(":");
        // "SUPERVISOR_DIRECTIVE:THROTTLE" → ["SUPERVISOR_DIRECTIVE", "THROTTLE"]


        if (parts.length >= 2) {
            String action = parts[1];

            System.out.println("📋 Directive superviseur reçue: " + action);

            // 2️. EXÉCUTER L'ORDRE
            switch (action) {
                case "REALLOCATE":
                    // Réallouer les tâches entre machines
                    break;
                case "RESOURCE_TRANSFER":
                    // Transférer des ressources vers un autre site
                    break;
                case "LOAD_DISTRIBUTION":
                    // Distribuer la charge différemment
                    break;
                case "THROTTLE":
                    // Limiter les nouvelles tâches
                    System.out.println("🚦 Throttling activé sur site " + siteId);
                    break;
                case "REDUCE_LOAD":
                    System.out.println("📉 Réduction de charge demandée");
                    break;
            }
        }
    }
    /**
     * expl !
     * GlobalSupervisor → Coordinateur Paris : "SUPERVISOR_DIRECTIVE:THROTTLE"
     * Coordinateur Paris : " limite les nouvelles tâches !"
     * */

    /**
     * Tâches inter-sites
     */
    private void handleInterSiteTask(ACLMessage msg) {
        // 1️. DÉCOUPER
        String[] parts = msg.getContent().split(":");
        // "INTER_SITE_TASK:T99" → ["INTER_SITE_TASK", "T99"]
        if (parts.length >= 2) {
            String taskId = parts[1];  // "T99"

            System.out.println("🌐 Tâche inter-site reçue: " + taskId +
                    " pour site " + siteId);

            //2. Trouver la meilleure machine pour cette tâche
            // Cherche la machine la moins chargée : expl M4 (20%)
            String bestMachine = findBestMachine();

            if (bestMachine != null) {
                // 3️. ENVOYER LA TÂCHE À bestMachine
                ACLMessage taskMsg = new ACLMessage(ACLMessage.REQUEST);
                taskMsg.addReceiver(machines.get(bestMachine).aid);
                taskMsg.setContent("TASK:" + taskId + ":8"); // Priorité haute
                send(taskMsg);
            }
        }
    }

    /**
     * Surveillance du site
     */
    private void monitorSite() {
        // Calculer la charge moyenne du site
        double totalLoad = 0;
        int validMachines = 0;

        for (MachineInfo machine : machines.values()) {
            totalLoad += machine.load;
            validMachines++;
        }

        siteLoadAverage = validMachines > 0 ? totalLoad / validMachines : 0;

        // Vérifier l'état des machines
        operationalMachines = 0;
        for (MachineInfo machine : machines.values()) {
            if (machine.operational) {
                operationalMachines++;
            }
        }

        // Détecter les problèmes
        if (siteLoadAverage > 85) {
            System.out.println("⚠️ Site " + siteId + " en surcharge: " +
                    String.format("%.1f", siteLoadAverage) + "%");
        }
    }

    /**
     * Rapports au superviseur
     */
    private void reportToSupervisor() {
        // 1️. CRÉER LE MESSAGE
        ACLMessage report = new ACLMessage(ACLMessage.INFORM);
        report.addReceiver(globalSupervisorAID);

        // 2️. FORMATER LE RAPPORT
        String content = String.format(
                "SITE_STATUS:%s:LOAD:%.1f:MACHINES:%d:OPERATIONAL:%d:FAILURES:%d:TASKS:%d",
                siteId, siteLoadAverage, totalMachines,
                operationalMachines, siteFailures, tasksCompleted
        ).replace(",", ".");

        report.setContent(content);
        send(report);
    }

    /**
     * Résolution de conflits locaux
     */
    private void resolveLocalConflicts() {
        //creation 2 listes
        List<MachineInfo> overloadedMachines = new ArrayList<>(); // Machines surchargées
        List<MachineInfo> underloadedMachines = new ArrayList<>();  // Machines vides
        // 2️. CLASSER LES MACHINES
        for (MachineInfo machine : machines.values()) {
            if (machine.operational) {
                if (machine.load > 80) {
                    overloadedMachines.add(machine);
                } else if (machine.load < 30) {
                    underloadedMachines.add(machine);
                }
            }
        }

        // 3️. SI DÉSÉQUILIBRE → ÉQUILIBRER
        if (!overloadedMachines.isEmpty() && !underloadedMachines.isEmpty()) {
            System.out.println("⚖️ Équilibrage automatique sur site " + siteId);
            conflictsResolved++;
        }
    }

    /**
     * Tentatives de résolution locale
     */
    private boolean attemptLocalResolution(String failedMachine) {
        System.out.println("🔧 Tentative résolution locale pour " + failedMachine);

        // Chercher une machine de remplacement
        String replacement = findReplacementMachine(failedMachine);

        if (replacement != null) {
            System.out.println("✅ Remplacement trouvé: " + replacement);
            return true;
        }

        return false;
    }

    private boolean attemptLocalLoadBalancing(String overloadedMachine) {
        String targetMachine = findLightlyLoadedMachine(overloadedMachine);

        if (targetMachine != null) {
            System.out.println("⚖️ Équilibrage: " + overloadedMachine +
                    " → " + targetMachine);
            return true;
        }

        return false;
    }

    /**
     * Escalade vers le superviseur : Quand le coordinateur ne peut pas résoudre un problème, il prévient le grand patron.
     */
    private void escalateToSupervisor(String issue, String machineId, String severity) {
        System.out.println("⬆️ Escalade vers superviseur - Issue: " + issue);
        // Créer un message urgent
        ACLMessage escalation = new ACLMessage(ACLMessage.REQUEST);
        escalation.addReceiver(globalSupervisorAID);
        // le message :                Type     :Site :Problème:Machine:Gravité
        escalation.setContent("ESCALATION:" + siteId + ":" + issue +
                ":" + machineId + ":" + severity);
        send(escalation);
    }

    /**
     * Fonctions utilitaires
     */

    // TROUVER UNE MACHINE LÉGÈRE => Chercher dans l'annuaire une machine peu chargée.
    private String findLightlyLoadedMachine(String exclude) {
        String bestMachine = null;
        double minLoad = Double.MAX_VALUE; // Très grand nombre

        // Parcourir TOUTES les machines
        for (Map.Entry<String, MachineInfo> entry : machines.entrySet()) {
            if (!entry.getKey().equals(exclude) && entry.getValue().operational) {
                if (entry.getValue().load < minLoad) {
                    minLoad = entry.getValue().load;   // Nouvelle meilleure charge
                    bestMachine = entry.getKey(); // Nouvelle meilleure machine
                }
            }
        }
        // Retourner seulement si charge < 70%
        return (minLoad < 70) ? bestMachine : null;
    }
    private String findReplacementMachine(String failedMachine) {
        MachineInfo failed = machines.get(failedMachine);
        if (failed == null) return null;

        for (Map.Entry<String, MachineInfo> entry : machines.entrySet()) {
            MachineInfo candidate = entry.getValue();
            if (!entry.getKey().equals(failedMachine) &&
                    candidate.operational &&
                    candidate.machineType.equals(failed.machineType) &&
                    candidate.load < 60) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String findBestMachine() {
        String bestMachine = null;
        double minLoad = Double.MAX_VALUE;

        for (Map.Entry<String, MachineInfo> entry : machines.entrySet()) {
            if (entry.getValue().operational && entry.getValue().load < minLoad) {
                minLoad = entry.getValue().load;
                bestMachine = entry.getKey();
            }
        }

        return bestMachine;
    }
//  REDISTRIBUER LES TÂCHES :Transférer des tâches d'une machine surchargée vers une machine libre.
    private void redistributeTasks(String from, String to, int count) {
        System.out.println("🔄 Redistribution: " + from + " → " + to +
                " (" + count + " tâches)");

        // Notifier les machines concernées
        ACLMessage msgFrom = new ACLMessage(ACLMessage.REQUEST);
        msgFrom.addReceiver(machines.get(from).aid);
        msgFrom.setContent("TRANSFER_TASKS:" + to + ":" + count);
        send(msgFrom);
    }

    protected void takeDown() {
        System.out.println("🛑 Coordinateur Site " + siteId + " arrêté");
    }




    /**
     * Classe interne pour info machine
     */
    private static class MachineInfo {
        String machineId;
        String machineType;
        int capacity;
        boolean operational;
        double load;
        int tasksCompleted;
        AID aid;
    }
}