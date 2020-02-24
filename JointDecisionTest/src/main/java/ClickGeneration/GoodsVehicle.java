package ClickGeneration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
/**
 * 
 * @author Ashraf
 *
 */
public class GoodsVehicle implements Vehicle{
	/**
	 * a convenient class to store all the goods vehicle information
	 */

	private double vehicleType;
	private Id<Vehicle> vehicleId;
	private double vehicleWeight;
	private HashMap<Double,FreightTrip> trips=new HashMap<>();
	private double ageOfgoodsVehicle;
	private double yearOfPurchase;
	private double purchasingPrice;
	private double averageFuelCost;
	private double routeCoiceCriteria;
	private List<Person> Persons;
	private List<Vehicle> vehicles;
	private double unitofAverageFuelCost;
	private double yearlyMaintainenceCost;
	private boolean crossHerbourTrip;
	private Double crossHerbourTunnelUsed;
	private Double timeAcrossCrossHerbourTunnel;
	private Double tripPurposeCrossHerbourTunnel;
	private String crossBoundaryControlPoint;


	public GoodsVehicle(double vehicleType,String gvId, double vehicleWeight,double age,
			double purchasingYear, double purchasingPrice, double fuelCost,double routeChoiceCriteria, double unitOfFuelCost,
			double yearlyMaintainanceCost, Double crossHerbourTrip,Double crossHerbourTunnelUsed, Double timeAcrossHerbour, 
			Double tripPurposeHerbour,String crossBoundaryControlPoint ) {
		this.vehicleType=vehicleType;
		this.vehicleWeight=vehicleWeight;
		this.vehicleId=Id.create(gvId,Vehicle.class);
		this.ageOfgoodsVehicle=age;
		this.yearOfPurchase=purchasingYear;
		this.averageFuelCost=fuelCost;
		this.routeCoiceCriteria=routeChoiceCriteria;
		this.purchasingPrice=purchasingPrice;
		this.unitofAverageFuelCost=unitOfFuelCost;
		this.yearlyMaintainenceCost=yearlyMaintainanceCost;
		if(crossHerbourTrip!=null && crossHerbourTrip==1) {this.crossHerbourTrip=true;}else {this.crossHerbourTrip=false;}
		this.crossHerbourTunnelUsed=crossHerbourTunnelUsed;
		this.timeAcrossCrossHerbourTunnel=timeAcrossHerbour;
		this.tripPurposeCrossHerbourTunnel=tripPurposeHerbour;
		this.crossBoundaryControlPoint=crossBoundaryControlPoint;
	}

	public void addTrip(FreightTrip trip) {
		
		if(trip.getVehicleId().equals(this.vehicleId)) {
			this.trips.put(trip.getTripId(),trip);
		}else {
			throw new IllegalArgumentException("Trip does not belong to this vehicle, Please Check.");
		}
		
	}

	/**
	 * will create vehicleType
	 * @param type
	 * @return
	 */
	private VehicleType vehicleTypeGenearator(double type) {
		Vehicles vehicles=ScenarioUtils.createScenario(ConfigUtils.createConfig()).getVehicles();
		VehiclesFactory vf=vehicles.getFactory();
		VehicleType vt=vf.createVehicleType(Id.create(""+type, VehicleType.class));
		String desc;
		if(type==1) {
			desc="Goods Vehicle";
		}else if(type==2) {
			desc="Light Goods Vehicle";
		}else if(type==3) {
			desc="Medium Goods Vehicle";
		}else if(type==4) {
			desc="Heavy Goods Vehicle";
		}else if(type==5) {
			desc="Trailer Unit TU";
		}else {
			desc="tructor??";
		}
		vt.setDescription(desc);

		return vt;
	}
	
