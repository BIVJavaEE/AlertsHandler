import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws IOException {
        CommandLine cmd = getCommandLine(args);
        Optional<String> configFilePath = Optional.ofNullable(cmd.getOptionValue("config"));
        JsonNode configuration = new ConfigurationService(configFilePath.orElse("config/config.json")).read();
        JsonNode databaseConfig = configuration.get("database");

        AlertsDetector alertsDetector = new AlertsDetector(databaseConfig);
        AlertsDetectorRunner alertsDetectorRunner = new AlertsDetectorRunner(alertsDetector);
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(alertsDetectorRunner, 0, 10, TimeUnit.SECONDS);
    }


    private static CommandLine getCommandLine(String[] args) {
        try {
            return new DefaultParser().parse(createOptions(), args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return null;
        }
    }

    private static Options createOptions() {
        Options options = new Options();

        Option mqttOption = new Option("c", "config", true, "Path to the config config");
        mqttOption.setRequired(false);
        options.addOption(mqttOption);

        return options;
    }
}
