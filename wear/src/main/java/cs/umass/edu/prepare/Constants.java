package cs.umass.edu.prepare;

public class Constants {
    public interface ACTION {
        String RECORD_LABEL = "edu.umass.cs.mygestures.action.record";
        String START_SERVICE = "edu.umass.cs.mygestures.action.start-service";
        String STOP_SERVICE = "edu.umass.cs.mygestures.action.stop-service";
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 101;
    }
}
