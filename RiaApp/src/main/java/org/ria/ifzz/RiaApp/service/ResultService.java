package org.ria.ifzz.RiaApp.service;

import org.ria.ifzz.RiaApp.domain.*;
import org.ria.ifzz.RiaApp.repository.ControlCurveRepository;
import org.ria.ifzz.RiaApp.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.ria.ifzz.RiaApp.domain.DomainConstants.RESULT_POINTER;
import static org.ria.ifzz.RiaApp.domain.HormonesPattern.CORTISOL_PATTERN;
import static org.ria.ifzz.RiaApp.utils.CustomFileReader.readFromStream;
import static org.ria.ifzz.RiaApp.utils.FileUtils.setFileName;

@RestController
public class ResultService implements FileUtils, CustomFileReader {

    private final CountResultUtil countResultUtil;
    private final ControlCurveRepository controlCurveRepository;
    private final ControlCurveService controlCurveService;
    private final DataAssigner dataAssigner;

    public ResultService(CountResultUtil countResultUtil, ControlCurveRepository controlCurveRepository, ControlCurveService controlCurveService, DataAssigner dataAssigner) {
        this.countResultUtil = countResultUtil;
        this.controlCurveRepository = controlCurveRepository;
        this.controlCurveService = controlCurveService;
        this.dataAssigner = dataAssigner;
    }

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * takes file store in local disc space
     *
     * @param data data from uploaded file
     * @return expected List of Strings
     */
    public List<String> getFileData(DataFileMetadata data) throws IOException {
        return readFromStream(data);
    }

    /**
     * takes fileName of upload file and set specific id for each entities
     * reads given Strings List and create Result entity for each lines of given,
     *
     * @param data pre-cleaned list
     * @return Result entities
     */
    private List<Result> setResultFromColumnsLength(List<String> data,Backlog backlog) {
        Result result;
        List<Result> results = new ArrayList<>();
        for (String dataLine : data) {
            if (dataLine.startsWith(RESULT_POINTER)) {
                result = new Result();
                result.setFileName(data.indexOf(dataLine) + "_" + backlog.getDataId());
                result.setBacklog(backlog);
                results.add(result);
            }
        }
        return results;
    }

    /**
     * find Result entity in database by {@code}fileName_index,
     * which is created by file's fileName + _ + index, and
     * then assign results from "CCMP" table
     *
     * @param fileData generated by upload file
     */
    private List<Result> assignDataToResult(List<String> fileData, FileEntity fileEntity, List<Result> results) {

        List<Result> resultsWithData;
        List<Result> assignedResults = new ArrayList<>();

        resultsWithData = dataAssigner.setCpm(fileData, results);
        for (int i = 0; i < resultsWithData.size(); i++) {
            Result result = resultsWithData.get(i);
            assignedResults.add(result);
        }
        resultsWithData = dataAssigner.setSamples(fileData, fileEntity.getDataId(), results);
        for (int i = 0; i < resultsWithData.size(); i++) {
            Result result = resultsWithData.get(i);
            assignedResults.add(result);
        }

        resultsWithData = dataAssigner.setPosition(fileData, results);
        for (int i = 0; i < resultsWithData.size(); i++) {
            Result result = resultsWithData.get(i);
            assignedResults.add(result);
        }
        return assignedResults;
    }

    private void setStandardPattern(List<String> fileData) {
        if (fileData.get(0).equals("KORTYZOL_5_MIN")) {
            logger.info("Pattern detected: " + fileData.get(0));
            countResultUtil.logDose(CORTISOL_PATTERN);
        }
    }

    private void setBindingPercent(List<Double> curve, List<Point> points, List<ControlCurve> controlCurveList) {
        setControlPointsFromControlCurve(controlCurveList, curve, points);
        countResultUtil.setControlCurveCpmWithFlag(points);
        countResultUtil.setStandardsCpmWithFlags(points);
        countResultUtil.bindingPercent();
    }

