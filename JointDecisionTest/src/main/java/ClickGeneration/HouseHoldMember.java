package ClickGeneration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;




/**
 * 
 * @author Ashraf
 *
 */
public class HouseHoldMember {
/**
 * This Class will represent the household member as provided in TCS database 2011
 */
	
	private Id<HouseHoldMember> HmId;
	private Id<HouseHold> HhId;
	private double relation;
	private double age;
	private double sex;
	private Double haveJob;
	private Double currentLevelofStudy;
	private Double haveLisence;
	private double monthlyHouseHoldIncome;
	private Double referenceTravelDay;
	private Double totalFeeofSchoolBus;
	private Double isOneWay;
	private Double enteredFromOverseas;
	private Double controlPointUsed;
	private Double destinationAfterEnteringHK;
	private TPUSB addressTPUSB;
	private double hmExpansionFactor;
	private double usualOrMobileResident;
	private double econominStatus;
	private HashMap<Double,TCSTrip> trips=new HashMap<>();
	private ArrayList<Id<Person>> personsWithCar=new ArrayList<>();
	private ArrayList<Id<Person>> personsWithoutCar=new ArrayList<>();
	
	
	public HouseHoldMember(double qNo,double memberNo,double relation, double sex, double age,Double haveJob, Double levelOfStudy,
			Double haveLisence, double MonthlyIncome,Double refTravelDay,Double feeOfShuttle, Double isOneWay,Double isFromOverseas,
			Double controlPoint, Double destinationHk, TPUSB address,double hmExpansionFactor,double usualormobileResidence, double EconStatus) {
		this.HmId=Id.create(Double.toString(qNo)+"_"+Double.toString(memberNo), HouseHoldMember.class);
		this.HhId=Id.create(Double.toString(qNo), HouseHold.class);
		this.relation=relation;
		this.age=age;
		this.sex=sex;
		this.haveJob=haveJob;
		this.currentLevelofStudy=levelOfStudy;
		this.haveLisence=haveLisence;
		this.monthlyHouseHoldIncome=MonthlyIncome;
		this.referenceTravelDay=refTravelDay;
		this.totalFeeofSchoolBus=feeOfShuttle;
		this.isOneWay=isOneWay;
		this.enteredFromOverseas=isFromOverseas;
		this.controlPointUsed=controlPoint;
		this.destinationAfterEnteringHK=destinationHk;
		this.addressTPUSB=address;
		this.hmExpansionFactor=hmExpansionFactor;
		this.usualOrMobileResident=usualormobileResidence;
		this.econominStatus=EconStatus;
		
	}
	public HouseHoldMember Clone() {
		
		return new HouseHoldMember(Double.parseDouble((this.HmId.toString().split("_")[0])),Double.parseDouble((this.HmId.toString().split("_")[1])),
				this.relation,this.sex,this.age,this.haveJob,this.currentLevelofStudy,this.haveLisence,this.monthlyHouseHoldIncome,this.referenceTravelDay,
				this.totalFeeofSchoolBus,this.isOneWay,this.enteredFromOverseas,this.controlPointUsed,this.destinationAfterEnteringHK,this.addressTPUSB,
				this.hmExpansionFactor,this.usualOrMobileResident,this.econominStatus);
	}
	
