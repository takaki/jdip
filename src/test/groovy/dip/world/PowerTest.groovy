package dip.world

import spock.lang.Specification

class PowerTest extends Specification {
    def names0 = ["000", "111"]
    def power = new Power(names0, "adj", true)
    static def n0 = ["000", "111"]

    def "constructor check arguments"() {
        when:
        new Power(names, adjective, isActive)
        then:
        thrown(IllegalArgumentException)
        where:
        names | adjective | isActive
        n0    | ""        | true
        []    | "a"       | true
    }

    def "constructor null check arguments"() {
        when:
        new Power(names, adjective, isActive)
        then:
        thrown(NullPointerException)
        where:
        names | adjective | isActive
        null  | "adj"     | true
        n0    | null      | true
    }

    def "test methods"() {
        expect:
        power.getName() == "000"
        power.getAdjective() == "adj"
        power.getNames() == ["000", "111"]
        power.isActive()
        power.hashCode() == "000".hashCode()
        power.toString() == "000"
    }

    def "test compareTo"() {
        def power1 = new Power(["001", "111"], "adj", true)
        def power2 = new Power(["000", "0111"], "adj", true)
        expect:
        power.compareTo(power1) < 0
        power1.compareTo(power) > 0
        power.compareTo(power) == 0
    }
}
