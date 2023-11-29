/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package fish.focus.uvms.incident.mock;

import fish.focus.uvms.spatial.model.schemas.AreaByLocationSpatialRQ;
import fish.focus.uvms.spatial.model.schemas.AreaExtendedIdentifierType;
import fish.focus.uvms.spatial.model.schemas.AreaType;

import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("spatial/spatialnonsecure/json")
@Stateless
public class SpatialModuleMock {


    @POST
    @Path("getAreaByLocation")
    @Consumes(value = {MediaType.APPLICATION_JSON})
    @Produces(value = {MediaType.APPLICATION_JSON})
    public Response getSegmentCategoryType(AreaByLocationSpatialRQ areaByLocationSpatialRQ) {
        List<AreaExtendedIdentifierType> response = new ArrayList<>();
        String spatialAreaType = System.getProperty("SPATIAL_AREA_TYPE", AreaType.EEZ.value());
        AreaExtendedIdentifierType area = new AreaExtendedIdentifierType("5555", AreaType.fromValue(spatialAreaType), "Code", "Area of type " + spatialAreaType);
        response.add(area);
        return Response.ok(response).build();
    }
    
}
