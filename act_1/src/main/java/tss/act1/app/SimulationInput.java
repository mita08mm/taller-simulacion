package tss.act1.app;

public record SimulationInput(
        int partOneSamples,
        int problemOneMinMachines,
        int problemOneMaxMachines,
        int problemOneReplications,
        double problemOneHorizonHours,
        double problemOneIdleCostPerHour,
        double problemOneMechanicSalaryPerHour,
        int problemTwoMinTeam,
        int problemTwoMaxTeam,
        int problemTwoReplications,
        double problemTwoShiftHours,
        double problemTwoWorkerSalaryPerHour,
        double problemTwoWaitingCostPerHour) {

    public static SimulationInput defaults() {
        return new SimulationInput(
                100,
                1,
                20,
                100,
                365.0 * 24.0,
                500.0,
                50.0,
                3,
                6,
                100,
                8.0,
                25.0,
                50.0);
    }
}
