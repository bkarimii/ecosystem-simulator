package uk.co.cpsd.javaproject1;

public class SimulatorRunner {
    public static void noGUISimulation(int ticks, int numOfGoats, int numOfLions, boolean isGUIMode,
            AudioPlayer player) {
        World world = new World(numOfGoats, numOfLions, isGUIMode, player);

        for (int i = 1; i <= ticks; i++) {
            world.tick();
        }

        world.writeToCSV(world.getGoatPopulationHistory(), world.getGrassPopulationHistory(),
                world.getLionPopulationHistory());
        GoatChart chart = new GoatChart(world.getGoatPopulationHistory(), world.getGrassPopulationHistory(),
                world.getLionPopulationHistory());
        chart.setVisible(true);
    }

}
