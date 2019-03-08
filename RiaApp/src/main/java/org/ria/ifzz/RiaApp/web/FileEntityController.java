package org.ria.ifzz.RiaApp.web;

import org.ria.ifzz.RiaApp.domain.Backlog;
import org.ria.ifzz.RiaApp.domain.ControlCurve;
import org.ria.ifzz.RiaApp.domain.FileEntity;
import org.ria.ifzz.RiaApp.domain.Result;
import org.ria.ifzz.RiaApp.repository.*;
import org.ria.ifzz.RiaApp.service.ControlCurveService;
import org.ria.ifzz.RiaApp.service.GraphCurveService;
import org.ria.ifzz.RiaApp.service.ResultService;
import org.ria.ifzz.RiaApp.service.StorageService;
import org.ria.ifzz.RiaApp.utils.CountResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.constraints.NotNull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:3000")
public class FileEntityController {

    private final StorageService storageService;
    private final FileEntityRepository fileEntityRepository;
    private final ResultService resultService;
    private final BacklogRepository backlogRepository;
    private final ResultRepository resultRepository;
    private final ControlCurveService controlCurveService;
    private final ControlCurveRepository controlCurveRepository;
    private final GraphCurveService graphCurveService;

    @Autowired
    public FileEntityController(StorageService storageService,
                                FileEntityRepository fileEntityRepository,
                                ResultService resultService,
                                BacklogRepository backlogRepository,
                                ResultRepository resultRepository,
                                ControlCurveService controlCurveService,
                                ControlCurveRepository controlCurveRepository,
                                GraphCurveService graphCurveService) {

        this.storageService = storageService;
        this.fileEntityRepository = fileEntityRepository;
        this.resultService = resultService;
        this.backlogRepository = backlogRepository;
        this.resultRepository = resultRepository;
        this.controlCurveService = controlCurveService;
        this.controlCurveRepository = controlCurveRepository;
        this.graphCurveService = graphCurveService;
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> getFile() {
        long amountOfFiles = fileEntityRepository.count();
        Long randomPrimaryKey;

        if (amountOfFiles == 0) {
            return ResponseEntity.ok(new byte[0]);
        } else if (amountOfFiles == 1) {
            randomPrimaryKey = 1L;
        } else {
            randomPrimaryKey = ThreadLocalRandom.current().nextLong(1, amountOfFiles + 1);
        }

        FileEntity fileEntity = fileEntityRepository.findById(randomPrimaryKey).get();

        HttpHeaders header = new HttpHeaders();

        header.setContentType(MediaType.valueOf(fileEntity.getContentType()));
        header.setContentLength(fileEntity.getData().length);
        header.set("Content-Disposition", "attachment; filename=" + fileEntity.getFileName());
        return new ResponseEntity<>(fileEntity.getData(), header, HttpStatus.OK);
    }

    @GetMapping("/download/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);
        if (file.exists()) {
            HttpHeaders headers = new HttpHeaders();
            //instructing web browser how to treat downloaded file
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"");
            //allowing web browser to read additional headers from response
            headers.add("Access-Control-Expose-Headers", HttpHeaders.CONTENT_DISPOSITION + "," + HttpHeaders.CONTENT_LENGTH);

            //put headers and file within response body
            return ResponseEntity.ok().headers(headers).body(file);
        }
        //in case requested file does not exists
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{dataId}")
    public ResponseEntity<FileEntity> getFileEntityById(@PathVariable String dataId) throws FileNotFoundException {
        FileEntity fileEntity = storageService.getByDataId(dataId);
        return new ResponseEntity<>(fileEntity, HttpStatus.OK);
    }

    @GetMapping("/all")
    public Iterable<FileEntity> getAllFiles() {
        return storageService.loadAll();
    }

    /**
     * it is responsible for handle a file upload and generate database tables,
     * then assign values from file to appropriate variables.
     *
     * @param file               which will be handle
     * @param redirectAttributes message shown if upload goes well
     * @throws IOException
     */
    @PostMapping
    public ResponseEntity<?> handleFileUpload(@NotNull @RequestParam("file") MultipartFile file,
                                              RedirectAttributes redirectAttributes) throws IOException {

        // File Entity
        FileEntity fileEntity = new FileEntity(file.getOriginalFilename(), file.getContentType(),
                file.getBytes());

        storageService.store(file, redirectAttributes);

        fileEntityRepository.save(fileEntity);
        fileEntity.setDataId(fileEntity.getFileName() + "_" + fileEntity.getId());
        fileEntityRepository.save(fileEntity);

        // Backlog
        Backlog backlog = new Backlog();
        backlog.setFileEntity(fileEntity);
        backlog.setFileName(fileEntity.getFileName());
        backlog.setDataId(fileEntity.getDataId());
        backlog.setContentType(fileEntity.getContentType());

        backlogRepository.save(backlog);

        fileEntity.setBacklog(backlog);
        fileEntityRepository.save(fileEntity);

        List<String> cleanedList = resultService.getFileData(file);

        // Result
        resultService.setResultFromColumnsLength(cleanedList, file, backlog);
        Result result = resultService.assignDataToResult(cleanedList, file, fileEntity);
        resultRepository.save(result);

        // Control Curve
        controlCurveService.setControlCurveFromColumnsLength(cleanedList, file, backlog);
        ControlCurve controlCurve = controlCurveService.setDataToControlCurve(cleanedList, file, fileEntity);
        controlCurveRepository.save(controlCurve);

        result = resultService.assignNgPerMl(file, cleanedList);
        resultRepository.save(result);
        graphCurveService.setGraphCurveFileName(file, fileEntity);

        return new ResponseEntity<>(fileEntity, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id) {
        storageService.delete(id);
        return new ResponseEntity<>("File with ID: " + id + "was deleted", HttpStatus.OK);
    }

}



