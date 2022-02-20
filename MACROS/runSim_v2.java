// Written by Maurice Nayman
package macro;

import java.util.*;
import java.io.*;
import java.io.File;

import star.common.*;
import star.coupledflow.*;
import star.base.neo.*;
import star.base.report.*;
import star.flow.*;
import star.vis.*;
import star.meshing.*;
import star.metrics.*;

public class runSim_v2 extends StarMacro {
    private static int averagingIterations = 200;
    private static int totalIts = 500;
    
    public static double A_C = 1.3;                 //Lateral acceleration [G's]
    public static double V = 16.0;                  //Straight Line speed for half-car [m/s]

  public void execute() {
    Simulation sim = getActiveSimulation();
    String sessionDir = sim.getSessionDir();
    String simname = sim.getPresentationName();
    String sep = File.separator;
    
    setAveraging(sim, averagingIterations, totalIts);
    runSim(sim);
    
    sim.saveState(sessionDir + sep + simname + ".sim");
  }

private static void setAveraging(Simulation sim, int aveIts, int totalIts){
    Collection<SolverStoppingCriterion> criteria = sim.getSolverStoppingCriterionManager().getObjects();

    Collection<Report> reps = sim.getReportManager().getObjects();

    sim.println("\nSetting number of iterations for averaging to " + aveIts);
    sim.println("\nSetting total number of iterations to " + totalIts);

    for (SolverStoppingCriterion cri : criteria){
        if (cri instanceof StepStoppingCriterion){
            StepStoppingCriterion c = (StepStoppingCriterion) cri;
            c.setMaximumNumberSteps(totalIts);
        }
    }

    for (Report rep : reps){
        if (rep instanceof StatisticsReport){
            StatisticsReport r = (StatisticsReport) rep;
            LastNSamplesFilter filter = ((LastNSamplesFilter) r.getSampleFilterManager().getObject("Last N Samples"));
            filter.setNSamples(aveIts);
        }
    }

    Collection<Monitor> means = sim.getMonitorManager().getObjects();

    for(Monitor mon : means){
        if (mon instanceof FieldMeanMonitor){
            FieldMeanMonitor meanmon = ((FieldMeanMonitor) sim.getMonitorManager().getMonitor(mon.getPresentationName()));

            meanmon.resetData();

            StarUpdate starUpdate_0 = meanmon.getStarUpdate();

            IterationUpdateFrequency iterationUpdateFrequency_2 = starUpdate_0.getIterationUpdateFrequency();

            iterationUpdateFrequency_2.setStart(totalIts - aveIts);
        }
    }
  }

private void runSim(Simulation sim) {
    Collection<Region> regs = sim.getRegionManager().getRegions();
    int straight = 1;

    for(Region r: regs){
        if(r.getPresentationName().contains("FR Toint")){
            straight = 0;
        }
    }
    
    if (straight == 0){    
        ScalarGlobalParameter Ac = ((ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("a_c"));
        Ac.getQuantity().setDefinition(A_C + "*${g}");
    } else {
        ScalarGlobalParameter Vel = ((ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("VMag"));
        Vel.getQuantity().setValue(V);
    }
    
    int yplus = 100;
    int doGSI = 0;
    
    PhysicsContinuum physicsContinuum_0 = ((PhysicsContinuum) sim.getContinuumManager().getContinuum("Physics 1"));

    GradientsModel gradientsModel_0 = physicsContinuum_0.getModelManager().getModel(GradientsModel.class);
    
    if(yplus == 1){
        gradientsModel_0.setNormalizedCurvatureFactor(1.0E-20);
    }else{
        gradientsModel_0.setNormalizedCurvatureFactor(1);
    }
    
    CoupledImplicitSolver coupledImplicitSolver_0 = ((CoupledImplicitSolver) sim.getSolverManager().getSolver(CoupledImplicitSolver.class));
    
    if(doGSI == 1){
        coupledImplicitSolver_0.getExpertInitManager().getExpertInitOption().setSelected(ExpertInitOption.Type.GRID_SEQ_METHOD);
    }else{
        coupledImplicitSolver_0.getExpertInitManager().getExpertInitOption().setSelected(ExpertInitOption.Type.NO_METHOD);
    }
    
    Solution solution_0 = sim.getSolution();

    solution_0.clearSolution();

    sim.getSimulationIterator().runAutomation();
  }
}
