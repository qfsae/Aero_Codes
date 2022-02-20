// Written by: Maurice Nayman
package macro;

import java.util.regex.Pattern;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import star.common.*;
import star.base.neo.*;
import star.base.report.*;
import star.base.report.Report;
import star.base.report.StatisticsReport ;
import star.flow.*;
import star.vis.*;
import star.cae.common.*;
import star.mapping.*;

public class PostPro_v2 extends StarMacro {

public void execute() {
	//Initializing simulation variables and paths for all methods
	Simulation sim = getActiveSimulation();
	String sep = File.separator;
	String sessionDir = sim.getSessionDir();
        String simname = sim.getPresentationName();
    
	String macropath = resolveWorkPath();
	String[] splitPath = macropath.split(Pattern.quote(sep));
	String basepath = splitPath[0];
	for (int i = 1; i < splitPath.length - 2; i++){
		basepath = basepath + sep + splitPath[i];
	}

	String folder = sim.getPresentationName();

	String expath = basepath + sep + "POST" + sep + folder + sep;

	//Post processing parameters
	//Slice parameters: min, max and step are in mm, relative to global coordinate system
	//doLIC, doCP, doCPT acceptable values are 1 or 0, for yes or no
	int xmin = -1000;
	int xmax = 2700;
	int xstep = 25;
	int doXLIC = 1;
	int doXCP = 1;
	int doXCPT = 1;

	int ymin = -850;
	int ystep = 25;
	int doYLIC = 1;
	int doYCP = 1;
	int doYCPT = 1;

	int zmin = 5;
	int zmax = 1755;
	int zstep = 25;
	int doZLIC = 0;
	int doZCP = 0;
	int doZCPT = 1;

	//SurfaceSetup
	int doSurfCP = 1;
	int doSurfLIC = 0;

	//deltaSetup
	int doDelta = 0;
	String baseline = "trial0001";
	String baselinePath = basepath + sep + "POST" + sep + baseline + sep; //DO NOT CHANGE

	//GBMR Setup
	int exportGBMR = 1;

	//hardcopy sizing in pixels. (no need to change right now)
	int hardx = 2300;
	int hardy = 850;

	//Run desired methods
	outputForces(sim, expath, folder);
	outputSurfVis(sim, expath, folder, doSurfCP, doSurfLIC, hardx, hardy);
	Slices(sim, expath, folder, "X", xmin, xmax, xstep, doXLIC, doXCP, doXCPT, hardx, hardy);
	Slices(sim, expath, folder, "Y", ymin, 0, ystep, doYLIC, doYCP, doYCPT, hardx, hardy);
	Slices(sim, expath, folder, "Z", zmin, zmax, zstep, doZLIC, doZCP, doZCPT, hardx, hardy);
	exportSurfCP(sim, expath, folder);

	if(doDelta == 1){
		DeltaCP(sim, expath, folder, baselinePath, baseline, hardx, hardy);	
	}

	if(exportGBMR == 1){
		doGBMR(sim,expath,folder);
	}
	sim.saveState(sessionDir + sep + simname + ".sim");
}

private void outputForces(Simulation sim, String exportpath, String folder) {

	MonitorPlot coeffs = ((MonitorPlot) sim.getPlotManager().getObject("Coefficients"));

    coeffs.export(resolvePath(exportpath + folder + "_Coefficients.csv"), ",");

    File file = new File(resolvePath(exportpath + folder + "_AveragedReports.csv"));
    Collection<Report> reps = sim.getReportManager().getObjects();

    try {
        BufferedWriter out = new BufferedWriter(new FileWriter(file));

        for(Report ri : reps){
            if(ri instanceof StatisticsReport && ri.getPresentationName().contains("Ave")){
            	StatisticsReport vr  = (StatisticsReport) ri;
                out.append(ri.getPresentationName() + ",");
            	}
            }

        out.newLine();

        for(Report ri : reps){
        	if(ri instanceof StatisticsReport && ri.getPresentationName().contains("Ave")){
        		StatisticsReport vr  = (StatisticsReport) ri;
                out.append(vr.getValue() + ",");
            	}
            }

        out.close();
          
    	} catch (IOException ex) {
        	sim.println("Error type: " + ex.getMessage());
    	}
  }

private void outputSurfVis(Simulation sim, String exportpath, String folder, int doSurfCP, int doSurfLIC, int xsize, int ysize) {
	String sep = File.separator;

	double[] orient = {1.0, -1.0};

    String[] names = {"Top", "Bottom"};

    String[] partorder = {"FW", "RW", "Floor"};

    String[] scenes = {"SurfVisCP", "SurfVisShear"};

    int offset = 0;
    int length = scenes.length;

    if(doSurfCP == 0){
    	offset = 1;
    } else if (doSurfLIC == 0){
    	length = 1;
    }

    for(int k = offset; k < length; k++){

		Scene scene = sim.getSceneManager().getScene(scenes[k]);

    	scene.open();

    	Collection<Displayer> disps = scene.getDisplayerManager().getObjects();

    	CurrentView view = scene.getCurrentView();

    	for (int j = 0; j < orient.length; j++){

    		view.setInput(new DoubleVector(new double[] {0.35, 0.0, 0.0}), new DoubleVector(new double[] {0.35, 0.0, orient[j]}), new DoubleVector(new double[] {0.0, orient[j], 0.0}), 0.92, 1, 30.0);

    		for (int i = 0; i < partorder.length; i++){
    			for (Displayer d : disps){
    				if (d.getPresentationName().contains(partorder[i])){
    					d.setOpacity(1.0);

    				} else {
    					d.setOpacity(0.1);

    				}
    				scene.printAndWait(resolvePath(exportpath + sep + scenes[k] + sep + folder + "_" + partorder[i] + " " + names[j] + ".png"), 1, xsize, ysize, true, false);
    			}
    		}
    	}

    	scene.close();
  	}
  }

private void Slices(Simulation sim, String exportpath, String folder, String direction, int minbound, int maxbound, int step, int doLIC, int doCP, int doCPT, int xsize, int ysize) {
	Collection<Region> regs = sim.getRegionManager().getRegions();
	int straight = 1;

	String sep = File.separator;
	String simname = sim.getPresentationName();

	Units m = ((Units) sim.getUnitsManager().getObject("m"));
	Units mm = ((Units) sim.getUnitsManager().getObject("mm"));

	double[] xfoc = {0.0, 0.0, 1.0};
	double[] xpos = {-5.0, 0.0, 1.0};
	double xscale = 1.0;
	double[] yfoc = {2.0, 0.0, 1.75};
	double[] ypos = {2.0, -5.0, 1.75};
	double yscale = 1.75;
	double[] zfoc = {1.5, 0.0, 0.0};
	double[] zpos = {1.5, 0.0, 5.0};
	double zscale = 1.9;

	for(Region r: regs){
		if(r.getPresentationName().contains("FR Toint")){
			straight = 0;
		}
	}

	if (direction == "Y"){
		if (straight == 1){
			maxbound = -1 * step;
		} else {
			maxbound = -1 * minbound;
		}
	}

	PlaneSection slice = (PlaneSection) sim.getPartManager().createImplicitPart(new NeoObjectVector(new Object[] {}), new DoubleVector(new double[] {0.0, 0.0, 1.0}), new DoubleVector(new double[] {0.0, 0.0, 0.0}), 0, 1, new DoubleVector(new double[] {0.0}));
	slice.setPresentationName(direction + "Section");

	slice.getInputParts().setObjects(regs);

	Coordinate coord = slice.getOrientationCoordinate();

	if (direction == "X"){
		coord.setCoordinate(m, m, m, new DoubleVector(new double[] {1.0, 0.0, 0.0}));

	} else if (direction == "Y"){
		coord.setCoordinate(m, m, m, new DoubleVector(new double[] {0.0, 1.0, 0.0}));

	} else {
		coord.setCoordinate(m, m, m, new DoubleVector(new double[] {0.0, 0.0, 1.0}));
	
	}

	Coordinate org = slice.getOriginCoordinate();

	int steps = (maxbound - minbound)/step + 1;

	if (doLIC == 1){
		Scene LIC = sim.getSceneManager().getScene("LIC");

		LIC.open();

		CurrentView view = LIC.getCurrentView();

		if (direction == "X"){
			view.setInput(new DoubleVector(xfoc), new DoubleVector(xpos), new DoubleVector(new double[] {0.0, 0.0, 1.0}), xscale, 1, 30.0);

		} else if (direction == "Y") {
			view.setInput(new DoubleVector(yfoc), new DoubleVector(ypos), new DoubleVector(new double[] {0.0, 0.0, 1.0}), yscale, 1, 30.0);

		} else {
			view.setInput(new DoubleVector(zfoc), new DoubleVector(zpos), new DoubleVector(new double[] {0.0, 1.0, 0.0}), zscale, 1, 30.0);

		}
		
		VectorDisplayer LICDisp = ((VectorDisplayer) LIC.getDisplayerManager().getObject("LIC"));

		LICDisp.getInputParts().setObjects(slice);

		for(int i=0; i<steps; i++){
            double position = (i * step * 1.0 + minbound);

            if (direction == "X"){
				org.setCoordinate(mm, mm, mm, new DoubleVector(new double[] {position, 0.0, 0.0}));

			} else if (direction == "Y"){
				org.setCoordinate(mm, mm, mm, new DoubleVector(new double[] {0.0, position, 0.0}));

			} else {
				org.setCoordinate(mm, mm, mm, new DoubleVector(new double[] {0.0, 0.0, position}));

			}

			LIC.printAndWait(resolvePath(exportpath + sep+ "LIC" + sep + direction + sep + folder + "_" + direction + i + "=" + (int)position + ".png"), 1, xsize, ysize);
        }

        LIC.close(); 
	}

	if (doCP == 1){
		Scene CP = sim.getSceneManager().getScene("Cp");

		CP.open();

		CurrentView view = CP.getCurrentView();

		if (direction == "X"){
			view.setInput(new DoubleVector(xfoc), new DoubleVector(xpos), new DoubleVector(new double[] {0.0, 0.0, 1.0}), xscale, 1, 30.0);

		} else if (direction == "Y") {
			view.setInput(new DoubleVector(yfoc), new DoubleVector(ypos), new DoubleVector(new double[] {0.0, 0.0, 1.0}), yscale, 1, 30.0);

		} else {
			view.setInput(new DoubleVector(zfoc), new DoubleVector(zpos), new DoubleVector(new double[] {0.0, 1.0, 0.0}), zscale, 1, 30.0);

		}
		
		ScalarDisplayer CPDisp = ((ScalarDisplayer) CP.getDisplayerManager().getObject("Cp"));

		CPDisp.getInputParts().setObjects(slice);

		for(int i=0; i<steps; i++){
            double position = (i * step * 1.0 + minbound);

            if (direction == "X"){
				org.setCoordinate(mm, mm, mm, new DoubleVector(new double[] {position, 0.0, 0.0}));

			} else if (direction == "Y"){
				org.setCoordinate(mm, mm, mm, new DoubleVector(new double[] {0.0, position, 0.0}));

			} else {
				org.setCoordinate(mm, mm, mm, new DoubleVector(new double[] {0.0, 0.0, position}));

			}

			CP.printAndWait(resolvePath(exportpath + sep+ "Cp" + sep + direction + sep + folder + "_" + direction + i + "=" + (int)position + ".png"), 1, xsize, ysize);
        }

        CP.close(); 
	}

	if (doCPT == 1){
		Scene CPT = sim.getSceneManager().getScene("CpT");

		CPT.open();

		CurrentView view = CPT.getCurrentView();

		if (direction == "X"){
			view.setInput(new DoubleVector(xfoc), new DoubleVector(xpos), new DoubleVector(new double[] {0.0, 0.0, 1.0}), xscale, 1, 30.0);

		} else if (direction == "Y") {
			view.setInput(new DoubleVector(yfoc), new DoubleVector(ypos), new DoubleVector(new double[] {0.0, 0.0, 1.0}), yscale, 1, 30.0);

		} else {
			view.setInput(new DoubleVector(zfoc), new DoubleVector(zpos), new DoubleVector(new double[] {0.0, 1.0, 0.0}), zscale, 1, 30.0);

		}
		
		ScalarDisplayer CPTDisp = ((ScalarDisplayer) CPT.getDisplayerManager().getObject("CpT"));

		CPTDisp.getInputParts().setObjects(slice);

		for(int i=0; i<steps; i++){
            double position = (i * step * 1.0 + minbound);

            if (direction == "X"){
				org.setCoordinate(mm, mm, mm, new DoubleVector(new double[] {position, 0.0, 0.0}));

			} else if (direction == "Y"){
				org.setCoordinate(mm, mm, mm, new DoubleVector(new double[] {0.0, position, 0.0}));

			} else {
				org.setCoordinate(mm, mm, mm, new DoubleVector(new double[] {0.0, 0.0, position}));

			}

			CPT.printAndWait(resolvePath(exportpath + sep+ "CpT" + sep + direction + sep + folder + "_" + direction + i + "=" + (int)position + ".png"), 1, xsize, ysize);
        }

        CPT.close(); 
	}

	sim.getPartManager().removeObjects(slice);
  }

private void exportSurfCP(Simulation sim, String exportpath, String folder){
	Collection<Boundary> allbounds = new ArrayList<Boundary>();

	Collection<Region> regstemp = sim.getRegionManager().getRegions();

	Collection<Region> regs = new ArrayList<Region>();

	for (Region r : regstemp){
		if (!r.getPresentationName().contains("Rad") && !r.getPresentationName().contains("Fan")){
			regs.add(r);
		}
	}

	for (Region r : regs){
		Collection<Boundary> tempbounds = r.getBoundaryManager().getBoundaries();

		for (Boundary b : tempbounds){
			if (!b.getPresentationName().contains("Toint") && !b.getPresentationName().contains("Interface") && !b.getPresentationName().contains("Tunnel")){
				allbounds.add(b);
			}
		}
	}

	UserFieldFunction CP = ((UserFieldFunction) sim.getFieldFunctionManager().getFunction("Cp"));

	ImportManager importManager_0 = sim.getImportManager();

    importManager_0.export(resolvePath(exportpath + folder + "SBD.sbd"), new NeoObjectVector(new Object[] {}), allbounds, new NeoObjectVector(new Object[] {}), new NeoObjectVector(new Object[] {}), new NeoObjectVector(new Object[] {CP}), NeoProperty.fromString("{\'exportFormatType\': 5, \'appendToFile\': false, \'solutionOnly\': false, \'dataAtVerts\': false}"));

  }

private void DeltaCP(Simulation sim, String exportpath, String folder, String basepath, String baseline, int xsize, int ysize){
	String sep = File.separator;

	Collection<Boundary> targetBounds = new ArrayList<Boundary>();

	Collection<Region> regstemp = sim.getRegionManager().getRegions();

	Collection<Region> regs = new ArrayList<Region>();

	double[] orient = {1.0, -1.0};

    String[] names = {"Top", "Bottom"};

	for (Region r : regstemp){
		if (!r.getPresentationName().contains("Rad") && !r.getPresentationName().contains("Fan")){
			regs.add(r);
		}
	}

	for (Region r : regs){
		Collection<Boundary> tempbounds = r.getBoundaryManager().getBoundaries();

		for (Boundary b : tempbounds){
			if (!b.getPresentationName().contains("Toint") && !b.getPresentationName().contains("Interface") && !b.getPresentationName().contains("Tunnel") && !b.getPresentationName().contains("Rad")){
				targetBounds.add(b);
			}
		}
	}

	Units baseUnits = sim.getUnitsManager().getPreferredUnits(Dimensions.Builder().length(1).build());

    CaeImportManager caeImportManager_0 = sim.get(CaeImportManager.class);

    caeImportManager_0.importSbdModelFile(resolvePath(basepath + baseline + "SBD.sbd"), baseUnits);

    PrimitiveFieldFunction impCP = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("ImportedCp"));

