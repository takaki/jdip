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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses an XML ProvinceData description.
 */
public class XMLProvinceParser implements ProvinceParser {
    private static final Logger LOG = LoggerFactory.getLogger(
            XMLProvinceParser.class);

    private final List<ProvinceData> provinceList;
    private final List<BorderData> borderList;

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
    public XMLProvinceParser(final URL provinceXMLURL) {
        final long time = System.currentTimeMillis();
        try (InputStream is = new BufferedInputStream(
                provinceXMLURL.openStream())) {
            final RootProvinces rootProvinces = JAXB
                    .unmarshal(is, RootProvinces.class);
            borderList = rootProvinces.borderDatas;
            provinceList = rootProvinces.provinces;
            LOG.debug(Log.printTimed(time, "   province parse time: "));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

    }// XMLProvinceParser()

    /**
     * Returns the ProvinceData objects, or an empty list.
     */
    @Override
    public List<ProvinceData> getProvinceData() {
        return Collections.unmodifiableList(provinceList);
    }// getProvinceData()

    /**
     * Returns the BorderData objects, or an empty list.
     */
    @Override
    public List<BorderData> getBorderData() {
        return Collections.unmodifiableList(borderList);
    }// getBorderData()


}// class XMLProvinceParser



