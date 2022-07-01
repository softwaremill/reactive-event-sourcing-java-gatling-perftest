package perftest;

public class CapacityLoadTesting {
    public boolean enabled;
    public int from;
    public int times;
    public int step;
    public int levelLastingSec;

    @Override
    public String toString() {
        return "CapacityLoadTesting{" +
                "enabled=" + enabled +
                ", from=" + from +
                ", times=" + times +
                ", step=" + step +
                ", levelLastingSec=" + levelLastingSec +
                '}';
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public int getLevelLastingSec() {
        return levelLastingSec;
    }

    public void setLevelLastingSec(int levelLastingSec) {
        this.levelLastingSec = levelLastingSec;
    }
}