package org.ria.ifzz.RiaApp.services.strategies;

import lombok.Setter;
import lombok.ToString;
import org.ria.ifzz.RiaApp.exception.ControlCurveException;
import org.ria.ifzz.RiaApp.exception.GraphException;
import org.ria.ifzz.RiaApp.models.results.ControlCurve;
import org.ria.ifzz.RiaApp.services.examination.ControlCurveService;
import org.ria.ifzz.RiaApp.services.examination.ExaminationPointService;
import org.ria.ifzz.RiaApp.services.examination.GraphService;
import org.ria.ifzz.RiaApp.utils.counter.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@ToString
@Service
public class ExaminationResultSolution extends ExaminationResultStrategyImpl {

    @Setter
    private List<String> metadata;
    Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final Counter counter;
    private final ControlCurveService controlCurveService;
    private final GraphService graphService;
    private final ExaminationPointService examinationPointService;
    private List<ControlCurve> controlCurvePoints;

    public ExaminationResultSolution(Counter counter,
                                     ControlCurveService controlCurveService,
                                     GraphService graphService,
                                     ExaminationPointService examinationPointService) {
        this.counter = counter;
        this.controlCurveService = controlCurveService;
        this.graphService = graphService;
        this.examinationPointService = examinationPointService;
    }


    @Override
    void start() {
        if (metadata.isEmpty()) {
            LOGGER.warn("Reading file:");
        } else {
            stop();
        }
    }

    @Override
    boolean isControlCurve() throws ControlCurveException {
        if (metadata.size() >= 1) {
            controlCurvePoints = new ArrayList<>();
            controlCurvePoints.addAll(controlCurveService.create(metadata.subList(0,26)));
        }
        return false;
    }

    @Override
    boolean isResultPoint() throws ControlCurveException {
        if (metadata.size() > 25) {
            examinationPointService.create(metadata, controlCurvePoints);
        }
        return false;
    }

    @Override
    boolean isGraphPoint() throws GraphException {
        if (!controlCurvePoints.isEmpty()) {
            graphService.create(metadata);
        }
        return false;
    }

    @Override
    boolean stop() {
        LOGGER.warn("Stop");
        return false;
    }
}
