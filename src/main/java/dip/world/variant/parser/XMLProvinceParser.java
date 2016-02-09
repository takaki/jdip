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

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses an XML ProvinceData description.
 */
public class XMLProvinceParser implements ProvinceParser {

    private final List<ProvinceData> provinceList;
    private final List<BorderData> borderList ;

    @XmlRootElement(name = "PROVINCES")
    public static class RootProvinces {
        @XmlElementWrapper(name = "BORDER_DEFINITIONS")
        @XmlElement(name = "BORDER")
        private final List<BorderData> borderDatas = new ArrayList<>();
        @XmlElement(name = "PROVINCE")
        private final List<ProvinceData> provinces = new ArrayList<>();
    }

    /**
     * Create an XMLProvinceParser
     */
    public XMLProvinceParser(final InputStream is) {
        final long time = System.currentTimeMillis();

        final RootProvinces rootProvinces = JAXB
                .unmarshal(is, RootProvinces.class);
        borderList = rootProvinces.borderDatas;
        provinceList = rootProvinces.provinces;
        Log.printTimed(time, "   province parse time: ");
    }// XMLProvinceParser()

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


}// class XMLProvinceParser



