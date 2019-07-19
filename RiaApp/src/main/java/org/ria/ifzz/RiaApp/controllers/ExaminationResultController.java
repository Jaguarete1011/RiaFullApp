package org.ria.ifzz.RiaApp.controllers;

import org.ria.ifzz.RiaApp.exception.ControlCurveException;
import org.ria.ifzz.RiaApp.models.DataFileMetadata;
import org.ria.ifzz.RiaApp.services.examination.ExaminationPointService;
import org.ria.ifzz.RiaApp.services.strategies.ExaminationResultSolution;
import org.ria.ifzz.RiaApp.utils.CustomFileReader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.ria.ifzz.RiaApp.utils.CustomFileReader.readFromStream;

@RestController
@RequestMapping("/api/examination/")
@CrossOrigin(origins = "http://localhost:3000")
public class ExaminationResultController implements CustomFileReader {

    private final ExaminationResultSolution solution;
    private final ExaminationPointService service;
    private Map<String,String> response = new HashMap<>();

    public ExaminationResultController(ExaminationResultSolution solution, ExaminationPointService service) {
        this.solution = solution;
        this.service = service;
    }

    @PostMapping(value = "/")
    public ResponseEntity<?> handleFileUpload(@Valid DataFileMetadata metadata, Principal principal) throws IOException {

        List<String> examinationContent = readFromStream(metadata);

        try {
            solution.setMetadata(examinationContent);
            solution.create();
            response.put("message", "Upload successful");
            return ResponseEntity.ok().body(response);
        } catch (ControlCurveException curveError) {
            return new ResponseEntity<>( curveError.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/")
    public ResponseEntity<?> getExaminationPoints() {
        return service.getExaminationResults();
    }

    @GetMapping("/{filename}")
    public ResponseEntity<?> getExaminationResultsByFilename(@PathVariable String filename) {
        return service.getExaminationResultsByFilename(filename);
    }
}