	/**
	 * This method creates person with only one trip. 
	 * i.e. this method is for creating fantom persons.
	 * @param trip
	 * @param vehiclesFactory 
	 * @param populationFactory 
	 * @return
	 */
	private ArrayList<Tuple<Person,Vehicle>> getTripPersonAndVehicle(FreightTrip trip, double weight, PopulationFactory populationFactory, VehiclesFactory vehiclesFactory,HashMap<Double,String>activityDetails) {
		ArrayList<Tuple<Person,Vehicle>> personList=new ArrayList<>();
		PopulationFactory popfac=populationFactory;
		VehiclesFactory vf=vehiclesFactory;
		if(trip.getOtpusb()!=null && trip.getDtpusb()!=null) {
			for(int i=0;i<weight;i++) {
				HashMap<Id<TPUSB>,Tuple<Double,Double>> randXY=this.generateTripSpecificRandomNumber(trip);
				Person person=popfac.createPerson(Id.createPersonId(this.getId().toString()+"_"+trip.getTripId()+"_"+i));
				Plan plan=popfac.createPlan();
				Activity oAct=popfac.createActivityFromCoord(activityDetails.get((Double)trip.getLandUseOrigin()), 
						new Coord(trip.getOtpusb().getSatCoord().getX()+randXY.get(trip.getOtpusb().getTPUSBId()).getFirst(),
								trip.getOtpusb().getSatCoord().getY()+randXY.get(trip.getOtpusb().getTPUSBId()).getSecond()));
				Activity dAct=popfac.createActivityFromCoord(activityDetails.get((Double)trip.getLandUseDestination()), 
						new Coord(trip.getDtpusb().getSatCoord().getX()+randXY.get(trip.getDtpusb().getTPUSBId()).getFirst(),
								trip.getDtpusb().getSatCoord().getY()+randXY.get(trip.getDtpusb().getTPUSBId()).getSecond()));
				double tripDepartureTime=trip.getDepartureTime();
				double tripArrivalTime=trip.getArrivalTime();
				
				oAct.setEndTime(tripDepartureTime);
				dAct.setStartTime(tripArrivalTime);
				Leg leg=popfac.createLeg("car");
				leg.setDepartureTime(tripDepartureTime);
				leg.setTravelTime(tripArrivalTime-tripDepartureTime);
				plan.addActivity(oAct);
				plan.addLeg(leg);
				plan.addActivity(dAct);
				person.addPlan(plan);
				Vehicle vehicle=vf.createVehicle(Id.createVehicleId(person.getId().toString()), this.vehicleTypeGenearator(this.vehicleType));
				personList.add(new Tuple<Person,Vehicle>(person,vehicle));
				if(i==500) {
					break;
				}
			}
		}
		
		return personList;
	}
	private void checkAndResetTripOrder() {
		HashMap<Double,FreightTrip> tripsnew=new HashMap<>(this.trips);
		HashMap<Double,FreightTrip> tripsnew1=new HashMap<>();
		Map<Double,FreightTrip>tripStartTimes=new HashMap<>();
		for(FreightTrip tp:tripsnew.values()) {
			tripStartTimes.put(tp.getDepartureTime(), tp);
		}
		ArrayList<Double> startTimes=new ArrayList<>(tripStartTimes.keySet());
		Collections.sort(startTimes);
		double j=1;
		for(Double d:startTimes) {
			FreightTrip tp=tripStartTimes.get(d);
			tripsnew1.put(j,tp);
			j++;
		}
		
		this.trips=tripsnew1;
		
	}
	/**
	 * This will create plans directly from vehicle 
	 * @param vehiclesFactory 
	 * @param populationFactory 
	 * @return
	 */
	private ArrayList<Tuple<Person,Vehicle>> getMinWeightPersonAndVehicle(PopulationFactory populationFactory, VehiclesFactory vehiclesFactory,HashMap<Double,String>activityDetails){
		ArrayList<Tuple<Person,Vehicle>> personList=new ArrayList<>();
		PopulationFactory popfac=populationFactory;
		VehiclesFactory vf=vehiclesFactory;
		
		for(int j=0;j<this.getMinimumVehicleWeight();j++) {
			HashMap<Id<TPUSB>,Tuple<Double,Double>> randXY=this.generatePersonSpecificRandomNumber();
			
			Person person=popfac.createPerson(Id.createPersonId(this.getId().toString()+"_"+j));
			
			Vehicle vehicle=vf.createVehicle(Id.createVehicleId(person.getId().toString()), this.vehicleTypeGenearator(this.vehicleType));
			Plan plan=popfac.createPlan();
			ArrayList<Activity> activities=new ArrayList<>();
			ArrayList<Leg> tripLegs=new ArrayList<>();
			ArrayList<Double> tripOrder=new ArrayList<>(this.trips.keySet());
			Collections.sort(tripOrder);
			int i=0;
			for(double tOrder:tripOrder) {
				FreightTrip trip=this.trips.get(tOrder);
				if(trip.getOtpusb()!=null && trip.getDtpusb()!=null) {
					if(i==0) {
						Coord ocoord=new Coord(trip.getOtpusb().getSatCoord().getX()+randXY.get(trip.getOtpusb().getTPUSBId()).getFirst(),
								trip.getOtpusb().getSatCoord().getY()+randXY.get(trip.getOtpusb().getTPUSBId()).getSecond());
						Coord dcoord=new Coord(trip.getDtpusb().getSatCoord().getX()+randXY.get(trip.getDtpusb().getTPUSBId()).getFirst(),
								trip.getDtpusb().getSatCoord().getY()+randXY.get(trip.getDtpusb().getTPUSBId()).getSecond());	
						Activity oact=popfac.createActivityFromCoord(activityDetails.get((Double)trip.getLandUseOrigin()),ocoord);
						Activity dact=popfac.createActivityFromCoord(activityDetails.get((Double)trip.getLandUseDestination()),dcoord);
						double tripDepartureTime=trip.getDepartureTime();
						double tripArrivalTime=trip.getArrivalTime();
						
						oact.setEndTime(tripDepartureTime);
						dact.setStartTime(tripArrivalTime);
						activities.add(oact);
						activities.add(dact);
					}else {
						Coord ocoord=new Coord(trip.getOtpusb().getSatCoord().getX()+randXY.get(trip.getOtpusb().getTPUSBId()).getFirst(),
								trip.getOtpusb().getSatCoord().getY()+randXY.get(trip.getOtpusb().getTPUSBId()).getSecond());
						Coord dcoord=new Coord(trip.getDtpusb().getSatCoord().getX()+randXY.get(trip.getDtpusb().getTPUSBId()).getFirst(),
								trip.getDtpusb().getSatCoord().getY()+randXY.get(trip.getDtpusb().getTPUSBId()).getSecond());
						Activity oact=popfac.createActivityFromCoord(activityDetails.get((Double)trip.getLandUseOrigin()),ocoord);
						Activity dact=popfac.createActivityFromCoord(activityDetails.get((Double)trip.getLandUseDestination()),dcoord);
						
						if(!oact.getCoord().equals(activities.get(i).getCoord())) {

							double tripDepartureTime=trip.getDepartureTime();
							double tripArrivalTime=trip.getArrivalTime();
							
							activities.get(i).setEndTime(tripDepartureTime);
							oact.setStartTime(tripDepartureTime);
							oact.setEndTime(tripDepartureTime);
							dact.setStartTime(tripArrivalTime);
							activities.add(oact);
							activities.add(dact);
							i++;

							Leg legDummy=popfac.createLeg("car");
							legDummy.setDepartureTime(tripDepartureTime);
							legDummy.setTravelTime(0);
							tripLegs.add(legDummy);

							//throw new IllegalArgumentException("Discontinuous Trip Chain!!!");
							//lets think about it later.
						}else {
							double tripDepartureTime=trip.getDepartureTime();
							double tripArrivalTime=trip.getArrivalTime();
						
							activities.get(i).setEndTime(tripDepartureTime);
							dact.setStartTime(tripArrivalTime);
							activities.add(dact);
						}
					}
					Leg leg=popfac.createLeg("car");
					double tripDepartureTime=trip.getDepartureTime();
					double tripArrivalTime=trip.getArrivalTime();
					
					leg.setDepartureTime(tripDepartureTime);
					leg.setTravelTime(tripArrivalTime-tripDepartureTime);
					tripLegs.add(leg);
					i++;
				}
				
			}
			if(activities.size()!=0) {
			plan.addActivity(activities.get(0));
			for(int k=0;k<activities.size()-1;k++) {
				plan.addLeg(tripLegs.get(k));
				plan.addActivity(activities.get(k+1));
				
			}
			person.addPlan(plan);
			personList.add(new Tuple<Person,Vehicle>(person,vehicle));
			}
		}
		return personList;
	}
	private double getMinimumVehicleWeight() {
		double minWeight=this.vehicleWeight;
		for(FreightTrip trip:this.trips.values()) {
			if(trip.getTripWeight()<minWeight) {
				minWeight=trip.getTripWeight();
			}
		}
		return minWeight;
	}