    /**
     * @param fileData         contains data which will be assign to Result
     * @param controlCurveList curve points
     * @param results          list of the Result entities with assigned data
     * @return list of Result entities with calculated mass of the hormone in nanograms
     */
    private List<Result> assignNgPerMl(List<String> fileData,
                                       List<ControlCurve> controlCurveList,
                                       List<Result> results) {

        List<Point> points = new ArrayList<>();
        List<Double> curve = new ArrayList<>();
        setStandardPattern(fileData);
        setBindingPercent(curve, points, controlCurveList);
        countResultUtil.logarithmRealZero();
        countResultUtil.countRegressionParameterB();
        countResultUtil.countRegressionParameterA();
        return getCountedResults(fileData, results);
    }

    private List<Result> getCountedResults(List<String> list, List<Result> results) {
        List<Result> countedResults = new ArrayList<>();
        for (int i = 25; i < list.size(); i++) {
            Result result = results.get(i);
            double point = result.getCpm();
            double counted = countResultUtil.countResult(point);
            if (Double.isNaN(counted)) {
                counted = 0.0;
            }
            result.setNg(counted);
            countedResults.add(result);
        }
        return countedResults;
    }

    private void setControlPointsFromControlCurve(List<ControlCurve> controlCurveLines, List<Double> curve, List<Point> points) {
        ControlCurve controlCurve;
        for (int i = 0; i < 24; i++) {
            controlCurve = controlCurveLines.get(i);
            Double pointValue = controlCurve.getCpm();
            boolean flag = controlCurve.isFlagged();
            curve.add(pointValue);
            Point point = new Point(pointValue, flag);
            points.add(point);
        }
    }

    public List<Result> setDataToResult(List<String> data, Backlog backlog, FileEntity fileEntity) {

        List<Result> resultsWithData = new ArrayList<>();
        List<Result> resultsWithNgCounted = new ArrayList<>();
        List<ControlCurve> controlCurveWithFileData;
        List<ControlCurve> controlCurveWithParameters;
        List<Result> results = setResultFromColumnsLength(data, backlog);

        if (data.size() > 24) {
            try {
                resultsWithData = assignDataToResult(data, fileEntity, results);
            } catch (Exception error) {
                logger.error("Assign Data To Result: " + error.getMessage() + " | " + error.getCause());
            }
        }
        controlCurveWithFileData = controlCurveService.setControlCurveFromFileData(data, backlog);
        controlCurveWithParameters = controlCurveService.setDataToControlCurve(data, fileEntity, controlCurveWithFileData);
        if (isStandardCpmAboveZero(controlCurveWithParameters)) {
            return getResultsWithData(resultsWithData, controlCurveWithFileData, controlCurveWithParameters);
        }
        //is they aren't ng will be set to results
        else {
            try {
                resultsWithNgCounted = assignNgPerMl(data, controlCurveWithParameters, resultsWithData);
            } catch (Exception error) {
                logger.error("Exception ng: " + error.getMessage() + " with cause: " + error.getCause());
            }
            controlCurveRepository.saveAll(controlCurveWithParameters);
            dataAssigner.setHormoneAverage(resultsWithNgCounted);
            return resultsWithNgCounted;
        }
    }

    private List<Result> getResultsWithData(List<Result> resultsWithData, List<ControlCurve> controlCurveWithFileData, List<ControlCurve> controlCurveWithParameters) {
        List<Point> points = new ArrayList<>();
        List<Double> curve = new ArrayList<>();
        setBindingPercent(curve, points, controlCurveWithFileData);
        countResultUtil.logarithmRealZero();
        controlCurveRepository.saveAll(controlCurveWithParameters);
        return resultsWithData;
    }

    /**
     * @param controlCurvePoints list of curve points
     * @return false if any controlCurve points is not flagged (is false),
     * and true if any from those are flagged
     */
    private boolean isStandardCpmAboveZero(List<ControlCurve> controlCurvePoints) {
        boolean flag = false;
        for (int i = 8; i < 21; i++) {
            ControlCurve controlCurve = controlCurvePoints.get(i);
            if (controlCurve.isFlagged()) flag = true;
        }
        return flag;
    }
}

