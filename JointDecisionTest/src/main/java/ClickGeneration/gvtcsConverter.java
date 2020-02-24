package ClickGeneration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.CursorBuilder;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

public class gvtcsConverter {
/**
 * [The purpose for this class is to convert the trip information from GVTCS to Matsim Trip information 
 * The GVTCS has three trip databases
 * 
 * The weight based trip still is another problem
 * Connecting the database spatially is also another issue as the origin and destination code provided is most probably not TPUSB codes as the 454 PDZ zones do not match.
 * 
 * Have to confirm
 * Currently mapping to TPUSB] JAN 18
 * @throws IOException 
 * 
 * 
 * 
 */
	private static final double weightFactor=1.0;
	
	public static void main(String[] args) throws IOException {
		
		Double personPerson=0.;
		Double tripPerson=0.;
		
		Database gvtcsDatabase=DatabaseBuilder.open(new File("data/GVTCS DATABASE/GVTCS.accdb"));
		Table govOwner=gvtcsDatabase.getTable("GOV-OWNER");
		Table govTrip=gvtcsDatabase.getTable("GOV-TRIP");
		Table govVehicle=gvtcsDatabase.getTable("GOV-VEH");
		Table ngovOwner=gvtcsDatabase.getTable("NGOV-OWNER");
		Table ngovTrip=gvtcsDatabase.getTable("NGOV-TRIP");
		Table ngovVehicle=gvtcsDatabase.getTable("NGOV-VEH");
		Table sectors=gvtcsDatabase.getTable("Sectors");
		Table sgisTrip=gvtcsDatabase.getTable("SGIS-TRIP");
		
		Database tcsDatabase=DatabaseBuilder.open(new File("data/TCSDatabase/TCS2011 database.accdb"));
		
		Table tpusbCoord=tcsDatabase.getTable("TPUSB_coordinate_Mod");
		Table tpusb11=tcsDatabase.getTable("11TPUSB");
		
		
		Config config=ConfigUtils.createConfig();
		BufferedReader bf=new BufferedReader(new FileReader(new File("data/GVTCS DATABASE/LandUseCode.csv")));
		bf.readLine();
		String line;
		HashMap<Double,String>activityDetails=new HashMap<>();
		while((line=bf.readLine())!=null) {
			String[] part=line.split(",");
			activityDetails.put(Double.parseDouble(part[0].trim()), part[1].trim());
			addActivityPlanParameter(config.planCalcScore(),part[1].trim(),30*60);
 		}
		ConfigWriter configWriter=new ConfigWriter(config);
		configWriter.write("data/GVTCS DATABASE/gvtcsConfig.xml");
		
		
		HashMap<Id<TPUSB>,TPUSB> tpusbs=tpusbCreator(tpusbCoord,tpusb11);
		HashMap<Id<Vehicle>,GoodsVehicle> goodsVehicles=createGovVehicles(govTrip,govVehicle,tpusbs,weightFactor,true);
		
		Scenario scenario=ScenarioUtils.createScenario(config);
		Population population=scenario.getPopulation();
		Vehicles vehicles=scenario.getVehicles();
		
		goodsVehicles.putAll(createNonGovVehicles(ngovTrip,ngovVehicle,tpusbs,weightFactor,true));
		
		//ArrayList<Tuple<Person, Vehicle>> personAndVehicle=new ArrayList<>();
		
		
		for(GoodsVehicle gv:goodsVehicles.values()) {
			gv.loadClonedVehicleAndPersons(scenario, activityDetails, "person", "trip",tripPerson,personPerson);
		}
		
		
//		for(Tuple<Person,Vehicle> t:personAndVehicle) {
//			
//			if(!vehicles.getVehicleTypes().containsKey(t.getSecond().getType().getId())) {
//				vehicles.addVehicleType(t.getSecond().getType());
//			}
//			
//			population.addPerson(t.getFirst());
//			vehicles.addVehicle(t.getSecond());
//
//		}
		PopulationWriter popWriter=new PopulationWriter(population);
		VehicleWriterV1 vehWriter=new VehicleWriterV1(vehicles);
		
		
		popWriter.write("data/GVTCS Database/populationGVTCS.xml");
		vehWriter.writeFile("data/GVTCS Database/VehiclesGVTCS.xml");
		//new ObjectAttributesXmlWriter(population.getPersonAttributes()).writeFile("data/GVTCS Database/personAttributesGvtcs.xml");
  		
		System.out.println("TestLine");
	}
	public static void addActivityPlanParameter(PlanCalcScoreConfigGroup config,String name,int typicalDuration){
		ActivityParams act = new ActivityParams(name);
		act.setTypicalDuration(typicalDuration);
		config.addActivityParams(act);
	}
	