	public ArrayList<Tuple<Person,Vehicle>> getClonedVehicleAndPersons(PopulationFactory populationFactory, VehiclesFactory vehiclesFactory,HashMap<Double,String>activityDetails){
		ArrayList<Tuple<Person,Vehicle>> personsAndVehicles=new ArrayList<>();
		if(this.trips.size()==0) {
			return personsAndVehicles;
		}
		personsAndVehicles.addAll(this.getMinWeightPersonAndVehicle(populationFactory,vehiclesFactory,activityDetails));
		for(FreightTrip trip:this.trips.values()) {
			personsAndVehicles.addAll(this.getTripPersonAndVehicle(trip, trip.getTripWeight()-this.getMinimumVehicleWeight(),populationFactory,vehiclesFactory,activityDetails));
		}
		
		return personsAndVehicles;
	}
	
	public Scenario loadClonedVehicleAndPersons(Scenario scenario,HashMap<Double,String>activityDetails,String personGroupName,String tripGroupName,Double tripPerson,Double personPerson){
		PopulationFactory populationFactory=scenario.getPopulation().getFactory();
		VehiclesFactory vehiclesFactory=scenario.getVehicles().getFactory();
		ArrayList<Tuple<Person,Vehicle>> personsAndVehiclessub1=new ArrayList<>();
		ArrayList<Tuple<Person,Vehicle>> personsAndVehiclessub2=new ArrayList<>();
		this.checkAndResetTripOrder();
		if(this.trips.size()==0) {
			return scenario;
		}
		personsAndVehiclessub1.addAll(this.getMinWeightPersonAndVehicle(populationFactory,vehiclesFactory,activityDetails));
		for(FreightTrip trip:this.trips.values()) {
			personsAndVehiclessub2.addAll(this.getTripPersonAndVehicle(trip, trip.getTripWeight()-this.getMinimumVehicleWeight(),populationFactory,vehiclesFactory,activityDetails));
		}
		
		Population population=scenario.getPopulation();
		Vehicles vehicles=scenario.getVehicles();
		
		for(Tuple<Person,Vehicle> t:personsAndVehiclessub1) {
			if(!vehicles.getVehicleTypes().containsKey(t.getSecond().getType().getId())) {
				vehicles.addVehicleType(t.getSecond().getType());
			}
			
			population.addPerson(t.getFirst());
			t.getFirst().getAttributes().putAttribute("SUBPOP_ATTRIB_NAME", personGroupName+"_GV");
			//population.getPersonAttributes().putAttribute(t.getFirst().getId().toString(), "SUBPOP_ATTRIB_NAME", personGroupName+"_GV");
			vehicles.addVehicle(t.getSecond());
		}
		
		for(Tuple<Person,Vehicle> t:personsAndVehiclessub2) {
			if(!vehicles.getVehicleTypes().containsKey(t.getSecond().getType().getId())) {
				vehicles.addVehicleType(t.getSecond().getType());
			}
			
			population.addPerson(t.getFirst());
			t.getFirst().getAttributes().putAttribute("SUBPOP_ATTRIB_NAME", tripGroupName+"_GV");
			//population.getPersonAttributes().putAttribute(t.getFirst().getId().toString(), "SUBPOP_ATTRIB_NAME", tripGroupName+"_GV");
			vehicles.addVehicle(t.getSecond());
		}
		personPerson+=personsAndVehiclessub1.size();
		tripPerson+=personsAndVehiclessub2.size();
		
		return scenario;
	}
	
