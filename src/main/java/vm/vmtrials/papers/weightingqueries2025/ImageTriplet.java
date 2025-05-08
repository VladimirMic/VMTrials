/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.papers.weightingqueries2025;

/**
 *
 * @author au734419
 */
public class ImageTriplet implements Comparable<ImageTriplet> {

    private final int tripletId;
    private final String queryImageId;
    private final String leftImageId;
    private final String rightImageId;

    public ImageTriplet(int tripletId, String queryImageId, String leftImageId, String rightImageId) {
        this.tripletId = tripletId;
        this.queryImageId = queryImageId;
        this.leftImageId = leftImageId;
        this.rightImageId = rightImageId;
    }

    public int getTripletId() {
        return tripletId;
    }

    public String getQueryImageId() {
        return queryImageId;
    }

    public String getLeftImageId() {
        return leftImageId;
    }

    public String getRightImageId() {
        return rightImageId;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + this.tripletId;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ImageTriplet other = (ImageTriplet) obj;
        return this.tripletId == other.tripletId;
    }

    @Override
    public int compareTo(ImageTriplet o) {
        return Integer.compare(tripletId, o.tripletId);
    }

    @Override
    public String toString() {
        return Integer.toString(tripletId);
    }

}
