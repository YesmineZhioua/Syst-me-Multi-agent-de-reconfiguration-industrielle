package festo.distributed;


import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import festo.utils.Logger;
public class DistributedSystemLauncher {

    public static void main(String[] args) {
        try {
            System.out.println("🚀 ===== DÉMARRAGE SYSTÈME DISTRIBUÉ =====");
            System.out.println("📊 Architecture à 3 niveaux + Générateur de tâches");
            System.out.println("==========================================");

            // Créer le conteneur principal
            Runtime runtime = Runtime.instance();
            // Configurer le profil
            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.MAIN_HOST, "localhost");
            profile.setParameter(Profile.MAIN_PORT, "2099");
            profile.setParameter(Profile.LOCAL_HOST, "localhost");
            profile.setParameter(Profile.LOCAL_PORT, "2099");
            profile.setParameter(Profile.PLATFORM_ID, "DistributedManufacturingPlatform");
            profile.setParameter(Profile.CONTAINER_NAME, "MainContainer");
            profile.setParameter(Profile.GUI, "true");

            // Créer le conteneur principal
            AgentContainer mainContainer = runtime.createMainContainer(profile);
            System.out.println("✅ Plateforme JADE initialisée");

            //  CRÉER LE SUPERVISEUR GLOBAL (NIVEAU 3)
            System.out.println("\n🌐 === NIVEAU 3: SUPERVISEUR GLOBAL ===");
            createGlobalSupervisor(mainContainer);
            Thread.sleep(2000);

            //  CRÉER LES COORDINATEURS (NIVEAU 2)
            System.out.println("\n🏢 === NIVEAU 2: COORDINATEURS DE SITE ===");
            createSiteCoordinators(mainContainer);
            Thread.sleep(2000);

            //CRÉER LES MACHINES (NIVEAU 1)
            System.out.println("\n🤖 === NIVEAU 1: MACHINES AUTONOMES ===");
            createAutonomousMachines(mainContainer);
            Thread.sleep(2000);

            // Transport
            System.out.println("\n🚛 === TRANSPORT COORDINATOR ===");
            createTransportCoordinator(mainContainer);
            Thread.sleep(1000);

            // CRÉER LE GÉNÉRATEUR DE TÂCHES
            System.out.println("\n🎯 === GÉNÉRATEUR DE TÂCHES ===");
            createTaskGenerator(mainContainer);


            //CRÉER LE MONITEUR
            System.out.println("\n📊 === SYSTÈME MONITOR ===");
            AgentController monitor = mainContainer.createNewAgent(
                    "SystemMonitor",
                    "festo.distributed.SystemMonitorAgent",
                    new Object[0]
            );
            monitor.start();
            System.out.println("✅ Moniteur système créé");


            System.out.println("\n✅ ===== SYSTÈME COMPLÈTEMENT OPÉRATIONNEL =====");
            System.out.println("📊 Composants actifs:");
            System.out.println("   ✓ Superviseur Global");
            System.out.println("   ✓ 2 Coordinateurs de Site");
            System.out.println("   ✓ 4 Machines Autonomes");
            System.out.println("   ✓ Coordinateur Transport");
            System.out.println("   ✓ Générateur de Tâches");
            System.out.println("================================================");
            System.out.println("\n🎮 Le système traite maintenant des tâches!");
            System.out.println("👀 Observez les logs pour voir l'activité...\n");

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createGlobalSupervisor(AgentContainer container)
            throws StaleProxyException {
        AgentController supervisor = container.createNewAgent(
                "GlobalSupervisor",
                "festo.distributed.GlobalSupervisorAgent",
                new Object[0]
        );
        supervisor.start();
        System.out.println("✅ Superviseur Global créé");
    }

    private static void createSiteCoordinators(AgentContainer container)
            throws StaleProxyException {
        // Site A
        AgentController coordA = container.createNewAgent(
                "SiteCoordinator_A",
                "festo.distributed.SiteCoordinatorAgent",
                new Object[] { "A" }
        );
        coordA.start();
        System.out.println("✅ Coordinateur Site A créé");

        // Site B
        AgentController coordB = container.createNewAgent(
                "SiteCoordinator_B",
                "festo.distributed.SiteCoordinatorAgent",
                new Object[] { "B" }
        );
        coordB.start();
        System.out.println("✅ Coordinateur Site B créé");
    }

    private static void createAutonomousMachines(AgentContainer container)
            throws StaleProxyException {
        // M1 - Distributeur (Site A)
        AgentController m1 = container.createNewAgent(
                "M1",
                "festo.distributed.AutonomousMachineAgent",
                new Object[] { "M1", "DISTRIBUTION", "A", 10, 2, 50.0 }
        );
        m1.start();
        System.out.println("✅ M1 (Distributeur - Site A)");

        // M2 - Usinage (Site A)
        AgentController m2 = container.createNewAgent(
                "M2",
                "festo.distributed.AutonomousMachineAgent",
                new Object[] { "M2", "MACHINING", "A", 8, 5, 100.0 }
        );
        m2.start();
        System.out.println("✅ M2 (Usinage - Site A)");

        // M3 - Assembleur (Site B)
        AgentController m3 = container.createNewAgent(
                "M3",
                "festo.distributed.AutonomousMachineAgent",
                new Object[] { "M3", "ASSEMBLY", "B", 12, 3, 80.0 }
        );
        m3.start();
        System.out.println("✅ M3 (Assembleur - Site B)");

        // M4 - Contrôle Qualité (Site B)
        AgentController m4 = container.createNewAgent(
                "M4",
                "festo.distributed.AutonomousMachineAgent",
                new Object[] { "M4", "QUALITY_CONTROL", "B", 15, 2, 60.0 }
        );
        m4.start();
        System.out.println("✅ M4 (Contrôle Qualité - Site B)");
    }

    private static void createTransportCoordinator(AgentContainer container)
            throws StaleProxyException {
        AgentController transport = container.createNewAgent(
                "TransportCoordinator",
                "festo.agents.TransportCoordinatorAgent",
                new Object[0]
        );
        transport.start();
        System.out.println("✅ Coordinateur Transport créé");
    }

    /**
     *  NOUVEAU: Créer le générateur de tâches
     */
    private static void createTaskGenerator(AgentContainer container)
            throws StaleProxyException {
        AgentController generator = container.createNewAgent(
                "TaskGenerator",
                "festo.distributed.TaskGeneratorAgent",
                new Object[0]
        );
        generator.start();
        System.out.println("✅ Générateur de Tâches créé");
    }
}