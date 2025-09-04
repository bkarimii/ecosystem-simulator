package uk.co.cpsd.javaproject1;

import java.awt.event.ActionEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class SimulatorFrame extends JFrame {
    private World world;
    private WorldPanel worldPanel;
    private Timer timer;

    public SimulatorFrame(int tickLimitSimulation, int numOfGoats, int numOfLions, boolean isGUIMode,
            AudioPlayer player) {
        world = new World(numOfGoats, numOfLions, isGUIMode, player);

        worldPanel = new WorldPanel(world);
        add(worldPanel);

        setTitle("Ecosystem Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);

        // Set up simulation timer (1 tick per 500ms)
        timer = new Timer(1000, (ActionEvent e) -> {
            world.tick(); // Tick: grow grass, energy, reproduction
            worldPanel.repaint(); // Show updates on the screen

            if (world.getTicksElapsed() >= tickLimitSimulation) {
                world.writeToCSV(world.getGoatPopulationHistory(), world.getGrassPopulationHistory(),
                        world.getLionPopulationHistory());
                timer.stop();

                SwingUtilities.invokeLater(() -> {
                    GoatChart chart = new GoatChart(world.getGoatPopulationHistory(),
                            world.getGrassPopulationHistory(), world.getLionPopulationHistory());
                    chart.setVisible(true);
                });
            }
        });

        // Start the simulation
        timer.start();

    }
}
