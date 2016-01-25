package dip.world

import spock.lang.Specification

class PowerTest extends Specification {
    def names = ["000", "111"] as String[]
    def power = new Power(names, "adj", true)

    def "test methods"() {
        expect:
        power.getName() == "000"
        power.getAdjective() == "adj"
        Arrays.equals(power.getNames(), ["000", "111"] as String[])
        power.isActive()
        power.hashCode() == "000".hashCode()
        power.toString() == "000"
    }

    def "test compareTo"() {
        def power1 = new Power(["001", "111"] as String[], "adj", true)
        def power2 = new Power(["000", "0111"] as String[], "adj", true)
        expect:
        power.compareTo(power1) < 0
        power1.compareTo(power) > 0
        power.compareTo(power) == 0
    }
}
