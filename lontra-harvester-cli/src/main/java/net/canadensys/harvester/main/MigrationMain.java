package net.canadensys.harvester.main;

import java.util.List;

import net.canadensys.harvester.config.CLIMigrationConfig;
import net.canadensys.harvester.migration.LontraMigrator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Main class for migration related actions.
 * 
 * @author cgendreau
 *
 */
public class MigrationMain {

	@Autowired
	private LontraMigrator lontraMigrator;

	public enum Mode {
		DRYRUN, CREATE, MIGRATE
	}

	/**
	 * Migration Entry point
	 * 
	 * @param args
	 */
	public static void main(Mode mode, String configFileLocation) {
		if (StringUtils.isNotBlank(configFileLocation)) {
			CLIMigrationConfig.setConfigFileLocation(configFileLocation);
		}

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(CLIMigrationConfig.class);
		MigrationMain mm = ctx.getBean(MigrationMain.class);

		switch (mode) {
			case DRYRUN:
				mm.displayChangeSets();
				break;
			case CREATE:
				mm.create();
				break;
			case MIGRATE:
				mm.migrate();
				break;
			default:
				mm.displayChangeSets();
				break;
		}

	}

	public void create() {
		lontraMigrator.create();
	}

	public void migrate() {
		lontraMigrator.migrate();
	}

	public void displayChangeSets() {
		List<String> changeSets = lontraMigrator.getChangeSetList();

		if (changeSets.isEmpty()) {
			System.out.println("Database up-to-date. No change sets to apply.");
		}
		for (String cs : changeSets) {
			System.out.println(cs);
		}
	}

}
