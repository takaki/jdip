// Copyright (C) 2016 TANIGUCHI Takaki
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//

package dip.world.variant.data;

import java.util.Objects;

public final class VersionNumber implements Comparable<VersionNumber> {
    int major;
    int minor;

    public VersionNumber(final int major, final int minor) {
        this.major = major;
        this.minor = minor;
    }

    public void set(final String version) {
        final String[] tokens = version.split("\\.");
        switch (tokens.length) {
            case 1:
                major = Integer.valueOf(tokens[0]);
                minor = 0;
                break;
            case 2:
                major = Integer.valueOf(tokens[0]);
                minor = Integer.valueOf(tokens[1]);
                break;
            default:
                throw new IllegalArgumentException(
                        "Not version number: " + version);
        }
    }

    @Override
    public int compareTo(final VersionNumber o) {
        return major == o.major ? Integer.compare(minor, o.minor) : Integer
                .compare(major, o.major);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof VersionNumber) {
            final VersionNumber v = (VersionNumber) o;
            return major == v.major && minor == v.minor;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor);
    }

    @Override
    public String toString() {
        return String
                .join(".", Integer.toString(major), Integer.toString(minor));
    }
}
