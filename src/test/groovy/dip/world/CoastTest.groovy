package dip.world

import dip.order.OrderException
import spock.lang.Specification

class CoastTest extends Specification {
    def coast = Coast.EAST

    def "test instance"() {
        expect:
        coast.getName() == "East Coast"
        coast.getAbbreviation() == "ec"
        coast.getIndex() == 7
        coast.toString() == "East Coast"
        coast.hashCode() == "East Coast".hashCode()

    }

    def "test isDirectional"() {
        Coast.SOUTH.isDirectional()
        !Coast.NONE.isDirectional()
    }

    def "test static"() {
        expect:
        Coast.getCoast(7) == Coast.EAST
        Coast.getProvinceName("spa-sc") == "spa"

    }

    def "test normalize"() {
        expect:
        Coast.normalize(arg) == result
        where:
        arg           || result
        "stp-sc"      || "stp/sc"
        "stp(sc)"     || "stp/sc"
        "stp( sc)"    || "stp/sc"
        "stp(.s.c.)"  || "stp/sc"
        "stp (sc)"    || "stp/sc"
        "stp    (sc)" || "stp/sc"
    }

    def "empty parentheses throw exception"() {
        when:
        Coast.normalize("stp()")
        then:
        thrown(OrderException)
    }

//    def "normalize illegal name throw exception"() {
//        when:
//        Coast.normalize("stp((sc)s)")
//        then:
//        thrown(OrderException)
//    }
//
//    def "normalize illegal name throw exception 2"() {
//        when:
//        Coast.normalize("stp/sc/sc")
//        then:
//        thrown(OrderException)
//    }

    def "test isDisplayable"() {
        expect:
        Coast.isDisplayable(Coast.EAST)
        Coast.isDisplayable(Coast.WEST)
        !Coast.isDisplayable(Coast.SINGLE)
        !Coast.isDisplayable(Coast.NONE)
    }

    def "test parse"() {
        expect:
        Coast.parse(str) == cst
        where:
        str       || cst
        "spa-sc"  || Coast.SOUTH
        "spa-nc"  || Coast.NORTH
        "hoge-wc" || Coast.WEST
        "hoge-ec" || Coast.EAST
        "hoge-xc" || Coast.SINGLE
        "hoge-mv" || Coast.LAND
        "hoge-xx" || Coast.UNDEFINED
        "nc"      || Coast.NORTH
    }

    def "test equals"() {
        def coast0 = Coast.WEST
        expect:
        !coast.equals(coast0)
        Coast.WEST.equals(Coast.WEST)
        !coast0.equals(null)
    }

}

