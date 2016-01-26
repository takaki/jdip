package dip.world

import spock.lang.Specification

class UnitTest extends Specification {
    def power = new Power(["aaa", "000"] as String[], "adj", true)
    def unit = new Unit(power, Unit.Type.ARMY)

    def "test setCoast"() {
        def coast = Coast.EAST
        unit.setCoast(coast)
        expect:
        unit.getCoast() == coast
        unit.getPower() == power
        unit.getType() == Unit.Type.ARMY
    }

    def "test clone"() {
        def clone = unit.clone()
        expect:
        unit.equals(clone)
    }

    def "test toString"() {
        expect:
        unit.toString() == "Unit:[type=A,power=aaa,coast=Undefined]"
    }

    def "undefined throws exception" (){
        when:
        new Unit(power, Unit.Type.parse(null))
        then:
        thrown(IllegalArgumentException)
    }

    def "Unit.Type" (){
        expect:
        Unit.Type.parse("f") == Unit.Type.FLEET
        Unit.Type.parse("a") == Unit.Type.ARMY
        Unit.Type.parse("army") == Unit.Type.ARMY
        Unit.Type.parse("army1") == null
        Unit.Type.parse("w") == Unit.Type.WING
        Unit.Type.parse("A") == Unit.Type.ARMY
        Unit.Type.parse("F") == Unit.Type.FLEET
        Unit.Type.parse("W") == Unit.Type.WING
    }


}
