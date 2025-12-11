package festo;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import java.util.Scanner;

public class MainLauncher {

    // Couleurs ANSI (optionnel - s'affichent dans les terminaux compatibles)
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";
    private static final String UNDERLINE = "\u001B[4m";

    public static void main(String[] args) {
        clearScreen();
        printHeader();
        printMenu();

        try {
            Scanner scanner = new Scanner(System.in);
            int choix = scanner.nextInt();

            switch(choix) {
                case 1:
                    clearScreen();
                    printSectionHeader("ARCHITECTURE CENTRALISÉE");
                    launchCentralisedArchitecture();
                    break;
                case 2:
                    clearScreen();
                    printSectionHeader("ARCHITECTURE COMPOSITE");
                    launchCompositeArchitecture();
                    break;
                default:
                    System.out.println(RED + "\n[ERREUR] Choix invalide. Lancement centralisé par défaut." + RESET);
                    waitSeconds(2);
                    clearScreen();
                    printSectionHeader("ARCHITECTURE CENTRALISÉE");
                    launchCentralisedArchitecture();
            }
        } catch (Exception e) {
            System.out.println(RED + "\n[ERREUR] " + e.getMessage() + RESET);
            e.printStackTrace();
        }
    }

    private static void launchCentralisedArchitecture() throws StaleProxyException {
        Runtime rt = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "1099");
        profile.setParameter(Profile.GUI, "true");

        AgentContainer mainContainer = rt.createMainContainer(profile);

        printProgressBar(0);
        System.out.println(CYAN + "\n[INITIALISATION] Démarrage du conteneur JADE..." + RESET);
        waitSeconds(1);

        printProgressBar(20);
        System.out.println(CYAN + "[AGENT] Lancement du contrôleur RLRA Centralisé..." + RESET);

        // Agent Contrôleur RLRA (centralisé)
        AgentController rlraController = mainContainer.createNewAgent(
                "RLRA_Controller_Central",
                "festo.agents.centralised.RLRAControllerAgent",
                new Object[]{}
        );
        rlraController.start();
        printSuccess("Contrôleur RLRA Centralisé");
        waitSeconds(1);

        printProgressBar(40);
        System.out.println(CYAN + "[AGENT] Lancement des monitors..." + RESET);

        // Monitors
        AgentController monitorA = mainContainer.createNewAgent(
                "Monitor_SiteA_Central",
                "festo.agents.centralised.SiteMonitorAgent",
                new Object[]{"SiteA", new String[]{"M1", "M2"}}
        );
        monitorA.start();

        AgentController monitorB = mainContainer.createNewAgent(
                "Monitor_SiteB_Central",
                "festo.agents.centralised.SiteMonitorAgent",
                new Object[]{"SiteB", new String[]{"M3", "M4"}}
        );
        monitorB.start();
        printSuccess("Monitors centralisés (SiteA, SiteB)");
        waitSeconds(1);

        printProgressBar(60);
        System.out.println(CYAN + "[AGENT] Lancement des machines..." + RESET);

        // Machines (communes)
        createMachineAgents(mainContainer, "Central");
        waitSeconds(1);

        printProgressBar(80);
        System.out.println(CYAN + "[AGENT] Lancement du coordinateur transport..." + RESET);

        // Transport Coordinator (commun)
        try {
            AgentController transportCoord = mainContainer.createNewAgent(
                    "Transport_Coordinator_Central",
                    "festo.agents.common.TransportCoordinatorAgent",
                    new Object[]{}
            );
            transportCoord.start();
            printSuccess("Coordinateur Transport");
        } catch (Exception e) {
            printWarning("Transport Coordinator non disponible");
        }

        printProgressBar(100);
        waitSeconds(1);

        printSuccessBox("ARCHITECTURE CENTRALISÉE PRÊTE");

        System.out.println("\n" + BLUE + "=".repeat(60) + RESET);
        System.out.println(YELLOW + "INFORMATIONS DE CONNEXION:" + RESET);
        System.out.println(BLUE + "-".repeat(60) + RESET);
        System.out.println("Interface JADE: " + UNDERLINE + "http://localhost:1099" + RESET);
        System.out.println("Conteneur Principal: " + GREEN + "Actif" + RESET);
        System.out.println("Architecture: " + PURPLE + "Centralisée" + RESET);
        System.out.println("Agents Déployés: " + GREEN + "9 agents" + RESET);
        System.out.println(BLUE + "=".repeat(60) + RESET);

