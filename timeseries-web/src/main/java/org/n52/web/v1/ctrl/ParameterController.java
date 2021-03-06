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
package org.n52.web.v1.ctrl;

import static org.n52.io.QueryParameters.createFromQuery;
import static org.n52.web.v1.ctrl.Stopwatch.startStopwatch;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import org.n52.io.IoParameters;
import org.n52.io.v1.data.ParameterOutput;
import org.n52.web.BaseController;
import org.n52.web.ResourceNotFoundException;
import org.n52.sensorweb.v1.spi.LocaleAwareSortService;
import org.n52.sensorweb.v1.spi.ParameterService;
import org.n52.sensorweb.v1.spi.ServiceParameterService;
import org.n52.web.WebExceptionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@RequestMapping(produces = {"application/json"})
public abstract class ParameterController extends BaseController implements RestfulUrls {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterController.class);

    private ServiceParameterService serviceParameterService;

    private ParameterService<ParameterOutput> parameterService;

    @RequestMapping(method = GET)
    public ModelAndView getCollection(@RequestParam(required=false) MultiValueMap<String, String> query) {
        IoParameters queryMap = createFromQuery(query);

        if (queryMap.isExpanded()) {
            Stopwatch stopwatch = startStopwatch();
            ParameterOutput[] result = doPostProcessOn(parameterService.getExpandedParameters(queryMap));
            LOGGER.debug("Processing request took {} seconds.", stopwatch.stopInSeconds());

            // TODO add paging

            return new ModelAndView().addObject(result);
        }
        else {
            ParameterOutput[] results = parameterService.getCondensedParameters(queryMap);

            // TODO add paging

            return new ModelAndView().addObject(results);
        }
    }

    @RequestMapping(value = "/{item}", method = GET)
    public ModelAndView getItem(@PathVariable("item") String id,
                                @RequestParam(required = false) MultiValueMap<String, String> query) {
        IoParameters queryMap = createFromQuery(query);
        ParameterOutput parameter = doPostProcessOn(parameterService.getParameter(id, queryMap));

        if (parameter == null) {
            throw new ResourceNotFoundException("Found no parameter for id '" + id + "'.");
        }

        return new ModelAndView().addObject(parameter);
    }

    protected ParameterOutput[] doPostProcessOn(ParameterOutput[] toBeProcessed) {
        return toBeProcessed; // return unprocessed
    }

    protected ParameterOutput doPostProcessOn(ParameterOutput toBeProcessed) {
        return toBeProcessed; // return unprocessed
    }

    public ServiceParameterService getServiceParameterService() {
        return serviceParameterService;
    }

    public void setServiceParameterService(ServiceParameterService serviceParameterService) {
        this.serviceParameterService = serviceParameterService;
    }

    public ParameterService<ParameterOutput> getParameterService() {
        return parameterService;
    }

    public void setParameterService(ParameterService<ParameterOutput> parameterService) {
        ParameterService<ParameterOutput> service = new WebExceptionAdapter<ParameterOutput>(parameterService);
        this.parameterService = new LocaleAwareSortService<ParameterOutput>(service);
    }

}
