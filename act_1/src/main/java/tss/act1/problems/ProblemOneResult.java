package tss.act1.problems;

public record ProblemOneResult(
        int machinesPerMechanic,
        double averageDowntimeHoursPerMachine,
        double idleCostPerMachineHour,
        double mechanicSalaryPerMachineHour,
        double totalCostPerMachineHour) {
}
