package animationModule;

import java.util.HashMap;
import java.util.Map;

public class ViewerEvent {
        private double t;
        private String id;
        Map<String, Object> params;

        public ViewerEvent(String id, double t) {
            this.t = t;
            this.id = id;
            this.params = new HashMap<>(); 
        }

        public ViewerEvent(String id) {
            this(id, 0);
        }

        public double getT() {
            return t;
        }

        public void setT(double t) {
            this.t = t;
        }

        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }

    }