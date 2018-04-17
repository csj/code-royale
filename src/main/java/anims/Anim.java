package anims;

import java.util.HashMap;
import java.util.Map;

public class Anim {
    private double t;
    private String id;
    private Map<String, Object> params;

    public Anim(String id, double t) {
        this.t = t;
        this.id = id;
        this.params = new HashMap<>(); 
    }

    public Anim(String id) {
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

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