	public static HashMap<Id<TPUSB>,TPUSB> tpusbCreator(Table tpusbCoord,Table tpusb11) {
		HashMap<Id<TPUSB>,TPUSB> tpusbs=new HashMap<>();
		
		for(Row row:tpusbCoord) {
			
			
			try {
				double tpusbId=(double) row.get("FUL");
				Double tpuId=(Double)row.get("TPU");
				Double sbvcId=(Double)row.get("SB_VC");
				Coord satCoord=new Coord((double)row.get("XcalSat"),(double)row.get("YcalSat"));
				Cursor cursor=CursorBuilder.createCursor(tpusb11);
				cursor.findFirstRow(Collections.singletonMap("11TPUSB", tpusbId));
				Row rowTpusb11=cursor.getCurrentRow();
				Double pdz454=(Double)rowTpusb11.get("454PDZ");
				Double db26Id=(Double)rowTpusb11.get("DB26");
				String db26Name=(String)rowTpusb11.get("DB26_name");
				Double area=(Double)row.get("SumOfShape_Area");
				TPUSB tpusb=new TPUSB(tpusbId,satCoord,pdz454.intValue(),db26Id.intValue(),db26Name,area);
				tpusbs.put(tpusb.getTPUSBId(), tpusb);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		return tpusbs;
	}
	public static void createFakeTPUSBCrossHarbour(FreightTrip trip) {
		if(trip.getOtpusb()!=null && trip.getDtpusb()!=null) {
		TPUSB tpusb1=new TPUSB("CHT",new Coord(36662.2,18205.8),0,0,null,100.);
		TPUSB tpusb2=new TPUSB("EHC",new Coord(42051.36,17985.4),0,0,null,100.);
		TPUSB tpusb3=new TPUSB("WHC",new Coord(34436.52,18383.08),0,0,null,100.);
		if(trip.getOtpusb().getDistrict26Id()>4 && trip.getDtpusb().getDistrict26Id()<=4) {
			if(trip.getTunnelOrTollUsed().contains(1.)) {
				trip.setOtpusb(tpusb1);
			}else if(trip.getTunnelOrTollUsed().contains(2.)) {
				trip.setOtpusb(tpusb2);
			}else if(trip.getTunnelOrTollUsed().contains(3.)) {
				trip.setOtpusb(tpusb3);
			}else {
				ArrayList<Double>distance=new ArrayList<>();
				distance.add(calcUclideanDistance(trip.getOtpusb().getSatCoord(),tpusb1.getSatCoord()));
				distance.add(calcUclideanDistance(trip.getOtpusb().getSatCoord(),tpusb2.getSatCoord()));
				distance.add(calcUclideanDistance(trip.getOtpusb().getSatCoord(),tpusb3.getSatCoord()));
				
				if(distance.get(0)<distance.get(1) &&distance.get(0)<distance.get(2)) {
					trip.setOtpusb(tpusb1);
				}else if(distance.get(1)<distance.get(2) &&distance.get(1)<distance.get(0)) {
					trip.setOtpusb(tpusb2);
				}else {
					trip.setOtpusb(tpusb3);
				}
			}
		}else if(trip.getDtpusb().getDistrict26Id()>4 && trip.getOtpusb().getDistrict26Id()<=4) {
			if(trip.getTunnelOrTollUsed().contains(1.)) {
				trip.setDtpusb(tpusb1);
			}else if(trip.getTunnelOrTollUsed().contains(2.)) {
				trip.setDtpusb(tpusb2);
			}else if(trip.getTunnelOrTollUsed().contains(3.)) {
				trip.setDtpusb(tpusb3);
			}else {
				ArrayList<Double>distance=new ArrayList<>();
				distance.add(calcUclideanDistance(trip.getDtpusb().getSatCoord(),tpusb1.getSatCoord()));
				distance.add(calcUclideanDistance(trip.getDtpusb().getSatCoord(),tpusb2.getSatCoord()));
				distance.add(calcUclideanDistance(trip.getDtpusb().getSatCoord(),tpusb3.getSatCoord()));
				
				if(distance.get(0)<distance.get(1) &&distance.get(0)<distance.get(2)) {
					trip.setDtpusb(tpusb1);
				}else if(distance.get(1)<distance.get(2) &&distance.get(1)<distance.get(0)) {
					trip.setDtpusb(tpusb2);
				}else {
					trip.setDtpusb(tpusb3);
				}
			}
		}else if (trip.getDtpusb().getDistrict26Id()>4 && trip.getOtpusb().getDistrict26Id()>4) {
			trip.setOtpusb(null);
		}
		}
	}
	public static double calcUclideanDistance(Coord coord1,Coord coord2) {
		return Math.sqrt(Math.pow(coord1.getX()-coord2.getX(),2)+Math.pow(coord1.getY()-coord2.getY(),2));
	}
	
	public static HashMap<Id<Vehicle>,GoodsVehicle> createGovVehicles(Table govTrip,Table govVehicle,HashMap<Id<TPUSB>,TPUSB> tpusbs,double weightFactor,boolean onlyHkiTrip){
		HashMap<Id<Vehicle>,GoodsVehicle> vehicles=new HashMap<>();
		
		for(Row row:govVehicle) {
			GoodsVehicle gv;
			if((Double)row.get("Q5")!=null && (double)row.get("Q5")==1) {
				gv=new GoodsVehicle((double)row.get("Veh_Type"),(String)row.get("GV_id"),(double)row.get("WT_VEH")*weightFactor,(double)row.get("Q7a"),
					(double)row.get("Q7b"),(double)row.get("Q7c"),(double)row.get("Q9c_Amount"),(double)row.get("Q4_Driver"),(double)row.get("Q9c_Unit"),
					(double)row.get("Q9d"),(Double)row.get("Q5"),
					Double.parseDouble((String)row.get("Q6_Cross_Harbour_Used")),
					Double.parseDouble((String)row.get("Q6_Cross_Harbour_Time")),
					Double.parseDouble((String)row.get("Q6_Trip_Purpose")),
					(String)row.get("Q6_Control_Point"));
			}else {
				gv=new GoodsVehicle((double)row.get("Veh_Type"),(String)row.get("GV_id"),(double)row.get("WT_VEH")*weightFactor,(double)row.get("Q7a"),
						(double)row.get("Q7b"),(double)row.get("Q7c"),(double)row.get("Q9c_Amount"),(double)row.get("Q4_Driver"),(double)row.get("Q9c_Unit"),
						(double)row.get("Q9d"),(Double)row.get("Q5"),null,null,null,null);
			}
			vehicles.put(gv.getId(), gv);
		}
		
		for(Row row:govTrip) {
			FreightTrip trip;
			String a=(String)row.get("Q2_Control_Point");
			if(row.get("Q2_Control_Point")!=null && !"".equals((String)(row.get("Q2_Control_Point")))) {
				TPUSB otpusb=tpusbs.get(Id.create(""+(double)row.get("Q2_Origin_Code"),TPUSB.class));
				TPUSB dtpusb=tpusbs.get(Id.create(""+(double)row.get("Q2_Origin_Code"),TPUSB.class));
				trip=new FreightTrip(Id.create((String)row.get("GV_id"), Vehicle.class),(double)row.get("Trips_id"),
					tpusbs.get(Id.create(""+(double)row.get("Q2_Origin_Code"),TPUSB.class)), 
					tpusbs.get(Id.create(""+(double)row.get("Q2_Destination_Code"),TPUSB.class)),(String)row.get("Q2_Street_Origin"),
					(String)row.get("Q2_Building_Origin"),(String)row.get("Q2_Street_Destination"),(String)row.get("Q2_Building_Destination"),
					(double)row.get("Q2_Land_Use_Origin"),(double)row.get("Q2_Land_Use_Destination"),(double)row.get("Q2_Departure_Time"),
					(double)row.get("Q2_Arrival_Time"),(double)row.get("Q2_Trip_Purpose"),
					(double)row.get("WT_TRIP")*weightFactor,
					(double)row.get("O454"),
					(double)row.get("D454"),Double.parseDouble((String)row.get("Q2_Control_Point")),
					Double.parseDouble((String)row.get("Q2_Control_Point_Time")),(double)row.get("Ti_Pe"));
			}else {
				TPUSB otpusb=tpusbs.get(Id.create(Double.toString((double)row.get("Q2_Origin_Code")),TPUSB.class));
				TPUSB dtpusb=tpusbs.get(Id.create(Double.toString((double)row.get("Q2_Origin_Code")),TPUSB.class));
				trip=new FreightTrip(Id.create((String)row.get("GV_id"), Vehicle.class),(double)row.get("Trips_id"),
						tpusbs.get(Id.create(""+(double)row.get("Q2_Origin_Code"),TPUSB.class)), 
						tpusbs.get(Id.create(""+(double)row.get("Q2_Destination_Code"),TPUSB.class)),(String)row.get("Q2_Street_Origin"),
						(String)row.get("Q2_Building_Origin"),(String)row.get("Q2_Street_Destination"),(String)row.get("Q2_Building_Destination"),
						(double)row.get("Q2_Land_Use_Origin"),(double)row.get("Q2_Land_Use_Destination"),(double)row.get("Q2_Departure_Time"),
						(double)row.get("Q2_Arrival_Time"),(double)row.get("Q2_Trip_Purpose"),
						(double)row.get("WT_TRIP")*weightFactor,(double)row.get("O454"),(double)row.get("D454"),null,null,(double)row.get("TiPer"));
			}
			
			for(int i=1;i<=12;i++) {
				if(row.get("Q2_Toll_facility_"+i)!=null && !"".equals((String)row.get("Q2_Toll_facility_"+i)) ) {
					trip.addTollFacilityUsed(Double.parseDouble((String)row.get("Q2_Toll_facility_"+i)));
				}
			}
			if(onlyHkiTrip) {
				gvtcsConverter.createFakeTPUSBCrossHarbour(trip);
			}
			vehicles.get(trip.getVehicleId()).addTrip(trip);
		}

		return vehicles;
		
		
	}

	public static HashMap<Id<Vehicle>,GoodsVehicle> createNonGovVehicles(Table nonGovTrip,Table nonGovVehicle,HashMap<Id<TPUSB>,TPUSB> tpusbs,double weightFactor,boolean onlyHkiTrip){
		HashMap<Id<Vehicle>,GoodsVehicle> vehicles=new HashMap<>();
		
		for(Row row:nonGovVehicle) {
			GoodsVehicle gv;
			if((Double)row.get("Q6")!=null && (double)row.get("Q6")==1) {
				gv=new GoodsVehicle((double)row.get("Veh_Type"),(String)row.get("GV_id"),(double)row.get("WT_VEH")*weightFactor,(double)row.get("Q8a"),
					(double)row.get("Q8b"),(double)row.get("Q8c"),(double)row.get("Q10c_Amount"),(double)row.get("Q5_Driver"),(double)row.get("Q10c_Unit"),
					(double)row.get("Q10e"),(Double)row.get("Q6"),
					Double.parseDouble((String)""+row.get("Q7_Cross_Harbour_Used")),
					Double.parseDouble((String)""+row.get("Q7_Cross_Harbour_Time")),
					Double.parseDouble((String)""+row.get("Q7_Trip_Purpose")),
					(String)row.get("Q7_Control_Point"));
			}else {
				gv=new GoodsVehicle((double)row.get("Veh_Type"),(String)row.get("GV_id"),(double)row.get("WT_VEH")*weightFactor,(double)row.get("Q8a"),
						(double)row.get("Q8b"),(double)row.get("Q8c"),(double)row.get("Q10c_Amount"),(double)row.get("Q5_Driver"),(double)row.get("Q10c_Unit"),
						(double)row.get("Q10e"),
						(Double)row.get("Q6"),null,null,null,null);
			}
			vehicles.put(gv.getId(), gv);
		}
		
		for(Row row:nonGovTrip) {
			FreightTrip trip;
			String a=(String)row.get("Q2_Control_Point");
			if(row.get("Q3_Control_Point")!=null && !"".equals((String)(row.get("Q3_Control_Point")))) {
				trip=new FreightTrip(Id.create((String)row.get("GV_id"), Vehicle.class),(double)row.get("Trips_id"),
					tpusbs.get(Id.create(""+(double)row.get("Q3_Origin_Code"),TPUSB.class)), 
					tpusbs.get(Id.create(""+(double)row.get("Q3_Destination_Code"),TPUSB.class)),(String)row.get("Q3_Origin_Street"),
					(String)row.get("Q3_Origin_Building"),(String)row.get("Q3_Destination_Street"),(String)row.get("Q2_Destination_Building"),
					(double)row.get("Q3_Land_Use_Origin"),(double)row.get("Q3_Land_Use_Destination"),(double)row.get("Q3_Departure_Time"),
					(double)row.get("Q3_Arrival_Time"),(double)row.get("Q3_Trip_Purpose"),
					(double)row.get("WT_TRIP")*weightFactor,
					(double)row.get("O454"),
					(double)row.get("D454"),Double.parseDouble((String)row.get("Q3_Control_Point")),
					Double.parseDouble((String)row.get("Q3_Control_Point_Time")),(double)row.get("Ti_Pe"));
			}else {
				trip=new FreightTrip(Id.create((String)row.get("GV_id"), Vehicle.class),(double)row.get("Trips_id"),
						tpusbs.get(Id.create(""+(double)row.get("Q3_Origin_Code"),TPUSB.class)), 
						tpusbs.get(Id.create(""+(double)row.get("Q3_Destination_Code"),TPUSB.class)),(String)row.get("Q3_Origin_Street"),
						(String)row.get("Q3_Origin_Building"),(String)row.get("Q3_Destination_Street"),(String)row.get("Q2_Destination_Building"),
						(double)row.get("Q3_Land_Use_Origin"),(double)row.get("Q3_Land_Use_Destination"),(double)row.get("Q3_Departure_Time"),
						(double)row.get("Q3_Arrival_Time"),(double)row.get("Q3_Trip_Purpose"),
						(double)row.get("WT_TRIP")*weightFactor,
						(double)row.get("O454"),
						(double)row.get("D454"),null,null,(double)row.get("TiPer"));
			}
			
			for(int i=1;i<=12;i++) {
				if(row.get("Q2_Toll_facility_"+i)!=null && !"".equals((String)row.get("Q2_Toll_facility_"+i)) ) {
					trip.addTollFacilityUsed(Double.parseDouble((String)row.get("Q2_Toll_facility_"+i)));
				}
			}
			if(onlyHkiTrip) {
				gvtcsConverter.createFakeTPUSBCrossHarbour(trip);
			}
			vehicles.get(trip.getVehicleId()).addTrip(trip);
		}

		return vehicles;
	}
}

