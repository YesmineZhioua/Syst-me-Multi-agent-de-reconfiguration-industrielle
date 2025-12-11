package festo.agents.composite;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import festo.models.ReconfigurationRequest;
import festo.utils.Logger;

public class CompositeMonitorModule {
    private Agent agent;
    private CompositeLearnerModule learnerModule;
    private int alertsReceived;

    public CompositeMonitorModule(Agent agent) {
        this.agent = agent;
        this.alertsReceived = 0;

        System.out.println("\n[MONITOR COMPOSITE] Module initialisé");
        System.out.println("└─ Agent: " + agent.getAID().getName());
    }

    public void setLearnerModule(CompositeLearnerModule learnerModule) {
        this.learnerModule = learnerModule;
        System.out.println("[MONITOR] Learner connecté");
    }

    public CyclicBehaviour getBehaviour() {
        return new CyclicBehaviour(agent) {
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = agent.receive(mt);

                if (msg != null) {
                    processAlert(msg);
                } else {
                    block();
                }
            }
        };
    }

    private void processAlert(ACLMessage msg) {
        alertsReceived++;
        String content = msg.getContent();
        String sender = msg.getSender().getLocalName();

        System.out.println("\n[MONITOR] Alerte reçue");
        System.out.println("├─ Expéditeur: " + sender);
        System.out.println("├─ Contenu: " + content);
        System.out.println("├─ Type: " + getAlertType(content));

        Logger.log("[Composite] Alerte de " + sender + ": " + content);

        ReconfigurationRequest request = createRequestFromAlert(content, sender);

        if (request != null) {
            System.out.println("├─ Demande créée: " + request.getRequestId());

            if (learnerModule != null) {
                learnerModule.addRequest(request);
                System.out.println("└─ Transmise au Learner");
            } else {
                System.out.println("└─ Erreur: Learner non disponible");
            }
        } else {
            System.out.println("└─ Ignorée (type non reconnu)");
        }
    }

    private String getAlertType(String content) {
        if (content.startsWith("FAILURE")) {
            return "PANNE";
        } else if (content.startsWith("HIGH_LOAD")) {
            return "SURCHARGE";
        } else if (content.startsWith("REQUEST")) {
            return "DEMANDE";
        } else {
            return "INCONNU";
        }
    }

    private ReconfigurationRequest createRequestFromAlert(String content, String sender) {
        String scenarioType = "";
        String requestId = "REQ_" + System.currentTimeMillis();

        if (content.startsWith("FAILURE")) {
            scenarioType = "FAILURE";
        } else if (content.startsWith("HIGH_LOAD")) {
            scenarioType = "HIGH_LOAD";
        } else {
            return null; // Type non supporté
        }

        System.out.println("  ├─ Scénario: " + scenarioType);
        System.out.println("  └─ ID Demande: " + requestId);

        return new ReconfigurationRequest(requestId, scenarioType, content);
    }

    public void printStats() {
        System.out.println("\n[STATS MONITOR]");
        System.out.println("├─ Alertes reçues: " + alertsReceived);
        System.out.println("└─ Learner: " + (learnerModule != null ? "Connecté" : "Absent"));
    }
}