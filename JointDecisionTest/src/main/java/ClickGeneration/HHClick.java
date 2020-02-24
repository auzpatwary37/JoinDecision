package ClickGeneration;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.framework.cliques.population.CliquesWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Household;

public class HHClick {
public static void main(String[] args) {
	
	Config config = ConfigUtils.createConfig();
	ConfigUtils.loadConfig("fullHk/config_Ashraf.xml");
	config.plans().setInputFile("fullHk/populationHKI.xml");
	config.households().setInputFile("fullHk/householdHKI.xml");
	Scenario scenario = ScenarioUtils.loadScenario(config);
	
	
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
}
}
