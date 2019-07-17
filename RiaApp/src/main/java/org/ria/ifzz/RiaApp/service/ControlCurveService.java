package org.ria.ifzz.RiaApp.service;

import org.ria.ifzz.RiaApp.domain.Backlog;
import org.ria.ifzz.RiaApp.domain.ControlCurve;
import org.ria.ifzz.RiaApp.domain.FileEntity;
import org.ria.ifzz.RiaApp.exception.FileEntityNotFoundException;
import org.ria.ifzz.RiaApp.repository.ControlCurveRepository;
import org.ria.ifzz.RiaApp.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static org.ria.ifzz.RiaApp.domain.HormonesPattern.CORTISOL_PATTERN;
import static org.ria.ifzz.RiaApp.utils.CustomFileReader.getMatchingStrings;

@Service
public class ControlCurveService implements FileUtils {

    private final ControlCurveRepository controlCurveRepository;
    private final FileEntityService fileEntityService;

    public ControlCurveService(ControlCurveRepository controlCurveRepository, FileEntityService fileEntityService) {
        this.controlCurveRepository = controlCurveRepository;
        this.fileEntityService = fileEntityService;
    }

    private Logger logger = LoggerFactory.getLogger(getClass());

    public List<ControlCurve> setControlCurveFromFileData(List<String> fileData, Backlog backlog) {
        List<ControlCurve> controlCurveList = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            String line = fileData.get(i);
            if (line.startsWith(" \tUnk")) {
                ControlCurve controlCurvePoint = new ControlCurve();
                controlCurvePoint.setFileName(i + "_" + backlog.getDataId());
                controlCurvePoint.setBacklog(backlog);
                controlCurveList.add(controlCurvePoint);
            }
        }
        return controlCurveList;
    }

    /**
     * assigns data from list to ControlCurve object
     *
     * @param fileData contains data
     * @return list of all control curve points
     */
    List<ControlCurve> setDataToControlCurve(List<String> fileData, FileEntity fileEntity, List<ControlCurve> curveList) {

        List<ControlCurve> controlCurveList = new ArrayList<>();
        String fileId = fileEntity.getDataId();
        ControlCurve controlCurve;

        //Assign CPM to Result
        for (int i = 0; i < 24; i++) {
            List<String> CPMs = getMatchingStrings(fileData, 3);

            controlCurve = curveList.get(i);

            // convert String value of CPM to Integer
            String cpmString = CPMs.get(i).toString();
            Double cpmInteger = Double.parseDouble(cpmString);
            controlCurve.setCpm(cpmInteger);
            logger.info("Control Curve CPM value: " + controlCurve.getCpm());
            controlCurveList.add(controlCurve);
        }

        //Check if NSBs or Zeros have too large spread and flag those which are
        if (!controlCurveList.isEmpty()) {
            isNSBsZEROsSpreadTooLarge(2, 3, 4, controlCurveList, 10);
            isNSBsZEROsSpreadTooLarge(5, 6, 7, controlCurveList, 10);
            isPatternPointsSpreadTooLarge(controlCurveList);
        }

        //Assign position to Result
        for (int i = 0; i < 24; i++) {
            List position = getMatchingStrings(fileData, 2);

            controlCurve = curveList.get(i);

            if (i == 0 || i == 1) {
                String preConvertedPosition = position.get(i).toString();
                String converted = preConvertedPosition.replaceAll("A", "Total");
                String postConvert = converted.replaceAll("[0-9]", "");
                controlCurve.setPosition(postConvert);
                controlCurveList.add(controlCurve);

            } else if (i == 2 || i == 3 || i == 4) {
                String preConvertedPosition = position.get(i).toString();
                String converted = preConvertedPosition.replaceAll("A", "NSB");
                String postConvert = converted.replaceAll("[0-9]", "");
                controlCurve.setPosition(postConvert);
                controlCurveList.add(controlCurve);

            } else if (i == 5 || i == 6 || i == 7) {
                String preConvertedPosition = position.get(i).toString();
                String converted = preConvertedPosition.replaceAll("[A-Z]", "O");
                String postConvert = converted.replaceAll("[0-9]", "");
                controlCurve.setPosition(postConvert);
                controlCurveList.add(controlCurve);

            } else if (i < 22) {
                String preConvertedPosition = position.get(i).toString();
                double point = CORTISOL_PATTERN[i - 8];
                String convert = preConvertedPosition.replaceAll("[0-9]", "");
                String postConvert = convert.replaceAll("[A-Z]", String.valueOf(point));
                controlCurve.setPosition(postConvert);
                controlCurveList.add(controlCurve);
            } else if (i == 22 || i == 23) {
                String preConvertedPosition = position.get(i).toString();
                String converted = preConvertedPosition.replaceAll("[A-Z]", "K");
                String postConvert = converted.replaceAll("[0-9]", "");
                controlCurve.setPosition(postConvert);
                controlCurveList.add(controlCurve);
            }
        }

        //Assign samples to Result
        for (int i = 0; i < 25 - 1; i++) {
            controlCurve = curveList.get(i);
            controlCurve.setDataId(fileId);
            controlCurve.setSamples(i);
            controlCurveList.add(controlCurve);
        }
        return controlCurveList;
    }


    /**
     * takes 3 of NSBs or Zeros curve points and performs setNSBsZerosFlag method on them
     *
     * @param first            curve point
     * @param second           curve point
     * @param third            curve point
     * @param controlCurveList list of flagged curve point
     * @param percentage       not accepted percentage difference between the points
     */
    private void isNSBsZEROsSpreadTooLarge(int first, int second, int third, List<ControlCurve> controlCurveList, int percentage) {
        ControlCurve controlCurve1 = controlCurveList.get(first);
        ControlCurve controlCurve2 = controlCurveList.get(second);
        ControlCurve controlCurve3 = controlCurveList.get(third);
        setNSBsZerosFlag(controlCurve1, controlCurve2, controlCurve3, percentage);
    }

    private void isPatternPointsSpreadTooLarge(List<ControlCurve> controlCurveList) {
        setPatternFlag(controlCurveList);
    }

    /**
     * takes list elements and define if any of them has CPM value above any of NSBs CPM
     *
     * @param controlCurveList list of Control Curve points
     */
    private void setPatternFlag(List<ControlCurve> controlCurveList) {
        double nsb1 = controlCurveList.get(5).getCpm();
        double nsb2 = controlCurveList.get(6).getCpm();
        double nsb3 = controlCurveList.get(7).getCpm();
        for (int i = 8; i < controlCurveList.size(); i++) {
            ControlCurve point = controlCurveList.get(i);
            double pointCpm = point.getCpm();
            if (pointCpm > nsb1 || pointCpm > nsb2 || pointCpm > nsb3) point.setFlagged(true);
        }
    }

    /**
     * takes absolute values of 3 of NSBs or Zeros curve points and checks if one of them is greater than acceptable percent value,
     * if it is flagged with value "true"
     * <p>
     *
     * @param first   curve point
     * @param second  curve point
     * @param third   curve point
     * @param percent not accepted percentage difference between the points
     */
    private void setNSBsZerosFlag(ControlCurve first, ControlCurve second, ControlCurve third, int percent) {
        double a = first.getCpm();
        double b = second.getCpm();
        double c = third.getCpm();

        if (a - b != 0 || b - c != 0 || c - a != 0) {
            if ((a - b) > (b / percent) || (a - c) > (c / percent)) {
                first.setFlagged(true);
                logger.warn(first.getCpm() + " flagged " + first.isFlagged());
            } else if ((b - a) > (a / percent) || (b - c) > (c / percent)) {
                second.setFlagged(true);
                logger.warn(second.getCpm() + " flagged " + second.isFlagged());
            } else if ((c - b) > (b / percent) || (c - a) > (a / percent)) {
                third.setFlagged(true);
                logger.warn(third.getCpm() + " flagged " + third.isFlagged());
            }
        }
    }

    public Iterable<ControlCurve> findCCBacklogByDataId(String dataId) throws FileNotFoundException {
        fileEntityService.findFileEntityByDataId(dataId);
        return controlCurveRepository.findByDataIdOrderByFileName(dataId);
    }

    public ControlCurve findResultByDataId(String dataId, String fileName) throws FileNotFoundException, FileEntityNotFoundException {
        fileEntityService.findFileEntityByDataId(dataId);
        ControlCurve controlCurve = controlCurveRepository.findByFileName(fileName);
        if (controlCurve == null) {
            throw new FileEntityNotFoundException("File with ID: '" + fileName + "' not found");
        }
        if (!controlCurve.getDataId().equals(dataId)) {
            throw new FileEntityNotFoundException("Curve '" + fileName + "' does not exist: '" + dataId);
        }
        return controlCurve;
    }
}
