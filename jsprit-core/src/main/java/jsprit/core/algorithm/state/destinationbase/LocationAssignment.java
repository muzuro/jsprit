package jsprit.core.algorithm.state.destinationbase;

import jsprit.core.problem.Location;

public class LocationAssignment {
    private final Location location;
    private int count = 0;
    public LocationAssignment(Location aLocation) {
        location = aLocation;
    }
    public LocationAssignment(Location aLocation, int aCount) {
        location = aLocation;
        count = aCount;
    }
    public Location getLocation() {
        return location;
    }
    public int getCount() {
        return count;
    }
    public void incrementCount() {
        count++;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((location == null) ? 0 : location.hashCode());
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
        LocationAssignment other = (LocationAssignment) obj;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        return true;
    }
}