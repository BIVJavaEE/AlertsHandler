public class AlertsDetectorRunner implements Runnable {

    private AlertsDetector _alertsDetector;

    public AlertsDetectorRunner(AlertsDetector alertsDetector) {
        _alertsDetector = alertsDetector;
    }

    @Override
    public void run() {
        _alertsDetector.detectAlerts();
    }
}
