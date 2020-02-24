package ClickGeneration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.shared.utils.io.FileUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.socnetsim.framework.cliques.population.CliquesWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;




/**
 * 
 * @author ashraf
 *
 */
public class SubPopulationTry {
	
	private static final boolean HkiSeperation=true;
	private static final double weightFactorgvtcs=1.;
	private static final double weightFactorTCS=1.;
	private static Double tripPerson=0.;
	private static Double personPerson=0.;
	public static void main(String[] args) throws IOException {
		Double tripPerson=0.;
		Double personPerson=0.;
		Config config=ConfigUtils.createConfig();
		Scenario scenario=ScenarioUtils.createScenario(config);
		Population population=scenario.getPopulation();
		Vehicles vehicles=scenario.getVehicles();
		
		/**
		 * TCS Database
		 */
		Database tcsDatabase=DatabaseBuilder.open(new File("data/TCSDatabase/TCS2011 database.accdb"));
		Table tpusbCoord=tcsDatabase.getTable("TPUSB_coordinate_Mod");
		Table tpusb11=tcsDatabase.getTable("11TPUSB");
		HashMap<Id<TPUSB>,TPUSB> tpusbs=gvtcsConverter.tpusbCreator(tpusbCoord,tpusb11);
		HashMap<Double,String> activityDetailsTCS=new HashMap<>();
		HashMap<Double,TCSMode> modesDetails=new HashMap<>();
		String activityFileLoc="data/TCSDatabase/ActivityManual1.csv";
		String modeFileLoc="data/TCSDatabase/ModeManual.csv";
		
		TCSExtractor.readModeAndActivityTypeManual(activityFileLoc, modeFileLoc, activityDetailsTCS, modesDetails);
		
		Table HH=tcsDatabase.getTable("HH");
		Table HM=tcsDatabase.getTable("HM");
		Table TP=tcsDatabase.getTable("TP");
		
		HashMap<Id<HouseHold>,HouseHold> houseHolds=TCSExtractor.createHouseHolds(HH,tpusbs,weightFactorTCS);
		HashMap<Id<HouseHoldMember>,HouseHoldMember> members=TCSExtractor.createMember(HM,TP,tpusbs,houseHolds,weightFactorTCS);
		for(HouseHold hh:houseHolds.values()) {
			if(!hh.getMembers().isEmpty())hh.generateMinimumWeightHouseHolds(scenario);
		}
		
//		Measurements fullHKMeasurements=new MeasurementsReader().readMeasurements("fullHk/ATCMeasurementsPeak.xml");
//		Map<String,Map<String,Double>> multiplier=prepODMultiplier("optimizedODMultiplier/");
		
//		Map<String,Tuple<Double,Double>>timeBeans=fullHKMeasurements.getTimeBean();
		
//		for(HouseHoldMember hm: members.values()) {
//			for(TCSTrip trip:hm.getTrips().values()) {
//				double time=trip.getDepartureTime();
//				if(time>24*3600) {
//					time=time-24*3600;
//				}
//				String timeId=null;
//				for(Entry<String, Tuple<Double, Double>> timeBean:timeBeans.entrySet()) {
//					if(time>timeBean.getValue().getFirst() && time<=timeBean.getValue().getSecond()) {
//						timeId=timeBean.getKey();
//					}
//				}
//				if(timeId!=null) {
//					String odId=trip.getOtpusb().getDistrict26Id()+"_"+trip.getDtpusb().getDistrict26Id();
//					double num=multiplier.get(timeId).get(odId);
//					trip.setTripExpansionFactor(trip.getTripExpansionFactor()*num);
//				}
//			}
//		}
		
		if(HkiSeperation) {
			TCSExtractor.HKITripExtractor(members);
		}
		
		for(HouseHoldMember hm:members.values()) {

			hm.loadClonedVehicleAndPersons(scenario, activityDetailsTCS, modesDetails, "person", "trip",tripPerson,personPerson,true);

		}
		
		population = scenario.getPopulation();
		
		Households hhs = scenario.getHouseholds();
		
		
		
		
		/**
		 * GVTCS Database
		 */
		
		Database gvtcsDatabase=DatabaseBuilder.open(new File("data/GVTCS DATABASE/GVTCS.accdb"));
		Table govOwner=gvtcsDatabase.getTable("GOV-OWNER");
		Table govTrip=gvtcsDatabase.getTable("GOV-TRIP");
		Table govVehicle=gvtcsDatabase.getTable("GOV-VEH");
		Table ngovOwner=gvtcsDatabase.getTable("NGOV-OWNER");
		Table ngovTrip=gvtcsDatabase.getTable("NGOV-TRIP");
		Table ngovVehicle=gvtcsDatabase.getTable("NGOV-VEH");
		Table sectors=gvtcsDatabase.getTable("Sectors");
		Table sgisTrip=gvtcsDatabase.getTable("SGIS-TRIP");
		
		BufferedReader bf=new BufferedReader(new FileReader(new File("data/GVTCS DATABASE/LandUseCode.csv")));
		bf.readLine();
		String line;
		HashMap<Double,String>activityDetailsgvtcs=new HashMap<>();
		while((line=bf.readLine())!=null) {
			String[] part=line.split(",");
			activityDetailsgvtcs.put(Double.parseDouble(part[0].trim()), part[1].trim());
			gvtcsConverter.addActivityPlanParameter(config.planCalcScore(),part[1].trim(),30*60);
 		}
		
		HashMap<Id<Vehicle>,GoodsVehicle> goodsVehicles=gvtcsConverter.createGovVehicles(govTrip,govVehicle,tpusbs,weightFactorgvtcs,!HkiSeperation);
		
		goodsVehicles.putAll(gvtcsConverter.createNonGovVehicles(ngovTrip,ngovVehicle,tpusbs,weightFactorgvtcs,!HkiSeperation));
		
//		for(GoodsVehicle gv: goodsVehicles.values()) {
//			for(FreightTrip trip:gv.getTrips().values()) {
//				double time=trip.getDepartureTime();
//				if(time>24*3600) {
//					time=time-24*3600;
//				}
//				String timeId=null;
//				for(Entry<String, Tuple<Double, Double>> timeBean:timeBeans.entrySet()) {
//					if(time>timeBean.getValue().getFirst() && time<=timeBean.getValue().getSecond()) {
//						timeId=timeBean.getKey();
//					}
//				}
//				if(timeId!=null) {
//					String odId=Double.toString(trip.getOtpusb().getDistrict26Id())+"_"+Double.toString(trip.getDtpusb().getDistrict26Id());
//					trip.setTripWeight(trip.getTripWeight()*multiplier.get(timeId).get(odId));
//				}
//			}
//		}
		
		
		for(GoodsVehicle gv:goodsVehicles.values()) {
			gv.loadClonedVehicleAndPersons(scenario, activityDetailsgvtcs, "person", "trip",tripPerson,personPerson);
		}
		boolean isConsistant = activityConsistancyTester(population);
		ActivityAnalyzer ac=new ActivityAnalyzer();
		HashMap<String,Double>activityDuration= ac.getAverageActivityDuration(population);
		HashMap<String,Double>activityStartTime=ac.getAverageStartingTime(population);
		HashMap<String,Double>activityEndTime=ac.getAverageClosingTime(population);
		
		Set<String>activityTypes=null;
		activityTypes=ac.getActivityTypes(population);
		Set<String>startAndEndActivities=ac.getStartOrEndActivityTypes(population);
		PlanCalcScoreConfigGroup cp=config.planCalcScore();
		//ac.ActivitySplitter(population, config, "Home", 12*3600.);
		ac.analyzeActivities(population, "toyScenarioLarge/activityDetails1.csv","toyScenarioLarge/activityDistributions.csv");
		
//		ActivityAnalyzer.addActivityPlanParameter(cp, activityTypes, activityDuration, activityStartTime,activityEndTime,startAndEndActivities, 
//				15,15, 8*60*60, 15*60, 8*3600,20*3600, true);
		ac.readActivityTimings("data/toyScenarioLargeData/ActivityTimings.csv", config);
		
		ac.ActivitySplitter(population, config, "Usual place of work", 3600., true);
		
		//config.addModule(cp);
//		for(String s:activityDetailsTCS.values()) {
//			if(activityDuration.containsKey(s)) {
//				if(activityDuration.get(s)==0) {
//					activityDuration.put(s, 1.0);
//				}
//				TCSExtractor.addActivityPlanParameter(config.planCalcScore(),s,activityDuration.get(s).intValue());
//			}else {
//				TCSExtractor.addActivityPlanParameter(config.planCalcScore(),s,8*60*60);
//			}
//		}
//		for(String s:activityDetailsgvtcs.values()) {
//			if(activityDuration.containsKey(s)) {
//				if(activityDuration.get(s)==0) {
//					activityDuration.put(s, 1.0);
//				}
//			
//				TCSExtractor.addActivityPlanParameter(config.planCalcScore(),s,activityDuration.get(s).intValue());
//			}else {
//				TCSExtractor.addActivityPlanParameter(config.planCalcScore(),s,8*60*60);
//			}
//		}
		
		
		//ActivityAnalyzer.ActivitySplitter(population, config, "Usual place of work", 60*30.);
		
		
		ConfigWriter configWriter=new ConfigWriter(config);
		PopulationWriter popWriter=new PopulationWriter(population);
		VehicleWriterV1 vehWriter=new VehicleWriterV1(vehicles);
		
		
		popWriter.write("fullHk/populationHKI.xml");
		vehWriter.writeFile("fullHk/VehiclesHKI.xml");
		new HouseholdsWriterV10(hhs).writeFile("fullHk/householdHKI.xml");
		configWriter.write("fullHk/config_Ashraf.xml");
		
		CliquesWriter clw= new CliquesWriter();
		clw.openAndStartFile("fullHk/hhClick.xml");
		for(Household hh:scenario.getHouseholds().getHouseholds().values()) {
			List<Identifiable> mem=new ArrayList<>();
			for(Id<Person> pId:hh.getMemberIds() ) {
				mem.add(scenario.getPopulation().getPersons().get(pId));
			}
			clw.writeClique(hh.getId(), mem);
		}
		
		clw.finishAndCloseFile();
		
		//new ObjectAttributesXmlWriter(population.getPersonAttributes()).writeFile("fullHk/personAttributesHKI.xml");
		
//		popWriter.write("data/toyScenarioLargeData/populationHKIPaper.xml");
//		vehWriter.writeFile("data/toyScenarioLargeData/VehiclesHKIPaper.xml");
//		configWriter.write("data/toyScenarioLargeData/configPaperactivityParam.xml");
		//new ObjectAttributesXmlWriter(population.getPersonAttributes()).writeFile("data/LargeScaleScenario/personAttributesHKI.xml");
		
		System.out.println("total Population = "+population.getPersons().size());
		System.out.println("total Vehicles = "+vehicles.getVehicles().size());
  		
		
		
		//System.out.println("total tripPerson = "+tripPerson);
		//System.out.println("total personPerson = "+personPerson);
		
		//System.out.println("ratio of the background population with real population = "+tripPerson/(tripPerson+personPerson));
		
		System.out.println("TestLine");

	}
	
