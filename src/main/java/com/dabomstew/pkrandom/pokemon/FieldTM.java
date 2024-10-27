package com.dabomstew.pkrandom.pokemon;

public class FieldTM {
    
    private String description;
    private int tm;

    public FieldTM(String description, int tm) {
        super();
        this.description = description;
        this.tm = tm;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTm() {
        return tm;
    }

    public void setTm(int tm) {
        this.tm = tm;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + tm;
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FieldTM other = (FieldTM) obj;
        if (tm != other.tm)
            return false;
        return true;
    }

}

