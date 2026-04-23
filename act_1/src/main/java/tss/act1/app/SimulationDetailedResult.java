package tss.act1.app;

import java.util.List;

import tss.act1.problems.ProblemOneResult;
import tss.act1.problems.ProblemTwoResult;

public record SimulationDetailedResult(
        PartOneResult partOne,
        List<ProblemOneResult> problemOneResults,
        ProblemOneResult bestProblemOne,
        List<ProblemTwoResult> problemTwoResults,
        ProblemTwoResult bestProblemTwo) {
}
