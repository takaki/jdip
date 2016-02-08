//
//  @(#)XMLProvinceParser.java			7/2002
//
//  Copyright 2002 Zachary DelProposto. All rights reserved.
//  Use is subject to license terms.
//
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//  Or from http://www.gnu.org/
//
package dip.world.variant.parser;

import dip.misc.Log;
import dip.world.variant.data.BorderData;
import dip.world.variant.data.ProvinceData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Parses an XML ProvinceData description.
 */
public class XMLProvinceParser implements ProvinceParser {
    // Element constants
    public static final String EL_PROVINCES = "PROVINCES";
    public static final String EL_PROVINCE = "PROVINCE";
    public static final String EL_UNIQUENAME = "UNIQUENAME";
    public static final String EL_ADJACENCY = "ADJACENCY";
    public static final String EL_BORDER_DEFINITIONS = "BORDER_DEFINITIONS";
    public static final String EL_BORDER = "BORDER";

    // Attribute constants
    public static final String ATT_SHORTNAME = "shortname";
    public static final String ATT_FULLNAME = "fullname";
    public static final String ATT_NAME = "name";
    public static final String ATT_TYPE = "type";
    public static final String ATT_REFS = "refs";
    public static final String ATT_CONVOYABLE_COAST = "isConvoyableCoast";
    public static final String ATT_ID = "id";
    public static final String ATT_DESCRIPTION = "description";
    public static final String ATT_UNIT_TYPES = "unitTypes";
    public static final String ATT_FROM = "from";
    public static final String ATT_ORDER_TYPES = "orderTypes";
    public static final String ATT_BASE_MOVE_MODIFIER = "baseMoveModifier";
    public static final String ATT_BORDERS = "borders";
    public static final String ATT_YEAR = "year";
    public static final String ATT_SEASON = "season";
    public static final String ATT_PHASE = "phase";

    // instance variables
    private Document doc;
    private final DocumentBuilder docBuilder;
    private final List<ProvinceData> provinceList = new ArrayList<>(100);
    private final List<BorderData> borderList = new ArrayList<>(10);


    /**
     * Create an XMLProvinceParser
     */
    public XMLProvinceParser(
            final DocumentBuilderFactory dbf) throws ParserConfigurationException {
        docBuilder = dbf.newDocumentBuilder();
        docBuilder.setErrorHandler(new XMLErrorHandler());
        FastEntityResolver.attach(docBuilder);

    }// XMLProvinceParser()


    /**
     * Parse the given input stream; parsed data available via <code>getProvinceData()</code>
     */
    // TODO:remove
    public void parse(final InputStream is) throws IOException, SAXException {
        final long time = System.currentTimeMillis();
        provinceList.clear();
        borderList.clear();

        doc = docBuilder.parse(is);
        procProvinceData();
        Log.printTimed(time, "   province parse time: ");
    }// parse()


    /**
     * Cleanup, clearing any references/resources
     */
    // TODO:remove
    public void close() {
        provinceList.clear();
        borderList.clear();
    }// close()


    /**
     * Returns the ProvinceData objects, or an empty list.
     */
    @Override
    public ProvinceData[] getProvinceData() {
        return provinceList.toArray(new ProvinceData[provinceList.size()]);
    }// getProvinceData()

    /**
     * Returns the BorderData objects, or an empty list.
     */
    @Override
    public BorderData[] getBorderData() {
        return borderList.toArray(new BorderData[borderList.size()]);
    }// getBorderData()

    @XmlRootElement(name = "PROVINCES")
    public static class RootProvinces {
        @XmlElementWrapper(name = "BORDER_DEFINITIONS")
        @XmlElement(name = "BORDER")
        private List<BorderData> borderDatas;

        @XmlElement(name = "PROVINCE")
        private List<ProvinceData> provinces;
    }

