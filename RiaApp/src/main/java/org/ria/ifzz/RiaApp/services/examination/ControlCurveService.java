package org.ria.ifzz.RiaApp.services.examination;

import org.ria.ifzz.RiaApp.models.results.ControlCurve;
import org.ria.ifzz.RiaApp.repositories.results.ControlCurveRepository;
import org.ria.ifzz.RiaApp.utils.CustomFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.ria.ifzz.RiaApp.models.pattern.HormonesPattern.CORTISOL_PATTERN;
import static org.ria.ifzz.RiaApp.services.strategies.SpreadCounter.isSpread;
import static org.ria.ifzz.RiaApp.utils.CustomFileReader.getMatchingStrings;
import static org.ria.ifzz.RiaApp.utils.constants.ControlCurveConstants.*;
import static org.ria.ifzz.RiaApp.utils.constants.ExaminationConstants.CORTISOL_5MIN;

@Service
public class ControlCurveService implements CustomFileReader {

    private Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final ControlCurveRepository controlCurveRepository;

    public ControlCurveService(ControlCurveRepository controlCurveRepository) {
        this.controlCurveRepository = controlCurveRepository;
    }

    /**
     * generate ControlCurve entities and returns them with metadata set to the attributes
     *
     * @param metadata lines of data from uploaded file
     * @return controlCurve entities with all needed data from file metadata
     */
    public List<ControlCurve> create(List<String> metadata) {
        String filename = metadata.get(0);
        String pattern = metadata.get(1);
        metadata = metadata.stream().skip(2).collect(Collectors.toList());
        List<Integer> probeNumbers = setProbeNumber();
        List<String> positions = setPosition(pattern);
        List<Integer> CPMs = setCPMs(metadata);
        List<Boolean> flags = isFlagged(CPMs);
        List<ControlCurve> controlCurve = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            ControlCurve controlCurvePoint = new ControlCurve(filename, pattern, probeNumbers.get(i), positions.get(i), CPMs.get(i), flags.get(i));
            LOGGER.info(controlCurvePoint.toString());
            controlCurve.add(controlCurvePoint);
        }
        controlCurveRepository.saveAll(controlCurve);
        return controlCurve;
    }

    /**
     * takes List of String with data from uploaded file
     *
     * @param metadata data from uploaded file
     * @return List containing CPM values as Strings
     */
    private List<Integer> setCPMs(List<String> metadata) {
        List<Integer> CPMs = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            List<String> matchingStrings = getMatchingStrings(metadata, 3);
            String cpmString = matchingStrings.get(i);
            Integer cpmInteger = Integer.parseInt(cpmString);
            CPMs.add(cpmInteger);
        }
        return CPMs;
    }

    /**
     * set positions in accordance to standards and the identified hormone
     *
     * @return List containing position values as Strings
     */
    private List<String> setPosition(String pattern) {
        List<String> positions = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            if (i == 0 || i == 1) {
                positions.add(TOTAL);
            } else if (i == 2 || i == 3 || i == 4) {
                positions.add(NSB);
            } else if (i == 5 || i == 6 || i == 7) {
                positions.add(ZERO);
            } else if (i < 22) {
                getPattern(pattern, positions, i);
            } else {
                positions.add(CONTROL_POINT);
            }
        }
        return positions;
    }

    private void getPattern(String pattern, List<String> positions, int i) {
        if (pattern.equals(CORTISOL_5MIN)) {
            double point = CORTISOL_PATTERN[i - 8];
            String patternConvert = String.valueOf(point);
            positions.add(patternConvert);
        }
    }

    /**
     * @return probeNumbers generated by natural order
     */
    private List<Integer> setProbeNumber() {
        List<Integer> probeNumber = new ArrayList<>();
        for (int i = 1; i < 26 - 1; i++) {
            probeNumber.add(i);
        }
        return probeNumber;
    }

    private List<Boolean> isFlagged(List<Integer> CPMs) {
        List<Boolean> flagged = new ArrayList<>();
        List<Boolean> NSB = getZeroOrNsb(CPMs, 2, 4);
        List<Boolean> Zeros = getZeroOrNsb(CPMs, 5, 7);
        double nsb1 = CPMs.get(5), nsb2 = CPMs.get(6), nsb3 = CPMs.get(7);
        flagged.add(false);
        flagged.add(false);
        flagged.addAll(NSB);
        flagged.addAll(Zeros);
        for (int element = 8; element < CPMs.size(); element++) {
            if (CPMs.get(element) > nsb1 || nsb2 > CPMs.get(element) || CPMs.get(element) > nsb3) {
                flagged.add(true);
            } else {
                flagged.add(false);
            }
        }
        return flagged;
    }

    /**
     * {@code CPMs} is a list of double value, which elements will be check,
     * to define the difference. If difference between one element and another
     * is more than 10%, element return true, in another way return false;
     *
     * @param CPMs List of elements which element will be check
     * @param from first element which will be checking
     * @param to   last element which will be checking
     * @return list of boolean value for all checking elements
     */
    private List<Boolean> getZeroOrNsb(List<Integer> CPMs, int from, int to) {
        List<Integer> zeroOrNsbPoints = CPMs.stream().skip(from).limit(to).collect(Collectors.toList());
        return checkNSBsOrZeros(zeroOrNsbPoints);
    }

    private List<Boolean> checkNSBsOrZeros(List<Integer> CPMs) {
        return isSpread(CPMs);
    }

    public ResponseEntity<?> findAll() {
        List<ControlCurve> controlCurves = (List<ControlCurve>) controlCurveRepository.findAll();
        return new ResponseEntity<>(controlCurves, HttpStatus.FOUND);
    }
}