	/**
	 * This will create vehicle
	 * @return
	 */
	public VehicleType getType() {
		return vehicleTypeGenearator(vehicleType);
	}


	public double getVehicleWeight() {
		return vehicleWeight;
	}

	public HashMap<Double, FreightTrip> getTrips() {
		return trips;
	}

	public double getAgeOfgoodsVehicle() {
		return ageOfgoodsVehicle;
	}

	public double getYearOfPurchase() {
		return yearOfPurchase;
	}

	public double getPurchasingPrice() {
		return purchasingPrice;
	}

	public double getAverageFuelCost() {
		return averageFuelCost;
	}

	public double getRouteCoiceCriteria() {
		return routeCoiceCriteria;
	}

	public List<Person> getPersons() {
		return Persons;
	}
	@Override
	public Id<Vehicle> getId() {
		return this.vehicleId;
	}

	/**
	 * This will clone vehicle 
	 * @return
	 */
	public GoodsVehicle cloneVehicle(String addedId) {
		
		GoodsVehicle newVehicle=null;
		if(this.crossHerbourTrip) {
			newVehicle=new GoodsVehicle(this.vehicleType,this.vehicleId.toString()+addedId,this.vehicleWeight,this.ageOfgoodsVehicle,this.yearOfPurchase,this.purchasingPrice,
					this.averageFuelCost,this.routeCoiceCriteria,this.unitofAverageFuelCost,this.yearlyMaintainenceCost,1.,new Double(this.crossHerbourTunnelUsed),
					new Double(this.timeAcrossCrossHerbourTunnel),new Double(this.tripPurposeCrossHerbourTunnel),new String(this.crossBoundaryControlPoint));
		}else {
			newVehicle=new GoodsVehicle(this.vehicleType,this.vehicleId.toString()+addedId,this.vehicleWeight,this.ageOfgoodsVehicle,this.yearOfPurchase,this.purchasingPrice,
					this.averageFuelCost,this.routeCoiceCriteria,this.unitofAverageFuelCost,this.yearlyMaintainenceCost,2.,null,null,null,null);
		}
		
		for(FreightTrip trip:this.trips.values()) {
			newVehicle.addTrip(trip.cloneTrip());
			
		}
		return newVehicle;
	}

