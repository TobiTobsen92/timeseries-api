/**
 * Copyright (C) 2013-2014 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package org.n52.series.api.v1.db.srv;

import java.util.ArrayList;
import java.util.List;

import org.n52.io.IoParameters;
import org.n52.io.v1.data.OfferingOutput;
import org.n52.io.v1.data.ParameterOutput;
import org.n52.io.v1.data.ProcedureOutput;
import org.n52.series.api.v1.db.da.DataAccessException;
import org.n52.series.api.v1.db.da.DbQuery;
import org.n52.series.api.v1.db.da.ProcedureRepository;
import org.n52.web.InternalServerException;
import org.n52.sensorweb.v1.spi.ParameterService;

public class OfferingsAccessService extends ServiceInfoAccess implements ParameterService<OfferingOutput> {

    @Override
    public OfferingOutput[] getExpandedParameters(IoParameters query) {
        try {
            DbQuery dbQuery = DbQuery.createFrom(query);
            ProcedureRepository repository = createProcedureRepository();
            List<? extends ParameterOutput> results = repository.getAllExpanded(dbQuery);
            return results.toArray(new OfferingOutput[0]);
        } catch (DataAccessException e) {
            throw new InternalServerException("Could not get offering data.");
        }
    }

    @Override
    public OfferingOutput[] getCondensedParameters(IoParameters query) {
        try {
            DbQuery dbQuery = DbQuery.createFrom(query);
            ProcedureRepository repository = createProcedureRepository();
            List<OfferingOutput> results = new ArrayList<OfferingOutput>();
            List<ProcedureOutput> procedures = repository.getAllCondensed(dbQuery);
            for (ProcedureOutput procedureOutput : procedures) {
                results.add(createOfferingFrom(procedureOutput));
            }
            return results.toArray(new OfferingOutput[0]);
        } catch (DataAccessException e) {
            throw new InternalServerException("Could not get offering data.");
        }
    }

    @Override
    public OfferingOutput[] getParameters(String[] offeringIds) {
        return getParameters(offeringIds, IoParameters.createDefaults());
    }

    @Override
    public OfferingOutput[] getParameters(String[] offeringIds, IoParameters query) {
        try {
            DbQuery dbQuery = DbQuery.createFrom(query);
            ProcedureRepository repository = createProcedureRepository();
            List<OfferingOutput> results = new ArrayList<OfferingOutput>();
            for (String offeringId : offeringIds) {
                ProcedureOutput procedure = repository.getInstance(offeringId, dbQuery);
                results.add(createOfferingFrom(procedure));
            }
            return results.toArray(new OfferingOutput[0]);
        } catch (DataAccessException e) {
            throw new InternalServerException("Could not get offering data.");
        }
    }

    @Override
    public OfferingOutput getParameter(String offeringId) {
        return getParameter(offeringId, IoParameters.createDefaults());
    }

    @Override
    public OfferingOutput getParameter(String offeringId, IoParameters query) {
        try {
            DbQuery dbQuery = DbQuery.createFrom(query);
            ProcedureRepository repository = createProcedureRepository();
            return createOfferingFrom(repository.getInstance(offeringId, dbQuery));
        } catch (DataAccessException e) {
            throw new InternalServerException("Could not get offering data");
        }
    }

    private OfferingOutput createOfferingFrom(ProcedureOutput procedure) {
        OfferingOutput offering = new OfferingOutput();
        offering.setId(procedure.getId());
        offering.setLabel(procedure.getLabel());
        offering.setService(procedure.getService());
        return offering;
    }

    private ProcedureRepository createProcedureRepository() {
        // offerings equals procedures in our case
        return new ProcedureRepository(getServiceInfo());
    }


}
