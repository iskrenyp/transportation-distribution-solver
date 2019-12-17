

class TransportationDistributionSolver {

    double [] consumers
    double [] suppliers
    double [][] cost
    List<AllocatedCell> feasibleAllocation = []

    int suppliersNumber
    int consumersNumber

    boolean [][] isAllocated

    /**
     * Loads problem inputs from a text file. Please use the following order:
     * First line - Number of suppliers
     * Second line - Number of consumers (or warehouses)
     * Third line - Supply capacity for each of the suppliers (comma separated)
     * Fourth line - Demand capacity for each of the consumers (comma separated)
     * Fifth line - Transportation costs (comma separated)
     * Whitespaces will be trimmed, so no worry about that
     * @param textFile
     * @return
     */
    static TransportationDistributionSolver loadFromTextFile(File textFile) {
        assert textFile && textFile.exists() : {
            println("You need real input data file to solve the distribution! Exiting program execution")
            System.exit(1)
        }
        println("Input file (${textFile.name}) content:")
        println(textFile.text)
        List<String> lines = textFile.readLines()  // get the file content as List of Strings
        // assign all input data elements to the variables
        Integer numOfSuppliers = lines.first().toInteger()
        Integer numOfWarehouses = lines[1].toInteger()
        // Instantiate the solver object
        TransportationDistributionSolver inputData = new TransportationDistributionSolver(numOfSuppliers,numOfWarehouses)
        List<String> suppliersCapacity = lines[2].trim().tokenize(',')
        assert suppliersCapacity.size() == numOfSuppliers : println("The number of supply capacity values != number of suppliers!")
        List<String> consumersCapacity = lines[3].trim().tokenize(',')
        assert consumersCapacity.size() == numOfWarehouses : println("The number of demand capacity values (warehouses) != number of consumers")
        List<String> transportationCosts = lines[4].trim().tokenize(',')
        assert transportationCosts.size() == numOfSuppliers*numOfWarehouses : println("The number of transportation cost values != number of suppliers*number of consumers")
        // set the supply/demand capacities per each supplier/consumer
        numOfWarehouses.times { inputData.setConsumerCapacity(consumersCapacity[it].toDouble(), it) }
        numOfSuppliers.times { inputData.setSupplierCapacity(suppliersCapacity[it].toDouble(), it) }
        // set the costs
        Integer costIndex = 0
        numOfSuppliers.times { supplier ->
            numOfWarehouses.times { consumer ->
                inputData.setCost(transportationCosts[costIndex].toDouble(), supplier, consumer)
                costIndex ++
            }
        }
        inputData
    }

    TransportationDistributionSolver(Integer numberOfSuppliers, Integer numberOfWarehouses ){
        this.suppliersNumber = numberOfSuppliers
        this.consumersNumber = numberOfWarehouses

        suppliers = new double[numberOfSuppliers]
        consumers = new double[numberOfWarehouses]
        cost = new double[numberOfSuppliers][numberOfWarehouses]

        (numberOfWarehouses + numberOfSuppliers -1).times { feasibleAllocation.add(new AllocatedCell()) }

        //isAllocated is responsible for marking cells that have been allocated
        isAllocated = new boolean[suppliersNumber][consumersNumber]
    }

    def cleanAllocationSolution() {
        suppliersNumber.times { supplier ->
            consumersNumber.times { consumer -> isAllocated[supplier][consumer] = false }
        }
    }

    def setSupplierCapacity(double value, int index){
        suppliers[index] = value
    }

    def setConsumerCapacity(double value, int index){
        consumers[index] = value
    }

    def setCost(double value, int supply, int demand){
        cost[supply][demand] = value
    }

    /**
     * Uses the North-West Corner algorithm to solve the distribution
     * @return
     */
    def solveWithNorthWestCorner() {
        double min
        //feasible solutions counter
        int counter = 0
        cleanAllocationSolution()
        consumersNumber.times { consumer ->
            suppliersNumber.times { supplier ->
                if (!isAllocated[supplier][consumer]) {
                    // allocating stock
                    min = Math.min(consumers[consumer], suppliers[supplier])
                    feasibleAllocation[counter].demand = consumer
                    feasibleAllocation[counter].supply = supplier
                    feasibleAllocation[counter].value = min
                    counter++
                    suppliers[supplier] -= min
                    consumers[consumer] -= min
                    // setting up allocated values
                    if (suppliers[supplier] == 0) consumersNumber.times { isAllocated[supplier][it] = true }
                    else suppliersNumber.times { isAllocated[it][consumer] = true }
                }
            }
        }
    }

    /**
     * Uses the Least Cost algorithm to solve the distribution
     * @return
     */
    def solveWithLeastCostRule() {
        cleanAllocationSolution()
        AllocatedCell minCost = new AllocatedCell()
        //this iteration sets cell values according to the lest cost
        (suppliersNumber + consumersNumber -1).times { counter ->
            minCost.value = Double.MAX_VALUE
            suppliersNumber.times { supplier ->
                consumersNumber.times { consumer ->
                    if (!isAllocated[supplier][consumer] && cost[supplier][consumer] < minCost.value) {
                        minCost.supply = supplier
                        minCost.demand = consumer
                        minCost.value = cost[supplier][consumer]
                    }
                }
            }
            int minCostSupply = minCost.supply
            int minCostDemand = minCost.demand
            //allocating supply
            Double min = Math.min(consumers[minCostDemand], suppliers[minCostSupply])
            feasibleAllocation[counter].demand = minCostDemand
            feasibleAllocation[counter].supply = minCostSupply
            feasibleAllocation[counter].value = min

            consumers[minCostDemand] -= min
            suppliers[minCostSupply] -= min

            //allocating null values in the removed row/column
            if(suppliers[minCostSupply] == 0) consumersNumber.times { consumer -> isAllocated[minCostSupply][consumer] = true }
            else suppliersNumber.times { supplier -> isAllocated[supplier][minCostDemand] = true }
        }
    }

    /**
     * Calculates the Zmin target function representing the total costs
     * associated with the used solution algorithm
     * @return
     */
    Double getTotalCosts(){
        double totalCosts = 0
        feasibleAllocation.each { totalCosts += it.value * cost[it.supply][it.demand] }
        totalCosts
    }

    /**
     * Writes the feasible solution to a text file
     * The file has to be there before writing into it
     * @param outputTextFile
     * @return
     */
    def writeSolutionToFile(File outputTextFile) {
        assert outputTextFile && outputTextFile.exists() : {
            println("You need real output data file to write the solution! Exiting program execution")
            System.exit(1)
        }
        outputTextFile.text = ''
        feasibleAllocation.each { outputTextFile << "$it\n" }
        outputTextFile << "Total costs (Z min function) : ${getTotalCosts()}"
        println(outputTextFile.text)
    }

    static void main(String[] args) throws IOException {
        TransportationDistributionSolver solver = loadFromTextFile(new File('input_data.txt'))
        solver.solveWithLeastCostRule()
        solver.writeSolutionToFile(new File('least_cost_output.txt'))
        solver.solveWithNorthWestCorner()
        solver.writeSolutionToFile(new File('north_west_corner_output.txt'))
    }
}
