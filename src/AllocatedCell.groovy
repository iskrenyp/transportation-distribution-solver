/**
 * Holds the allocated feasible solution
 */
class AllocatedCell {

    Integer supply
    Integer demand
    Double value

    AllocatedCell() {
        this.supply = 0
        this.demand = 0
    }

    @Override
    String toString() { "cell[supply:${supply+1}, demand:${demand+1}] = $value" }
}
