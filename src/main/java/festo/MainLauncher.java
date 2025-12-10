package festo;
import jade.core.Profile;
        import jade.core.ProfileImpl;
        import jade.core.Runtime;
        import jade.wrapper.AgentContainer;
        import jade.wrapper.AgentController;
        import jade.wrapper.StaleProxyException;

public class MainLauncher {

    public static void main(String[] args) {
        try {
            // 1. Obtenir l'instance de JADE Runtime
            Runtime rt = Runtime.instance();

            // 2. Créer un Profile pour le conteneur principal
            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.MAIN_HOST, "localhost");
            profile.setParameter(Profile.MAIN_PORT, "1099");
            profile.setParameter(Profile.GUI, "true"); // Activer l'interface graphique

            // 3. Créer le conteneur principal
            AgentContainer mainContainer = rt.createMainContainer(profile);

            System.out.println("=== Démarrage du Système FESTO MAS ===");

            // 4. Créer et démarrer les agents

            // Agent Contrôleur RLRA
            AgentController rlraController = mainContainer.createNewAgent(
                    "RLRA_Controller",
                    "festo.agents.RLRAControllerAgent",
                    new Object[]{}  // Pas d'arguments
            );
            rlraController.start();

            // Agent Monitor Site A
            AgentController monitorA = mainContainer.createNewAgent(
                    "Monitor_SiteA",
                    "festo.agents.SiteMonitorAgent",
                    new Object[]{"SiteA", new String[]{"M1", "M2"}}
            );
            monitorA.start();

            // Agent Monitor Site B
            AgentController monitorB = mainContainer.createNewAgent(
                    "Monitor_SiteB",
                    "festo.agents.SiteMonitorAgent",
                    new Object[]{"SiteB", new String[]{"M3", "M4"}}
            );
            monitorB.start();

            // Agents Machines
            createMachineAgents(mainContainer);

            // Agent Transport Coordinator
            AgentController transportCoord = mainContainer.createNewAgent(
                    "Transport_Coordinator",
                    "festo.agents.TransportCoordinatorAgent",
                    new Object[]{}
            );
            transportCoord.start();

            // Agent Product Manager
            try {
                AgentController productManager = mainContainer.createNewAgent(
                        "Product_Manager",
                        "festo.agents.ProductAgent",
                        new Object[]{}
                );
                productManager.start();
                System.out.println("Product Manager démarré");
            } catch (Exception e) {
                System.out.println("Product Agent non disponible - continuation sans");
            }










            System.out.println("=== Tous les agents ont été démarrés ===");
            System.out.println("Interface JADE: http://localhost:1099");
            System.out.println("Appuyez sur Ctrl+C pour arrêter");

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    private static void createMachineAgents(AgentContainer container)
            throws StaleProxyException {

        String[] machines = {"M1", "M2", "M3", "M4", "T1"};
        String[] types = {"DISTRIBUTION", "MACHINING", "ASSEMBLY", "QUALITY_CONTROL", "TRANSPORT"};
        String[] sites = {"A", "A", "B", "B", "TRANSPORT"};

        for (int i = 0; i < machines.length; i++) {
            AgentController machineAgent = container.createNewAgent(
                    "Machine_" + machines[i],
                    "festo.agents.MachineAgent",
                    new Object[]{machines[i], types[i], sites[i]}
            );
            machineAgent.start();

            System.out.println("Machine agent créé: " + machines[i] +
                    " (" + types[i] + " - Site " + sites[i] + ")");
        }
    }
}