    SurfaceDataMapper surfaceDataMapper_0 = sim.get(DataMapperManager.class).createMapper(SurfaceDataMapper.class, "Surface Data Mapper");

    ImportedModel importedModel_0 = ((ImportedModel) sim.get(ImportedModelManager.class).getImportedModel("Sbd: " + baseline + "SBD"));

    Collection<ImportedSurface> impSurfs = importedModel_0.getImportedSurfaceManager().getImportedSurfaces();

    surfaceDataMapper_0.getSourceParts().setObjects(impSurfs);

    surfaceDataMapper_0.setScalarFieldFunctions(new NeoObjectVector(new Object[] {impCP}));

    SurfaceTargetSpecification surfaceTargetSpecification_0 = ((SurfaceTargetSpecification) surfaceDataMapper_0.getTargetSpecificationManager().getObject("Surface 1"));

    surfaceTargetSpecification_0.getTargetParts().setObjects(targetBounds);

    surfaceDataMapper_0.setSourceStencil(DataMapper.SurfaceDataLocationOption.FACE);

    surfaceTargetSpecification_0.setDataMappingMethod(1);

    surfaceTargetSpecification_0.setTargetStencil(DataMapper.SurfaceDataLocationOption.FACE);

    surfaceDataMapper_0.mapData();

