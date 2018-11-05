import com.fasterxml.jackson.databind.JsonNode;
import entity.Alert;
import entity.AlertTriggered;
import entity.Measure;

import javax.persistence.*;
import java.io.*;
import java.sql.Timestamp;
import java.util.*;

public class AlertsDetector {

    private static final String DatePersistenceFilePath = "lastmeasuredate.txt";

    private EntityManager _em;
    private Date _lastDetection;

    public AlertsDetector(JsonNode databaseConfig) {
        readLatestMeasureDate();
        createEntityManager(databaseConfig);
    }

    public void detectAlerts() {
        Set<Alert> triggeredAlerts = getTriggeredAlerts();

        _em.getTransaction().begin();

        for (Alert alert : triggeredAlerts) {
            AlertTriggered alertTriggered = new AlertTriggered();
            alertTriggered.setAlert(alert);
            alertTriggered.setTriggerDate(new Timestamp(System.currentTimeMillis()));
            alertTriggered.setSeen(false);
            _em.persist(alertTriggered);
        }

        _em.getTransaction().commit();
    }

    private void writeLatestMeasureDate() {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(DatePersistenceFilePath))) {
            dos.writeLong(_lastDetection.getTime());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readLatestMeasureDate() {
        if (!new File(DatePersistenceFilePath).canRead()) {
            _lastDetection = null;
        }
        try (DataInputStream dis = new DataInputStream(new FileInputStream(DatePersistenceFilePath))) {
            _lastDetection = new Date(dis.readLong());
        } catch (IOException ignored) {
            _lastDetection = null;
        }
    }

    private void createEntityManager(JsonNode databaseConfig) {
        Map<String, String> properties = new HashMap<>();
        properties.put("javax.persistence.jdbc.url", databaseConfig.get("url").asText());
        properties.put("javax.persistence.jdbc.user", databaseConfig.get("user").asText());
        properties.put("javax.persistence.jdbc.password", databaseConfig.get("password").asText());
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(databaseConfig.get("name").asText(), properties);
        _em = factory.createEntityManager();
    }

    private List<Measure> getNewlyUpdatedMeasures() {
        List<Measure> result = _em
                .createQuery("SELECT m FROM Measure m WHERE :timestamp IS NULL OR m.timestamp > :timestamp", Measure.class)
                .setParameter("timestamp", _lastDetection, TemporalType.TIMESTAMP)
                .getResultList();
        _lastDetection = new Date();
        writeLatestMeasureDate();
        return result;
    }

    private List<Alert> getAllAlerts() {
        return _em
                .createQuery("SELECT a FROM Alert a WHERE :timestamp BETWEEN a.beginDate AND a.endDate", Alert.class)
                .setParameter("timestamp", new Timestamp(System.currentTimeMillis()), TemporalType.TIMESTAMP)
                .getResultList();
    }

    private boolean testAlert(Alert alert, Measure measure) {
        double actualValue = measure.getValue();
        double maxValue = alert.getTreshold();
        return actualValue > maxValue;
    }

    private Set<Alert> getTriggeredAlerts() {
        System.out.println("Getting alerts...");
        List<Alert> alerts = getAllAlerts();
        System.out.println("Found " + alerts.size() + " alerts");

        System.out.println("Getting new measures...");
        List<Measure> measures = getNewlyUpdatedMeasures();
        System.out.println("Found " + measures.size() + " new measures");

        System.out.println("Testing alerts..");
        Set<Alert> triggeredAlerts = new HashSet<>();
        for (Alert alert : alerts) {
            for (Measure measure : measures) {
                if (alert.getSensor() != measure.getSensor()) continue;
                boolean triggered = testAlert(alert, measure); // **TRIGGERED**
                if (triggered) {
                    triggeredAlerts.add(alert);
                }
            }
        }
        System.out.println("Triggered " + triggeredAlerts.size() + " alerts\n\n");

        return triggeredAlerts;
    }

}
