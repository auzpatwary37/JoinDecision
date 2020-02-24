package ClickGeneration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.utils.collections.Tuple;
/**
 * 
 * @author Ashraf
 *
 */
public class ActivityAnalyzer {
	private HashMap<String,Tuple<Double,Integer>> averageStartingTimeCalculator=new HashMap<>();
	private HashMap<String,Tuple<Double,Integer>> averageEndTimeCalculator=new HashMap<>();
	private HashMap<String,Double>activityDuration=new HashMap<>();
	/**
	 * This function finds the average activity duration for each activity inside a popualtion file
	 * @param population
	 * @return
	 */
	public HashMap<String,Double> getAverageActivityDuration(Population population) {
		HashMap<String,Double>actDurations=new HashMap<>();
		HashMap<String,Tuple<Double,Integer>> activities=new HashMap<>();
		for(Person p:population.getPersons().values()) {
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Activity) {
					Activity a=(Activity)pe;
					
					if(a.getStartTime()!=Double.NEGATIVE_INFINITY && a.getEndTime()!=Double.NEGATIVE_INFINITY) {
//						if(a.getStartTime()>a.getEndTime()) {
//							a.setEndTime(24*3600);
//						}
						double duration=a.getEndTime()-a.getStartTime();
						if(duration<0) {
							throw new IllegalArgumentException("duration can not be negative");
						}
						if(activities.containsKey(a.getType())) {
							Tuple<Double,Integer> oldActDetails=activities.get(a.getType());
							Tuple<Double,Integer> newActDetails=new Tuple<>((oldActDetails.getFirst()*oldActDetails.getSecond()+duration)/(oldActDetails.getSecond()+1)
									,oldActDetails.getSecond()+1);
							activities.put(a.getType(), newActDetails);
						}else {
							Tuple<Double,Integer> newActDetails=new Tuple<>(duration,1);
							activities.put(a.getType(), newActDetails);
						}
					}
				}
			}
		}
		for(String s:activities.keySet()) {
			actDurations.put(s,activities.get(s).getFirst());
		}
		this.activityDuration=actDurations;
		return actDurations;
	}

	public HashMap<String, Double> getAverageStartingTime(Population population) {
		for(Person p:population.getPersons().values()) {
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Activity) {
					Activity a=(Activity)pe;
					if(a.getStartTime()!=Double.NEGATIVE_INFINITY) {
					if(averageStartingTimeCalculator.containsKey(a.getType())) {
						Tuple<Double,Integer>oldTuple=this.averageStartingTimeCalculator.get(a.getType());
						Tuple<Double,Integer>newTuple=new Tuple<>(oldTuple.getFirst()+a.getStartTime(),oldTuple.getSecond()+1);
						this.averageStartingTimeCalculator.put(a.getType(),newTuple);
					}else {
						Tuple<Double,Integer>newTuple=new Tuple<>(a.getStartTime(),1);
						this.averageStartingTimeCalculator.put(a.getType(),newTuple);
					}
					}
				}
			}
		}
		HashMap<String, Double> averageStartingTime=new HashMap<>();
		for(String s:this.averageStartingTimeCalculator.keySet()) {
			averageStartingTime.put(s, this.averageStartingTimeCalculator.get(s).getFirst()/this.averageStartingTimeCalculator.get(s).getSecond());
		}
		return averageStartingTime;
	}
	
	public HashMap<String, Double> getAverageClosingTime(Population population) {
		for(Person p:population.getPersons().values()) {
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Activity) {
					Activity a=(Activity)pe;
					if(a.getEndTime()!=Double.NEGATIVE_INFINITY) {
					if(averageEndTimeCalculator.containsKey(a.getType())) {
						Tuple<Double,Integer>oldTuple=this.averageEndTimeCalculator.get(a.getType());
						Tuple<Double,Integer>newTuple=new Tuple<>(oldTuple.getFirst()+a.getEndTime(),oldTuple.getSecond()+1);
						this.averageEndTimeCalculator.put(a.getType(),newTuple);
					}else {
						Tuple<Double,Integer>newTuple=new Tuple<>(a.getEndTime(),1);
						this.averageEndTimeCalculator.put(a.getType(),newTuple);
					}
					}
				}
			}
		}
		HashMap<String, Double> averageStartingTime=new HashMap<>();
		for(String s:this.averageStartingTimeCalculator.keySet()) {
			averageStartingTime.put(s, this.averageStartingTimeCalculator.get(s).getFirst()/this.averageStartingTimeCalculator.get(s).getSecond());
		}
		return averageStartingTime;
	}
	public Set<String> getStartOrEndActivityTypes(Population pop){
		Set<String> activities=new HashSet<String>();
		for(Person person:pop.getPersons().values()) {
			for(Plan plan: person.getPlans()) {
				Activity startingActivity=(Activity)plan.getPlanElements().get(0);
				Activity endingActivity=(Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1);
				
				startingActivity.setType(startingActivity.getType()+"_StartOrEnd");
				endingActivity.setType(endingActivity.getType()+"_StartOrEnd");
				activities.add(startingActivity.getType());
				activities.add(endingActivity.getType());
			}
		}
		
		return activities;
	}
	
	public Set<String> getActivityTypes(Population population){
		Set<String> ActivityTypes=new HashSet<String>();
		for(Person p:population.getPersons().values()) {
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Activity) {
					Activity a=(Activity)pe;
					ActivityTypes.add(a.getType());
				}
			}
		}
		return ActivityTypes;
	}

	/**
	 * This function splits an activity into multiple activity and writes the activityparams on the config file
	 * Only works for activities that are not start or end activity of the chain
	 * @param population
	 * @param config
	 * @param activityType
	 * @param timeGapInSecond
	 */
	public void ActivitySplitter(Population population,Config config, String activityType,Double timeGapInSecond,boolean shouldAddearliestEndTimeAndLatestStartTime) {
		HashMap<String,Tuple<Double,Double>> activities=new HashMap<>();
		HashMap<String,Integer> activityCounter=new HashMap<>();
		HashMap<String,Double> activityDurationSum=new HashMap<>();
		HashMap<String,ArrayList<Double>> activityStartTime=new HashMap<>();
		HashMap<String,ArrayList<Double>> activityEndTime=new HashMap<>();
		double startTime=3*3600;
		double endTime=27*3600;
		for(double d=startTime;d<endTime;d=d+timeGapInSecond) {
			activities.put(activityType+"_"+d, new Tuple<>(d,d+timeGapInSecond));
			activityCounter.put(activityType+"_"+d, 0);
			activityDurationSum.put(activityType+"_"+d, 0.);
			activityStartTime.put(activityType+"_"+d,new ArrayList<>());
			activityEndTime.put(activityType+"_"+d, new ArrayList<>());
		}

		for(Person p:population.getPersons().values()) {
			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
				if(pe instanceof Activity) {
					Activity a=(Activity)pe;
					if(a.getType().equals(activityType)) {
						for(Tuple<Double,Double>t:activities.values()) {
							if(a.getStartTime()>=t.getFirst()&&a.getStartTime()<t.getSecond()&&a.getStartTime()!=Double.NEGATIVE_INFINITY) {
								a.setType(activityType+"_"+t.getFirst());
								activityCounter.put(activityType+"_"+t.getFirst(),activityCounter.get(activityType+"_"+t.getFirst())+1);
								activityDurationSum.put(activityType+"_"+t.getFirst(),activityDurationSum.get(activityType+"_"+t.getFirst())+(a.getEndTime()-a.getStartTime()));
								activityStartTime.get(activityType+"_"+t.getFirst()).add(a.getStartTime());
								activityEndTime.get(activityType+"_"+t.getFirst()).add(a.getEndTime());
								break;
							}
						}
					}
				}
			}
		}
		ActivityParams aParams=config.planCalcScore().getActivityParams(activityType);
		for(String s:activityCounter.keySet()) {
			if(activityCounter.get(s)!=0) {
				ActivityParams ap=new ActivityParams(s);
				if(activityDurationSum.get(s)/activityCounter.get(s)!=0) {
					ap.setTypicalDuration(activityDurationSum.get(s)/activityCounter.get(s));
				}else {
					ap.setTypicalDuration(activityDurationSum.get(s)/activityCounter.get(s)+300);
				}
				ap.setClosingTime(Math.ceil((this.calcAverage(activityEndTime.get(s))+this.calcSD(activityEndTime.get(s)))/1800)*1800);
				if(shouldAddearliestEndTimeAndLatestStartTime) {
					ap.setLatestStartTime(Math.floor(this.calcAverage(activityStartTime.get(s))/900)*900);
					ap.setEarliestEndTime(Math.ceil(this.calcAverage(activityEndTime.get(s))/900)*900);
				}
				ap.setOpeningTime(Math.floor((this.calcAverage(activityStartTime.get(s))-this.calcSD(activityStartTime.get(s)))/1800)*1800);
				config.planCalcScore().addActivityParams(ap);
			}
		}
	}
	
	public static void addActivityPlanParameter(PlanCalcScoreConfigGroup config,ArrayList<String>activityTypes,HashMap<String,Double>typicalDurations,
			HashMap<String,Double>typicalStartingTime,int addedlatestStartTime,int earliestStartTime,
			int defaultTypicalDuration,int defaultTypicalStartingTime,int defaultOpenningTime){
		if(activityTypes==null) {
			
			activityTypes=new ArrayList<String>();
			for(String s:typicalStartingTime.keySet()) {
				if(!activityTypes.contains(s)) {
					activityTypes.add(s);
				}
			}
			for(String s:typicalDurations.keySet()) {
				if(!activityTypes.contains(s)) {
					activityTypes.add(s);
				}
			}
			
		}
		for(String s:activityTypes) {
			ActivityParams act = new ActivityParams(s);
			if(typicalDurations.get(s)!=null && typicalDurations.get(s)!=0) {
				act.setTypicalDuration(typicalDurations.get(s));
			}else {
				act.setTypicalDuration(defaultTypicalDuration);
			}
			if(typicalStartingTime.get(s)!=null) {
				act.setLatestStartTime(typicalStartingTime.get(s)+15*60);
				act.setOpeningTime(typicalStartingTime.get(s)-3600);
			}else {
				act.setLatestStartTime(0+defaultTypicalStartingTime);
				act.setOpeningTime(defaultOpenningTime);
			}
			act.setClosingTime(26*3600);
			config.addActivityParams(act);
		}
	}
	
	public static void addActivityPlanParameter(PlanCalcScoreConfigGroup config,Set<String>activityTypes,HashMap<String,Double>typicalDurations,
			HashMap<String,Double>typicalStartingTime,HashMap<String,Double>typicalEndTime,Set<String>startAndEndActivities, int addedlatestStartTimeMin,int earliestEndTimeMin,
			int defaultTypicalDuration,int defaultTypicalStartingTime,int defaultOpenningTime, int defaultEndTime, boolean addstartandendTime){
		
		for(String s:activityTypes) {
			ActivityParams act = new ActivityParams(s);
			if(typicalDurations.get(s)!=null && typicalDurations.get(s)!=0) {
				act.setTypicalDuration(typicalDurations.get(s));
			}else {
				act.setTypicalDuration(defaultTypicalDuration);
			}
			if(addstartandendTime==true) {
			if(typicalStartingTime.get(s)!=null && !startAndEndActivities.contains(act.getActivityType())) {
				act.setLatestStartTime(typicalStartingTime.get(s)+addedlatestStartTimeMin*60);
				if(typicalStartingTime.get(s)-1800<0) {
					act.setOpeningTime(typicalStartingTime.get(s));
				}else {
					act.setOpeningTime(typicalStartingTime.get(s)-1800);
				}
			}else {
				act.setLatestStartTime(defaultTypicalStartingTime);
				if(!startAndEndActivities.contains(act.getActivityType())) {
					act.setOpeningTime(defaultOpenningTime);
				}
			}
			if(typicalEndTime.get(s)!=null && !startAndEndActivities.contains(act.getActivityType())) {
				act.setEarliestEndTime(typicalEndTime.get(s)-earliestEndTimeMin*60);
				if(typicalEndTime.get(s)+1800>24*3600) {
					act.setClosingTime(typicalEndTime.get(s));
				}else {
					act.setClosingTime(typicalEndTime.get(s)+1800);
				}
			}else {
				act.setEarliestEndTime(defaultEndTime-earliestEndTimeMin);
				if(!startAndEndActivities.contains(act.getActivityType())) {
					act.setClosingTime(defaultEndTime);
				}
			}
			if(act.getOpeningTime()==0 && act.getClosingTime()==24*3600) {
				System.out.println(act.getActivityType());
			}
			}
			config.addActivityParams(act);
			
			
		}
	}
	
	
	public Map<String,Map<String,ArrayList<Double>>> analyzeActivities(Population population,String writeLocation,String distWriteLoc) {
		//attributes name
		final String startTimeString="startTime";
		final String endTimeString="endTime";
		final String durationString="duration";
		final String coordString="coord";
		String[] attributes=new String[] {startTimeString,endTimeString,durationString};
		
		Set<String> activityTypes=this.getActivityTypes(population);
		Map<String,Integer>counter=new HashMap<>();
		for(String ss:activityTypes) {
			counter.put(ss, 0);
		}
		
		//the map is Map<activityType,Map<attributes,value>>
		Map<String,Map<String,ArrayList<Double>>> activityDetails=new HashMap<>();
		for(String s:activityTypes) {
			
			Map<String,ArrayList<Double>> attributeDetails=new HashMap<>();
			for(String ss:attributes) {
				attributeDetails.put(ss, new ArrayList<Double>());
			}
			activityDetails.put(s,attributeDetails);
		}
		
		for(Person person: population.getPersons().values()) {
			for(Plan plan:person.getPlans()) {
				for(PlanElement pe:plan.getPlanElements()) {
					if(pe instanceof Activity) {
						Activity a=(Activity) pe;
						counter.put(a.getType(), counter.get(a.getType())+1);
						if(a.getStartTime()!=Double.NEGATIVE_INFINITY) {
						activityDetails.get(a.getType()).get(startTimeString).add(a.getStartTime());
						}
						if(a.getEndTime()!=Double.NEGATIVE_INFINITY) {
						activityDetails.get(a.getType()).get(endTimeString).add(a.getEndTime());
						}
						if(a.getStartTime()!=Double.NEGATIVE_INFINITY && a.getEndTime()!=Double.NEGATIVE_INFINITY) {
							if(a.getStartTime()<=a.getEndTime()) {
								activityDetails.get(a.getType()).get(durationString).add(a.getEndTime()-a.getStartTime());
							}else {
								activityDetails.get(a.getType()).get(durationString).add(a.getEndTime()+3600*24-a.getStartTime());
							}
						}
					}else {
						continue;
					}
				}
			}
		}
		List<Double> timeFrq=new ArrayList<>();
		for(double i=.5;i<=27;i=i+.5) {
			timeFrq.add(i*3600);
		}
		
		try {
			FileWriter fww=new FileWriter(new File(distWriteLoc),false);
			fww.append("ActivityType,attribute");
			for(double d: timeFrq) {
				fww.append(","+d);
			}
			fww.append("\n");
			for(String s:activityDetails.keySet()) {
				for(String ss:activityDetails.get(s).keySet()) {
					fww.append(s);
					fww.append(","+ss);
					for(int i:this.calcFrequecy(activityDetails.get(s).get(ss), timeFrq).values()) {
						fww.append(","+i);
					}
					fww.append("\n");
				}
			}
			fww.flush();
			fww.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		try {
			FileWriter fw= new FileWriter(new File(writeLocation),false);
			//appendHeader
			fw.append("ActivityType,AverageStartTime,SDStartTime,PercentStartTimeAvailable,AverageEndTime,SDEndTime,PercentEndTimeAvailable,AverageDuration,SDDuration,PercentDurationAvailable,instance\n");
			for(String s:activityDetails.keySet()) {
				
				double averageStartTime=this.calcAverage(activityDetails.get(s).get(startTimeString));
				double sdStartTime=this.calcSD(activityDetails.get(s).get(startTimeString));
				//System.out.println(activityDetails.get(s).get(startTimeString).size());
				double percentageStartTime=activityDetails.get(s).get(startTimeString).size()/(double)counter.get(s)*100;
				
				double averageEndTime=this.calcAverage(activityDetails.get(s).get(endTimeString));
				double sdEndTime=this.calcSD(activityDetails.get(s).get(endTimeString));
				double percentageEndTime=activityDetails.get(s).get(endTimeString).size()/(double)counter.get(s)*100;
				
				double averageDuration=this.calcAverage(activityDetails.get(s).get(durationString));
				double sdDuration=this.calcSD(activityDetails.get(s).get(durationString));
				double percentageDuration=activityDetails.get(s).get(durationString).size()/(double)counter.get(s)*100;
				fw.append(s+","+averageStartTime+","+sdStartTime+","+percentageStartTime+","+averageEndTime+","+sdEndTime+","+percentageEndTime+","+averageDuration+","+sdDuration+","+percentageDuration+","+counter.get(s)+"\n");
			}
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return activityDetails;
	}
	
	private double calcAverage(List<Double> a) {
		double average=0;
		for(double d:a) {
			average+=d;
		}
		return average/a.size();
	}
	private double calcSD(List<Double> a) {
		double sd=0;
		double avreage=this.calcAverage(a);
		for(double d:a) {
			sd+=(d-avreage)*(d-avreage);
		}
		sd=Math.sqrt(sd/a.size());
		return sd;
	}
	
	private Map<Double,Integer> calcFrequecy(List<Double>dataset,List<Double>x) {
		Map<Double,Integer> data=new HashMap<>();
		for(double d:x) {
			data.put(d, 0);
		}
		for(double d:dataset) {
			for(int i=1;i<x.size();i++) {
				if(d>=0 && d<=x.get(0)) {
					data.put(x.get(0), data.get(x.get(0))+1);
					break;
				}else if(d>x.get(i-1) && d<=x.get(i)) {
					data.put(x.get(i), data.get(x.get(i))+1);
					break;
				}
			}
		}
		return data;
	}
	
	public Map<String,Map<String,Double>> readActivityTimings(String fileLoc,Config config){
		Map<String,Map<String,Double>> timings=new HashMap<>();
		String openingTimeString="openingTime";
		String closingTimeString="closingTime";
		String latestStartTimeString="latestStartTime";
		String earliestEndTimeString="earliestEndTime";
		String typicalDurationString="typicalDuration";
		try {
			BufferedReader bf=new BufferedReader(new FileReader(new File(fileLoc)));
			String line;
			bf.readLine();
			while((line=bf.readLine())!=null) {
				String[] part=line.split(",");
				Map<String,Double> map=new HashMap<>();
				if(!part[1].equals("na")) {
					map.put(openingTimeString, Double.parseDouble(part[1]));
					map.put(closingTimeString, Double.parseDouble(part[3]));
					
				}
				if(!part[5].equals("na")) {
					map.put(typicalDurationString, Double.parseDouble(part[5]));
				}
				if(!part[2].equals("na")) {
					map.put(latestStartTimeString, Double.parseDouble(part[2]));
				}
				if(!part[4].equals("na")) {
					map.put(earliestEndTimeString, Double.parseDouble(part[4]));
				}
				timings.put(part[0], map);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(String s:timings.keySet()) {
			ActivityParams a=new ActivityParams();
			a.setActivityType(s);
			if(timings.get(s).get(openingTimeString)!=null) {
				a.setOpeningTime(timings.get(s).get(openingTimeString));
				a.setClosingTime(timings.get(s).get(closingTimeString));
				
			}
			if(timings.get(s).get(typicalDurationString)!=null) {
				if( timings.get(s).get(typicalDurationString)!=0.) {
					a.setTypicalDuration(timings.get(s).get(typicalDurationString));
				}else {
					a.setTypicalDuration(1.);
				}
			}else {
				a.setTypicalDuration(8*3600);
			}
			if(timings.get(s).get(earliestEndTimeString)!=null) {
				a.setEarliestEndTime(timings.get(s).get(earliestEndTimeString));
				
			}
			if(timings.get(s).get(latestStartTimeString)!=null) {
				a.setLatestStartTime(timings.get(s).get(latestStartTimeString));
			}
			config.planCalcScore().addActivityParams(a);
		}
		
		return timings;
	}
}