        System.out.println(YELLOW + "\nCommandes:" + RESET);
        System.out.println("• " + CYAN + "Appuyez sur Ctrl+C pour arrêter le système" + RESET);
        System.out.println("• " + CYAN + "Consultez l'interface web pour les détails des agents" + RESET);
        System.out.println();
    }

    private static void launchCompositeArchitecture() throws StaleProxyException {
        Runtime rt = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "1099");
        profile.setParameter(Profile.GUI, "true");

        AgentContainer mainContainer = rt.createMainContainer(profile);

        printProgressBar(0);
        System.out.println(CYAN + "\n[INITIALISATION] Démarrage du conteneur JADE..." + RESET);
        waitSeconds(1);

        printProgressBar(20);
        System.out.println(CYAN + "[AGENT] Lancement du contrôleur RLRA Composite..." + RESET);

        // Agent RLRA Composite
        AgentController rlraComposite = mainContainer.createNewAgent(
                "RLRA_Controller_Composite",
                "festo.agents.composite.RLRACompositeAgent",
                new Object[]{}
        );
        rlraComposite.start();
        printSuccess("Contrôleur RLRA Composite");
        waitSeconds(1);

        printProgressBar(40);
        System.out.println(CYAN + "[AGENT] Lancement des monitors..." + RESET);

        // Monitors
        AgentController monitorA = mainContainer.createNewAgent(
                "Monitor_SiteA_Composite",
                "festo.agents.centralised.SiteMonitorAgent",
                new Object[]{"SiteA", new String[]{"M1", "M2"}}
        );
        monitorA.start();

        AgentController monitorB = mainContainer.createNewAgent(
                "Monitor_SiteB_Composite",
                "festo.agents.centralised.SiteMonitorAgent",
                new Object[]{"SiteB", new String[]{"M3", "M4"}}
        );
        monitorB.start();
        printSuccess("Monitors composites (SiteA, SiteB)");
        waitSeconds(1);

        printProgressBar(60);
        System.out.println(CYAN + "[AGENT] Lancement des machines..." + RESET);

        // Machines (communes)
        createMachineAgents(mainContainer, "Composite");
        waitSeconds(1);

        printProgressBar(80);
        System.out.println(CYAN + "[AGENT] Lancement du coordinateur transport..." + RESET);

        // Transport Coordinator (commun)
        try {
            AgentController transportCoord = mainContainer.createNewAgent(
                    "Transport_Coordinator_Composite",
                    "festo.agents.common.TransportCoordinatorAgent",
                    new Object[]{}
            );
            transportCoord.start();
            printSuccess("Coordinateur Transport");
        } catch (Exception e) {
            printWarning("Transport Coordinator non disponible");
        }

        printProgressBar(100);
        waitSeconds(1);

        printSuccessBox("ARCHITECTURE COMPOSITE PRÊTE");

        System.out.println("\n" + BLUE + "=".repeat(60) + RESET);
        System.out.println(YELLOW + "INFORMATIONS DE CONNEXION:" + RESET);
        System.out.println(BLUE + "-".repeat(60) + RESET);
        System.out.println("Interface JADE: " + UNDERLINE + "http://localhost:1099" + RESET);
        System.out.println("Conteneur Principal: " + GREEN + "Actif" + RESET);
        System.out.println("Architecture: " + PURPLE + "Composite" + RESET);
        System.out.println("Agents Déployés: " + GREEN + "9 agents" + RESET);
        System.out.println(BLUE + "=".repeat(60) + RESET);

        System.out.println(YELLOW + "\nCaractéristiques de l'architecture composite:" + RESET);
        System.out.println("✓ " + GREEN + "Décision distribuée" + RESET);
        System.out.println("✓ " + GREEN + "Communication inter-agents optimisée" + RESET);
        System.out.println("✓ " + GREEN + "Redondance améliorée" + RESET);

        System.out.println(YELLOW + "\nCommandes:" + RESET);
        System.out.println("• " + CYAN + "Appuyez sur Ctrl+C pour arrêter le système" + RESET);
        System.out.println("• " + CYAN + "Consultez l'interface web pour les détails des agents" + RESET);
        System.out.println();
    }

    private static void createMachineAgents(AgentContainer container, String architecture)
            throws StaleProxyException {

        String[] machines = {"M1", "M2", "M3", "M4", "T1"};
        String[] types = {"DISTRIBUTION", "MACHINING", "ASSEMBLY", "QUALITY_CONTROL", "TRANSPORT"};
        String[] sites = {"A", "A", "B", "B", "TRANSPORT"};

        System.out.println("\n" + CYAN + "[MACHINES] Déploiement des équipements..." + RESET);
        System.out.println(BLUE + "-".repeat(50) + RESET);

        for (int i = 0; i < machines.length; i++) {
            String agentName = "Machine_" + machines[i] + "_" + architecture;

            AgentController machineAgent = container.createNewAgent(
                    agentName,
                    "festo.agents.common.MachineAgent",
                    new Object[]{machines[i], types[i], sites[i]}
            );
            machineAgent.start();

            String siteDisplay = sites[i].equals("TRANSPORT") ? "Transport" : "Site " + sites[i];
            System.out.printf("  %s%-3s%s │ %-20s │ Site: %-10s │ Statut: %sACTIF%s\n",
                    YELLOW, machines[i], RESET,
                    formatMachineType(types[i]),
                    siteDisplay,
                    GREEN, RESET);

            waitMilliseconds(200);
        }
        System.out.println(BLUE + "-".repeat(50) + RESET);
    }

    // Méthodes d'affichage améliorées
    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void printHeader() {
        System.out.println(BLUE + "╔══════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BLUE + "║" + BOLD + "                SYSTÈME FESTO MAS - JADE                 " + BLUE + "║" + RESET);
        System.out.println(BLUE + "╠══════════════════════════════════════════════════════════════╣" + RESET);
        System.out.println(BLUE + "║" + RESET + "  Architecture Multi-Agents pour Contrôle Industriel   " + BLUE + "║" + RESET);
        System.out.println(BLUE + "╚══════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
    }

    private static void printMenu() {
        System.out.println(YELLOW + "SÉLECTIONNEZ L'ARCHITECTURE :" + RESET);
        System.out.println();
        System.out.println(BLUE + "┌────────────────────────────────────────────────────────────┐" + RESET);
        System.out.println(BLUE + "│" + RESET + "  1. " + BOLD + "ARCHITECTURE CENTRALISÉE" + RESET + "                                   " + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + RESET + "     • Contrôle central unique                         " + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + RESET + "     • Décision hiérarchique                          " + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + RESET + "     • Supervision globale                            " + BLUE + "│" + RESET);
        System.out.println(BLUE + "├────────────────────────────────────────────────────────────┤" + RESET);
        System.out.println(BLUE + "│" + RESET + "  2. " + BOLD + "ARCHITECTURE COMPOSITE" + RESET + "                                      " + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + RESET + "     • Contrôle distribué                             " + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + RESET + "     • Décision coopérative                           " + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + RESET + "     • Redondance et résilience                       " + BLUE + "│" + RESET);
        System.out.println(BLUE + "└────────────────────────────────────────────────────────────┘" + RESET);
        System.out.println();
        System.out.print(YELLOW + "Votre choix (1-2) : " + RESET);
    }

    private static void printSectionHeader(String title) {
        System.out.println(BLUE + "\n╔══════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BLUE + "║" + BOLD + PURPLE + "                    " + title + "                    " + BLUE + "║" + RESET);
        System.out.println(BLUE + "╚══════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
    }

    private static void printProgressBar(int percentage) {
        int width = 50;
        int filled = (percentage * width) / 100;

        System.out.print("\r" + CYAN + "[");
        for (int i = 0; i < width; i++) {
            if (i < filled) {
                System.out.print("█");
            } else {
                System.out.print("░");
            }
        }
        System.out.print("] " + percentage + "%" + RESET);
        System.out.flush();
    }

    private static void printSuccess(String message) {
        System.out.println("  " + GREEN + "✓ " + message + " démarré avec succès" + RESET);
    }

    private static void printWarning(String message) {
        System.out.println("  " + YELLOW + "⚠ " + message + RESET);
    }

    private static void printSuccessBox(String message) {
        System.out.println("\n" + GREEN + "╔══════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(GREEN + "║" + BOLD + "                      " + message + "                      " + GREEN + "║" + RESET);
        System.out.println(GREEN + "╚══════════════════════════════════════════════════════════════╝" + RESET);
    }

    private static String formatMachineType(String type) {
        switch(type) {
            case "DISTRIBUTION": return "Distributeur";
            case "MACHINING": return "Machine Usinage";
            case "ASSEMBLY": return "Station Assemblage";
            case "QUALITY_CONTROL": return "Contrôle Qualité";
            case "TRANSPORT": return "Transporteur";
            default: return type;
        }
    }

    private static void waitSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void waitMilliseconds(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}