    Scene scene = sim.getSceneManager().getScene("deltaCP");

    ScalarDisplayer dCP = ((ScalarDisplayer) scene.getDisplayerManager().getObject("dCP"));

    dCP.getInputParts().setObjects(targetBounds);

    CurrentView view = scene.getCurrentView();

    for (int i = 0; i < orient.length; i++){

    	view.setInput(new DoubleVector(new double[] {0.35, 0.0, 0.0}), new DoubleVector(new double[] {0.35, 0.0, orient[i]}), new DoubleVector(new double[] {0.0, orient[i], 0.0}), 0.92, 1, 30.0);

		scene.printAndWait(resolvePath(exportpath + "DeltaTo_" + baseline + sep + folder + "_vs_" + baseline + "_" + names[i] + ".png"), 1, xsize, ysize, true, false);

    }

    scene.close();

    sim.get(DataMapperManager.class).removeObjects(surfaceDataMapper_0);

    ShellRegion impReg = ((ShellRegion) sim.getRegionManager().getRegion("Sbd: " + baseline + "SBD"));

    sim.getRegionManager().removeRegions(Arrays.<Region>asList(impReg));

  }

private void doGBMR(Simulation sim, String exportpath, String folder){
	ThresholdPart GBMR = ((ThresholdPart) sim.getPartManager().getObject("GBMR_Volume"));

	FvRepresentation fvRep = ((FvRepresentation) sim.getRepresentationManager().getObject("Volume Mesh"));

	GBMR.exportDataSourceSTL(fvRep, false, resolvePath(exportpath + folder + "_GBMR.stl"));
  }
}
