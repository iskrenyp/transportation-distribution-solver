

class AllocatedCell {

    Integer supply
    Integer demand
    Double value

    AllocatedCell() {
        this.supply = 0
        this.demand = 0
    }

    @Override
    String toString() {
        Formatter f = new Formatter();
        f.format("x[supply:%d,demand:%d]=%f", this.supply +1, this.demand +1, this.value);
        return f.toString();
    }


}