	public void addTrip(TCSTrip trip) {
		
		if(trip.getArrivalTime()<trip.getDepartureTime()) {
			throw new IllegalArgumentException();
		}
		this.trips.put(trip.getTripNo(), trip);
	}
	public Id<HouseHoldMember> getMemberId() {
		return HmId;
	}
	public Id<HouseHold> getHouseHoldId() {
		return HhId;
	}
	public double getRelation() {
		return relation;
	}
	public double getAge() {
		return age;
	}
	public double getSex() {
		return sex;
	}
	public double getHaveJob() {
		return haveJob;
	}
	public double getCurrentLevelofStudy() {
		return currentLevelofStudy;
	}
	public double getHaveLisence() {
		return haveLisence;
	}
	public double getMonthlyHouseHoldIncome() {
		return monthlyHouseHoldIncome;
	}
	public double getReferenceTravelDay() {
		return referenceTravelDay;
	}
	public double getTotalFeeofSchoolBus() {
		return totalFeeofSchoolBus;
	}
	public double getIsOneWay() {
		return isOneWay;
	}
	public double getEnteredFromOverseas() {
		return enteredFromOverseas;
	}
	public double getControlPointUsed() {
		return controlPointUsed;
	}
	public double getDestinationAfterEnteringHK() {
		return destinationAfterEnteringHK;
	}
	public TPUSB getAddressTPUSB() {
		return addressTPUSB;
	}
	public double getHmExpansionFactor() {
		return hmExpansionFactor;
	}
	public double getUsualOrMobileResident() {
		return usualOrMobileResident;
	}
	public double getEconominStatus() {
		return econominStatus;
	}
	public HashMap<Double, TCSTrip> getTrips() {
		return trips;
	}
	protected double getMinimumVehicleWeight() {
		double minWeight=this.hmExpansionFactor;
		minWeight=Double.MAX_VALUE;
		if(this.trips!=null && this.trips.size()!=0) {
		for(TCSTrip trip:this.trips.values()) {
			if(trip.getTripExpansionFactor()<minWeight) {
				minWeight=trip.getTripExpansionFactor();
			}
		}
		}
		return minWeight;
	}
	private ArrayList<Tuple<Person,Vehicle>> getMinWeightPersonAndVehicle(Households hh,HashMap<Double,TCSMode>modesDetails,HashMap<Double,String> activityDetails,PopulationFactory populationFactory, VehiclesFactory vehiclesFactory){
		ArrayList<Tuple<Person,Vehicle>> personList=new ArrayList<>();
		PopulationFactory popfac=populationFactory;
		VehiclesFactory vf=vehiclesFactory;
		for(int j=0;j<this.getMinimumVehicleWeight();j++) {
			HashMap<Id<TPUSB>,Tuple<Double,Double>>randXY=this.generatePersonSpecificRandomNumber();
			Person person=popfac.createPerson(Id.createPersonId(this.getMemberId().toString()+"_"+j));
			Id<Household> householdId = Id.create(this.HhId.toString()+"_"+j, Household.class);
			Vehicle vehicle=this.createVehicle(vf, modesDetails, "_"+j);
			if(vehicle.getType().getDescription().equals("taxi_car")) {
				this.personsWithoutCar.add(person.getId());
			}else{
				this.personsWithCar.add(person.getId());
			}
			Plan plan=popfac.createPlan();
			ArrayList<Activity> activities=new ArrayList<>();
			ArrayList<Leg> activityConnectorTripLegs=new ArrayList<>();
			ArrayList<Double> tripOrder=new ArrayList<>(this.trips.keySet());
			Collections.sort(tripOrder);
			int i=0;
			for(double tOrder:tripOrder) {
				TCSTrip trip=this.trips.get(tOrder);
				if(trip.getOtpusb()!=null && trip.getDtpusb()!=null) {
					if(i==0) {
						Coord ocoord=new Coord(trip.getOtpusb().getSatCoord().getX()+randXY.get(trip.getOtpusb().getTPUSBId()).getFirst(),
								trip.getOtpusb().getSatCoord().getY()+randXY.get(trip.getOtpusb().getTPUSBId()).getSecond());
						Coord dcoord=new Coord(trip.getDtpusb().getSatCoord().getX()+randXY.get(trip.getDtpusb().getTPUSBId()).getFirst(),
								trip.getDtpusb().getSatCoord().getY()+randXY.get(trip.getDtpusb().getTPUSBId()).getSecond());
						Activity oact=popfac.createActivityFromCoord(activityDetails.get(trip.getOriginActivity()),ocoord);
						Activity dact=popfac.createActivityFromCoord(activityDetails.get(trip.getDestinationActivity()),dcoord);
						oact.setEndTime(trip.getDepartureTime());
						dact.setStartTime(trip.getArrivalTime());
						activities.add(oact);
						activities.add(dact);
					}else {
						Coord ocoord=new Coord(trip.getOtpusb().getSatCoord().getX()+randXY.get(trip.getOtpusb().getTPUSBId()).getFirst(),
								trip.getOtpusb().getSatCoord().getY()+randXY.get(trip.getOtpusb().getTPUSBId()).getSecond());
						Coord dcoord=new Coord(trip.getDtpusb().getSatCoord().getX()+randXY.get(trip.getDtpusb().getTPUSBId()).getFirst(),
								trip.getDtpusb().getSatCoord().getY()+randXY.get(trip.getDtpusb().getTPUSBId()).getSecond());
						
						Activity oact=popfac.createActivityFromCoord(activityDetails.get(trip.getOriginActivity()),ocoord);
						Activity dact=popfac.createActivityFromCoord(activityDetails.get(trip.getDestinationActivity()),dcoord);
						if(!oact.getCoord().equals(activities.get(i).getCoord())) {
							if(activities.get(i).getStartTime()<=trip.getDepartureTime()) {
								activities.get(i).setEndTime(trip.getDepartureTime());
								oact.setStartTime(trip.getDepartureTime());
								oact.setEndTime(trip.getDepartureTime());
								dact.setStartTime(trip.getDepartureTime());
							}else {
								activities.get(i).setEndTime(activities.get(i).getStartTime());
								oact.setStartTime(activities.get(i).getEndTime());
								oact.setEndTime(activities.get(i).getEndTime());
								dact.setStartTime(activities.get(i).getEndTime()+trip.getArrivalTime()-trip.getDepartureTime());
							}
							activities.add(oact);
							activities.add(dact);
							
							i++;
							Leg legDummy=popfac.createLeg("car");
							legDummy.setDepartureTime(activities.get(i).getEndTime());
							legDummy.setTravelTime(0);
							activityConnectorTripLegs.add(legDummy);
							
							//throw new IllegalArgumentException("Discontinuous Trip Chain!!!");
							//lets think about it later.
						}else {
							if(activities.get(i).getStartTime()<=trip.getDepartureTime()) {
								activities.get(i).setEndTime(trip.getDepartureTime());
								dact.setStartTime(trip.getArrivalTime());
							}else {
								activities.get(i).setEndTime(activities.get(i).getStartTime());
								dact.setStartTime(activities.get(i).getStartTime()+trip.getArrivalTime()-trip.getDepartureTime());
							}
							activities.add(dact);
						}
					}
					Leg leg=popfac.createLeg(trip.getMainMode(modesDetails).getFlatMode());
					leg.setDepartureTime(activities.get(i).getEndTime());
					leg.setTravelTime(trip.getArrivalTime()-trip.getDepartureTime());
					activityConnectorTripLegs.add(leg);
					i++;
				}
				
			}
			if(activities.size()!=0) {
			plan.addActivity(activities.get(0));
			for(int k=0;k<activities.size()-1;k++) {
				plan.addLeg(activityConnectorTripLegs.get(k));
				
				plan.addActivity(activities.get(k+1));
				
			}
			
			person.addPlan(plan);
			personList.add(new Tuple<Person,Vehicle>(person,vehicle));
			if(hh.getHouseholds().containsKey(householdId)) {
				hh.getHouseholds().get(householdId).getMemberIds().add(person.getId());
			}
			}
		}
		return personList;
	}
	