	public static boolean activityConsistancyTester(Population population) {
		boolean isConsistant=true;
		for(Person person:population.getPersons().values()) {
			for(Plan plan:person.getPlans()) {
				for(PlanElement pe:plan.getPlanElements()) {
					if(pe instanceof Activity) {
						Activity a=(Activity)pe;
						if(a.getStartTime()!=Double.NEGATIVE_INFINITY && a.getEndTime()!=Double.NEGATIVE_INFINITY && a.getStartTime()>a.getEndTime()) {
							isConsistant=false;
							return isConsistant;
						}
					}else {
						continue;
					}
				}
			}
		}
		return isConsistant;
	}
	
	
	
	public static Map<String,Map<String,Double>> prepODMultiplier(String fileLoc){
		Map<String,Map<String,Double>> multiplier=new HashMap<>();
		File folder = new File(fileLoc);
		File[] listOfFiles = folder.listFiles();
		for(File file:listOfFiles) {
			if(file.isFile() && (FileUtils.getExtension(file.getName())).equals("csv") && file.getName().contains("optimizationResult")) {
				try {
					BufferedReader bf = new BufferedReader(new FileReader(file));
					bf.readLine();
					bf.readLine();
					String line=null;
					String timeId=null;
					Map<String,Double> origins=new HashMap<>();
					Map<String,Double> destinations=new HashMap<>();
					
					while((line=bf.readLine())!=null) {
						String[] part = line.split(",");
						timeId=part[1];
						if(part[2].equals("origin")) {
							origins.put(part[0], Double.parseDouble(part[3]));
						}else {
							destinations.put(part[0], Double.parseDouble(part[3]));
						}
					}
					bf.close();
					multiplier.put(timeId, new HashMap<>());
					Map<String,Double> innerMap = multiplier.get(timeId);
					
					for(Entry<String, Double> o:origins.entrySet()) {
						for(Entry<String, Double> d:destinations.entrySet()) {
							innerMap.put(Double.parseDouble(o.getKey())+"_"+Double.parseDouble(d.getKey()), o.getValue()*d.getValue());
						}
					}
					
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		return multiplier;	
	}
	
}