	public static Plan clonePlan(Plan plan,PopulationFactory popfac) {
		Plan clonedPlan=popfac.createPlan();
		for(PlanElement pe:plan.getPlanElements()) {
			if(pe instanceof Activity) {
				Activity act=popfac.createActivityFromCoord(new String(((Activity)pe).getType()), new Coord(((Activity)pe).getCoord().getX(),((Activity)pe).getCoord().getY()));
				act.setStartTime(((Activity)pe).getStartTime());
				act.setEndTime(((Activity)pe).getEndTime());
				clonedPlan.addActivity(act);
			}else {
				Leg leg=popfac.createLeg(new String(((Leg)pe).getMode()));
				leg.setDepartureTime(((Leg)pe).getDepartureTime());
				leg.setTravelTime(((Leg)pe).getTravelTime());
				clonedPlan.addLeg(leg);
			}
		}
		return clonedPlan;
	}
	private HashMap<Id<TPUSB>,Tuple<Double,Double>> generatePersonSpecificRandomNumber() {
		HashMap<Id<TPUSB>,Tuple<Double,Double>> tpusbSpecificRandomXYPair=new HashMap<>();
		for(FreightTrip trip:this.trips.values()) {
			TPUSB otpusb=trip.getOtpusb();
			TPUSB dtpusb=trip.getDtpusb();
			if(otpusb!=null && dtpusb!=null) {
				if(!tpusbSpecificRandomXYPair.containsKey(otpusb.getTPUSBId())) {
					double randx=Math.random()*otpusb.getHalfLength();
					//3.14/4 is taken to convert the square assumption to circle assumption while calculating the half length
					double randy=Math.random()*otpusb.getHalfLength();
					tpusbSpecificRandomXYPair.put(otpusb.getTPUSBId(), new Tuple<>(randx,randy));
				}
				if(!tpusbSpecificRandomXYPair.containsKey(dtpusb.getTPUSBId())) {
					double randx=Math.random()*dtpusb.getHalfLength();
					//3.14/4 is taken to convert the square assumption to circle assumption while calculating the half length
					double randy=Math.random()*dtpusb.getHalfLength();
					tpusbSpecificRandomXYPair.put(dtpusb.getTPUSBId(), new Tuple<>(randx,randy));
				}
			}
		}
		
		return tpusbSpecificRandomXYPair;
	}
	private HashMap<Id<TPUSB>,Tuple<Double,Double>> generateTripSpecificRandomNumber(FreightTrip trip){
		HashMap<Id<TPUSB>,Tuple<Double,Double>> tpusbSpecificRandomXYPair=new HashMap<>();
		TPUSB otpusb=trip.getOtpusb();
		TPUSB dtpusb=trip.getDtpusb();
		
		double randx=Math.random()*otpusb.getHalfLength();
		//3.14/4 is taken to convert the square assumption to circle assumption while calculating the half length
		double randy=Math.random()*otpusb.getHalfLength();
		tpusbSpecificRandomXYPair.put(otpusb.getTPUSBId(), new Tuple<>(randx,randy));


		double drandx=Math.random()*dtpusb.getHalfLength();
		//3.14/4 is taken to convert the square assumption to circle assumption while calculating the half length
		double drandy=Math.random()*dtpusb.getHalfLength();
		tpusbSpecificRandomXYPair.put(dtpusb.getTPUSBId(), new Tuple<>(drandx,drandy));

		
		return tpusbSpecificRandomXYPair;
	}
}
