// Written by Maurice Nayman
package macro;

import java.util.regex.Pattern; 
import star.common.*;
import star.base.neo.*;
import java.io.*;
import java.util.*;
import star.vis.*;
import star.meshing.*;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import java.lang.Math.*;
import star.cadmodeler.*;

public class runMesh_v2 extends StarMacro {
    //Definition of all publica variables that get used for model setup
    public static double cgx = 0.50;                //CG location along the x-axis, relative to front axle in percent
    public static double wheelbase = 1.550;         //Wheelbase in metres
    public static double tire_rad = 18.3;           //Tire diameter in inches
    public static double Front_Camber = -1.5;       //Degrees
    public static double Front_Toe = 0.0;           //Degrees, toe-out positive
    public static double FT = 0.575;                //Half Front Track [m]
    public static double RT = 0.5625;               //Half Rear Track [m]
    public static double Rear_Camber = 0.0;
    public static double Rear_Toe = 0.0;
    public static double FRH_Off = 0.0;             //Front Ride Height Offset [m]
    public static double RRH_Off = 0.0;             //Rear Ride Height Offset [m]
    public static double Roll = 0.0;
    public static double Yaw = 0.0;

    public static double COR = 30;                  //Corner radius [m]

    public static double[] rad_1_O = {1.090997, -0.56896, 0.106872};
    public static double[] rad_1_X = {0.9762960071199334, 0.0, 0.21643961393810288};
    public static double[] rad_1_Y = {0.0, 1.0, 0.0};

