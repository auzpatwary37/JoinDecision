package ClickGeneration;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsFactory;
import org.matsim.households.Income;


/**
 * 
 * @author Ashraf
 *
 */

public class HouseHold {
	/**
	 * This class will represent the household of TCS database
	 * This class will use the TPUSB class already defined in the GVTCS package
	 * 
	 */
	private Id<HouseHold> houseHoldId;
	private double typeOfHousing;
	private double noOfHouseHoldMember;
	private double vehicleAvailable;
	private double samplingDistrict;
	private double typeOfHousingTYPE;
	private double attachmentSurveyNo;
	private double monthlyHouseholdIncome;
	private TPUSB tpusbAddress;
	private double householdExpansionFactor;
	private ArrayList<HouseHoldMember> members=new ArrayList<>();
	
	public HouseHold(double qNo,double typeofhousingA1,double noOfMember,double vehicleAvailable,double samplingDistrict, 
			double typeOfHousingTYPE,double attachmentSurveyNo, double householdIncome,TPUSB addressTPUSB,double expansionFactor) {
		this.houseHoldId=Id.create(Double.toString(qNo),HouseHold.class);
		this.typeOfHousing=typeofhousingA1;
		this.noOfHouseHoldMember=noOfMember;
		this.vehicleAvailable=vehicleAvailable;
		this.samplingDistrict=samplingDistrict;
		this.typeOfHousingTYPE=typeOfHousingTYPE;
		this.attachmentSurveyNo=attachmentSurveyNo;
		this.monthlyHouseholdIncome=householdIncome;
		this.tpusbAddress=addressTPUSB;
		this.householdExpansionFactor=expansionFactor;
		
	}
	
	public HouseHold clone() {
		return new HouseHold(Double.parseDouble(this.houseHoldId.toString()),
				this.typeOfHousing,this.noOfHouseHoldMember,this.vehicleAvailable,this.samplingDistrict,this.typeOfHousingTYPE,
				this.attachmentSurveyNo,this.monthlyHouseholdIncome,this.tpusbAddress,this.householdExpansionFactor);
	}

	public Id<HouseHold> getHouseHoldId() {
		return houseHoldId;
	}

	public double getTypeOfHousing() {
		return typeOfHousing;
	}

	public double getNoOfHouseHoldMember() {
		return noOfHouseHoldMember;
	}

	public double getVehicleAvailable() {
		return vehicleAvailable;
	}

	public double getSamplingDistrict() {
		return samplingDistrict;
	}

	public double getTypeOfHousingTYPE() {
		return typeOfHousingTYPE;
	}

	public double getAttachmentSurveyNo() {
		return attachmentSurveyNo;
	}

	public double getMonthlyHouseholdIncome() {
		return monthlyHouseholdIncome;
	}

	public TPUSB getTpusbAddress() {
		return tpusbAddress;
	}

	public double getHouseholdExpansionFactor() {
		return householdExpansionFactor;
	}
	
	public void addMember(HouseHoldMember member) {
		this.members.add(member);
	}
	public ArrayList<HouseHoldMember> getMembers(){
		return this.members;
	}
	
	public void generateMinimumWeightHouseHolds(Scenario sc) {
		Households hhs=sc.getHouseholds();
		HouseholdsFactory hhsf=hhs.getFactory();
		double minWeight = this.getHouseholdExpansionFactor();
		for(HouseHoldMember hhm:this.getMembers()) {
			double hmFactor = hhm.getMinimumVehicleWeight();
			if(hmFactor<minWeight)minWeight=hmFactor;
		}
		
		for(int i=0;i<minWeight;i++) {
			Household hh = hhsf.createHousehold(Id.create(this.houseHoldId.toString()+"_"+i, Household.class));
			hh.setIncome(hhsf.createIncome(this.getMonthlyHouseholdIncome(), Income.IncomePeriod.month));
			hhs.getHouseholds().put(hh.getId(), hh);
		}
		
	}
}