	public Vehicle createVehicle(VehiclesFactory vf,HashMap<Double,TCSMode>modesDetails,String addedtoMemberId) {
		if((this.getMemberId().toString()+addedtoMemberId).equals("24097.0_1.0_0")) {
			System.out.println("Testing!!!");
		}
		if(this.trips!=null && this.trips.size()!=0) {
			for(TCSTrip trip:this.trips.values()) {
				TCSMode mode=trip.getMainMode(modesDetails);
				if(mode.getFlatMode().equals("car")) {
					VehicleType vt=vf.createVehicleType(Id.create(Double.toString(mode.getModeId()), VehicleType.class));
					vt.setPcuEquivalents(mode.getPcu());
					vt.setDescription(mode.getModeDetails()+"_"+mode.getFlatMode());
					Vehicle v= vf.createVehicle(Id.createVehicleId(this.getMemberId().toString()+addedtoMemberId),vt);
					
					return v;
				}
				
			}
			
		}
		VehicleType vt=vf.createVehicleType(Id.create(Double.toString(100), VehicleType.class));
		vt.setPcuEquivalents(1);
		vt.setDescription("taxi"+"_"+"car");
		Vehicle v= vf.createVehicle(Id.createVehicleId(this.getMemberId().toString()+addedtoMemberId),vt);
		return v;
	}