    public static double[] rad_2_O = {1.090997, 0.56896, 0.106872};
    public static double[] rad_2_X = {0.9762960071199334, 0.0, 0.21643961393810288};
    public static double[] rad_2_Y = {0.0, 1.0, 0.0};

public void execute() {
    Simulation sim = getActiveSimulation();
    String sessionDir = sim.getSessionDir();
    String simname = sim.getPresentationName();
    String sep = File.separator;

    Collection<Region> regs = sim.getRegionManager().getRegions();
    int straight = 1;

    for(Region r: regs){
        if(r.getPresentationName().contains("FR Toint")){
            straight = 0;
        }
    }

    UpdateParams(sim, straight);
    Mesher(sim, straight);
    sim.saveState(sessionDir + sep + simname + ".sim");
  }

private void Mesher(Simulation sim, int straight) {
    String sep = File.separator;
    
    //Read in setup file that contains the parts to import
    ArrayList<String> inParts = new ArrayList<>();

    try{
        File f = new File(sim.getSessionDir() + sep + "meshFiles.txt");
        Scanner s = new Scanner(f);
        while(s.hasNextLine()){
            String line = s.nextLine();
            if(!line.isEmpty()){
                inParts.add(line);
            }
        }
    } catch(Exception e){
        e.printStackTrace();
    }

    //Loop over the parts, other than subtracts, check if the file is already in the sim file, and delete it if it's not in the text file
    ArrayList<GeometryPart> delParts = new ArrayList<>();

    Collection<GeometryPart> simParts = sim.get(SimulationPartManager.class).getParts();

    for(GeometryPart part : simParts){
        String pName = part.getPresentationName();
        if(!pName.contains("Sub") && !pName.contains("Refine") && !pName.contains("Tunnel") && !pName.contains("Wrap")){
            delParts.add(part);
        }
    }

    for(GeometryPart part : delParts){

        int partCheck = 0;
        String pName = part.getPresentationName();

        for(String p : inParts){
            String inP = p;
            if(p.endsWith(".stl")){
                inP = p.substring(0, p.length() - 4);
            }
            if(pName.equals(inP)){
                partCheck = 1;
                inParts.remove(p);
                break;
            }
        }

        if(partCheck == 0){
            sim.get(SimulationPartManager.class).removeParts(new NeoObjectVector(new Object[] {part}));
        }
    }

    for(String p : inParts){
        partImporting(sim, p);
    }
    
    //Determine if there's a GBMR file in the text file and modify the automated mesh setup
    Collection<GeometryPart> finalParts = sim.get(SimulationPartManager.class).getParts();

    AutoMeshOperation autoMesh = ((AutoMeshOperation) sim.get(MeshOperationManager.class).getObject("Automated Mesh"));

    VolumeCustomMeshControl Refine = ((VolumeCustomMeshControl) autoMesh.getCustomMeshControls().getObject("50% Refine"));

    CompositePart compositePart_0 = ((CompositePart) sim.get(SimulationPartManager.class).getPart("Z - Refinement Blocks"));

    GeometryPart floorRefine = compositePart_0.getChildParts().getPart("Floor Refine");

    MeshOperationPart meshOperationPart_0 = ((MeshOperationPart) compositePart_0.getChildParts().getPart("Offset1"));
    
    for(GeometryPart p : finalParts){
        if(p.getPresentationName().contains("GBMR")){

            Refine.getGeometryObjects().setObjects(p, meshOperationPart_0, floorRefine);
            
            break;

        } else {
            SolidModelPart solidModelPart_1 = ((SolidModelPart) compositePart_0.getChildParts().getPart("Wake Refine 1"));

            Refine.getGeometryObjects().setObjects(meshOperationPart_0, solidModelPart_1, floorRefine);
        }
    }
    
    //Save sim file, then save as final model for trial copying.
    String sessionDir = sim.getSessionDir();
    String simname = sim.getPresentationName();

    sim.saveState(sessionDir + sep + simname + "_PreMesh.sim");
    sim.saveState(sessionDir + sep + simname + ".sim");
    
    //Run through mesh operations
    Collection<MeshOperation> meshOps = sim.get(MeshOperationManager.class).getObjects();

    for(MeshOperation op : meshOps){
        op.execute();
    }
  }

private void partImporting(Simulation sim, String part){
    String sep = File.separator;

    //Find the base path where all the files are to be imported and import 'em as an stl in mm
    String macropath = resolveWorkPath();
    String[] splitPath = macropath.split(Pattern.quote(sep));
    String basepath = splitPath[0];
    for (int i = 1; i < splitPath.length - 2; i++){
        basepath = basepath + sep + splitPath[i];
    }
    String importpath = basepath + sep + "IMPORT" + sep;

    PartImportManager partImportManager_0 = sim.get(PartImportManager.class);

    Units units_1 = ((Units) sim.getUnitsManager().getObject("mm"));

    String pName = part;
    if(part.endsWith(".stl")){
        pName = part.substring(0, part.length() - 4);
        partImportManager_0.importStlParts(new StringVector(new String[] {resolvePath(importpath + part)}), "OneSurfacePerPatch", "OnePartPerFile", units_1, true, 1.0E-5);
    } else{
        partImportManager_0.importStlParts(new StringVector(new String[] {resolvePath(importpath + part + ".stl")}), "OneSurfacePerPatch", "OnePartPerFile", units_1, true, 1.0E-5);
    }

    MeshPart inPart = ((MeshPart) sim.get(SimulationPartManager.class).getPart(pName));

    Collection<PartSurface> surfs = inPart.getPartSurfaceManager().getPartSurfaces();

    if(!pName.contains("GBMR")){
        PartCurve featureedge = inPart.createPartCurvesOnPartSurfaces(surfs, true, false, true, false, false, false, true, 31.0, false);
    }

  }

private void UpdateParams(Simulation sim, int straight){
    double PI = Math.PI;

    double FL_Steering = Math.toDegrees(Math.atan(wheelbase/COR)); //Degrees, left positive
    double FR_Steering = Math.toDegrees(Math.atan(wheelbase/COR)); //Degrees, left positive

    // Convert all angles to radians
    double alfa_front_rad = -Front_Toe*PI/180;
    double theta_front_rad = Front_Camber*PI/180;
    double beta_front_right_rad = FR_Steering*PI/180;
    double beta_front_left_rad = FL_Steering*PI/180;
    double alfa_rear_rad = -Rear_Toe*PI/180;
    double theta_rear_rad = Rear_Camber*PI/180;
    double roll_rad = Roll*PI/180;
    double tire_half = tire_rad*0.0254/2;

    if(straight == 1){
        beta_front_left_rad = 0;
        beta_front_right_rad = 0;
        roll_rad = 0;
    }

    //**************************** SET UP GROUND PLANE ********************************//
    double GROUND_ORIGIN_Z = -FRH_Off;
    double GROUND_X_X = wheelbase;
    double GROUND_X_Z = -RRH_Off - GROUND_ORIGIN_Z;
    double GROUND_Y_Y = 0.1;
    double GROUND_Y_Z = -0.1*Math.sin(roll_rad); 

    GROUND_X_X = GROUND_X_X/Math.sqrt(Math.pow(GROUND_X_X,2) + Math.pow(GROUND_X_Z,2));
    GROUND_X_Z = GROUND_X_Z/Math.sqrt(Math.pow(GROUND_X_X,2) + Math.pow(GROUND_X_Z,2));

    GROUND_Y_Y = GROUND_Y_Y/Math.sqrt(Math.pow(GROUND_Y_Y,2) + Math.pow(GROUND_Y_Z,2));
    GROUND_Y_Z = GROUND_Y_Z/Math.sqrt(Math.pow(GROUND_Y_Y,2) + Math.pow(GROUND_Y_Z,2));

    SetCoord(sim, "Ground", new double[] {0, 0, GROUND_ORIGIN_Z}, new double[] {GROUND_X_X, 0, GROUND_X_Z}, new double[] {0.0, GROUND_Y_Y, GROUND_Y_Z});

    //**************************** SET FREESTREAM PARAMETERS AND UPDATE DOMAIN SIZE *****************************//
    if (straight == 0){    
        ScalarGlobalParameter CoR = ((ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Radius"));

        CoR.getQuantity().setValue(COR);

        ScalarGlobalParameter cg = ((ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("cgx"));

        cg.getQuantity().setValue(cgx);

        ScalarGlobalParameter Y = ((ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Yaw"));

        Y.getQuantity().setValue(Yaw);

    }

    CadModel cadModel_0 = ((CadModel) sim.get(SolidModelManager.class).getObject("3D-CAD Model 1"));

    cadModel_0.update();

    CompositePart compositePart_0 = ((CompositePart) sim.get(SimulationPartManager.class).getPart("Z - Refinement Blocks"));

    SolidModelPart solidModelPart_0 = ((SolidModelPart) compositePart_0.getChildParts().getPart("Wake Refine 3"));

    SolidModelPart solidModelPart_1 = ((SolidModelPart) compositePart_0.getChildParts().getPart("Wake Refine 2"));

    SolidModelPart solidModelPart_2 = ((SolidModelPart) compositePart_0.getChildParts().getPart("Wake Refine 1"));

    SolidModelPart solidModelPart_3 = ((SolidModelPart) sim.get(SimulationPartManager.class).getPart("Tunnel"));

    sim.get(SimulationPartManager.class).updateParts(new NeoObjectVector(new Object[] {solidModelPart_0, solidModelPart_1, solidModelPart_2, solidModelPart_3}));

    //**************************** SET COORDINATE SYSTEMS FOR TIRES *******************//
    //FR Origin X
    double FR_X = -(Math.sqrt(Math.pow((wheelbase*cgx),2)+Math.pow(FT,2)))*Math.cos(Math.asin(FT/Math.sqrt(Math.pow((wheelbase*cgx),2)+Math.pow(FT,2)))) + (wheelbase*cgx);
    //FR Origin Y
    double FR_Y = (Math.sqrt(Math.pow((wheelbase*cgx),2)+Math.pow(FT,2)))*Math.sin(Math.asin(FT/Math.sqrt(Math.pow((wheelbase*cgx),2)+Math.pow(FT,2))));
    //FR Origin Z
    double FR_Z = tire_half - FRH_Off - FT/2*Math.sin(roll_rad);
    //FL Origin X
    double FL_X = -(Math.sqrt(Math.pow((wheelbase*cgx),2)+Math.pow(FT,2)))*Math.cos(Math.asin(FT/Math.sqrt(Math.pow((wheelbase*cgx),2)+Math.pow(FT,2)))) + (wheelbase*cgx);
    //FL Origin Y
    double FL_Y = -(Math.sqrt(Math.pow((wheelbase*cgx),2)+Math.pow(FT,2)))*Math.sin(Math.asin(FT/Math.sqrt(Math.pow((wheelbase*cgx),2)+Math.pow(FT,2))));
    //FL Origin Z
    double FL_Z = tire_half - FRH_Off + FT/2*Math.sin(roll_rad);
    //RR Origin X
    double RR_X = (Math.sqrt(Math.pow((wheelbase*(1-cgx)),2)+Math.pow(RT,2)))*Math.cos(Math.asin(RT/Math.sqrt(Math.pow((wheelbase*(1-cgx)),2)+Math.pow(RT,2)))) + (wheelbase*cgx);
    //RR Origin Y
    double RR_Y = (Math.sqrt(Math.pow((wheelbase*(1-cgx)),2)+Math.pow(RT,2)))*Math.sin(Math.asin(RT/Math.sqrt(Math.pow((wheelbase*(1-cgx)),2)+Math.pow(RT,2))));
    //RR Origin Z
    double RR_Z = tire_half - RRH_Off - RT/2*Math.sin(roll_rad);
    //RL Origin X
    double RL_X = (Math.sqrt(Math.pow((wheelbase*(1-cgx)),2)+Math.pow(RT,2)))*Math.cos(Math.asin(RT/Math.sqrt(Math.pow((wheelbase*(1-cgx)),2)+Math.pow(RT,2)))) + (wheelbase*cgx);
    //RL Origin Y
    double RL_Y = -(Math.sqrt(Math.pow((wheelbase*(1-cgx)),2)+Math.pow(RT,2)))*Math.sin(Math.asin(RT/Math.sqrt(Math.pow((wheelbase*(1-cgx)),2)+Math.pow(RT,2))));
    //RL Origin Z
    double RL_Z = tire_half - RRH_Off + RT/2*Math.sin(roll_rad);

    // Cos and sine inputs 
    // FR Tire
    double cos_ab_fr = Math.cos(alfa_front_rad + beta_front_right_rad);
    double sin_ab_fr = Math.sin(alfa_front_rad + beta_front_right_rad);
    double sin_t_fr = Math.sin(-theta_front_rad);
    double cos_t_fr = Math.cos(-theta_front_rad);
    double FR_XI = cos_ab_fr;
    double FR_XJ = sin_ab_fr*cos_t_fr;
    double FR_XK = sin_t_fr*sin_ab_fr;
    double FR_YI = 0.0;
    double FR_YJ = -sin_t_fr;
    double FR_YK = cos_t_fr;

    // FL Tire
    double cos_ab_fl = Math.cos(-alfa_front_rad + beta_front_left_rad);
    double sin_ab_fl = Math.sin(-alfa_front_rad + beta_front_left_rad);
    double sin_t_fl = Math.sin(theta_front_rad);
    double cos_t_fl = Math.cos(theta_front_rad);
    double FL_XI = cos_ab_fl;
    double FL_XJ = sin_ab_fl*cos_t_fl;
    double FL_XK = sin_t_fl*sin_ab_fl;
    double FL_YI = 0.0;
    double FL_YJ = -sin_t_fl;
    double FL_YK = cos_t_fl;

    // RR Tire
    double cos_a_rr = Math.cos(alfa_rear_rad);
    double sin_a_rr = Math.sin(alfa_rear_rad);
    double sin_t_rr = Math.sin(-theta_rear_rad);
    double cos_t_rr = Math.cos(-theta_rear_rad);
    double RR_XI = cos_a_rr;
    double RR_XJ = sin_a_rr*cos_t_rr;
    double RR_XK = sin_t_rr*sin_a_rr;
    double RR_YI = 0.0;
    double RR_YJ = -sin_t_rr;
    double RR_YK = cos_t_rr;

    // RL Tire
    double cos_a_rl = Math.cos(-alfa_rear_rad);
    double sin_a_rl = Math.sin(-alfa_rear_rad);
    double sin_t_rl = Math.sin(theta_rear_rad);
    double cos_t_rl = Math.cos(theta_rear_rad);
    double RL_XI = cos_a_rl;
    double RL_XJ = sin_a_rl*cos_t_rl;
    double RL_XK = sin_t_rl*sin_a_rl;
    double RL_YI = 0.0;
    double RL_YJ = -sin_t_rl;
    double RL_YK = cos_t_rl;

    // Modify Coordinate Systems
    if(straight == 0){
        SetCoord(sim, "FR", new double[] {FR_X, FR_Y, FR_Z}, new double[] {FR_XI, FR_XJ, FR_XK}, new double[] {FR_YI, FR_YJ, FR_YK});

        SetCoord(sim, "RR", new double[] {RR_X, RR_Y, RR_Z}, new double[] {RR_XI, RR_XJ, RR_XK}, new double[] {RR_YI, RR_YJ, RR_YK});
    }

        SetCoord(sim, "FL", new double[] {FL_X, FL_Y, FL_Z}, new double[] {FL_XI, FL_XJ, FL_XK}, new double[] {FL_YI, FL_YJ, FL_YK});

        SetCoord(sim, "RL", new double[] {RL_X, RL_Y, RL_Z}, new double[] {RL_XI, RL_XJ, RL_XK}, new double[] {RL_YI, RL_YJ, RL_YK});

    //*********************** CALCULATE RADII OF TIRES *****************************
    if(straight == 0){
        double dxFL = cgx*wheelbase - FL_X;
        double dyFL = COR + FL_Y;
        double R_FL = Math.sqrt(Math.pow(dxFL,2)+Math.pow(dyFL,2));

        double dxFR = cgx*wheelbase - FR_X;
        double dyFR = COR + FR_Y;
        double R_FR = Math.sqrt(Math.pow(dxFR,2)+Math.pow(dyFR,2));

        double dxRL = cgx*wheelbase - RL_X;
        double dyRL = COR + RL_Y;
        double R_RL = Math.sqrt(Math.pow(dxRL,2)+Math.pow(dyRL,2));

        double dxRR = cgx*wheelbase - RR_X;
        double dyRR = COR + RR_Y;
        double R_RR = Math.sqrt(Math.pow(dxRR,2)+Math.pow(dyRR,2));

        ScalarGlobalParameter FLR = ((ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("FL_Rad"));

        FLR.getQuantity().setValue(R_FL);

        ScalarGlobalParameter FRR = ((ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("FR_Rad"));

        FRR.getQuantity().setValue(R_FR);

        ScalarGlobalParameter RLR = ((ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("RL_Rad"));

        RLR.getQuantity().setValue(R_RL);

        ScalarGlobalParameter RRR = ((ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("RR_Rad"));

        RRR.getQuantity().setValue(R_RR);
    }

    //************************* SET RADIATOR_1 COORDINATE SYSTEMS *********************//

    SetCoord(sim, "Rad_1", rad_1_O, rad_1_X, rad_1_Y);
    
    //****************************** RESET RADIATOR_2 ***********************************
    if(straight == 0){
        SetCoord(sim, "Rad_2", rad_2_O, rad_2_X, rad_2_Y);
    }
  }

private void SetCoord(Simulation sim, String sys, double[] Origin, double[] XDir, double[] YDir){
    Units units_0 = ((Units) sim.getUnitsManager().getObject("m"));

    LabCoordinateSystem LabSys = sim.getCoordinateSystemManager().getLabCoordinateSystem();

    CartesianCoordinateSystem CS = ((CartesianCoordinateSystem) LabSys.getLocalCoordinateSystemManager().getObject(sys));

    CS.setBasis0(new DoubleVector(XDir));

    CS.setBasis1(new DoubleVector(YDir));

    Coordinate Org = CS.getOrigin();

    Org.setCoordinate(units_0, units_0, units_0, new DoubleVector(Origin));
  }
}