    /**
     * Parse the XML
     */
    private void procProvinceData() {
        // find root element
        final Element root = doc.getDocumentElement();
//        final Unmarshaller unmarshaller = JAXBContext
//                .newInstance(RootProvinces.class).createUnmarshaller();
//        RootProvinces rootProvinces = (RootProvinces) unmarshaller
//                .unmarshal(root);
//
        final NodeList borderNodes = root.getElementsByTagName(EL_BORDER);
        borderList.addAll(IntStream.range(0, borderNodes.getLength())
                .mapToObj(i -> {
                    try {
                        final Unmarshaller function = JAXBContext
                                .newInstance(BorderData.class)
                                .createUnmarshaller();
                        return (BorderData) function
                                .unmarshal(borderNodes.item(i));
                    } catch (JAXBException e) {
                        throw new IllegalArgumentException(e);
                    }
                }).collect(Collectors.toList()));


        // find all PROVINCE elements
        final NodeList provinceNodes = root.getElementsByTagName("PROVINCE");
//        provinceList.addAll(IntStream.range(0, provinceNodes.getLength())
//                .mapToObj(i -> {
//                    try {
//                        final Unmarshaller function = JAXBContext
//                                .newInstance(ProvinceData.class)
//                                .createUnmarshaller();
//                        return (ProvinceData) function
//                                .unmarshal(provinceNodes.item(i));
//                    } catch (JAXBException e) {
//                        throw new IllegalArgumentException(e);
//                    }
//                }).collect(Collectors.toList()));
        provinceList.addAll(IntStream.range(0, provinceNodes.getLength())
                .mapToObj(i -> (Element) provinceNodes.item(i))
                .map(elProvince -> {
                    try {
                        final Unmarshaller function = JAXBContext
                                .newInstance(ProvinceData.class)
                                .createUnmarshaller();
                        final ProvinceData provinceData = (ProvinceData) function
                                .unmarshal(elProvince);

                        // create short/unique name list
//                        final List<String> shortNames = new LinkedList<>();
//                        shortNames.add(elProvince.getAttribute("shortname"));
                        // unique name(s) (if any)
//                        final NodeList elementsByTagName = elProvince
//                                .getElementsByTagName("UNIQUENAME");
//                        shortNames.addAll(IntStream
//                                .range(0, elementsByTagName.getLength())
//                                .mapToObj(i -> (Element) elementsByTagName
//                                        .item(i))
//                                .map(element -> element.getAttribute("name"))
//                                .collect(Collectors.toList()));
                        // set all short & unique names
//                        provinceData.setShortNames(shortNames);

                        // region attributes
//                        provinceData.setFullName(
//                                elProvince.getAttribute("fullname"));

                        // convoyable coast
//                        provinceData.setConvoyableCoast(Boolean.valueOf(
//                                elProvince.getAttribute("isConvoyableCoast")));

                        // borders data (optional); a list of references, seperated by commas/spaces
                        final String borders = elProvince
                                .getAttribute("borders").trim();
                        provinceData.setBorders(borders.isEmpty() ? Collections
                                .emptyList() : new ArrayList<>(
                                Arrays.asList(borders.split("[, ]+"))));


                        // adjacency data
                        final NodeList adjNodes = elProvince
                                .getElementsByTagName("ADJACENCY");
                        final List<String> adjTypeNames = IntStream
                                .range(0, adjNodes.getLength())
                                .mapToObj(j -> (Element) adjNodes.item(j))
                                .map(element -> element.getAttribute("type"))
                                .collect(Collectors.toList());
                        final List<String> adjProvinceNames = IntStream
                                .range(0, adjNodes.getLength())
                                .mapToObj(j -> (Element) adjNodes.item(j))
                                .map(element -> element.getAttribute("refs"))
                                .collect(Collectors.toList());
                        provinceData.setAdjacentProvinceTypes(adjTypeNames
                                .toArray(new String[adjTypeNames.size()]));
                        provinceData.setAdjacentProvinceNames(adjProvinceNames
                                .toArray(new String[adjProvinceNames.size()]));

                        return provinceData;
                    } catch (JAXBException e) {
                        throw new IllegalArgumentException(e);
                    }
                }).collect(Collectors.toList()));
    }// procProvinceData()

}// class XMLProvinceParser