	private ArrayList<Tuple<Person,Vehicle>> getTripPersonAndVehicle(TCSTrip trip, double weight, PopulationFactory populationFactory, VehiclesFactory vehiclesFactory,HashMap<Double,String>activityDetails,HashMap<Double,TCSMode> modesDetails) {
		ArrayList<Tuple<Person,Vehicle>> personList=new ArrayList<>();
		PopulationFactory popfac=populationFactory;
		VehiclesFactory vf=vehiclesFactory;
		if(trip.getOtpusb()!=null && trip.getDtpusb()!=null) {
			for(int i=0;i<weight;i++) {
				HashMap<Id<TPUSB>,Tuple<Double,Double>>randXY=this.generateTripSpecificRandomNumber(trip);
				
				Person person=popfac.createPerson(Id.createPersonId(this.getMemberId().toString()+"_"+trip.getTripNo()+"_"+i));
				Plan plan=popfac.createPlan();
				
				Activity oAct=popfac.createActivityFromCoord(activityDetails.get(trip.getOriginActivity()), 
						new Coord(trip.getOtpusb().getSatCoord().getX()+randXY.get(trip.getOtpusb().getTPUSBId()).getFirst(),
								trip.getOtpusb().getSatCoord().getY()+randXY.get(trip.getOtpusb().getTPUSBId()).getSecond()));
				Activity dAct=popfac.createActivityFromCoord(activityDetails.get(trip.getDestinationActivity()), 
						new Coord(trip.getDtpusb().getSatCoord().getX()+randXY.get(trip.getDtpusb().getTPUSBId()).getFirst(),
								trip.getDtpusb().getSatCoord().getY()+randXY.get(trip.getDtpusb().getTPUSBId()).getSecond()));
				oAct.setEndTime(trip.getDepartureTime());
				dAct.setStartTime(trip.getArrivalTime());
				Leg leg=popfac.createLeg(trip.getMainMode(modesDetails).getFlatMode());
				leg.setDepartureTime(trip.getDepartureTime());
				leg.setTravelTime(trip.getArrivalTime()-trip.getDepartureTime());
				plan.addActivity(oAct);
				plan.addLeg(leg);
				plan.addActivity(dAct);
				person.addPlan(plan);
				Vehicle vehicle=this.createVehicle(vf, modesDetails,"_"+trip.getTripNo()+"_"+i);
//				if(vehicle.getType().getDescription().equals("taxi_car")) {
//					this.personsWithoutCar.add(person.getId());
//				}else if(vehicle.getType().getDescription().equals("car")) {
//					this.personsWithCar.add(person.getId());
//				}
				personList.add(new Tuple<Person,Vehicle>(person,vehicle));
				if(i==500) {
					break;
				}
			}
		}
		return personList;
	}
	
	public ArrayList<Tuple<Person,Vehicle>> getClonedVehicleAndPersons(Scenario scneario, PopulationFactory populationFactory, VehiclesFactory vehiclesFactory,HashMap<Double,String>activityDetails,HashMap<Double,TCSMode>modesDetails){
		ArrayList<Tuple<Person,Vehicle>> personsAndVehicles=new ArrayList<>();
		if(this.trips.size()==0) {
			return personsAndVehicles;
		}
		personsAndVehicles.addAll(this.getMinWeightPersonAndVehicle(scneario.getHouseholds(),modesDetails,activityDetails,populationFactory,vehiclesFactory));
		for(TCSTrip trip:this.trips.values()) {
			personsAndVehicles.addAll(this.getTripPersonAndVehicle(trip, trip.getTripExpansionFactor()-this.getMinimumVehicleWeight(),populationFactory,vehiclesFactory,activityDetails,modesDetails));
		}
		
		return personsAndVehicles;
	}
	
