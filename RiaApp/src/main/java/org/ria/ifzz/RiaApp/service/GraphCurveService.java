package org.ria.ifzz.RiaApp.service;

import org.ria.ifzz.RiaApp.domain.*;
import org.ria.ifzz.RiaApp.exception.FileEntityNotFoundException;
import org.ria.ifzz.RiaApp.repository.GraphCurveLinesRepository;
import org.ria.ifzz.RiaApp.repository.GraphCurveRepository;
import org.ria.ifzz.RiaApp.utils.CountResultUtil;
import org.ria.ifzz.RiaApp.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static org.ria.ifzz.RiaApp.domain.HormonesPattern.CORTISOL_PATTERN;

@Service
public class GraphCurveService {

    private List<GraphCurveLines> graphCurveLinesList = new ArrayList<>();

    private final GraphCurveRepository graphCurveRepository;
    private final FileEntityService fileEntityService;
    private final CountResultUtil countResultUtil;
    private final FileUtils fileUtils;
    private final GraphCurveLinesRepository graphCurveLinesRepository;

    @Autowired
    public GraphCurveService(GraphCurveRepository graphCurveRepository, FileEntityService fileEntityService, CountResultUtil countResultUtil, FileUtils fileUtils, GraphCurveLinesRepository graphCurveLinesRepository) {
        this.graphCurveRepository = graphCurveRepository;
        this.fileEntityService = fileEntityService;
        this.countResultUtil = countResultUtil;
        this.fileUtils = fileUtils;
        this.graphCurveLinesRepository = graphCurveLinesRepository;
    }

    private Logger logger = LoggerFactory.getLogger(getClass());

    public List<GraphCurveLines> setCoordinates(GraphCurve graphCurve, Backlog backlog,List<String> fileData ) {
        List<Double> listX = countResultUtil.getLogDoseList();
        List<Double> listY = countResultUtil.getLogarithmRealZeroTable();
        List<Double> patternPoints = setStandardPattern(fileData);
        try {
            graphCurveLinesList = new ArrayList<>();
            for (int i = 0; i < listX.size(); i++) {
                double x = listX.get(i);
                double y = listY.get(i);
                double patternPoint = patternPoints.get(i);
                List<Double> meterReadingPg = countResultUtil.countMeterReading();
                GraphCurveLines graphCurveLines = new GraphCurveLines();
                graphCurveLines.setX(x);
                graphCurveLines.setY(y);
                graphCurveLines.setDataId(graphCurve.getDataId());
                graphCurveLines.setFileName(graphCurve.getFileName());
                graphCurveLines.setBacklog(backlog);
                graphCurveLines.setGraphCurve(graphCurve);
                graphCurveLines.setStandard(patternPoint);
                graphCurveLines.setMeterReading(meterReadingPg.get(i));
                graphCurveLinesList.add(graphCurveLines);
            }
        } catch (Exception e) {
            logger.error(GraphCurveService.class.getName() + ".setGraphCurve() msg:" + e.getMessage() + " and cause: " + e.getCause());
        }
        return graphCurveLinesList;
    }

    public GraphCurve setGraphCurve(DataFileMetadata file, FileEntity fileEntity, Backlog backlog) {
        GraphCurve graphCurve = new GraphCurve();
        try {
            String fileId = fileEntity.getDataId();
            graphCurve.setFileName(fileUtils.setFileName(file));
            graphCurve.setDataId(fileId);
            graphCurve.setBacklog(backlog);
            Double correlation = countResultUtil.setCorrelation();
            graphCurve.setCorrelation(correlation);
            Double binding = countResultUtil.setZeroBindingPercent();
            graphCurve.setZeroBindingPercent(binding);
            Double regressionParameterB = countResultUtil.getRegressionParameterB();
            graphCurve.setRegressionParameterB(regressionParameterB);
        } catch (Exception e) {
            logger.error(GraphCurveService.class.getName() + ".setGraphCurve() msg: " + e.getMessage() + " and cause: " + e.getCause());
        }
        return graphCurve;
    }

    public Iterable<GraphCurve> findBacklogByDataId(String dataId) throws FileNotFoundException {
        fileEntityService.findFileEntityByDataId(dataId);
        return graphCurveRepository.findByDataIdOrderByFileName(dataId);
    }

    public Optional<GraphCurveLines> findResultForCoordinatesByDataId(String dataId, String fileName, Long id) throws FileNotFoundException {
        fileEntityService.findFileEntityByDataId(dataId);

        Optional<GraphCurveLines> graphCurveLines = graphCurveLinesRepository.findById(id);
        if (!graphCurveLines.isPresent()) {
            throw new FileEntityNotFoundException("File with ID: '" + fileName + "' not found");
        }
        return graphCurveLines;
    }

    private List<Double> setStandardPattern(List<String> fileData) {
        List<Double> hormonePattern = new ArrayList<>();
        if (fileData.get(0).equals("KORTYZOL_5_MIN")) {
            logger.info("Pattern detected: " + fileData.get(0));
            hormonePattern = DoubleStream.of(CORTISOL_PATTERN).boxed().collect(
                    Collectors.toCollection(ArrayList::new));

        }
        return hormonePattern;
    }
}