	public Scenario loadClonedVehicleAndPersons(Scenario scenario,HashMap<Double,String>activityDetails,HashMap<Double,TCSMode>modesDetails,String personGroupName,String tripGroupName,Double tripPerson,Double personPerson,boolean shouldLoadTripPerson){
		Population population =scenario.getPopulation();
		Vehicles vehicles=scenario.getVehicles();
		Households hhs= scenario.getHouseholds();
		ArrayList<Tuple<Person,Vehicle>> personsAndVehiclessub1=new ArrayList<>();
		ArrayList<Tuple<Person,Vehicle>> personsAndVehiclessub2=new ArrayList<>();
		if(this.trips.size()==0) {
			return scenario;
		}
		personsAndVehiclessub1.addAll(this.getMinWeightPersonAndVehicle(hhs,modesDetails,activityDetails,population.getFactory(),vehicles.getFactory()));
		if(shouldLoadTripPerson==true) {
			for(TCSTrip trip:this.trips.values()) {
				personsAndVehiclessub2.addAll(this.getTripPersonAndVehicle(trip, trip.getTripExpansionFactor()-this.getMinimumVehicleWeight(),population.getFactory(),vehicles.getFactory(),activityDetails,modesDetails));
			}
		}
		for(Tuple<Person,Vehicle> t:personsAndVehiclessub1) {
			if(t.getSecond()!=null && !vehicles.getVehicleTypes().containsKey(t.getSecond().getType().getId())) {
				vehicles.addVehicleType(t.getSecond().getType());
			}
			
			population.addPerson(t.getFirst());
			if(this.personsWithCar.contains(t.getFirst().getId())) {
				t.getFirst().getAttributes().putAttribute("SUBPOP_ATTRIB_NAME", personGroupName+"_TCS"+"withCar");
				//population.getPersonAttributes().putAttribute(t.getFirst().getId().toString(), "SUBPOP_ATTRIB_NAME", personGroupName+"_TCS"+"withCar");
			}else {
				t.getFirst().getAttributes().putAttribute("SUBPOP_ATTRIB_NAME", personGroupName+"_TCS"+"withoutCar");
				//population.getPersonAttributes().putAttribute(t.getFirst().getId().toString(), "SUBPOP_ATTRIB_NAME", personGroupName+"_TCS"+"withoutCar");
			}
			if(t.getSecond()!=null) {
				vehicles.addVehicle(t.getSecond());
			}
		}
		if(shouldLoadTripPerson==true) {
			for(Tuple<Person,Vehicle> t:personsAndVehiclessub2) {
				if(t.getSecond()!=null && !vehicles.getVehicleTypes().containsKey(t.getSecond().getType().getId())) {
					vehicles.addVehicleType(t.getSecond().getType());
				}
				
				population.addPerson(t.getFirst());
				t.getFirst().getAttributes().putAttribute("SUBPOP_ATTRIB_NAME", tripGroupName+"_TCS");
				//population.getPersonAttributes().putAttribute(t.getFirst().getId().toString(), "SUBPOP_ATTRIB_NAME", tripGroupName+"_TCS");
				if(t.getSecond()!=null) {
					vehicles.addVehicle(t.getSecond());
				}
			}
		}
		tripPerson+=personsAndVehiclessub2.size();
		personPerson+=personsAndVehiclessub1.size();
		//double ratio=personsAndVehiclessub1.size()/(personsAndVehiclessub1.size()+personsAndVehiclessub2.size());
		return scenario;
	}
	
	private HashMap<Id<TPUSB>,Tuple<Double,Double>> generatePersonSpecificRandomNumber() {
		HashMap<Id<TPUSB>,Tuple<Double,Double>> tpusbSpecificRandomXYPair=new HashMap<>();
		for(TCSTrip trip:this.trips.values()) {
			TPUSB otpusb=trip.getOtpusb();
			TPUSB dtpusb=trip.getDtpusb();
			if(!tpusbSpecificRandomXYPair.containsKey(otpusb.getTPUSBId())) {
				double randx=(Math.random()*otpusb.getHalfLength());
				//3.14/4 is taken to convert the square assumption to circle assumption while calculating the half length
				double randy=(Math.random()*otpusb.getHalfLength());
				tpusbSpecificRandomXYPair.put(otpusb.getTPUSBId(), new Tuple<>(randx,randy));
			}
			if(!tpusbSpecificRandomXYPair.containsKey(dtpusb.getTPUSBId())) {
				double randx=(Math.random()*dtpusb.getHalfLength());
				//3.14/4 is taken to convert the square assumption to circle assumption while calculating the half length
				double randy=(Math.random()*dtpusb.getHalfLength());
				tpusbSpecificRandomXYPair.put(dtpusb.getTPUSBId(), new Tuple<>(randx,randy));
			}
		}
		
		return tpusbSpecificRandomXYPair;
	}
	private HashMap<Id<TPUSB>,Tuple<Double,Double>> generateTripSpecificRandomNumber(TCSTrip trip){
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


